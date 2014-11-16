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

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.sensor.SensorConfig;


public class FakeGpsConfig extends SensorConfig
{
    private static final long serialVersionUID = 1L;
    
    
    public String googleApiUrl = "http://maps.googleapis.com/maps/api/directions/json";
    
    public double centerLatitude; // in deg
    
    public double centerLongitude; // in deg
    
    public double areaSize = 0.1; // in deg
    
    public double vehicleSpeed = 40; // km/h
}
