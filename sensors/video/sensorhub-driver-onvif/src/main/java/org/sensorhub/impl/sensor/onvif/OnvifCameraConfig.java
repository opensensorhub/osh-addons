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

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * <p>
 * Implementation of ONVIF interface for generic cameras using SOAP ONVIF
 * protocol. This particular class stores configuration parameters.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since May 22, 2017
 */
public class OnvifCameraConfig extends SensorConfig {
	
	@Required
    @DisplayInfo(label="Host IP Address", desc="IP Address of the camera")
    public String hostIp = null;

	@DisplayInfo(label="Port", desc="Port number of the camera")
	public Integer port = 80;
	
    @DisplayInfo(label="User Login", desc="User that will be logged into for issueing PTZ commands")
    public String user = null;

    @DisplayInfo(label="Password", desc="Password used to login to user")
    public String password = null;
	
	@DisplayInfo(label="Path", desc="ONVIF route of the camera")
	public String path = "/onvif/device_service";
	
	@DisplayInfo(label="Timeout(ms)", desc="Timeout of connection to the camera")
	public Integer timeout = 5000;
	
    @DisplayInfo(label="Enable H264", desc="Enable H264 encoded video output (accessible through RTSP)")
    public boolean enableH264 = false;
    
    @DisplayInfo(label="Enable MPEG4", desc="Enable MPEG4 encoded video output (accessible through HTTP)")
    public boolean enableMPEG4 = false;
}
