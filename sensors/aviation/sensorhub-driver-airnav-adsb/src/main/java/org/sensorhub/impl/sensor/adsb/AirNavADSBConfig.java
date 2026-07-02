/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.adsb;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.comm.TCPCommProviderConfig;


public class AirNavADSBConfig extends SensorConfig {

    public enum InputFormat {
        SBS,   // BaseStation text format (port 30003)
        BEAST  // Beast binary format (port 30005)
    }

    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier for this ADS-B receiver")
    public String serialNumber = "adsb001";

    @DisplayInfo(desc = "Input format: SBS (port 30003) for text, BEAST (port 30005) for binary with GNSS altitude support")
    public InputFormat inputFormat = InputFormat.SBS;

    @DisplayInfo(desc = "Communication settings to connect to the dump1090 output (SBS: port 30003, Beast: port 30005)")
    public TCPCommProviderConfig commSettings;

    @DisplayInfo(desc = "ADS-B Receiver Location")
    public PositionConfig positionConfig = new PositionConfig();

    @DisplayInfo(label="ICAO Address Lookup URL", desc = "URL to query the ICAO Address of an aircraft")
    public String icaoAddressLookupUrl = "https://api.adsbdb.com/v0/aircraft/"; //"https://hexdb.io/api/v1/aircraft/";

    @Override
    public PositionConfig.LLALocation getLocation(){return positionConfig.location;}
    @Override
    public PositionConfig.EulerOrientation getOrientation(){ return positionConfig.orientation;}
}