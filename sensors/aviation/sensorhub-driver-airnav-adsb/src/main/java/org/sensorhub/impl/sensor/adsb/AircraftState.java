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
    double altBaroFt = Double.NaN;
    double altGeoFt = Double.NaN;
    double groundSpeed = Double.NaN;
    double track = Double.NaN;
    double verticalRate = Double.NaN;
    String squawk;
    boolean alert;
    boolean emergency;
    boolean isOnGround;
    long lastUpdateTime;

    int cprLatEven, cprLonEven;
    int cprLatOdd, cprLonOdd;
    long cprTimeEven, cprTimeOdd;

    double gnssBaroOffsetFt = Double.NaN;

    boolean hasPosition() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    void updateGeoAlt() {
        if (!Double.isNaN(altBaroFt) && !Double.isNaN(gnssBaroOffsetFt)) {
            altGeoFt = altBaroFt + gnssBaroOffsetFt;
        }
    }
}
