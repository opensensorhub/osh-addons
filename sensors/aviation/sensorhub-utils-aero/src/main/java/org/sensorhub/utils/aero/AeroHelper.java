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

import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.swe.SWEBuilders.CategoryBuilder;
import org.vast.swe.SWEBuilders.QuantityBuilder;
import org.vast.swe.SWEBuilders.TextBuilder;
import org.vast.swe.SWEBuilders.TimeBuilder;
import org.vast.swe.SWEBuilders.VectorBuilder;
import net.opengis.swe.v20.DataType;


/**
 * <p>
 * Helpers for creating SWE records for aviation data feeds
 * </p>
 *
 * @author Alex Robin
 * @since Jan 20, 2025
 */
public class AeroHelper extends GeoPosHelper
{
    // SWE definition URIs
    public static final String AERO_DEF_URI_PREFIX = "urn:osh:def:aero:";
    public static final String AERO_RECORD_URI_PREFIX = "urn:osh:def:aero:record:";
    
    public static final String ICAO_CODESPACE = AERO_DEF_URI_PREFIX + "ICAO";
    public static final String DEF_TAIL_NUMBER = AERO_DEF_URI_PREFIX + "TailNumber";
    public static final String DEF_AC_TYPE = AERO_DEF_URI_PREFIX + "AircraftType";
    public static final String DEF_FLIGHT_NUMBER = AERO_DEF_URI_PREFIX + "FlightNumber";
    public static final String DEF_CALLSIGN = AERO_DEF_URI_PREFIX + "Callsign";
    public static final String DEF_FLIGHT_ID = AERO_DEF_URI_PREFIX + "FlightID";
    public static final String DEF_AIRPORT_CODE = AERO_DEF_URI_PREFIX + "AirportCode";
    public static final String DEF_ORIGIN_AIRPORT = AERO_DEF_URI_PREFIX + "OriginAirport";
    public static final String DEF_DESTINATION_AIRPORT = AERO_DEF_URI_PREFIX + "DestinationAirport";
    public static final String DEF_ALTERNATE_AIRPORTS = AERO_DEF_URI_PREFIX + "AlternateAirports";
    public static final String DEF_FLIGHT_LEVEL = AERO_DEF_URI_PREFIX + "FlightLevel";
    public static final String DEF_FLIGHT_DATE = AERO_DEF_URI_PREFIX + "FlightDate";
    public static final String DEF_DEPARTURE_TIME = AERO_DEF_URI_PREFIX + "DepartureTime";
    public static final String DEF_ARRIVAL_TIME = AERO_DEF_URI_PREFIX + "ArrivalTime";
    public static final String DEF_COST_INDEX = AERO_DEF_URI_PREFIX + "CostIndex";
    public static final String DEF_FF_FACTOR = AERO_DEF_URI_PREFIX + "FuelCorrectionFactor";
    
    public static final String DEF_TRACKANGLE_TRUE = AERO_DEF_URI_PREFIX + "TrueTrack";
    public static final String DEF_TRACKANGLE_MAGNETIC = AERO_DEF_URI_PREFIX + "MagneticTrack";
    public static final String DEF_GROUNDSPEED = SWEHelper.getPropertyUri("GroundSpeed");
    public static final String DEF_ALT_RATE = AERO_DEF_URI_PREFIX + "AltitudeRate";
    public static final String DEF_TRUE_AIRSPEED = AERO_DEF_URI_PREFIX + "TrueAirspeed";
    public static final String DEF_CALIBRATED_AIRSPEED = AERO_DEF_URI_PREFIX + "CalibratedAirspeed";
    public static final String DEF_INDICATED_AIRSPEED = AERO_DEF_URI_PREFIX + "IndicatedAirspeed";
    public static final String DEF_MACH_NUMBER = AERO_DEF_URI_PREFIX + "MachNumber";
    public static final String DEF_STATIC_AIRTEMP = AERO_DEF_URI_PREFIX + "StaticAirTemperature";
    public static final String DEF_TOTAL_AIRTEMP = AERO_DEF_URI_PREFIX + "TotalAirTemperature";
    public static final String DEF_STATIC_AIRPRESS = AERO_DEF_URI_PREFIX + "StaticAirPressure";
    public static final String DEF_TOTAL_AIRPRESS = AERO_DEF_URI_PREFIX + "TotalAirPressure";
    public static final String DEF_GROSS_WEIGHT = AERO_DEF_URI_PREFIX + "GrossWeight";
    public static final String DEF_ZERO_FUEL_WEIGHT = AERO_DEF_URI_PREFIX + "ZeroFuelWeight";
    public static final String DEF_FUEL_ON_BOARD = AERO_DEF_URI_PREFIX + "FuelOnBoard";
    public static final String DEF_WIND_SPEED = SWEHelper.getCfUri("wind_speed");
    public static final String DEF_WIND_DIR = SWEHelper.getCfUri("wind_to_direction");

