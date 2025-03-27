/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.Waypoint;
import org.sensorhub.utils.aero.AeroHelper;
import org.sensorhub.utils.aero.impl.AeroUtils;
import org.sensorhub.utils.aero.impl.FlightPlanRecord;
import org.vast.data.DataBlockProxy;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Data producer output for flight plan data 
 * </p>
 *
 * @author Tony Cook
 * @since Sep 5, 2017
 */
public class FlightPlanOutput extends AbstractSensorOutput<FlightAwareDriver> implements FlightPlanListener
{
    private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toMillis(15); 

    DataComponent dataStruct;
    DataEncoding encoding;
    FlightPlanRecord flightPlan;
    
    Map<String, DataBlock> latestRecords = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(24, TimeUnit.HOURS)
            .<String, DataBlock>build().asMap();


    public FlightPlanOutput(FlightAwareDriver parentSensor) 
    {
        super("flightPlan", parentSensor);
    }
    

    protected void init()
    {
        var fac = new AeroHelper();

        // data structure
        this.dataStruct = FlightPlanRecord.getSchema(getName());
        this.flightPlan = DataBlockProxy.generate(dataStruct, FlightPlanRecord.class);

        // default encoding is text
        encoding = fac.newTextEncoding(",", "\n");
    }
    

    @Override
    public synchronized void newFlightPlan(FlightObject fltObj)
    {
        long msgTime = System.currentTimeMillis();
        
        // renew datablock
        var dataBlk = latestRecord == null ?
            dataStruct.createDataBlock() : latestRecord.renew();
        flightPlan.wrap(dataBlk);
        
        // set datablock values
        flightPlan.setIssueTime(toInstant(fltObj.pitr));
        flightPlan.setSource("FA");
        flightPlan.setFlightNumber(trim(fltObj.ident));
        flightPlan.setFlightDate(toInstant(fltObj.fdt));
        flightPlan.setOriginAirport(trim(fltObj.orig));
        flightPlan.setDestinationAirport(trim(fltObj.dest));
        flightPlan.setDepartureTime(toInstant(fltObj.edt));
        flightPlan.setArrivalTime(toInstant(fltObj.eta));
        flightPlan.setTailNumber(trim(fltObj.reg));
        flightPlan.setAircraftType(trim(fltObj.aircrafttype));
        flightPlan.setCruiseAltitude(toDouble(fltObj.alt));
        flightPlan.setCruiseSpeed(toDouble(fltObj.speed));
        flightPlan.setCruiseMach(Double.NaN);
        flightPlan.setCostIndex(Double.NaN);
        flightPlan.setFuelFactor(Double.NaN);
        flightPlan.setCodedRoute(trim(fltObj.route));
        
        // decoded waypoints
        if (fltObj.decodedRoute != null)
        {
            for (Waypoint wpt: fltObj.decodedRoute)
            {
                var fpWpt = flightPlan.addWaypoint();
                fpWpt.setCode(wpt.name);
                fpWpt.setLatitude(wpt.latitude);
                fpWpt.setLongitude(wpt.longitude);
                fpWpt.setBaroAltitude(wpt.altitude);
            }
        }
        
        // create FOI if needed
        var flightDate = LocalDate.ofInstant(flightPlan.getFlightDate(), ZoneOffset.UTC);
        var flightId = AeroUtils.getFlightID(
            flightPlan.getFlightNumber(),
            flightPlan.getDestinationAirport(),
            flightDate);
        String foiUid = AeroUtils.ensureFlightFoi(getParentProducer(), flightId);
        
        // skip if same as last record for a given foi
        if (isDuplicate(flightId, dataBlk))
            return;
        
        // update latest record and send event
        latestRecord = dataBlk;
        latestRecordTime = msgTime;
        eventHandler.publish(new DataEvent(latestRecordTime, this, foiUid, dataBlk));
	}
    
    
    protected String trim(String val)
    {
        return (val == null) ? val : val.trim();
    }
    
    
    protected Instant toInstant(String val)
    {
        if (Strings.isNullOrEmpty(val))
            return null;
        
        long epochSeconds = Long.parseLong(val);
        return Instant.ofEpochSecond(epochSeconds);
    }
    
    
    protected double toDouble(String val)
    {
        if (Strings.isNullOrEmpty(val))
            return Double.NaN;
        
        return Double.parseDouble(val);
    }
	
	
	protected boolean isDuplicate(String flightId, DataBlock newRec)
	{
	    DataBlock oldRec = latestRecords.get(flightId);
        latestRecords.put(flightId, newRec);
	    
	    // we're sure it's not duplicate if we never received anything
	    // or if the data blocks have different sizes
	    if (oldRec == null || oldRec.getAtomCount() != newRec.getAtomCount())
	        return false;
	    
	    // compare all fields except the first (issue time)
	    // because it's always set to current time
	    for (int i=1; i<newRec.getAtomCount(); i++)
	    {
	        String oldVal = oldRec.getStringValue(i);
	        String newVal = newRec.getStringValue(i);	        
	        if (oldVal != null && !oldVal.equals(newVal))
	            return false;	        
	    }
	    
	    parentSensor.getLogger().debug("Duplicate flight plan received for flight {}", flightId);
	    return true;
	}



    public double getAverageSamplingPeriod()
    {
        return AVERAGE_SAMPLING_PERIOD;
    }


    @Override 
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return encoding;
	}

}