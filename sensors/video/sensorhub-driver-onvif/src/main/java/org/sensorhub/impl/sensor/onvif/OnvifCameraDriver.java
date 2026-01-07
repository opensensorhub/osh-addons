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


import java.lang.Object;
import java.net.*;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import de.onvif.discovery.OnvifDiscovery;
import jakarta.xml.ws.BindingProvider;
import org.onvif.ver10.media.wsdl.Media;
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

    float panMax = 2f;
    float panMin = -1f;
    float tiltMax = 2f;
    float tiltMin = -1f;
    float zoomMax = 2f;
    float zoomMin = -1f;

    // Type: 0 = pan, 1 = tilt, 2 zoom
    public float degtoGeneric(float in, int type) {
        float min;
        float max;
        float largest;
        float out;
        switch (type) {
            case 0: // Type 0 Panning
                if (config.ptzRanges.panMax == null || config.ptzRanges.panMin == null)
                    return in;
                max = panMax;
                min = panMin;
                break;
            case 1: // Type 1 Tilting
                if (config.ptzRanges.tiltMax == null || config.ptzRanges.tiltMin == null)
                    return in;
                max = tiltMax;
                min = tiltMin;
                break;
            case 2: // Type 2 Zooming
                if (config.ptzRanges.zoomMax == null || config.ptzRanges.zoomMin == null)
                    return in;
                max = zoomMax;
                min = zoomMin;
                break;
            default:
                return in;
        }
        float range = (max - min);
        //out = ((in + 1) / 2) * range - min;
        out = (((in - min) / range * 2) - 1);
        return out;
        //return ((in - min) / range * 2f) - 1;
    }

    public float genericToDeg(float in, int type) {
        float min;
        float max;
        float largest;
        float out;
        switch (type) {
            case 0: // Type 0 Panning
                if (config.ptzRanges.panMax == null || config.ptzRanges.panMin == null)
                    return in;
                max = panMax;
                min = panMin;
                break;
            case 1: // Type 1 Tilting
                if (config.ptzRanges.tiltMax == null || config.ptzRanges.tiltMin == null)
                    return in;
                max = tiltMax;
                min = tiltMin;
                break;
            case 2: // Type 2 Zooming
                if (config.ptzRanges.zoomMax == null || config.ptzRanges.zoomMin == null)
                    return in;
                max = zoomMax;
                min = zoomMin;
                break;
            default:
                return in;
        }
        float range = (max - min);
        out = (((in + 1) / 2) * range + min);

        return out;
        //return ((in + 1) / 2f * range) + min;
    }

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

        if (config.ptzRanges.panMax != null && config.ptzRanges.panMin != null) {
            panMax = config.ptzRanges.panMax;
            panMin = config.ptzRanges.panMin;
        } else {
            panMax = 2f;
            panMin = -1f;
        }
        if (config.ptzRanges.tiltMax != null && config.ptzRanges.tiltMin != null) {
            tiltMax = config.ptzRanges.tiltMax;
            tiltMin = config.ptzRanges.tiltMin;
        } else {
            tiltMax = 2f;
            tiltMin = -1f;
        }
        if (config.ptzRanges.zoomMax != null && config.ptzRanges.zoomMin != null) {
            zoomMax = config.ptzRanges.zoomMax;
            zoomMin = config.ptzRanges.zoomMin;
        } else {
            zoomMax = 2f;
            zoomMin = -1f;
        }
        if (config.ptzRanges.invertPan) {
            float temp = panMax;
            panMax = panMin;
            panMin = temp;
        }
        if (config.ptzRanges.invertTilt) {
            float temp = tiltMax;
            tiltMax = tiltMin;
            tiltMin = temp;
        }

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

        rewriteServiceEndpoint(camera.getMedia());
        rewriteServiceEndpoint(camera.getPtz());
        rewriteServiceEndpoint(camera.getImaging());
        rewriteServiceEndpoint(camera.getDevice());
        Media mediaService = camera.getMedia();
        if (mediaService != null) {
            BindingProvider bp = (BindingProvider) mediaService;
            String currentUrl = (String) bp.getRequestContext()
                    .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);

            // Replace internal IP with the configured remote host
            if (currentUrl != null) {
                String fixedUrl = rewriteToPublicHost(currentUrl,
                        config.networkConfig.remoteHost, resolvePort);
                bp.getRequestContext()
                        .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, fixedUrl);
            }
        }

        // ONVIF profiles
        List<Profile> profiles = mediaService.getProfiles();
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

                String publicStreamEndpoint;
                try {
                    publicStreamEndpoint = getPublicStreamEndpoint(p);
                } catch (URISyntaxException e) {
                    logger.warn("Could not get public stream endpoint for profile {}", p.getName());
                    continue;
                }
                config.streamingConfig.autoStreamEndpoint.add(publicStreamEndpoint);

                // Search for profile with preferred codec if endpoint is not specified
                if (streamingProfile == null && (config.streamingConfig.streamEndpoint == null || config.streamingConfig.streamEndpoint.isBlank())) {
                    tempMedia = p;
                    //VideoEncoderConfigurationOptions encoderConfig = camera.getMedia().getVideoEncoderConfigurationOptions(null, p.getToken());
                    // If preferred codec is supported, select it
                    if (codec == p.getVideoEncoderConfiguration().getEncoding()){
                        streamingProfile = p;
                        //streamingProfile.getVideoEncoderConfiguration().setEncoding(codec);
                        config.streamingConfig.streamEndpoint = publicStreamEndpoint;
                        log.debug("Successfully switched to {}: {}", codec, streamingProfile.getVideoEncoderConfiguration().getEncoding().toString());
                    }

                    // If a streaming endpoint is specified and matches the current profile stream URI, select it and set codec
                } else if (streamingProfile == null && config.streamingConfig.streamEndpoint != null && config.streamingConfig.streamEndpoint.equals(publicStreamEndpoint)) {
                    streamingProfile = p;
                    //streamingProfile.getVideoEncoderConfiguration().setEncoding(codec);
                }
            }
        }

        // If endpoint or preferred codec not found, fall back to other discovered video profile
        if (streamingProfile == null && tempMedia != null) {
            streamingProfile = tempMedia;
            try {
                config.streamingConfig.streamEndpoint = getPublicStreamEndpoint(streamingProfile);
            } catch (URISyntaxException e) {
                logger.warn("Could not get public stream endpoint for profile {}", streamingProfile.getName());
            }
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
        String streamId;
        try {
            streamId = (streamingProfile.getName()) + ":" + streamingProfile.getVideoEncoderConfiguration().getEncoding();
        } catch (NullPointerException e) {
            streamId = "noVideo";
        }

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

    private String getPublicStreamEndpoint(Profile p) throws URISyntaxException {
        String localStreamEndpoint = camera.getStreamUri(p.getToken());
        String port = "" + (new URI(localStreamEndpoint)).getPort();
        return rewriteToPublicHost(localStreamEndpoint, config.networkConfig.remoteHost, port);
    }

    private void rewriteServiceEndpoint(Object servicePort) {
        if (servicePort == null) return;

        try {
            BindingProvider bp = (BindingProvider) servicePort;
            String currentUrl = (String) bp.getRequestContext()
                    .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);

            if (currentUrl != null) {
                String resolvePort = (config.networkConfig.remotePort == 0) ? "" : ":" + config.networkConfig.remotePort;
                String fixedUrl = rewriteToPublicHost(currentUrl,
                        config.networkConfig.remoteHost, resolvePort);
                bp.getRequestContext()
                        .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, fixedUrl);
                log.debug("Rewrote service endpoint: {} -> {}", currentUrl, fixedUrl);
            }
        } catch (ClassCastException e) {
            log.warn("Could not rewrite endpoint for service: {}", servicePort.getClass().getName());
        }
    }

    private String rewriteToPublicHost(String endpoint, String publicHost, String port) {
        try {
            URI uri = new URI(endpoint);
            String newHost = publicHost.replace("http://", "").replace("https://", "");
            int newPort = port.isEmpty() ? uri.getPort() : Integer.parseInt(port.replace(":", ""));
            return new URI(uri.getScheme(), null, newHost, newPort,
                    uri.getPath(), uri.getQuery(), null).toString();
        } catch (Exception e) {
            return endpoint;
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