/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2025 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils.aero.impl;

import java.time.Instant;
import java.util.Collection;
import org.sensorhub.utils.aero.AeroHelper;
import org.sensorhub.utils.aero.IFlightPlan;
import org.vast.data.IDataAccessor;
import org.vast.swe.SWEConstants;
import net.opengis.swe.v20.DataRecord;


/**
 * <p>
 * Accessor interface for Flight Plan records.
 * </p>
 *
 * @author Alex Robin
 * @since Jan 30, 2025
 */
public interface FlightPlanRecord extends IDataAccessor, IFlightPlan
{
    public static final String DEF_FP_REC = AeroHelper.AERO_RECORD_URI_PREFIX + "FlightPlan";
    
    
    public static DataRecord getSchema(String name)
    {
        AeroHelper fac = new AeroHelper();
        var numWptsCompId = "NUM_POINTS";
        
        return fac.createRecord()
            .name(name)
            .definition(DEF_FP_REC) 
            .addField("time", fac.createIssueTime())
            .addField("source", fac.createMessageSource()
                .addAllowedValues(FlightPlanSource.class))
            .addField("flightNum", fac.createFlightNumber())
            .addField("flightDate", fac.createFlightDate())
            .addField("origAirport", fac.createOriginAirport())
            .addField("destAirport", fac.createDestinationAirport())
            .addField("altAirports", fac.createAlternateAirports())
            .addField("departTime", fac.createDepartureTime())
            .addField("arrivalTime", fac.createArrivalTime())
            .addField("tailNum", fac.createTailNumber())
            .addField("acType", fac.createAircraftType())
            .addField("cruiseAlt", fac.createBaroAlt()
                .label("Cruise Altitude"))
            .addField("cruiseSpeed", fac.createTrueAirspeed()
                .label("Cruise Speed"))
            .addField("cruiseMach", fac.createMachNumber()
                .label("Cruise Mach"))
            .addField("costIndex", fac.createCostIndex())
            .addField("fuelFactor", fac.createFuelFactor())
            .addField("codedRoute", fac.createCodedRoute())
            .addField("numWpts", fac.createCount()
                .definition(SWEConstants.DEF_NUM_POINTS)
                .id(numWptsCompId)
                .label("Number of Waypoints"))
            .addField("waypoints", fac.createArray()
                .withVariableSize(numWptsCompId)
                .withElement("wpt", WaypointRecord.getSchema("")))
            .build();
    }
    
    
    @Override
    @SweMapping(path="time")
    Instant getIssueTime();

    @SweMapping(path="time")
    void setIssueTime(Instant val);
    
    @Override
    @SweMapping(path="source")
    String getSource();

    @SweMapping(path="source")
    void setSource(String val);
    
    @Override
    @SweMapping(path="flightNum")
    String getFlightNumber();

    @SweMapping(path="flightNum")
    void setFlightNumber(String val);
    
    @Override
    @SweMapping(path="flightDate")
    Instant getFlightDate();

    @SweMapping(path="flightDate")
    void setFlightDate(Instant val);
    
    @Override
    @SweMapping(path="origAirport")
    String getOriginAirport();

    @SweMapping(path="origAirport")
    void setOriginAirport(String val);
    
    @Override
    @SweMapping(path="destAirport")
    String getDestinationAirport();

    @SweMapping(path="destAirport")
    void setDestinationAirport(String val);
    
    @Override
    @SweMapping(path="altAirports")
    String getAlternateAirports();

    @SweMapping(path="altAirports")
    void setAlternateAirports(String val);
    
    @Override
    @SweMapping(path="departTime")
    Instant getDepartureTime();

    @SweMapping(path="departTime")
    void setDepartureTime(Instant val);
    
    @Override
    @SweMapping(path="arrivalTime")
    Instant getArrivalTime();

    @SweMapping(path="arrivalTime")
    void setArrivalTime(Instant val);
    
    @Override
    @SweMapping(path="tailNum")
    String getTailNumber();

    @SweMapping(path="tailNum")
    void setTailNumber(String val);
    
    @Override
    @SweMapping(path="acType")
    String getAircraftType();

    @SweMapping(path="acType")
    void setAircraftType(String val);
    
    @Override
    @SweMapping(path="cruiseAlt")
    double getCruiseAltitude();

    @SweMapping(path="cruiseAlt")
    void setCruiseAltitude(double val);
    
    @Override
    @SweMapping(path="cruiseSpeed")
    double getCruiseSpeed();

    @SweMapping(path="cruiseSpeed")
    void setCruiseSpeed(double val);
    
    @Override
    @SweMapping(path="cruiseMach")
    double getCruiseMach();

    @SweMapping(path="cruiseMach")
    void setCruiseMach(double val);
    
    @Override
    @SweMapping(path="costIndex")
    double getCostIndex();

    @SweMapping(path="costIndex")
    void setCostIndex(double val);
    
    @Override
    @SweMapping(path="sarFac")
    double getSARFactor();

    @SweMapping(path="sarFac")
    void setSARFactor(double val);
    
    @Override
    @SweMapping(path="codedRoute")
    String getCodedRoute();

    @SweMapping(path="codedRoute")
    void setCodedRoute(String val);
    
    @Override
    @SweMapping(path="waypoints")
    Collection<WaypointRecord> getWaypoints();
    
    @SweMapping(path="waypoints")
    WaypointRecord addWaypoint();
    
}
