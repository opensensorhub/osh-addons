/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc.. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.axis;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.sensor.rtpcam.RTSPConfig;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.VideoResolution;
import org.sensorhub.impl.sensor.videocam.ptz.PtzConfig;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */
public class AxisCameraConfig extends SensorConfig {
	
    @DisplayInfo(label="HTTP", desc="HTTP configuration")
    public HTTPConfig http = new HTTPConfig();
    
    @DisplayInfo(label="RTP/RTSP", desc="RTP/RTSP configuration (Remote host is obtained from HTTP configuration)")
    public RTSPConfig rtsp = new RTSPConfig();
    
    @DisplayInfo(label="Connection Options")
    public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();
    
    @DisplayInfo(label="Video", desc="Video settings")
    public VideoConfig video = new VideoConfig();
    
    @DisplayInfo(label="PTZ", desc="Pan-Tilt-Zoom configuration")
    public PtzConfig ptz = new PtzConfig();
    
    @DisplayInfo(desc="Camera geographic position")
    public PositionConfig position = new PositionConfig();
    
    @DisplayInfo(label="Enable H264", desc="Enable H264 encoded video output (accessible through RTSP)")
    public boolean enableH264;
    
    @DisplayInfo(label="Enable MJPEG", desc="Enable MJPEG encoded video output (accessible through HTTP)")
    public boolean enableMJPEG;

    
	// TODO: Set variable for mounting (top up, top down, top sideways) or better set mounting angles relative to NED/ENU
    // ALSO, use flip=yes/no to flip image if necessary	
	

    public class VideoConfig extends BasicVideoConfig
    {
        @DisplayInfo(desc="Resolution of video frames in pixels")
        public ResolutionEnum resolution;        
               
        public VideoResolution getResolution()
        {
            return resolution;
        }
    }
    
    
    public enum ResolutionEnum implements VideoResolution
    {
        VGA("VGA", 640, 480),
        _4CIF("CIF", 704, 480),
        _2CIF("CIF", 704, 240),
        CIF("CIF", 352, 240),
        QCIF("CIF", 176, 120),
        D1_NTSC("D1", 720, 480),
        D1_PAL("D1", 720, 576),
        HD_720P("HD", 1280, 720),
        SXGA("HD", 1280, 1024),
        HD_1080P("Full HD", 1920, 1080);
        
        private String text;
        private int width, height;
        
        private ResolutionEnum(String text, int width, int height)
        {
            this.text = text;
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() { return width; };
        public int getHeight() { return height; };
        public String toString() { return text + " (" + width + "x" + height + ")"; }
    };
    
    
    public AxisCameraConfig()
    {
        // default params for Axis
        video.resolution = ResolutionEnum._4CIF;
        rtsp.remotePort = 554;
        rtsp.videoPath = AxisCameraDriver.DEFAULT_RTSP_VIDEO_PATH;        
        rtsp.localUdpPort = 20100;
    }
    
    
    @Override
    public LLALocation getLocation()
    {
        return position.location;
    }
    
    
    @Override
    public EulerOrientation getOrientation()
    {
        return position.orientation;
    }
}
