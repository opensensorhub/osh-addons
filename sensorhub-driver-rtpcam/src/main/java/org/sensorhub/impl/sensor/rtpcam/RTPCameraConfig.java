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
    
    @DisplayInfo(desc="Remote host (IP address or host name) where RTSP commands are sent")
    public String remoteHost;
    
    @DisplayInfo(label="Remote RTSP Port", desc="Remote TCP port where RTSP commands are sent")
    public int remoteRtspPort;
    
    @DisplayInfo(label="Local UDP Port", desc="Local UDP port where to listen for RTP packets")
    public int localUdpPort;
    
    @DisplayInfo(desc="Width of video frames in pixels")
    public int frameWidth;
    
    @DisplayInfo(desc="Height of video frames in pixels")
    public int frameHeight;
    
    @DisplayInfo(desc="Frame rate in Hz")
    public int frameRate;
    
    @DisplayInfo(desc="Set to true if video is grayscale, false is RGB")
    public boolean grayscale = false;
    
    @DisplayInfo(desc="Path to local file to backup raw H264 stream")
    public String backupFile = null;

}
