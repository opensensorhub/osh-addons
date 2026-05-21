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

package org.sensorhub.impl.sensor.axis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.comm.RobustHTTPConnection;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.security.ClientAuth;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.rtpcam.RTSPClient;
import org.sensorhub.api.common.SensorHubException;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol
 * </p>
 * 
 * @author Mike Botts
 * @since October 30, 2014
 */
public class AxisCameraDriver extends AbstractSensorModule < AxisCameraConfig > {

    public static final String DEFAULT_RTSP_VIDEO_PATH = "/axis-media/media.amp?videocodec=h264";

    public final int CAMERA_ID = 1; //assume one camera at index 1 (some Axis camera can support multiple Image Sources)
    public final String VAPIX_QUERY_PTZ_PATH = "/com/ptz.cgi?query=position";
    public final String VAPIX_QUERY_PARAMS_LIST = "/operator/param.cgi?action=list";
    public final String VAPIX_QUERY_PARAMS_LIST_GROUP_PTZ = VAPIX_QUERY_PARAMS_LIST + "&group=PTZ.Limit";
    public final String VAPIX_QUERY_IMAGE_SIZE = "/view/imagesize.cgi?camera=" + CAMERA_ID;
    public final String VAPIX_API_BASE_URL = "/axis-cgi";

    RobustConnection connection;
    AxisVideoStream mjpegVideoStream;
    AxisVideoStream h264VideoStream;
    AxisVideoStream h265VideoStream;
    AxisPtzOutput ptzPosOutput;
    AxisVideoControl videoControlInterface;
    AxisPtzControl ptzControlInterface;

    String hostUrl;
    String serialNumber;
    String modelNumber;
    String longName;
    String shortName;

    int vapixVersion = 0;

    boolean ptzSupported = false;
    boolean mjpegSupported = false;
    boolean h264Supported = false;
    boolean h265Supported = false;
    boolean mpeg4Supported = false;

    public AxisCameraDriver() {

    }


    @Override
    public void setConfiguration(final AxisCameraConfig config) {
        super.setConfiguration(config);

        // use same config for HTTP and RTSP by default
        if (config.rtsp.localAddress == null)
            config.rtsp.localAddress = config.http.localAddress;
        if (config.rtsp.remoteHost == null)
            config.rtsp.remoteHost = config.http.remoteHost;
        if (config.rtsp.user == null)
            config.rtsp.user = config.http.user;
        if (config.rtsp.password == null)
            config.rtsp.password = config.http.password;

        // compute full host URL
        hostUrl = "http://" + config.http.remoteHost + ":" + config.http.remotePort + VAPIX_API_BASE_URL;
    }


