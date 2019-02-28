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
import org.sensorhub.impl.comm.RobustIPConnectionConfig;
import org.sensorhub.impl.sensor.rtpcam.RTSPConfig;


public class VirbXeConfig extends SensorConfig
{        
    @DisplayInfo(label="HTTP", desc="HTTP configuration")
    public HTTPConfig http = new HTTPConfig();
    
    @DisplayInfo(label="RTP/RTSP", desc="RTP/RTSP configuration (Remote host is obtained from HTTP config)")
    public RTSPConfig rtsp = new RTSPConfig();
    
    @DisplayInfo(label="Connection Options")
    public RobustIPConnectionConfig connection = new RobustIPConnectionConfig();
    
    
    public VirbXeConfig()
    {
        rtsp.remotePort = 554;
        rtsp.videoPath = "/livePreviewStream";
        rtsp.localUdpPort = 20200;
    }
      
}
