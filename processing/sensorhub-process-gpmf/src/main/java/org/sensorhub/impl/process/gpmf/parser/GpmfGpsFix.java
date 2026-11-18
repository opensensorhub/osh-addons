/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.process.gpmf.parser;

/**
 * One GPS fix decoded from a GPMF payload.
 * <p>
 * The altitude is whatever the camera reported, without any datum conversion. Cameras through the HERO7
 * report height above the WGS84 ellipsoid, while the HERO8 and newer report mean sea level, so the caller
 * has to say which datum applies to the camera it is reading.
 *
 * @param latitude       Latitude in degrees.
 * @param longitude      Longitude in degrees.
 * @param altitude       Altitude in metres, on the camera's own datum.
 * @param speed2D        Ground speed in metres per second, or NaN if the camera did not report one.
 * @param speed3D        Speed through the air in metres per second, or NaN if the camera did not report one.
 * @param fixType        GPS fix quality: 0 for no fix, 2 for a 2D fix, 3 for a 3D fix, or -1 if unreported.
 * @param dop            Dilution of precision, or NaN if the camera did not report one.
 * @param utcTime        Time of the fix in seconds since the epoch, or NaN if the camera did not report one.
 * @param altitudeSystem The four-character altitude system the camera declared, {@code MSLV} for mean sea
 *                       level or {@code GEOD} for the ellipsoid, or null if it declared none.
 */
public record GpmfGpsFix(
        double latitude,
        double longitude,
        double altitude,
        double speed2D,
        double speed3D,
        int fixType,
        double dop,
        double utcTime,
        String altitudeSystem) {

    /**
     * Reported when a camera does not include fix quality with its coordinates.
     */
    public static final int FIX_UNKNOWN = -1;

    /**
     * Fix quality reported when the receiver has no lock. The coordinates of such a fix are meaningless.
     */
    public static final int FIX_NONE = 0;

    /**
     * @return True if the receiver had a lock, so the coordinates describe a real position. A fix whose
     * quality the camera did not report is taken at face value, since there is nothing to judge it by.
     */
    public boolean hasLock() {
        return fixType != FIX_NONE;
    }
}