    @Override
    protected void doInit() throws SensorHubException {
        // reset internal state in case init() was already called
        super.doInit();
        mjpegVideoStream = null;
        h264VideoStream = null;
        h265VideoStream = null;
        ptzPosOutput = null;
        ptzControlInterface = null;
        ptzSupported = false;

        // create connection handler
        connection = new RobustHTTPConnection(this, config.connection, "Axis Camera") {
            public boolean tryConnect() throws IOException {
                // check we can reach the HTTP server
                // and access the param URL
                HttpURLConnection conn = tryConnectGET(getHostUrl() + VAPIX_QUERY_PARAMS_LIST);
                if (conn == null)
                    return false;

                // parse response to extract identifiers
                BufferedReader reader = null;
                try {
                    InputStream is = conn.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split("=");

                        if ((tokens[0].trim().equalsIgnoreCase("root.Brand.Brand")) && (tokens[1].trim().equalsIgnoreCase("AXIS")))
                            connected = true;
                        else if (tokens[0].trim().equalsIgnoreCase("root.Properties.PTZ.PTZ")) {
                            if (tokens[1].trim().equalsIgnoreCase("yes"))
                                ptzSupported = true;
                        } else if (tokens[0].trim().equalsIgnoreCase("root.Properties.Image.Format")) {
                            String formats = tokens[1].toLowerCase();
                            mpeg4Supported = formats.contains("mpeg4");
                            h264Supported = formats.contains("h264");
                            // Axis VAPIX reports HEVC as either "h265" or "hevc" depending on firmware.
                            h265Supported = formats.contains("h265") || formats.contains("hevc");
                            mjpegSupported = formats.contains("mjpeg");
                        } else if (tokens[0].trim().equalsIgnoreCase("root.Brand.ProdFullName"))
                            longName = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("root.Brand.ProdShortName"))
                            shortName = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("root.Brand.ProdNbr"))
                            modelNumber = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("root.Properties.System.SerialNumber"))
                            serialNumber = tokens[1];
                        else if (tokens[0].trim().equalsIgnoreCase("root.Properties.API.HTTP.Version"))
                            vapixVersion = Integer.parseInt(tokens[1]);

                    }
                } catch (IOException e) {
                    reportError("Cannot read camera information", e, true);
                    return false;
                } finally {
                    if (reader != null)
                        reader.close();
                }

                // check that we actually read a serial number
                if (serialNumber == null || serialNumber.trim().isEmpty())
                    throw new IOException("Cannot read camera serial number");

                if (!h264Supported && config.enableH264)
                    throw new IOException("Cannot connect to RTSP server - H264 not supported");

                if (!h265Supported && config.enableH265)
                    throw new IOException("Cannot connect to RTSP server - H265 not supported");

                if (vapixVersion != 2 && vapixVersion != 3)
                    throw new IOException("Unsupported VAPIX API for this camera. VAPIX API version returned: " + vapixVersion);

                // check connection to RTSP server (used for both H.264 and H.265 paths)
                boolean rtspWanted = (h264Supported && config.enableH264) || (h265Supported && config.enableH265);
                if (rtspWanted) {
                    try {

                        RTSPClient rtspClient = new RTSPClient(
                            config.rtsp.remoteHost,
                            config.rtsp.remotePort,
                            config.rtsp.videoPath,
                            config.rtsp.user,
                            config.rtsp.password,
                            config.rtsp.localUdpPort,
                            config.connection.connectTimeout);
                        rtspClient.sendOptions();
                    } catch (IOException e) {
                        reportError("Cannot connect to RTSP server", e, true);
                        return false;
                    }
                }

                return true;
            }
        };

        // TODO we could check if basic metadata is in cache, in which case
        // it's not necessary to connect to camera at this point

        // wait for valid connection to camera
        connection.waitForConnection();

        // generate identifiers
        generateUniqueID("urn:axis:cam:", config.uidExtension.isBlank() ? serialNumber : serialNumber.trim() + ":" + config.uidExtension);
        generateXmlID("AXIS_CAM_", config.uidExtension.isBlank() ? serialNumber : serialNumber.trim() + "_" + config.uidExtension);

        // create I/O objects
        String videoOutName = "video";
        int videoOutNum = 1;

        if (!mjpegSupported && config.enableMJPEG) {
            throw new SensorException("Cannot connect to MJPEG stream - MJPEG not supported");
        }

        // add MJPEG video output (HTTP via FFmpeg)
        if (mjpegSupported && config.enableMJPEG) {
            String outputName = videoOutName + videoOutNum++;
            mjpegVideoStream = new AxisVideoStream(this, outputName, buildMjpegUrl(), "-timeout 3000000");
            mjpegVideoStream.init();
            addOutput(mjpegVideoStream.getOutput(), false);
        }

        // add H.264 video output (RTSP via FFmpeg)
        if (h264Supported && config.enableH264) {
            String outputName = videoOutName + videoOutNum++;
            h264VideoStream = new AxisVideoStream(this, outputName, buildRtspUrl("h264"), "-rtsp_transport tcp -stimeout 3000000");
            h264VideoStream.init();
            addOutput(h264VideoStream.getOutput(), false);
        }

        // add H.265 video output (RTSP via FFmpeg)
        if (h265Supported && config.enableH265) {
            String outputName = videoOutName + videoOutNum++;
            h265VideoStream = new AxisVideoStream(this, outputName, buildRtspUrl("h265"), "-rtsp_transport tcp -stimeout 3000000");
            h265VideoStream.init();
            addOutput(h265VideoStream.getOutput(), false);
        }

        if (ptzSupported) {
            // add PTZ output
            ptzPosOutput = new AxisPtzOutput(this);
            addOutput(ptzPosOutput, false);
            ptzPosOutput.init();

            // add PTZ controller
            ptzControlInterface = new AxisPtzControl(this);
            addControlInput(ptzControlInterface);
            ptzControlInterface.init();
        }
    }


    @Override
    protected void doStart() throws SensorHubException {
        // wait for valid connection to camera
        connection.waitForConnection();

        // start video outputs
        if (mjpegVideoStream != null)
            mjpegVideoStream.start();

        if (h264VideoStream != null)
            h264VideoStream.start();

        if (h265VideoStream != null)
            h265VideoStream.start();

        // if PTZ supported
        if (ptzSupported) {
            ptzPosOutput.start();
            ptzControlInterface.start();
        }
    }


    @Override
    protected void updateSensorDescription() {
        synchronized(sensorDescLock) {
            // parent class reads SensorML from config if provided
            // and then sets unique ID, outputs and control inputs
            super.updateSensorDescription();

            SMLFactory smlFac = new SMLFactory();

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Axis Video Camera");

            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);

            Term term;
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Axis");
            identifierList.addIdentifier(term);

            if (modelNumber != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
                term.setLabel("Model Number");
                term.setValue(modelNumber);
                identifierList.addIdentifier(term);
            }

            if (serialNumber != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("SerialNumber"));
                term.setLabel("Serial Number");
                term.setValue(serialNumber);
                identifierList.addIdentifier(term);
            }

            if (longName != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("LongName"));
                term.setLabel("Long Name");
                term.setValue(longName);
                identifierList.addIdentifier(term);
            }

            if (shortName != null) {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
                term.setLabel("Short Name");
                term.setValue(shortName);
                identifierList.addIdentifier(term);
            }
        }
    }


    @Override
    public boolean isConnected() {
        if (connection == null)
            return false;

        return connection.isConnected();
    }


    protected void setAuth() {
        ClientAuth.getInstance().setUser(config.http.user);
        if (config.http.password != null)
            ClientAuth.getInstance().setPassword(config.http.password.toCharArray());
    }


    @Override
    protected void doStop() {
        if (connection != null)
            connection.cancel();

        if (ptzPosOutput != null)
            ptzPosOutput.stop();

        if (ptzControlInterface != null)
            ptzControlInterface.stop();

        if (mjpegVideoStream != null)
            mjpegVideoStream.stop();

        if (h264VideoStream != null)
            h264VideoStream.stop();

        if (h265VideoStream != null)
            h265VideoStream.stop();

        if (videoControlInterface != null)
            videoControlInterface.stop();
    }


    @Override
    public void cleanup() {}


    protected String getHostUrl() {
        setAuth();
        return hostUrl;
    }


    /**
     * Builds the FFmpeg-compatible MJPEG-over-HTTP URL, inlining credentials
     * if the user/password are configured (FFmpeg cannot consult
     * {@link ClientAuth} the way the Java {@code URL} opener can).
     */
    protected String buildMjpegUrl() {
        String userInfo = buildUserInfo(config.http.user, config.http.password);
        return "http://" + userInfo + config.http.remoteHost + ":" + config.http.remotePort + "/mjpg/video.mjpg";
    }


    /**
     * Builds the FFmpeg-compatible RTSP URL for the requested codec. The base
     * path is taken from {@code config.rtsp.videoPath}; the {@code videocodec}
     * query parameter is rewritten to the requested codec so the same
     * configuration field can serve both H.264 and H.265 streams.
     *
     * @param codec one of {@code "h264"}, {@code "h265"}, or {@code "jpeg"}.
     */
    protected String buildRtspUrl(String codec) {
        String userInfo = buildUserInfo(config.rtsp.user, config.rtsp.password);
        String path = config.rtsp.videoPath == null ? DEFAULT_RTSP_VIDEO_PATH : config.rtsp.videoPath;
        if (path.toLowerCase().contains("videocodec=")) {
            path = path.replaceAll("(?i)videocodec=[a-z0-9]+", "videocodec=" + codec);
        } else {
            path = path + (path.contains("?") ? "&" : "?") + "videocodec=" + codec;
        }
        if (!path.startsWith("/"))
            path = "/" + path;
        return "rtsp://" + userInfo + config.rtsp.remoteHost + ":" + config.rtsp.remotePort + path;
    }


    /**
     * Returns a URL userinfo segment ({@code "user:pass@"}) with both components
     * URL-encoded, or the empty string if no credentials are configured.
     */
    private static String buildUserInfo(String user, String password) {
        if (user == null || user.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append(URLEncoder.encode(user, StandardCharsets.UTF_8));
        if (password != null && !password.isEmpty())
            sb.append(':').append(URLEncoder.encode(password, StandardCharsets.UTF_8));
        sb.append('@');
        return sb.toString();
    }
}