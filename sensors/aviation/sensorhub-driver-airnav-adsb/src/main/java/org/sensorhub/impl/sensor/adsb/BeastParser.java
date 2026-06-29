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


/**
 * Parses dump1090 Beast binary format messages (port 30005) and decodes ADS-B messages
 */
public class BeastParser implements MessageParser {
    private static final int BEAST_ESCAPE = 0x1A; // frame start marker
    private static final int BEAST_MSG_TYPE_MODE_S_SHORT = 0x32; // '2' — 7-byte payload (Mode-S short)
    private static final int BEAST_MSG_TYPE_MODE_S_LONG = 0x33; // '3' — 14-byte payload (Mode-S long / ADS-B)
    private static final int NZ = 15; // NZ = number of latitude zones between the equator and a pole
    private static final double DLAT_EVEN = 360.0 / (4 * NZ); // 6.0 degrees
    private static final double DLAT_ODD = 360.0 / (4 * NZ - 1); // ~6.1017 degrees
    private static final double CPR_MAX = 1 << 17; // 131072
    private static final String CALLSIGN_CHARSET = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ##### ###############0123456789######";

    private final InputStream in;
    private final ConcurrentHashMap<String, AircraftState> aircraftMap;

    private final byte[] frameBuf = new byte[23];

    public BeastParser(InputStream in, ConcurrentHashMap<String, AircraftState> aircraftMap) {
        this.in = in;
        this.aircraftMap = aircraftMap;
    }


    /**
     * Reads the next complete aircraft position update from the Beast stream.
     * @return an {@link AircraftState} with updated position, or null if the stream ends
     */
    @Override
    public AircraftState readNext() throws IOException {
        while (true) {
            int b = readByte();
            if (b < 0) return null;
            if (b != BEAST_ESCAPE) continue;

            int msgType = readByte();
            if (msgType < 0) return null;
            if (msgType == BEAST_ESCAPE) continue;

            int payloadLen;
            switch (msgType) {
                case BEAST_MSG_TYPE_MODE_S_SHORT: payloadLen = 7; break;
                case BEAST_MSG_TYPE_MODE_S_LONG: payloadLen = 14; break;
                default: payloadLen = -1; break;
            }

            if (payloadLen < 0) continue;

            int totalLen = 6 + 1 + payloadLen;
            if (!readEscaped(frameBuf, totalLen))
                return null;

            if (payloadLen != 14) continue;

            AircraftState result = decodeADSB(frameBuf, 7);
            if (result != null)
                return result;
        }
    }


    /**
     * Reads a single byte from the input stream.
     * @return the byte value (0-255), or -1 if the stream has ended
     */
    private int readByte() throws IOException {
        return in.read();
    }


