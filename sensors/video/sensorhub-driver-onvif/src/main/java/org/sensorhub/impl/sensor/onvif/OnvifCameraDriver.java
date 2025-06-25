/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.onvif;


import java.net.*;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

//import javax.xml.soap.SOAPException;
import de.onvif.discovery.OnvifDiscovery;
import jakarta.xml.ws.http.HTTPException;
import net.opengis.gml.v32.TimePosition;
import net.opengis.gml.v32.impl.TimeInstantImpl;
import net.opengis.swe.v20.Text;
//import org.bytedeco.ffmpeg.avutil.AVDictionary;
//import org.bytedeco.ffmpeg.global.avutil;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.onvif.ver10.schema.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.ffmpeg.outputs.AudioOutput;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.sensorhub.mpegts.MpegTsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.onvif.soap.OnvifDevice;

/**
 * <p>
 * Implementation of sensor interface for generic Cameras using IP
 * protocol
 * </p>
 * 
 * @author Kyle Fitzpatrick
 * @since April 1, 2025
 */
public class OnvifCameraDriver extends AbstractSensorModule<OnvifCameraConfig>
{
    private static final Logger log = LoggerFactory.getLogger(OnvifCameraDriver.class);

    VideoOutput<OnvifCameraDriver> videoOutput;
    AudioOutput<OnvifCameraDriver> audioOutput;
    MpegTsProcessor mpegTsProcessor;
    protected ScheduledExecutorService executor;
    OnvifPtzOutput ptzPosOutput;
    OnvifPtzControl ptzControlInterface;

    OnvifDevice camera;
    Profile ptzProfile;
    Profile streamingProfile;

    String serialNumber;
    String modelNumber;
    String shortName;
    String longName;
    URI streamURI;
    String visualConnectionString;
    String streamTransport = "tcp";

    OnvifCameraConfig config;

    public OnvifCameraDriver() throws SensorHubException {
        super();
    }

