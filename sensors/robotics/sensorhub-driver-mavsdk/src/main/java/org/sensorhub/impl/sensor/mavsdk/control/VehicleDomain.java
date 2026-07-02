/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is GeoRobotix Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2026 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mavsdk.control;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * High-level classification of the connected vehicle, derived from the autopilot's
 * MAVLink HEARTBEAT (MAV_TYPE). Controls use this to refuse commands that don't make
 * sense for the connected platform (e.g. takeoff sent to a rover, or a ground "drive"
 * command sent to a multirotor).
 * </p>
 *
 * <p>
 * Note on ArduPilot: ground rovers and surface boats both run the <b>ArduRover</b>
 * firmware (a boat is Rover with FRAME_CLASS=2), so they share the same MAVSDK command
 * surface. They are split into GROUND and SURFACE here only for reporting; both are
 * treated identically by the "surface-bound" command set.
 * </p>
 *
 * <p>
 * Guard philosophy is <i>fail-open on UNKNOWN, fail-closed on a known mismatch</i>: a
 * command is only blocked when we positively know the vehicle is of the wrong class.
 * If the heartbeat hasn't been parsed yet (UNKNOWN), commands are allowed so SITL/bring-up
 * isn't bricked by a missed heartbeat. Flip the {@code permits*} methods if you'd rather
 * fail closed.
 * </p>
 *
 * @since Jul 2025
 */
public enum VehicleDomain
{
    AIR,
    GROUND,
    SURFACE,
    SUBMARINE,
    UNKNOWN;

    /**
     * @return true if it is safe to send a flight-only command (takeoff, land, etc.).
     *         Only false when we positively know the vehicle is ground/surface/sub.
     */
    public boolean permitsFlightCommand() {
        return this != GROUND && this != SURFACE && this != SUBMARINE;
    }

    /**
     * @return true if it is safe to send a ground/surface "drive" command.
     *         Only false when we positively know the vehicle is airborne.
     */
    public boolean permitsSurfaceCommand() {
        return this != AIR;
    }

    /**
     * Classify a vehicle from the JSON field payload of a HEARTBEAT message as provided
     * by the MavlinkDirect plugin. The "type" field may be serialized either as a numeric
     * MAV_TYPE (e.g. {@code "type":10}) or as an enum name (e.g.
     * {@code "type":"MAV_TYPE_GROUND_ROVER"}) depending on the MAVSDK/libmav build, so
     * both forms are handled.
     *
     * @param fieldsJson the HEARTBEAT fields as JSON
     * @return the classified domain, or UNKNOWN if it can't be determined / should be ignored
     */
    public static VehicleDomain fromHeartbeat(String fieldsJson) {

        if (fieldsJson == null)
            return UNKNOWN;

        String type = extractJsonValue(fieldsJson, "type");
        if (type == null)
            return UNKNOWN;

        // Numeric MAV_TYPE form
        try {
            return fromMavType(Integer.parseInt(type.trim()));
        } catch (NumberFormatException notANumber) {
            // Enum-name form
            String t = type.toUpperCase();

            if (t.contains("GROUND_ROVER")) return GROUND;
            if (t.contains("SURFACE_BOAT")) return SURFACE;
            if (t.contains("SUBMARINE"))    return SUBMARINE;

            // Components / non-vehicles we should ignore rather than misclassify
            if (t.contains("GCS") || t.contains("ANTENNA_TRACKER")
                    || t.contains("ONBOARD_CONTROLLER") || t.contains("ADSB")
                    || t.contains("GIMBAL") || t.contains("CAMERA")
                    || t.contains("GENERIC"))
                return UNKNOWN;

            if (t.contains("ROTOR") || t.contains("COPTER") || t.contains("WING")
                    || t.contains("HELICOPTER") || t.contains("VTOL")
                    || t.contains("AIRSHIP") || t.contains("BALLOON")
                    || t.contains("ROCKET") || t.contains("KITE")
                    || t.contains("PARAFOIL"))
                return AIR;

            return UNKNOWN;
        }
    }

    /**
     * Map a numeric MAV_TYPE to a domain. Types that are components or that we don't
     * recognize map to UNKNOWN (callers should not overwrite a known domain with UNKNOWN).
     */
    public static VehicleDomain fromMavType(int mavType) {

        switch (mavType) {

            case 10: return GROUND;     // MAV_TYPE_GROUND_ROVER
            case 11: return SURFACE;    // MAV_TYPE_SURFACE_BOAT
            case 12: return SUBMARINE;  // MAV_TYPE_SUBMARINE

            // Airborne types
            case 1:   // FIXED_WING
            case 2:   // QUADROTOR
            case 3:   // COAXIAL
            case 4:   // HELICOPTER
            case 7:   // AIRSHIP
            case 8:   // FREE_BALLOON
            case 9:   // ROCKET
            case 13:  // HEXAROTOR
            case 14:  // OCTOROTOR
            case 15:  // TRICOPTER
            case 16:  // FLAPPING_WING
            case 17:  // KITE
            case 19:  // VTOL_TAILSITTER_DUOROTOR
            case 20:  // VTOL_TAILSITTER_QUADROTOR
            case 21:  // VTOL_TILTROTOR
            case 22:  // VTOL_FIXEDROTOR
            case 23:  // VTOL_TAILSITTER
            case 24:  // VTOL_TILTWING
            case 25:  // VTOL_RESERVED5
            case 28:  // PARAFOIL
            case 29:  // DODECAROTOR
                return AIR;

            // 0 GENERIC, 5 ANTENNA_TRACKER, 6 GCS, 18 ONBOARD_CONTROLLER,
            // 26 GIMBAL, 27 ADSB, 30 CAMERA, ... -> ignore
            default:
                return UNKNOWN;
        }
    }

    /**
     * Pull a single scalar value (number or quoted string) out of a flat JSON object.
     * Sufficient for HEARTBEAT, which is a flat record.
     */
    private static String extractJsonValue(String json, String key) {
        Matcher m = Pattern
                .compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"?([A-Za-z0-9_\\-]+)\"?")
                .matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
