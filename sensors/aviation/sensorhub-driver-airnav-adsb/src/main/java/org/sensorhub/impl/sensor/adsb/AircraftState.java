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

public class AircraftState {
    String icao;
    String callsign;
    double lat = Double.NaN;
    double lon = Double.NaN;
    double altFt = Double.NaN;
    double groundSpeed = Double.NaN;
    double track = Double.NaN;
    double verticalRate = Double.NaN;
    String squawk;
    boolean alert;
    boolean emergency;
    boolean isOnGround;
    long lastUpdateTime;

    boolean hasPosition() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }
}
