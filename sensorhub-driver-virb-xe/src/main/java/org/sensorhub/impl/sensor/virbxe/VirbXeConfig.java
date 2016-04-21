/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.virbxe;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.HTTPConfig;
import org.sensorhub.impl.sensor.rtpcam.RTSPConfig;
import org.sensorhub.impl.sensor.videocam.BasicVideoConfig;
import org.sensorhub.impl.sensor.videocam.VideoResolution;


public class VirbXeConfig extends SensorConfig
{

//    @DisplayInfo(label="Video", desc="Video settings")
//    public VideoConfig video = new VideoConfig();
        
    @DisplayInfo(label="Network", desc="Network configuration")
    public HTTPConfig net = new HTTPConfig();
    
    @DisplayInfo(label="RTP/RTSP", desc="RTP/RTSP configuration")
    public RTSPConfig rtsp = new RTSPConfig();
    
//    @DisplayInfo(label="Enable H264", desc="Enable H264 encoded video output (accessible through RTSP)")
//    public boolean enableH264;
    
    
//    public class VideoConfig extends BasicVideoConfig
//    {
//        @DisplayInfo(desc="Resolution of video frames in pixels")
//        public ResolutionEnum resolution;        
//               
//        public VideoResolution getResolution()
//        {
//            return resolution;
//        }
//    }
      
}
