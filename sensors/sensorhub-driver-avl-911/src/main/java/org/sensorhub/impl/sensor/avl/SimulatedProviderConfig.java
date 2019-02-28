/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.avl;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.comm.CommProviderConfig;


public class SimulatedProviderConfig extends CommProviderConfig<Object>
{
    public String googleApiUrl = "http://maps.googleapis.com/maps/api/directions/json";
    public String googleApiKey = null;
    
    // vehicle configurations
    List<SimulatedVehicleConfig> vehicles = new ArrayList<SimulatedVehicleConfig>();
    
    // start date (ISO 8601 time string), current time will be used if null
    public String startDate;
    
    // otherwise use these to generate random start and stop locations
    public double centerLat = 34.7300; // in deg    
    public double centerLon = -86.5850; // in deg
    public double areaSize = 0.1; // in deg
    
    
    public SimulatedProviderConfig()
    {
        this.moduleClass = SimulatedProvider.class.getCanonicalName();
    }
}
