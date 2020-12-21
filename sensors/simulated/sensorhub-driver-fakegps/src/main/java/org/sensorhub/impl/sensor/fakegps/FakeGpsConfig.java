/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorConfig;


public class FakeGpsConfig extends SensorConfig
{    
    public String googleApiUrl = "https://maps.googleapis.com/maps/api/directions/json";
    public String googleApiKey = null;
    
    // use these to add specific start and stop locations
    public Double startLatitude = null;  // in degrees
    public Double startLongitude = null;  // in degrees
    public Double stopLatitude = null;  // in degrees
    public Double stopLongitude = null;  // in degrees
    
    // otherwise use these to generate random start and stop locations
    public double centerLatitude = 34.7300; // in deg    
    public double centerLongitude = -86.5850; // in deg
    public double areaSize = 0.1; // in deg
    
    public double minSpeed = 40; // km/h
    public double maxSpeed = 100; // km/h
    public boolean walkingMode = false;
    public double samplingPeriodSeconds = 1.0;
    
    //  parameters to limit number of calls to Google directions API
    public Integer apiRequestPeriodMinutes = 720;
    
    
    protected void validate() throws SensorHubException
    {
        if (googleApiKey == null || googleApiKey.isEmpty())
            throw new SensorHubException("A Google API key with access to the Directions API must be provided in the configuration");
        
        if (areaSize <= 0 || areaSize > 90)
            throw new SensorHubException(String.format("Invalid area size: %d", areaSize));
        
        if (minSpeed <= 0 || maxSpeed <= 0 || minSpeed > maxSpeed)
            throw new SensorHubException(String.format("Invalid speed range: [%d - %d]", minSpeed, maxSpeed));
    }
}
