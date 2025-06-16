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

package org.sensorhub.impl.sensor.onvif;

import org.onvif.ver10.schema.VideoEncoding;
import org.sensorhub.api.comm.ICommConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;
import de.onvif.discovery.OnvifDiscovery;
import org.sensorhub.impl.comm.TCPConfig;
import org.sensorhub.impl.sensor.rtpcam.RTPCameraConfig;

import java.net.URL;
import java.util.*;

import org.sensorhub.impl.sensor.ffmpeg.config.*;

/**
 * <p>
 * Implementation of ONVIF interface for generic cameras using SOAP ONVIF
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Kyle Fitzpatrick, Joshua Wolfe <developer.wolfe@gmail.com>
 * @since May 22, 2017
 */
public class OnvifCameraConfig extends SensorConfig {
    @Required
    @DisplayInfo(label="ONVIF Connection Options", desc="Configure ONVIF remote address and port")
    public OnvifConfig networkConfig = new OnvifConfig();

    @Required
    @DisplayInfo(label="ONVIF AV Streaming Options", desc="Configure video/audio streaming")
    public StreamingOptions streamingConfig = new StreamingOptions();

    public class OnvifConfig extends TCPConfig implements ICommConfig{
        public OnvifConfig() {
            this.remotePort = 80;
            this.user = "";
            this.password = "";
        }

        @DisplayInfo(label= "Discovered ONVIF Device Endpoints")
        public TreeSet<String> autoRemoteHost = new TreeSet<>();

        @DisplayInfo(label = "ONVIF Path", desc="Path for ONVIF device services.")
        public String onvifPath = "/onvif/device_service";
    }

    public class StreamingOptions {
        @DisplayInfo(label = "Manual Stream Endpoint", desc="Endpoint for AV streaming. Leave empty to automatically detect via ONVIF.")
        public String streamEndpoint = null;

        @DisplayInfo(label = "Discovered Stream Endpoints")
        public TreeSet<String> autoStreamEndpoint = new TreeSet<>();

        @DisplayInfo(label="Preferred Codec", desc="Select video codec for streaming.")
        public VideoEncoding codec = VideoEncoding.JPEG;
    }
}
