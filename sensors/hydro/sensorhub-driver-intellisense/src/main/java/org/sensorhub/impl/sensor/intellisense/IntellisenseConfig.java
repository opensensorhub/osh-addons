/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.intellisense;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


public class IntellisenseConfig extends SensorConfig
{
	@DisplayInfo(desc="Intellisense Configuration")
	public String configOption;

	@DisplayInfo(desc="Intellisense FlashFlood api url")
	public String apiUrl = "https://flashflood.info:8080";
	public String authPath = "api/auth/login";
	public String obsPath = "api/plugins/telemetry/DEVICE/DEVICE_ID/values/timeseries?";
	public String username;
	public String password;
	public int reportingMode = 3;// (Initial) Reporting Mode - 0 = auto mode, 1 = 10-minute mode, 2 = 30-minute mode 
	
	@DisplayInfo(desc="apiKeys for observables. Defaulting to all but config can overrise")
	public String[] apiKeys={
			"depth1",
			"NAVD88O",
			"NAVD88D1",
			"dropSDI",
			"soilSDI",
			"lat",
			"lon",
			"elev",
			"samp",
			"mode",
			"oPressure",
			"airTemp",
			"h2oTemp",
			"baro",
			"rssi",
			"time",
			"hex",
			"IMEI",
			"battery",
			"ffi1",

	};
	public String [] deviceNames;  // add if needed
	public String [] deviceIds = {
			"565f3bd0-0a98-11ea-b248-cf66a118b932",
			"11e46be0-aca3-11e9-abf5-5b632306c2a2",
			"566cd060-0a98-11ea-b248-cf66a118b932",
			"3985e150-0a78-11ea-9316-4b353223de62",
			"39a417b0-0a78-11ea-9316-4b353223de62",

	};
	//  TODO Have sensor populate lat/lon on initial pull
	public double [] deviceLats = {
			33.8429,
			33.843,
			33.8434,
			33.8428,
			33.843,
	};
	public double [] deviceLons = {
			-118.3131,
			-118.3129,
			-118.3132,
			-118.3134,
			-118.3129,
	};
	
	public String getKeyString() {
		StringBuilder b = new StringBuilder();
		b.append("keys=");
		for(String key:apiKeys) 
			b.append(key + ",");
			
		String keyStr = b.toString().substring(0,b.length() - 1);
		return keyStr;
	}
	
}
