/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trek1000;

import java.util.ArrayList;
import java.util.HashMap;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * <p>
 * Implementation of the Trek1000 sensor. This particular class stores 
 * configuration parameters.
 * </p>
 * 
 * @author Joshua Wolfe <developer.wolfe@gmail.com>
 * @since March 24, 2017
 */
public class Trek1000Config extends SensorConfig
{
	@DisplayInfo(label="Sensor ID", desc="Unique ID for sensor")
	public String sensorId = null;
	
	@Required
	@DisplayInfo(desc="Serial port of connected anchor")
	public String serialPort = null;

	@DisplayInfo(desc="Baud rate of connected anchor")
	public int baudRate = 9600;
	
	@Required
	@DisplayInfo(desc="Anchor locations in the form of [latitude, longitude]")
	public ArrayList<LLALocation> anchorLocations = new ArrayList<LLALocation>();
}
