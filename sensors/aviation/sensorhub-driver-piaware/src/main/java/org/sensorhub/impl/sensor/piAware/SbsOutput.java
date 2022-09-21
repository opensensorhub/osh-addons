/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.piAware;

import java.util.concurrent.TimeUnit;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.vast.swe.SWEBuilders.DataRecordBuilder;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;

/**
 * 
 * @author Tony Cook
 *
 */
public class SbsOutput extends AbstractSensorOutput<PiAwareSensor> { //implements IMultiSourceDataInterface {
	private static final int AVERAGE_SAMPLING_PERIOD = (int) TimeUnit.MINUTES.toSeconds(20);

	DataRecord recordStruct;
	DataEncoding recordEncoding;
//	Map<String, Long> latestUpdateTimes;
	//Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();
	static final String NAME = "sbsOutput";
	
	Logger logger;

	public SbsOutput(PiAwareSensor parentSensor) {
		super(NAME, parentSensor);
//		latestUpdateTimes = new HashMap<String, Long>();
		logger = parentSensor.getLogger();
		init();
	}

//	@Override
//	public String getName() {
//		return "SbsOutput";
//	}
//	
	 public Vector newLocationVectorLLA(String def)
	    {
	        GeoPosHelper geoFac = new GeoPosHelper();

	        return geoFac.createVector()
	            .definition(def != null ? def : GeoPosHelper.DEF_LOCATION)
	            .refFrame(SWEConstants.REF_FRAME_4979)
	            .addCoordinate("lat", geoFac.createQuantity()
	                .definition(GeoPosHelper.DEF_LATITUDE_GEODETIC)
					.description("Latitude- North and East positive. South and West negative.")
	                .label("Geodetic Latitude")
	                .axisId("Lat")
	                .uomCode("deg")
	                .build())
	            .addCoordinate("lon", geoFac.createQuantity()
	                .definition(GeoPosHelper.DEF_LONGITUDE)
					.description("Longitude- North and East positive. South and West negative.")
	                .label("Longitude")
	                .axisId("Lon")
	                .uomCode("deg")
	                .build())
	            .addCoordinate("alt", geoFac.createQuantity()
	                .definition(GeoPosHelper.DEF_ALTITUDE_ELLIPSOID)
					.description("Mode C altitude. Height relative to 1013.2mb (Flight Level). Not height AMSL..")
	                .label("Ellipsoidal Height")
	                .axisId("h")
	                .uomCode("[ft_i]")
	                .build())
	            .build();
	    }

	protected void init() {
		SWEHelper fac = new SWEHelper();
        GeoPosHelper geoFac = new GeoPosHelper();
        Vector vec = newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
		// SWE Common data structure
		DataRecordBuilder builder = fac.createRecord()
			.addSamplingTimeIsoUTC("messageTime")
			.addField("hexIdent", fac.createText()
				.label("hexIdent")
				.description("Aircraft Mode S hexadecimal code")
				.definition("")
				.build())
			.addField("flightId", fac.createText()
				.description("Database Flight record number")
				.definition("")
				.build())
			.addField("callSign", fac.createText()
				.description("")
				.definition("")
				.build())
			.addField("groundSpeed", fac.createQuantity()
				.description("Speed over ground (not indicated airspeed)")
				.definition("")
				.build())
			.addField("track", fac.createQuantity()
				.description("Track of aircraft (not heading). Derived from the velocity E/W and velocity N/S")
				.definition("")
				.uomCode("deg")
				.build())
//			.addField("latitude", fac.createQuantity()
//				.description("Latitude- North and East positive. South and West negative.")
//				.definition("")
//				.build())
//			.addField("longitude", fac.createQuantity()
//				.description("Longitude- North and East positive. South and West negative.")
//				.definition("")
//				.build())
//			.addField("altitude", fac.createQuantity()
//					.description("Mode C altitude. Height relative to 1013.2mb (Flight Level). Not height AMSL..")
//					.definition("")
//					.uomCode("[ft_i]")
//					.build())
			.addField("location", vec) //fac.createVector()
//		        .from(geoFac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC))
		        //geoFac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC)
		        //.description("Lat-Lon-Alt of aircraft")
//		        .build())
			.addField("verticalRate", fac.createQuantity()
				.description("64 foot resolution")
				.definition("")
				.uomCode("[ft_i]/s")
				.build())
			.addField("squawk", fac.createText()
				.description("Assigned Mode A squawk code.")
				.definition("")
				.build())
			.addField("squawkChange", fac.createBoolean()
				.description("Flag to indicate squawk has changed.")
				.definition("")
				.build())
			.addField("emergency", fac.createBoolean()
				.description("Flag to indicate emergency code has been set")
				.definition("")
				.build())
			.addField("spiIdent", fac.createBoolean()
				.description("Flag to indicate transponder Ident has been activated.")
				.definition("")
				.build())
			.addField("isOnGround", fac.createBoolean()
				.description("Flag to indicate ground squat switch is active")
				.definition("")
				.build());
			
		recordStruct = builder.build();
		recordStruct.setName("SBS Record");
		recordStruct.setLabel("SBS Record");
		recordStruct.setDefinition(SWEConstants.SWE_PROP_URI_PREFIX + "sbsOutput");
			
		// default encoding is text
		recordEncoding = fac.newTextEncoding(",", "\n");
	}

