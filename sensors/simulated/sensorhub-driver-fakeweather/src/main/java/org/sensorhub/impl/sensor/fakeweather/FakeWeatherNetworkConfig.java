/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakeweather;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;


public class FakeWeatherNetworkConfig extends SensorConfig
{
    
    @Required
    @DisplayInfo(desc="Network ID used to generate its unique ID")
    public String networkID = "001";
    
    
    @Required
    @DisplayInfo(desc="Number of randomly generated weather stations")
    public int numStations = 50;
    
    
    @DisplayInfo(desc="Center location of network area")
    public LLALocation centerLocation = new LLALocation();
    
    
    @DisplayInfo(desc="Size of square area within which stations will be located")
    public double areaSize = 0.1; // in deg
    
    
    public FakeWeatherNetworkConfig()
    {
        centerLocation.lat = 34.8038;        
        centerLocation.lon = -86.7228;
    }
}