    /**
     * When updating the config, also update the discovered WS Onvif devices
     * @param config
     */
    @Override
    public void setConfiguration(OnvifCameraConfig config) {
        super.setConfiguration(config);
        this.config = config;

        TreeSet<URL> temp = (TreeSet<URL>) OnvifDiscovery.discoverOnvifURLs();
        config.networkConfig.autoRemoteHost.clear();
        for (URL url : temp)
            config.networkConfig.autoRemoteHost.add(url.toString());
    }

    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("ONVIF-Compatible Camera");
        }
    }

    @Override
    public boolean isConnected() {
        return camera != null;
    }

    // This method override may not be necessary
    @Override
    protected void afterInit() {
        updateSensorDescription();
    }

    /**
     * ONVIF driver setup.
     * Creates camera connection, discovers ptz and AV streaming profiles, and creates rtsp streaming connection.
     * @throws SensorHubException
     */
    @Override
    protected void doInit() throws SensorHubException {
        streamingProfile = null;
        ptzProfile = null;
        ptzControlInterface = null;
        ptzPosOutput = null;
        videoOutput = null;
        audioOutput = null;

        String user = config.networkConfig.user;
        String password = config.networkConfig.password;

        // Attempt camera connection
        String resolvePort = (config.networkConfig.remotePort == 0) ? "" : ":" + config.networkConfig.remotePort;
        String resolvePath = (config.networkConfig.onvifPath == null) ? "" : config.networkConfig.onvifPath;

        try {
            camera = new OnvifDevice(config.networkConfig.remoteHost + resolvePort,
                    user, password, resolvePath);

        }  catch (Exception e) {
            // If this particular exception is thrown, sometimes a non-issue that is resolved by reconnecting.
            try {
                camera = new OnvifDevice(config.networkConfig.remoteHost + resolvePort,
                        user, password, resolvePath);
            } catch (Exception ex) {
                if (ex.getCause().getClass() == java.net.UnknownHostException.class)
                    throw new SensorHubException("Unknown ONVIF host " + config.networkConfig.remoteHost);
                else if (ex.getCause().getClass() == java.net.ConnectException.class) {
                    String message = "";
                    if (ex.getCause().getMessage().contains("getsockopt")) {
                        message += "Check ONVIF port, ";
                    }
                    message += "Socket connection refused";
                    throw new SensorHubException(message);

                } else if (ex.getCause().getClass() == org.apache.cxf.transport.http.HTTPException.class && ex.getCause().getMessage().contains("Unauthorized")) {
                    throw new SensorHubException("Invalid ONVIF username/password");

                } else {
                    throw new SensorHubException(ex.getCause().getMessage());
                }
            }
        }

        // ONVIF profiles
        List<Profile> profiles = camera.getMedia().getProfiles();
        if (profiles == null || profiles.isEmpty()) {
            throw new SensorHubException("Camera does not have any profiles to use");
        }

        // Iterate through profiles, select those with ptz or streaming
        if (config.streamingConfig.autoStreamEndpoint != null)
            config.streamingConfig.autoStreamEndpoint.clear();

        Profile tempMedia = null;
        VideoEncoding codec = config.streamingConfig.codec;
        for (Profile p: profiles) {
            // Select a profile that supports ptz
            if(ptzProfile == null && p.getPTZConfiguration() != null){
                ptzProfile = p;
            }

            // Select a profile that supports video
            if (p.getVideoEncoderConfiguration() != null) {
                config.streamingConfig.autoStreamEndpoint.add(camera.getStreamUri(p.getToken()));

                // Search for profile with preferred codec if endpoint is not specified
                if (streamingProfile == null && (config.streamingConfig.streamEndpoint == null || config.streamingConfig.streamEndpoint.isBlank())) {
                    tempMedia = p;
                    //VideoEncoderConfigurationOptions encoderConfig = camera.getMedia().getVideoEncoderConfigurationOptions(null, p.getToken());
                    // If preferred codec is supported, select it
                    if (codec == p.getVideoEncoderConfiguration().getEncoding()){
                        streamingProfile = p;
                        //streamingProfile.getVideoEncoderConfiguration().setEncoding(codec);
                        config.streamingConfig.streamEndpoint = camera.getStreamUri(streamingProfile.getToken());
                        log.debug("Successfully switched to {}: {}", codec, streamingProfile.getVideoEncoderConfiguration().getEncoding().toString());
                    }

                // If a streaming endpoint is specified and matches the current profile stream URI, select it and set codec
                } else if (streamingProfile == null && config.streamingConfig.streamEndpoint != null && config.streamingConfig.streamEndpoint.equals(camera.getStreamUri(p.getToken()))) {
                    streamingProfile = p;
                    //streamingProfile.getVideoEncoderConfiguration().setEncoding(codec);
                }
            }
        }

        // If endpoint or preferred codec not found, fall back to other discovered video profile
        if (streamingProfile == null && tempMedia != null) {
            streamingProfile = tempMedia;
            config.streamingConfig.streamEndpoint = camera.getStreamUri(streamingProfile.getToken());
        }

        if (ptzProfile == null) {
            log.trace("Camera has no profiles capable of PTZ");
        }
        if (streamingProfile == null) {
            log.trace("Camera has no profiles capable of video streaming");
        }

        // Determine streaming transport protocol
        if (camera.getMedia().getServiceCapabilities().getStreamingCapabilities().getRTPTCP()) {
            streamTransport = "tcp";
            log.trace("rtsp streaming protocol: tcp");
        } else {
            streamTransport = "udp";
            log.trace("rtsp streaming protocol: udp");
        }

        // Create this driver's ID
        serialNumber = camera.getDeviceInfo().getSerialNumber().trim();
        modelNumber = camera.getDeviceInfo().getModel().trim();
        shortName = camera.getDeviceInfo().getManufacturer().trim();
        longName = shortName + "_" + modelNumber + "_" + serialNumber;

        // generate identifiers.
        streamURI = URI.create(config.streamingConfig.streamEndpoint);
        String streamId = streamingProfile.getName() + ":" + streamingProfile.getVideoEncoderConfiguration().getEncoding();

        generateUniqueID("urn:onvif:cam:", serialNumber + (streamId != null ? (":" + streamId) : ""));
        generateXmlID("ONVIF_CAM_", serialNumber + (streamId != null ? (":" + streamId) : ""));

        // Add inputs/outputs for supported features

        // PTZ output, for displaying current ptz orientation
        if (ptzProfile != null) {
            // add PTZ output
            if (ptzPosOutput == null) {
                ptzPosOutput = new OnvifPtzOutput(this);
                ptzPosOutput.init();
                addOutput(ptzPosOutput, false);
            } else ptzPosOutput.init();

            // add PTZ controller
            if (ptzControlInterface == null) {
                ptzControlInterface = new OnvifPtzControl(this);
                ptzControlInterface.init();
                addControlInput(ptzControlInterface);
            } else ptzControlInterface.init();


        }
        // Streaming profile, for AV data stream output
        if (streamingProfile != null) {
            if (mpegTsProcessor != null) {
                mpegTsProcessor.closeStream();
            }
            mpegTsProcessor = null;
            videoOutput = null;
            audioOutput = null;

            if (user == null || user.isBlank())
                user = "";
            if (password == null || password.isBlank())
                password = "";

            // Add credentials if provided and not already included in uri
            if ((!user.isBlank() || !password.isBlank()) && !streamURI.getAuthority().contains("@")) {
                visualConnectionString = new StringBuilder(streamURI.toString())
                        .insert(streamURI.toString().indexOf("://") + 3, user + ":" + password + "@").toString();
            } else {
                visualConnectionString = streamURI.toString();
            }
            log.trace("Stream endpoint: {}", visualConnectionString);
            setupStream();
        }
    }

    /**
     * Start the driver using camera connection established in the init method.
     * @throws SensorHubException
     */
    @Override
    public void doStart() throws SensorHubException {
        // Prevent start when a camera is not connected
        if (config.networkConfig.remoteHost == null || config.networkConfig.remoteHost.isBlank()) {
            throw new SensorHubException("Connection not established. Connect to a camera before starting.");
        }

        if (ptzPosOutput != null)
            ptzPosOutput.start();

        if (ptzControlInterface != null)
            ptzControlInterface.start();

        if (streamingProfile != null && mpegTsProcessor != null && (videoOutput != null || audioOutput != null)) {
            setupStream();
            startStream();
        }
    }

    @Override
    public void doStop() throws SensorHubException {
        if(ptzPosOutput != null) {
            ptzPosOutput.stop();
            //ptzPosOutput = null;
        }
        if (ptzControlInterface != null) {
            ptzControlInterface.stop();
            //ptzControlInterface = null;
        }
        if (mpegTsProcessor != null) {
            stopStream();
            shutdownExecutor();
        }
    }

    protected void setupStream() throws SensorHubException {
        setupExecutor();
        openStreamVisual();
    }

    protected void setupExecutor() {
        if (executor == null)
            executor = Executors.newSingleThreadScheduledExecutor();
        if (videoOutput != null)
            videoOutput.setExecutor(executor);
        if (audioOutput != null)
            audioOutput.setExecutor(executor);
    }

    protected void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /**
     * Open rtsp stream from the ONVIF camera.
     * Code borrowed from the FFMPEG driver.
     * @throws SensorHubException
     */
    protected void openStreamVisual() throws SensorHubException{
    if (mpegTsProcessor == null) {
        mpegTsProcessor = new MpegTsProcessor(visualConnectionString);
        // Remove the following line if causing an error. Part of a separate ffmpeg mod.
        //mpegTsProcessor.setAvOption("transport", streamTransport);

        // Initialize the MPEG transport stream processor from the source named in the configuration.
        if (mpegTsProcessor.openStream()) {
            // If there is a video content in the stream
            if (mpegTsProcessor.hasVideoStream()) {
                // In case we were waiting until we got video data to make the video frame output,
                // we go ahead and do that now.
                if (videoOutput == null) {
                    videoOutput = new VideoOutput<>(this, mpegTsProcessor.getVideoStreamFrameDimensions(), mpegTsProcessor.getVideoCodecName(), "visual", "Visual Camera", "Visual stream using ffmpeg library");
                    if (executor != null) {
                        videoOutput.setExecutor(executor);
                    }
                    videoOutput.doInit();
                    addOutput(videoOutput, false);

                }
                // Set video stream packet listener to video output
                mpegTsProcessor.setVideoDataBufferListener(videoOutput);
            }

            // If there is an audio content in the stream
            if (mpegTsProcessor.hasAudioStream()) {
                // In case we were waiting until we got audio data to make the audio output,
                // we go ahead and do that now.
                if (audioOutput == null) {
                    audioOutput = new AudioOutput<>(this, mpegTsProcessor.getAudioSampleRate(), mpegTsProcessor.getAudioCodecName());
                    if (executor != null) {
                        audioOutput.setExecutor(executor);
                    }
                    audioOutput.doInit();
                    addOutput(audioOutput, false);
                }
                // Set audio stream packet listener to audio output
                mpegTsProcessor.setAudioDataBufferListener(audioOutput);
            }
        } else {
            throw new SensorHubException("Unable to open stream from data source");
        }
    }
}

    protected void startStream() throws SensorHubException {
        try {
            if (mpegTsProcessor != null) {
                mpegTsProcessor.processStream();
                mpegTsProcessor.setReconnect(true);
            }
        } catch (IllegalStateException e) {
            String message = "Failed to start stream processor";
            log.error(message);
            throw new SensorHubException(message, e);
        }
    }

    protected void stopStream() throws SensorHubException {

        log.info("Stopping MPEG TS processor for {}", getUniqueIdentifier());

        if (mpegTsProcessor != null) {
            mpegTsProcessor.stopProcessingStream();

            try {
                // Wait for thread to finish
                log.info("Waiting for stream processor to stop");
                mpegTsProcessor.join(1000);
            } catch (InterruptedException e) {
                log.error("Interrupted waiting for stream processor to stop", e);
                Thread.currentThread().interrupt();
                throw new SensorHubException("Interrupted waiting for stream processor to stop", e);
            } finally {
                // Close stream and cleanup resources
                mpegTsProcessor.closeStream();
                mpegTsProcessor = null;
            }
        }
    }
}
