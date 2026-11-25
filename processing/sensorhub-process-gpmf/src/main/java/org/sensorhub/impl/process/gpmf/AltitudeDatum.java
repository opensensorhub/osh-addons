/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.process.gpmf;

/**
 * The reference surface a camera's GPS altitude readings are measured from.
 * <p>
 * GoPro cameras disagree on this and the payload does not record which was used. Cameras through the
 * HERO7 report height above the WGS84 ellipsoid, while the HERO8, MAX, and newer report height above mean
 * sea level. The two differ by the local geoid offset, which reaches roughly a hundred metres in places,
 * so the process has to be told which applies in order to describe its position records correctly.
 * <p>
 * The process publishes the camera's readings unchanged either way; this setting decides only which
 * coordinate reference system the position output declares.
 */
public enum AltitudeDatum {
    /**
     * Height above mean sea level, as reported by the HERO8, MAX, and newer.
     */
    MEAN_SEA_LEVEL("mean sea level"),

    /**
     * Height above the WGS84 ellipsoid, as reported by the HERO7 and earlier.
     */
    WGS84_ELLIPSOID("WGS84 ellipsoid");

    private final String label;

    AltitudeDatum(String label) {
        this.label = label;
    }

    /**
     * @return A human-readable name for this datum, used in output descriptions.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Resolves the datum a camera declared in the {@code GPSA} entry of its GPS container.
     * <p>
     * Newer models record this, which settles the question for them; older ones do not, which is why the
     * process still has a configured parameter.
     *
     * @param altitudeSystem The four-character system name from {@code GPSA}, or null if absent.
     * @return The matching datum, or null if none was declared or the name is not one we recognize.
     */
    public static AltitudeDatum fromGpsAltitudeSystem(String altitudeSystem) {
        if (altitudeSystem == null) {
            return null;
        }

        return switch (altitudeSystem.trim().toUpperCase()) {
            case "MSLV" -> MEAN_SEA_LEVEL;
            case "GEOD" -> WGS84_ELLIPSOID;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}