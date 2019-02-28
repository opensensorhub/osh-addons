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

package org.sensorhub.impl.sensor.simulatedcbrn;

import datasimulation.PointSource;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;

/**
 * Created by Ianpa on 5/2/2017.
 */
public class SimCBRNConfig extends SensorConfig
{
    @Required
    @DisplayInfo(desc = "Serial number of the sensor used to generate its unique ID")
    public String serialNumber = "BIR012345";

    @DisplayInfo(desc = "Number of Sources")
    public int numSources = 1;


    public String googleApiUrl = "http://maps.googleapis.com/maps/api/directions/json";

    // use these to add specific start and stop locations
    public Double startLatitude = null;  // in degrees
    public Double startLongitude = null;  // in degrees
    public Double stopLatitude = null;  // in degrees
    public Double stopLongitude = null;  // in degrees

    // otherwise use these to generate random start and stop locations
    public double centerLatitude = 34.7278; // in deg
    public double centerLongitude = -86.5887; // in deg
    public double areaSize = 0.01; // in deg

    public double vehicleSpeed = 10; // km/h
    public boolean walkingMode = true;

    // Point source vars for construction of source 1
    public double src1_lat = 34.7278;
    public double src1_lon = -86.5887;
    public double src1_alt = 0;
    public double src1_intensity = 600;
    public double src1_radius = 1;
    public String src1_type = "VX";


    // Point source vars for construction source 2
    public double src2_lat = 34.728592;
    public double src2_lon = -86.588933;
    public double src2_alt = 0;
    public double src2_intensity = 600;
    public double src2_radius = 1;
    public String src2_type = "VX";

    // Point source vars for construction source 3
    public double src3_lat = 34.725528;
    public double src3_lon = -86.590242;
    public double src3_alt = 0;
    public double src3_intensity = 600;
    public double src3_radius = 1;
    public String src3_type = "VX";

}