    /**
     * @return true if all bytes were read successfully, false if the stream ended or frame was corrupt
     */
    private boolean readEscaped(byte[] buf, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            int b = readByte();
            if (b < 0) return false;
            if (b == BEAST_ESCAPE) {
                int next = readByte();
                if (next < 0) return false;
                if (next != BEAST_ESCAPE) return false;
            }
            buf[i] = (byte) b;
        }
        return true;
    }


    /**
     * @param buf the frame buffer containing the payload
     * @param offset byte offset where the payload begins in the buffer
     * @return an {@link AircraftState} if a position was decoded, or null otherwise
     */
    private AircraftState decodeADSB(byte[] buf, int offset) {
        int df = (buf[offset] >> 3) & 0x1F;
        if (df != 17 && df != 18) return null;

        String icao = String.format("%02X%02X%02X",
                buf[offset + 1] & 0xFF,
                buf[offset + 2] & 0xFF,
                buf[offset + 3] & 0xFF);

        AircraftState state = aircraftMap.computeIfAbsent(icao, k -> {
            AircraftState s = new AircraftState();
            s.icao = k;
            return s;
        });

        int meOffset = offset + 4;
        int tc = (buf[meOffset] >> 3) & 0x1F;

        boolean positionUpdate = false;

        if (tc >= 1 && tc <= 4) {
            // aircraft identification: callsign
            decodeIdentification(state, buf, meOffset);
        } else if (tc >= 9 && tc <= 18) {
            // airborne position with barometric altitude
            positionUpdate = decodeAirbornePositionBaro(state, buf, meOffset);
        } else if (tc == 19) {
            // airborne velocity: ground speed, track, vertical rate, GNSS/baro offset
            decodeAirborneVelocity(state, buf, meOffset);
        } else if (tc >= 20 && tc <= 22) {
            // airborne position with GNSS (geometric) altitude
            positionUpdate = decodeAirbornePositionGnss(state, buf, meOffset);
        }

        if (positionUpdate) {
            state.lastUpdateTime = System.currentTimeMillis();
            state.updateGeoAlt();
            return state;
        }

        return null;
    }


    /**
     * TC 1-4: Decodes the aircraft identification
     */
    private void decodeIdentification(AircraftState state, byte[] buf, int meOffset) {
        long chars = 0;
        for (int i = 1; i <= 6; i++) {
            chars = (chars << 8) | (buf[meOffset + i] & 0xFF);
        }

        StringBuilder sb = new StringBuilder(8);
        for (int i = 7; i >= 0; i--) {
            int idx = (int) ((chars >> (i * 6)) & 0x3F);
            if (idx > 0 && idx < CALLSIGN_CHARSET.length()) {
                char c = CALLSIGN_CHARSET.charAt(idx);
                if (c != '#') sb.append(c);
            }
        }

        String callsign = sb.toString().trim();
        if (!callsign.isEmpty())
            state.callsign = callsign;
    }

    /**
     * TC 9-18: Decodes an airborne position message baro
     * @return true if a valid lat/lon position was decoded
     */
    private boolean decodeAirbornePositionBaro(AircraftState state, byte[] buf, int meOffset) {
        // 12-bit altitude code spans ME bytes 1-2 (bits 8-19 of the ME field)
        int altCode = ((buf[meOffset + 1] & 0xFF) << 4) | ((buf[meOffset + 2] >> 4) & 0x0F);
        state.altBaroFt = decodeAC12(altCode);
        return decodeCprPosition(state, buf, meOffset);
    }


    /**
     * TC 20-22: Decode airborne position GNSS
     * @return true if a valid lat/lon position was decoded
     */
    private boolean decodeAirbornePositionGnss(AircraftState state, byte[] buf, int meOffset) {
        int altCode = ((buf[meOffset + 1] & 0xFF) << 4) | ((buf[meOffset + 2] >> 4) & 0x0F);
        state.altGeoFt = decodeAC12(altCode);
        return decodeCprPosition(state, buf, meOffset);
    }


    /**
     * Extracts CPR-encoded latitude and longitude from an airborne position message
     */
    private boolean decodeCprPosition(AircraftState state, byte[] buf, int meOffset) {
        // CPR format flag: 0 = even frame, 1 = odd frame (ME bit 21)
        int cprF = (buf[meOffset + 2] >> 2) & 0x01;

        // CPR-encoded latitude: 17 bits at ME bits 22-38
        int cprLat = ((buf[meOffset + 2] & 0x03) << 15) |
                     ((buf[meOffset + 3] & 0xFF) << 7) |
                     ((buf[meOffset + 4] >> 1) & 0x7F);

        // CPR-encoded longitude: 17 bits at ME bits 39-55
        int cprLon = ((buf[meOffset + 4] & 0x01) << 16) |
                     ((buf[meOffset + 5] & 0xFF) << 8) |
                     (buf[meOffset + 6] & 0xFF);

        long now = System.currentTimeMillis();

        // store the frame (even or odd) with its timestamp
        if (cprF == 0) {
            state.cprLatEven = cprLat;
            state.cprLonEven = cprLon;
            state.cprTimeEven = now;
        } else {
            state.cprLatOdd = cprLat;
            state.cprLonOdd = cprLon;
            state.cprTimeOdd = now;
        }

        // need both an even and odd frame to decode a position
        if (state.cprTimeEven == 0 || state.cprTimeOdd == 0)
            return false;
        // the two frames must be within 10 seconds of each other to be valid
        if (Math.abs(state.cprTimeEven - state.cprTimeOdd) > 10000)
            return false;

        return decodeCprGlobal(state);
    }


    /**
     * Global CPR position decoding using both the even and odd frames.
     */
    private boolean decodeCprGlobal(AircraftState state) {
        // normalize CPR values from 17-bit integers to [0, 1) range
        double latCprEven = state.cprLatEven / CPR_MAX;
        double lonCprEven = state.cprLonEven / CPR_MAX;
        double latCprOdd = state.cprLatOdd / CPR_MAX;
        double lonCprOdd = state.cprLonOdd / CPR_MAX;

        // compute the latitude zone index
        int j = (int) Math.floor(59 * latCprEven - 60 * latCprOdd + 0.5);

        // compute candidate latitudes for even frame (60 zones) and odd frame (59 zones)
        double latEven = DLAT_EVEN * (mod(j, 60) + latCprEven);
        double latOdd = DLAT_ODD * (mod(j, 59) + latCprOdd);

        // wrap latitudes from [270, 360) range to [-90, 0) for southern hemisphere
        if (latEven >= 270) latEven -= 360;
        if (latOdd >= 270) latOdd -= 360;

        // both frames must be in the same NL band, otherwise the aircraft crossed
        // a latitude zone boundary and this frame pair can't produce a valid position
        int nlEven = cprNL(latEven);
        int nlOdd = cprNL(latOdd);
        if (nlEven != nlOdd) return false;

        boolean useEven = state.cprTimeEven >= state.cprTimeOdd;
        double lat, lon;

        if (useEven) {
            lat = latEven;
            int nl = cprNL(lat);
            int ni = Math.max(nl, 1);         // number of longitude zones (at least 1)
            double dlon = 360.0 / ni;         // longitude zone width in degrees
            int m = (int) Math.floor(lonCprEven * (nl - 1) - lonCprOdd * nl + 0.5);
            lon = dlon * (mod(m, ni) + lonCprEven);
        } else {
            lat = latOdd;
            int nl = cprNL(lat);
            int ni = Math.max(nl - 1, 1);     // odd frame uses NL-1 longitude zones
            double dlon = 360.0 / ni;
            int m = (int) Math.floor(lonCprEven * (nl - 1) - lonCprOdd * nl + 0.5);
            lon = dlon * (mod(m, ni) + lonCprOdd);
        }

        // wrap longitude from [180, 360) to [-180, 0)
        if (lon >= 180) lon -= 360;

        // sanity check — reject positions outside valid coordinate ranges
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180)
            return false;

        state.lat = lat;
        state.lon = lon;
        return true;
    }


    /**
     * TC 19: Decode an airborne velocity
     */
    private void decodeAirborneVelocity(AircraftState state, byte[] buf, int meOffset) {
        int subtype = buf[meOffset] & 0x07;

        if (subtype == 1 || subtype == 2) {
            int ewDir = (buf[meOffset + 1] >> 2) & 0x01;
            int ewVel = ((buf[meOffset + 1] & 0x03) << 8) | (buf[meOffset + 2] & 0xFF);
            int nsDir = (buf[meOffset + 3] >> 7) & 0x01;
            int nsVel = ((buf[meOffset + 3] & 0x7F) << 3) | ((buf[meOffset + 4] >> 5) & 0x07);

            if (ewVel > 0 && nsVel > 0) {
                double vEW = (ewVel - 1) * (subtype == 2 ? 4 : 1);
                double vNS = (nsVel - 1) * (subtype == 2 ? 4 : 1);
                if (ewDir == 1) vEW = -vEW;
                if (nsDir == 1) vNS = -vNS;

                state.groundSpeed = Math.sqrt(vEW * vEW + vNS * vNS);
                state.track = Math.toDegrees(Math.atan2(vEW, vNS));
                if (state.track < 0) state.track += 360;
            }

            int vrSign = (buf[meOffset + 4] >> 3) & 0x01;
            int vrCode = ((buf[meOffset + 4] & 0x07) << 6) | ((buf[meOffset + 5] >> 2) & 0x3F);

            if (vrCode > 0) {
                state.verticalRate = (vrCode - 1) * 64.0;
                if (vrSign == 1) state.verticalRate = -state.verticalRate;
            }

            // this offset is used to derive geometric altitude: geoAlt = baroAlt + offset
            int diffSign = (buf[meOffset + 6] >> 7) & 0x01;
            int diffCode = buf[meOffset + 6] & 0x7F;

            if (diffCode > 0) {
                double diffFt = (diffCode - 1) * 25.0;
                state.gnssBaroOffsetFt = diffSign == 0 ? diffFt : -diffFt;
                state.updateGeoAlt();
            }
        }

        state.lastUpdateTime = System.currentTimeMillis();
    }


    /**
     * Decodes a 12-bit altitude code using Q-bit encoding.
     * @param altCode the 12-bit altitude code from the ME field
     * @return altitude in feet, or NaN if the code is 0
     */
    private double decodeAC12(int altCode) {
        if (altCode == 0) return Double.NaN;

        int qBit = (altCode >> 4) & 0x01;

        if (qBit == 1) {
            int n = ((altCode & 0xFE0) >> 1) | (altCode & 0x00F);
            return n * 25.0 - 1000.0;
        }

        return Double.NaN;
    }


    /**
     * Computes the NL (Number of Longitude zones) for a given latitude.
     * @param lat latitude in degrees
     * @return the number of longitude zones at this latitude (2 at poles, up to 59 at the equator)
     */
    static int cprNL(double lat) {
        if (Math.abs(lat) >= 87.0) return 2;
        if (lat == 0) return 59;

        double tmp = 1.0 - Math.cos(Math.PI / (2.0 * NZ));
        double cosLat = Math.cos(Math.PI * Math.abs(lat) / 180.0);
        double nz = 2.0 * Math.PI / Math.acos(1.0 - tmp / (cosLat * cosLat));
        return (int) Math.floor(nz);
    }


    /**
     * returns a non-negative result
     */
    private static int mod(int a, int b) {
        return ((a % b) + b) % b;
    }
}
