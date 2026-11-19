/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.sensor.simorientation;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;


public class SimOrientationConfig extends SensorConfig {
    
    @Required
    @DisplayInfo(desc="Serial number of the station used to generate its unique ID")
    public String serialNumber = "0123456879";
    
    
    @DisplayInfo(desc="Sensor Location")
    public LLALocation location = new LLALocation();
    
    
    public SimOrientationConfig() {
        location.lat = 34.8038;
        location.lon = -86.7228;
        location.alt = 0.000;
    }
    
    
    @Override
    public LLALocation getLocation() {
        return location;
    }
}
