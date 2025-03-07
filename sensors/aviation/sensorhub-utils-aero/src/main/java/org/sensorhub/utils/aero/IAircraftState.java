/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero;

import java.time.Instant;


/**
 * <p>
 * Read-only interface for aircraft state.<br/>
 * This is used for ownship as well as other aircraft.<br/>
 * Numeric values will be NaN if unknown.
 * </p>
 *
 * @author Alex Robin
 * @since Jan 27, 2025
 */
public interface IAircraftState extends IAircraftIdentification
{

    /**
     * @return Valid time of state vector
     */
    Instant getTime();


    /**
     * @return Aircraft Position, Geodetic Latitude (deg, WGS84)
     */
    double getLatitude();



    /**
     * @return Aircraft Position, Longitude (deg, WGS84)
     */
    double getLongitude();


    /**
     * @return Aircraft Position, Geometric Altitude (ft, WGS84)
     */
    double getGnssAltitude();


    /**
     * @return Barometric Altitude (ft, pressure altitude, QNE or QNH depending on alt setting)
     */
    double getBaroAltitude();


    /**
     * @return Pressure Altimeter Setting (hPa)
     */
    double getBaroAltSetting();


    /**
     * @return True Track Angle (deg from true north, clockwise)
     */
    double getTrueTrack();


    /**
     * @return Magnetic Track Angle (deg from magnetic north, clockwise)
     */
    double getMagneticTrack();


    /**
     * @return True Heading (deg from true north, clockwise)
     */
    double getTrueHeading();


    /**
     * @return Magnetic Heading (deg from magnetic north, clockwise)
     */
    double getMagneticHeading();


    /**
     * @return Ground Speed (GS, knot)
     */
    double getGroundSpeed();


    /**
     * @return Vertical Rate (ft/s)
     */
    double getVerticalRate();


    /**
     * @return True AirSpeed (TAS, knot)
     */
    double getTrueAirSpeed();


    /**
     * @return Calibrated AirSpeed (CAS, knot)
     */
    double getCalibratedAirSpeed();


    /**
     * @return Mach Number
     */
    double getMach();


    /**
     * @return Static Air Temperature (SAT, degC)
     */
    double getStaticAirTemperature();


    /**
     * @return Static Air Pressure (hPa)
     */
    double getStaticAirPressure();


    /**
     * @return Zero Fuel Weight (lbs)
     */
    double getZeroFuelWeight();


    /**
     * @return Fuel On Board (lbs)
     */
    double getFuelOnBoard();
    
}