	private DataBlock recordToDataBlock(SbsPojo rec) {
//		DataBlock dataBlock = latestRecords.get("");
//		if(dataBlock == null) 		
		// ???
		DataBlock dataBlock = recordStruct.createDataBlock();

		int index = 0;
		setDoubleValue(dataBlock, index++, rec.timeMessageGenerated/1000.);
		setStringValue(dataBlock, index++, rec.hexIdent);
		setStringValue(dataBlock, index++, rec.flightID);
		setStringValue(dataBlock, index++, rec.callsign);
		setDoubleValue(dataBlock, index++, rec.groundSpeed);
		setDoubleValue(dataBlock, index++, rec.track);
		setDoubleValue(dataBlock, index++, rec.latitude);
		setDoubleValue(dataBlock, index++, rec.longitude);
		setDoubleValue(dataBlock, index++, rec.altitude);
		setDoubleValue(dataBlock, index++, rec.verticalRate);
		setStringValue(dataBlock, index++, rec.squawk);
		setBooleanValue(dataBlock, index++, rec.squawkChange);
		setBooleanValue(dataBlock, index++, rec.emergency);
		setBooleanValue(dataBlock, index++, rec.spiIdent);
		setBooleanValue(dataBlock, index++, rec.isOnGround);
		return dataBlock;
	}

	// Thinking now that attempting to aggregate different message types and update records doesn't really make sense
	@Deprecated
	public boolean updateRecord(SbsPojo rec, DataBlock prevRec) {
		//  Check for existing record and update only new fields if it exists
		boolean timeUpdated = false;
		if(prevRec != null ) {
			Double prevTime = prevRec.getDoubleValue(0);
			if(!prevTime.equals(rec.timeMessageGenerated/1000.)) {
				prevRec.setDoubleValue(0, rec.timeMessageGenerated/1000.);
				timeUpdated = true;
			}
			if(rec.groundSpeed != null) 	prevRec.setDoubleValue(4, rec.groundSpeed);
			if(rec.track != null) 	prevRec.setDoubleValue(5, rec.track);
			if(rec.latitude != null) 	prevRec.setDoubleValue(6, rec.latitude);
			if(rec.longitude != null) 	prevRec.setDoubleValue(7, rec.longitude);
			if(rec.altitude != null) 	prevRec.setDoubleValue(8, rec.altitude);
			if(rec.verticalRate != null) 	prevRec.setDoubleValue(9, rec.verticalRate);
			if(rec.squawk != null) 	prevRec.setStringValue(10, rec.squawk);
			if(rec.squawkChange != null) 	prevRec.setBooleanValue(11, rec.squawkChange);
			if(rec.emergency!= null) 	prevRec.setBooleanValue(12, rec.emergency);
			if(rec.spiIdent != null) 	prevRec.setBooleanValue(13, rec.spiIdent);
			if(rec.isOnGround != null) 	prevRec.setBooleanValue(14, rec.isOnGround);
		}
		return timeUpdated;
	}
	
	public void publishRecord(SbsPojo rec, String foiUid) {
		try {
//			DataBlock prevRec =  latestRecords.get(rec.hexIdent);
//			boolean timeUpdated = true;
//			if(prevRec != null ) {
//				timeUpdated = updateRecord(rec, prevRec);
//			} else {
//				latestRecord = recordToDataBlock(rec);
//			}
//			DataBlock latestRecord = recordToDataBlock(rec);
			latestRecord = recordToDataBlock(rec);
			latestRecordTime = System.currentTimeMillis();
//			latestRecords.put(rec.hexIdent, latestRecord);
			eventHandler
				.publish(new DataEvent(latestRecordTime, rec.hexIdent, NAME, foiUid, latestRecord));

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void setDoubleValue(DataBlock block, int index, Double value) {
		if(value != null)
			block.setDoubleValue(index, value);
	}
	
	private void setStringValue(DataBlock block, int index, String value) {
		if(value == null)  value = "";
		block.setStringValue(index, value);
	}
	
	private void setBooleanValue(DataBlock block, int index, Boolean value) {
		if(value != null)
			block.setBooleanValue(index, value);
	}
	
	private void setIntValue(DataBlock block, int index, Integer value) {
		if (value != null)
			block.setIntValue(index, value);
		index++;
	}

	protected void stop() {
		
	}

	@Override
	public double getAverageSamplingPeriod() {
		return AVERAGE_SAMPLING_PERIOD;
	}

	@Override
	public DataComponent getRecordDescription() {
		return recordStruct;
	}

	@Override
	public DataEncoding getRecommendedEncoding() {
		return recordEncoding;
	}

//	@Override
//	public Collection<String> getEntityIDs() {
//		return parentSensor.getEntityIDs();
//	}
//
//	@Override
//	public Map<String, DataBlock> getLatestRecords() {
//		return Collections.unmodifiableMap(latestRecords);
//	}
//
//	@Override
//	public DataBlock getLatestRecord(String entityID) {
//		// DataBlock db = latestRecords.get(entityID);
////		for(Map.Entry<String, DataBlock> dbe: latestRecords.entrySet()) {
////			String key = dbe.getKey();
////			DataBlock val = dbe.getValue();
////		}
//		return latestRecords.get(entityID);
//	}

}