    public static final String DEF_CODED_ROUTE = AERO_DEF_URI_PREFIX + "CodedRoute";
    public static final String DEF_WAYPOINT_TYPE = AERO_DEF_URI_PREFIX + "WaypointType";
    public static final String DEF_WAYPOINT_CODE = AERO_DEF_URI_PREFIX + "WaypointCode";
    
    public static final String DEF_MSG_TEXT = AERO_DEF_URI_PREFIX + "RawMessage";
    public static final String DEF_MSG_SRC = AERO_DEF_URI_PREFIX + "MessageSource";
    public static final String DEF_MSG_ID = AERO_DEF_URI_PREFIX + "MessageID";
    
    
    /**
     * @return The tail number field
     */
    public TextBuilder createTailNumber()
    {
        return createText()
            .definition(DEF_TAIL_NUMBER)
            .label("Tail Number");
    }
    
    
    /**
     * @return The aircraft type field
     */
    public CategoryBuilder createAircraftType()
    {
        return createCategory()
            .definition(DEF_AC_TYPE)
            .label("Aircraft Type")
            .codeSpace(ICAO_CODESPACE);
    }
    
    
    /**
     * @return The fuel correction factor field
     */
    public QuantityBuilder createFuelFactor()
    {
        return createQuantity()
            .definition(DEF_FF_FACTOR)
            .label("Fuel Correction Factor")
            .uom("1");
    }
    
    
    /**
     * @return The flight number field
     */
    public TextBuilder createFlightNumber()
    {
        return createText()
            .definition(DEF_FLIGHT_NUMBER)
            .label("Flight Number");
    }
    
    
    /**
     * @return The callsign field
     */
    public TextBuilder createCallSign()
    {
        return createText()
            .definition(DEF_CALLSIGN)
            .label("Callsign");
    }
    
    
    /**
     * @return The flight identifier field (this is usually composed of flight number, destination airport and time stamp)
     */
    public TextBuilder createFlightID()
    {
        return createText()
            .definition(DEF_FLIGHT_ID)
            .label("Flight ID")
            .description("Fully unique flight ID, composed of flight number, destination airport code and flight date");
    }
    
    
    /**
     * @return The flight date field (local time at departure airport)
     */
    public TextBuilder createFlightDate()
    {
        return createText()
            .definition(DEF_FLIGHT_DATE)
            .label("Flight Date")
            .description("Original departure date (local time at departure airport)");
    }
    
    
    /**
     * @return The departure time field
     */
    public TimeBuilder createIssueTime()
    {
        return createTime()
            .asSamplingTimeIsoUTC()
            .definition(SWEConstants.DEF_SAMPLING_TIME)
            .label("Issue Time");
    }
    
    
    /**
     * @return The departure time field
     */
    public TimeBuilder createDepartureTime()
    {
        return createTime()
            .asSamplingTimeIsoUTC()
            .definition(DEF_DEPARTURE_TIME)
            .label("Departure Time");
    }
    
    
    /**
     * @return The arrival time field
     */
    public TimeBuilder createArrivalTime()
    {
        return createTime()
            .asSamplingTimeIsoUTC()
            .definition(DEF_ARRIVAL_TIME)
            .label("Arrival Time");
    }
    
    
    /**
     * @return The cost index field
     */
    public QuantityBuilder createCostIndex()
    {
        return createQuantity()
            .definition(DEF_COST_INDEX)
            .label("Cost Index")
            .uom("[lb_av]/h");
    }
    
    
    /**
     * @return The airport code field
     */
    public CategoryBuilder createAirportCode()
    {
        return createCategory()
            .definition(DEF_AIRPORT_CODE)
            .label("Airport Code")
            .description("4-letters ICAO airport code")
            .codeSpace(ICAO_CODESPACE);
    }
    
    
    /**
     * @return The origin airport field
     */
    public CategoryBuilder createOriginAirport()
    {
        return createCategory()
            .definition(DEF_ORIGIN_AIRPORT)
            .label("Origin Airport")
            .description("ICAO code of departure airport")
            .codeSpace(ICAO_CODESPACE);
    }
    
    
    /**
     * @return The destination airport field
     */
    public CategoryBuilder createDestinationAirport()
    {
        return createCategory()
            .definition(DEF_DESTINATION_AIRPORT)
            .label("Destination Airport")
            .description("ICAO code of arrival airport")
            .codeSpace(ICAO_CODESPACE);
    }
    
    
    /**
     * @return The alternate airports field
     */
    public TextBuilder createAlternateAirports()
    {
        return createText()
            .definition(DEF_ALTERNATE_AIRPORTS)
            .label("Alternate Airports")
            .description("Comma separated list of alternate airports ICAO codes");
    }
    
    
    /**
     * @return The aircraft position field
     */
    public VectorBuilder createAircraftLocation()
    {
        return createLocationVectorLatLon()
            .label("Aircraft Location")
            .description("Horizontal position of the aircraft in WGS84 coordinates");
    }
    
    
    /**
     * @return The GNSS altitude field in ft
     */
    public QuantityBuilder createGnssAlt()
    {
        return createAltitudeWGS84()
            .label("GNSS Altitude")
            .uom("[ft_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The geometric altitude field in ft
     */
    public QuantityBuilder createGeomAlt()
    {
        return createAltitudeMSL()
            .label("Geometric Altitude")
            .uom("[ft_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The barometric/pressure altitude field in ft
     */
    public QuantityBuilder createBaroAlt()
    {
        return createQuantity()
            .definition(DEF_ALTITUDE_BAROMETRIC)
            .label("Barometric Altitude")
            .uom("[ft_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The true track angle field in degrees
     */
    public QuantityBuilder createTrueTrack()
    {
        return createQuantity()
            .definition(DEF_TRACKANGLE_TRUE)
            .refFrame(SWEConstants.REF_FRAME_NED)
            .label("True Track")
            .description("Track angle from true north, measured clockwise")
            .uom("deg")
            .axisId("Z")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The true heading field in degrees
     */
    public QuantityBuilder createTrueHeading()
    {
        return createQuantity()
            .definition(DEF_HEADING_TRUE)
            .refFrame(SWEConstants.REF_FRAME_NED)
            .label("True Heading")
            .description("Heading angle from true north, measured clockwise")
            .uom("deg")
            .axisId("Z")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The magnetic heading field in degrees
     */
    public QuantityBuilder createMagneticHeading()
    {
        return createQuantity()
            .definition(DEF_HEADING_MAGNETIC)
            .refFrame(SWEConstants.REF_FRAME_NED)
            .label("Magnetic Heading")
            .description("Heading angle from magnetic north, measured clockwise")
            .uom("deg")
            .axisId("Z")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The ground speed field in knots
     */
    public QuantityBuilder createGroundSpeed()
    {
        return createQuantity()
            .definition(DEF_GROUNDSPEED)
            .label("Ground Speed")
            .uom("[kn_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The vertical rate field in ft/min
     */
    public QuantityBuilder createVerticalRate()
    {
        return createQuantity()
            .definition(DEF_ALT_RATE)
            .label("Vertical Rate")
            .uom("[ft_i]/min")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The true airspeed (TAS) field in knots
     */
    public QuantityBuilder createTrueAirspeed()
    {
        return createQuantity()
            .definition(DEF_TRUE_AIRSPEED)
            .label("True Airspeed")
            .uom("[kn_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The calibrated airspeed (CAS) field in knots
     */
    public QuantityBuilder createCalibratedAirspeed()
    {
        return createQuantity()
            .definition(DEF_CALIBRATED_AIRSPEED)
            .label("Calibrated Airspeed")
            .uom("[kn_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The indicated airspeed (IAS) field in knots
     */
    public QuantityBuilder createIndicatedAirspeed()
    {
        return createQuantity()
            .definition(DEF_CALIBRATED_AIRSPEED)
            .label("Indicated Airspeed")
            .uom("[kn_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The mach number field
     */
    public QuantityBuilder createMachNumber()
    {
        return createQuantity()
            .definition(DEF_MACH_NUMBER)
            .label("Mach Number")
            .uom("1")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The static air temperature field in degC
     */
    public QuantityBuilder createStaticAirTemp()
    {
        return createQuantity()
            .definition(DEF_STATIC_AIRTEMP)
            .label("Static Air Temperature")
            .uom("Cel")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The total air temperature field in degC
     */
    public QuantityBuilder createTotalAirTemp()
    {
        return createQuantity()
            .definition(DEF_TOTAL_AIRTEMP)
            .label("Total Air Temperature")
            .uom("Cel")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The static air pressure field in Pa
     */
    public QuantityBuilder createStaticAirPress()
    {
        return createQuantity()
            .definition(DEF_STATIC_AIRTEMP)
            .label("Static Air Pressure")
            .uom("hPa")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The total air pressure field in Pa
     */
    public QuantityBuilder createTotalAirPress()
    {
        return createQuantity()
            .definition(DEF_TOTAL_AIRTEMP)
            .label("Total Air Pressure")
            .uom("hPa")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The gross weight field in lb
     */
    public QuantityBuilder createGrossWeight()
    {
        return createQuantity()
            .definition(DEF_GROSS_WEIGHT)
            .label("Gross Weight")
            .uom("[lb_av]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The zero fuel weight field in lb
     */
    public QuantityBuilder createZeroFuelWeight()
    {
        return createQuantity()
            .definition(DEF_ZERO_FUEL_WEIGHT)
            .label("Zero Fuel Weight")
            .uom("[lb_av]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The fuel on board field in lb
     */
    public QuantityBuilder createFuelOnBoard()
    {
        return createQuantity()
            .definition(DEF_FUEL_ON_BOARD)
            .label("Fuel on Board")
            .uom("[lb_av]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The wind speed field in knot
     */
    public QuantityBuilder createWindSpeed()
    {
        return createQuantity()
            .definition(DEF_WIND_SPEED)
            .label("Wind Speed")
            .uom("[kn_i]")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The wind direction field in deg
     */
    public QuantityBuilder createWindDir()
    {
        return createQuantity()
            .definition(DEF_WIND_DIR)
            .label("Wind Direction")
            .description("Direction the wind is blowing to, measured clockwise from true north")
            .refFrame(SWEConstants.REF_FRAME_NED)
            .axisId("Z")
            .uom("deg")
            .dataType(DataType.FLOAT);
    }
    
    
    /**
     * @return The waypoint code field
     */
    public CategoryBuilder createWaypointCode()
    {
        return createCategory()
            .definition(DEF_WAYPOINT_CODE)
            .label("Waypoint Code")
            .description("4-letters ICAO waypoint code")
            .codeSpace(ICAO_CODESPACE);
    }
    
    
    /**
     * @return The waypoint code field
     */
    public TextBuilder createCodedRoute()
    {
        return createText()
            .definition(DEF_CODED_ROUTE)
            .label("Coded Route");
    }
    
    
    /**
     * @return The message ID field
     */
    public TextBuilder createMessageID()
    {
        return createText()
            .definition(DEF_MSG_ID)
            .label("Message ID");
    }
    
    
    /**
     * @return The message source field
     */
    public TextBuilder createMessageSource()
    {
        return createText()
            .definition(DEF_MSG_SRC)
            .label("Message Source");
    }
    
    
    /**
     * @return The raw message text field
     */
    public TextBuilder createRawMessageText()
    {
        return createText()
            .definition(DEF_MSG_TEXT)
            .label("Raw Message");
    }
}
