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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataInterface;
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
public class NavaidOutput extends AbstractSensorOutput<NavDriver> implements IMultiSourceDataInterface 
{
	private static final int AVERAGE_SAMPLING_PERIOD = 1;

	DataRecord navStruct;
	DataEncoding encoding;	
	Map<String, DataBlock> globalRecords = new TreeMap<>();  // key is navDbEntry uid
	Map<String, DataBlock> domesticRecords = new TreeMap<>();  // US & CAN only

	public NavaidOutput(NavDriver parentSensor) throws IOException
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "NavaidOutput";
	}

	protected void init()
	{
		SWEHelper fac = new SWEHelper();

		//	 Structure is {id, name, lat, lon}

		// SWE Common data structure
		navStruct = fac.newDataRecord(4);
		navStruct.setName(getName());
		navStruct.setDefinition("http://earthcastwx.com/ont/swe/property/navaids"); // ??

		Text id = fac.newText("http://sensorml.com/ont/swe/property/code", "navaid Code", "");
		navStruct.addComponent("code", id);
		//		Text type = fac.newText("http://sensorml.com/ont/swe/property/type", "Type", "Type (Waypoint/Navaid/etc.)" );
		//		waypt.addComponent("type", type);
		Text name = fac.newText("http://sensorml.com/ont/swe/property/name", "Name", "Long name" );
		navStruct.addComponent("name", name);
		Quantity latQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Latitude", "Geodetic Latitude", null, "deg", DataType.DOUBLE);
		navStruct.addComponent("lat", latQuant);
		Quantity lonQuant = fac.newQuantity("http://sensorml.com/ont/swe/property/Longitude", "Longitude", null, "deg", DataType.DOUBLE);
		navStruct.addComponent("lon", lonQuant);

		// default encoding is text
		encoding = fac.newTextEncoding(",", "\n");
	}

	public void start() throws SensorHubException {
		// Nothing to do 
	}

	public double[] getLons (List<NavDbEntry> recs) {
		double [] lons = new double[recs.size()];
		int i=0;
		for (NavDbEntry rec: recs) {
			lons[i++] = rec.lon;
		}
		return lons;
	}

	public double[] getLats (List<NavDbEntry> recs) {
		double [] lats = new double[recs.size()];
		int i=0;
		for (NavDbEntry rec: recs) {
			lats[i++] = rec.lat;
		}
		return lats;
	}

	public String[] getNames (List<NavDbEntry> recs) {
		String [] names = new String[recs.size()];
		int i=0;
		for (NavDbEntry rec: recs) {
			names[i++] = rec.name;
		}
		return names;
	}

	public String[] getIds (List<NavDbEntry> recs) {
		String [] ids = new String[recs.size()];
		int i=0;
		for (NavDbEntry rec: recs) {
			ids[i++] = rec.id;
		}
		return ids;
	}

	public void sendEntries(List<NavDbEntry> recs)
	{                
	    Map<String, DataBlock> newDomesticRecords = new TreeMap<>();
	    Map<String, DataBlock> newGlobalRecords = new TreeMap<>();
	    
		for(NavDbEntry rec: recs) {
			DataBlock dataBlock = navStruct.createDataBlock();

			dataBlock.setStringValue(0, rec.id);
			dataBlock.setStringValue(1, rec.name);
			dataBlock.setDoubleValue(2, rec.lat);
			dataBlock.setDoubleValue(3, rec.lon);
			
			newGlobalRecords.put(rec.id, dataBlock);
			if("USA".equals(rec.region) || "CAN".equals("rec.region"))
			    newDomesticRecords.put(rec.id, dataBlock);
			//long time = System.currentTimeMillis();
			//eventHandler.publishEvent(new SensorDataEvent(time, uid, NavaidOutput.this, dataBlock));
		}
		
		// switch to new records atomically
        globalRecords = newGlobalRecords;
		domesticRecords = newDomesticRecords;
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
	

	@Override
	public Collection<String> getEntityIDs()
	{
	    return parentSensor.getEntityIDs();
	}


	@Override
	public Map<String, DataBlock> getLatestRecords()
	{
	    return Collections.unmodifiableMap(domesticRecords);
	}


	@Override
	public DataBlock getLatestRecord(String entityID)
	{
	    return globalRecords.get(entityID);
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
