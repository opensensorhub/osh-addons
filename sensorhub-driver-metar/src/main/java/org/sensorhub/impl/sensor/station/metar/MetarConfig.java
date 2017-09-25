/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.station.metar;

import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * Configuration parameters for Metar Station Network sensor module
 * </p>
 *
 * @author Tony Cook
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since May 30, 2015
 */
public class MetarConfig extends SensorConfig
{   
    public String emwinRoot;
	public String metarStationMapPath;
	
	//  For realtime
	public String aviationWeatherUrl;
	
	//  For archive
	public String archiveServerUrl;
	public String archiveServerPath;
}
