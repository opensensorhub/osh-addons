/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;

import java.io.IOException;
import java.util.Collection;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.utils.aero.AeroHelper;
import org.sensorhub.utils.aero.INavDatabase.INavDbWaypoint;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;


/**
 * 
 * @author Tony Cook
 *
 */
public class WaypointOutput extends AbstractSensorOutput<NavDriver>
{
    private static final int AVERAGE_SAMPLING_PERIOD = 1;

	DataRecord dataStruct;
	DataEncoding encoding;

	
	public WaypointOutput(NavDriver parentSensor) throws IOException
	{
		super("waypoints", parentSensor);
	}


    protected void init()
    {
        var fac = new AeroHelper();

        // SWE Common data structure
        dataStruct = fac.createRecord()
            .name(getName())
            .definition(AeroHelper.AERO_RECORD_URI_PREFIX + "Waypoint")
            .addField("code", fac.createWaypointCode())
            .addField("name", fac.createText()
                .definition(SWEHelper.getPropertyUri("EntityName"))
                .label("Long Name"))
            .addField("lat", fac.createLatitude())
            .addField("lon", fac.createLongitude())
            .build();

        // default encoding is text
        encoding = fac.newTextEncoding(",", "\n");
    }

    public void start() throws SensorHubException {
        // Nothing to do 
    }
	

	public void sendEntries(Collection<INavDbWaypoint> recs)
	{
	    long time = System.currentTimeMillis();
        
        for (var rec: recs) {
            DataBlock dataBlock = dataStruct.createDataBlock();

            dataBlock.setStringValue(0, rec.getCode());
            dataBlock.setStringValue(1, rec.getName());
            dataBlock.setDoubleValue(2, rec.getLatitude());
            dataBlock.setDoubleValue(3, rec.getLongitude());
			
			// TODO send as a single ObsEvent w/ multiple IObsData
			eventHandler.publish(new DataEvent(time, WaypointOutput.this, dataBlock));
		}
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
    
    
    /*
     * Override so SOS service never times out
     */
    @Override
    public long getLatestRecordTime()
    {
        return System.currentTimeMillis();
    }

}
