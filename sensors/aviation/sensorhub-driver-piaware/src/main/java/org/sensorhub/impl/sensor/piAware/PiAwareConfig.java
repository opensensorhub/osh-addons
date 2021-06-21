/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.piAware;

import org.sensorhub.api.sensor.SensorConfig;


public class PiAwareConfig extends SensorConfig
{    
//    @DisplayInfo(desc="Airline codes to listen for")
//    public List<String> airlines = new ArrayList<>();
 
	String piawareDeviceIp = "192.168.1.124";
	int rawOutboundPort = 30002;
	int sbsOutboundPort = 30003;
	int beastOutboundPort = 30005;
	
	String dump1090Path = "/run/dump1090";
	String aircraftJsonFile = "aircraft.json";
}
