/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakeweather;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;


public class FakeWeatherConfig extends SensorConfig
{
    
    @Required
    @DisplayInfo(desc="Serial number of the station used to generate its unique ID")
    public String serialNumber = "0123456879";
    
    
    @DisplayInfo(desc="Station Location")
    public LLALocation location = new LLALocation();
    
    
    public FakeWeatherConfig()
    {
        location.lat = 34.8038;        
        location.lon = -86.7228;      
        location.alt = 0.000;
    }
    
    
    @Override
    public LLALocation getLocation()
    {
        return location;
    }
}
