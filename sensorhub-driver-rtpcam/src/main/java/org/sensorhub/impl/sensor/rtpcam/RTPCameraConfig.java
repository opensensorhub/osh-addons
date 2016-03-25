/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtpcam;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.NetworkConfig;
import org.sensorhub.impl.sensor.videocam.VideoResolution;


/**
 * <p>
 * Configuration class for the generic RTP/RTSP camera driver
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class RTPCameraConfig extends SensorConfig
{    
    @DisplayInfo(label="Camera ID", desc="Camera ID to be appended to UID prefix")
    public String cameraID;
    
    @DisplayInfo(label="Video", desc="Video settings")
    public BasicVideoConfig video = new BasicVideoConfig();
    
    @DisplayInfo(label="Network", desc="Network configuration")
    public NetworkConfig net = new NetworkConfig();
    
    @DisplayInfo(label="RTP/RTSP", desc="RTP/RTSP configuration")
    public RTSPConfig rtsp = new RTSPConfig();
    
    
    public enum Resolution implements VideoResolution
    {
        SD_640_480("SD", 640, 480),
        SD_704_480("SD", 704, 480),
        SD_720_480("SD", 720, 480),
        SD_576("SD", 720, 576),
        D1("D1", 704, 480),
        DVD_480("DVD", 720, 480),
        DVD_576("DVD", 720, 576),
        HD_720P("HD", 1280, 720),
        HD_1080P("Full HD", 1920, 1080),
        UHD_4K("Ultra HD", 3840, 2160);
        
        private String text;
        private int width, height;
        
        private Resolution(String text, int width, int height)
        {
            this.text = text;
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() { return width; };
        public int getHeight() { return height; };
        public String toString() { return text + " (" + width + "x" + height + ")"; }
    };
    
    
    public RTPCameraConfig()
    {
        video.resolution = Resolution.HD_720P;
    }
}
