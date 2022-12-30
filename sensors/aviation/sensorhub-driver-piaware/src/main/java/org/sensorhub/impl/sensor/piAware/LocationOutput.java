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
public class LocationOutput extends AbstractSensorOutput<PiAwareSensor> { 
	private static final int AVERAGE_SAMPLING_PERIOD = 1;
	DataRecord recordStruct;
	DataEncoding recordEncoding;
	static final String NAME = "locationOutput";

	Logger logger;

	public LocationOutput(PiAwareSensor parentSensor) {
		super(NAME, parentSensor);
		logger = parentSensor.getLogger();
		init();
	}

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
			.addField("time", geoFac.createTime()
		        .asSamplingTimeIsoUTC()
//		        .asSamplingTimeIsoGPS()
		        .description(""))
			.addField("hexIdent", fac.createText()
				.label("hexIdent")
				.description("Aircraft Mode S (ICAO) hexadecimal code")
				.definition(PiAwareSensor.DEF_HEX_ID)
				.build())
			.addField("flightId", fac.createText()
				.description("Flight Identifier")
				.definition(PiAwareSensor.DEF_FLIGHT_ID)
				.build())
			.addField("category", fac.createText()
					.description("ADS-B emitter category set")
					.definition("")
					.build())
			.addField("callSign", fac.createText()
				.description("")
				.definition("")
				.build())
//			.addField("location", vec) //fac.createVector()
	        .addField("location", geoFac.createVector()
	                .from(geoFac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC))
	                .description("Location measured by the GPS device")
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
		recordStruct.setName("Location Record");
		recordStruct.setLabel("Location Record");
		recordStruct.setDefinition(SWEConstants.SWE_PROP_URI_PREFIX + "Location");
			
		// default encoding is text
		recordEncoding = fac.newTextEncoding(",", "\n");
	}

	private DataBlock recordToDataBlock(SbsPojo rec) {
		DataBlock dataBlock = recordStruct.createDataBlock();

		int index = 0;
		Double time = (rec.timeMessageGenerated.doubleValue())/1000.;
//		System.err.println("   *** " + time + "," + rec.flightID + "," + rec.callsign + "," + rec.groundSpeed);

//		logger.debug("time: {}", t);
//		logger.debug("callsign: {}", rec.callsign);
//		setDoubleValue(dataBlock, index++, ((double)rec.timeMessageGenerated)/1000.);
		setDoubleValue(dataBlock, index++, time);
		setStringValue(dataBlock, index++, rec.hexIdent);
		setStringValue(dataBlock, index++, rec.flightID);
		setStringValue(dataBlock, index++, rec.category);
		setStringValue(dataBlock, index++, rec.callsign);
		setDoubleValue(dataBlock, index++, rec.latitude);
		setDoubleValue(dataBlock, index++, rec.longitude);
		setDoubleValue(dataBlock, index++, rec.altitude);
		setBooleanValue(dataBlock, index++, rec.squawkChange);
		setBooleanValue(dataBlock, index++, rec.emergency);
		setBooleanValue(dataBlock, index++, rec.spiIdent);
		setBooleanValue(dataBlock, index++, rec.isOnGround);
		return dataBlock;
	}

	public void publishRecord(SbsPojo rec, String foiUid) {
		try {
			latestRecord = recordToDataBlock(rec);
			latestRecordTime = System.currentTimeMillis();
			eventHandler
				.publish(new DataEvent(latestRecordTime, PiAwareSensor.SENSOR_UID, NAME, foiUid, latestRecord));

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private void setDoubleValue(DataBlock block, int index, Double value) {
		if(value != null)
			block.setDoubleValue(index, value);
		else
			block.setDoubleValue(index, Double.NaN); // will this work?
	}
	
	private void setStringValue(DataBlock block, int index, String value) {
		if(value == null)  
			value = "";
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
}
