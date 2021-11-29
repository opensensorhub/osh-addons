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

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;


/**
 * 
 * @author Tony Cook
 *
 */
public class AirportOutput extends AbstractSensorOutput<NavDriver>
{
    private static final int AVERAGE_SAMPLING_PERIOD = 1;

	DataRecord navStruct;
	DataEncoding encoding;

	
	public AirportOutput(NavDriver parentSensor) throws IOException
	{
		super("airports", parentSensor);
	}
    

    protected void init()
    {
        SWEHelper fac = new SWEHelper();

        // Structure is {id, name, lat, lon}

        // SWE Common data structure
        navStruct = fac.newDataRecord(4);
        navStruct.setName(getName());
        navStruct.setDefinition(SWEHelper.getPropertyUri("aero/Airport"));

        Text id = fac.newText(SWEHelper.getPropertyUri("aero/ICAO/Code"), "ICAO Code", "Airport ICAO identification code");
        navStruct.addComponent("code", id);
        Text name = fac.newText(SWEHelper.getPropertyUri("Name"), "Name", "Long name" );
        navStruct.addComponent("name", name);
        Quantity latQuant = fac.newQuantity(SWEHelper.getPropertyUri("GeodeticLatitude"), "Latitude", null, "deg", DataType.DOUBLE);
        navStruct.addComponent("lat", latQuant);
        Quantity lonQuant = fac.newQuantity(SWEHelper.getPropertyUri("Longitude"), "Longitude", null, "deg", DataType.DOUBLE);
        navStruct.addComponent("lon", lonQuant);

        // default encoding is text
        encoding = fac.newTextEncoding(",", "\n");
    }
    

	public void sendEntries(Collection<NavDbPointEntry> recs)
	{                
	    long time = System.currentTimeMillis();
        
        for (NavDbPointEntry rec: recs) {
			DataBlock dataBlock = navStruct.createDataBlock();

			dataBlock.setStringValue(0, rec.id);
			dataBlock.setStringValue(1, rec.name);
			dataBlock.setDoubleValue(2, rec.lat);
			dataBlock.setDoubleValue(3, rec.lon);
			
			// TODO send as a single ObsEvent w/ multiple IObsData
            var foiUID = NavDriver.AIRPORTS_UID_PREFIX + rec.id;
            eventHandler.publish(new DataEvent(time, AirportOutput.this, foiUID, dataBlock));
		}
	}
	

	public double getAverageSamplingPeriod()
	{
		return AVERAGE_SAMPLING_PERIOD;
	}


    @Override 
    public DataComponent getRecordDescription()
    {
        return navStruct;
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
