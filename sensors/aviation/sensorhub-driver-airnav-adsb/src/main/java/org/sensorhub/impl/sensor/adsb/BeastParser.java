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
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
public class BeastParser implements MessageParser {
    private final InputStream in;
    private final ConcurrentHashMap<String, AircraftState> aircraftMap;
    public BeastParser(InputStream in, ConcurrentHashMap<String, AircraftState> aircraftMap) {
        this.in = in;
        this.aircraftMap = aircraftMap;
    }
    public AircraftState readNext() throws IOException {
    }
}
