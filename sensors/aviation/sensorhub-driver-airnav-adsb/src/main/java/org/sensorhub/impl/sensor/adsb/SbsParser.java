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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;


/**
 * parses dump1090 SBS BaseStation format messages (port 30003)
 */
public class SbsParser implements MessageParser {

    private final BufferedReader reader;
    private final ConcurrentHashMap<String, AircraftState> aircraftMap;

    public SbsParser(InputStream in, ConcurrentHashMap<String, AircraftState> aircraftMap) {
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.aircraftMap = aircraftMap;
    }

    public AircraftState readNext() throws IOException {
        String line = reader.readLine();
        if (line == null)
            return null;
        return parseSbsLine(line);
    }

    private AircraftState parseSbsLine(String line) {
        try {
            String[] fields = line.split(",", -1);
            if (fields.length < 11 || !"MSG".equals(fields[0]))
                return null;

            String msgType = fields[1].trim();
            String icao = fields[4].trim();
            if (icao.isEmpty())
                return null;

            AircraftState state = aircraftMap.computeIfAbsent(icao, k -> {
                AircraftState s = new AircraftState();
                s.icao = k;
                return s;
            });

            boolean positionUpdate = false;

            switch (msgType) {
                case "1": // callsign
                    if (fields.length > 10 && !fields[10].trim().isEmpty())
                        state.callsign = fields[10].trim();
                    break;

                case "3": // position — lat, lon, barometric altitude
                    if (fields.length > 15) {
                        if (!fields[11].trim().isEmpty())
                            state.altBaroFt = Double.parseDouble(fields[11].trim());
                        if (!fields[14].trim().isEmpty())
                            state.lat = Double.parseDouble(fields[14].trim());
                        if (!fields[15].trim().isEmpty())
                            state.lon = Double.parseDouble(fields[15].trim());
                    }
                    state.lastUpdateTime = System.currentTimeMillis();
                    if (state.hasPosition())
                        positionUpdate = true;
                    break;

                case "4": // velocity — ground speed, heading, vertical rate
                    if (fields.length > 16) {
                        if (!fields[12].trim().isEmpty())
                            state.groundSpeed = Double.parseDouble(fields[12].trim());
                        if (!fields[13].trim().isEmpty())
                            state.track = Double.parseDouble(fields[13].trim());
                        if (!fields[16].trim().isEmpty())
                            state.verticalRate = Double.parseDouble(fields[16].trim());
                    }
                    state.lastUpdateTime = System.currentTimeMillis();
                    break;

                case "5": // surveillance alt
                    if (fields.length > 11 && !fields[11].trim().isEmpty())
                        state.altBaroFt = Double.parseDouble(fields[11].trim());
                    state.lastUpdateTime = System.currentTimeMillis();
                    break;

                case "6": // surveillance ID — squawk
                    if (fields.length > 17 && !fields[17].trim().isEmpty())
                        state.squawk = fields[17].trim();
                    state.lastUpdateTime = System.currentTimeMillis();
                    break;

                default:
                    break;
            }

            if (fields.length > 18 && !fields[18].trim().isEmpty())
                state.alert = "-1".equals(fields[18].trim());
            if (fields.length > 19 && !fields[19].trim().isEmpty())
                state.emergency = "-1".equals(fields[19].trim());
            if (fields.length > 21 && !fields[21].trim().isEmpty())
                state.isOnGround = "-1".equals(fields[21].trim());

            return positionUpdate ? state : null;

        } catch (NumberFormatException e) {
            return null;
        }
    }
}
