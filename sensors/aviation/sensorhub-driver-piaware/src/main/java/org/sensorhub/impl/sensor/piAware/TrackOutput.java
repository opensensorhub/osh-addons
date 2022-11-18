package org.sensorhub.impl.sensor.piAware;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.SWEBuilders.DataRecordBuilder;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;

public class TrackOutput extends AbstractSensorOutput<PiAwareSensor> { 
	private static final int AVERAGE_SAMPLING_PERIOD = 1;
	DataRecord recordStruct;
	DataEncoding recordEncoding;
	static final String NAME = "trackOutput";
	
	Logger logger;

	public TrackOutput(PiAwareSensor parentSensor) {
		super(NAME, parentSensor);
		logger = parentSensor.getLogger();
		init();
	}

	protected void init() {
		SWEHelper fac = new SWEHelper();
        GeoPosHelper geoFac = new GeoPosHelper();
		// SWE Common data structure
		DataRecordBuilder builder = fac.createRecord()
			.addField("time", geoFac.createTime()
		        .asSamplingTimeIsoUTC()
		        .description(""))
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
            .addField("track", fac.createQuantity()  // Using this as proxy for actual heading
                    .description("Track of aircraft (not heading). Derived from the velocity E/W and velocity N/S")
                    .definition("")
                    .uomCode("deg")
                    .build())
//          .addField("location", vec) //fac.createVector()
            .addField("verticalRate", fac.createQuantity()
                    .description("64 foot resolution")
                    .definition("")
                    .uomCode("[ft_i]/s")
                    .build());
			
		recordStruct = builder.build();
		recordStruct.setName("Track Record");
		recordStruct.setLabel("Track Record");
		recordStruct.setDefinition(SWEConstants.SWE_PROP_URI_PREFIX + "trackOutput");
			
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
		setStringValue(dataBlock, index++, rec.callsign);
        setDoubleValue(dataBlock, index++, rec.groundSpeed);
        setDoubleValue(dataBlock, index++, rec.track);
        setDoubleValue(dataBlock, index++, rec.verticalRate);
		return dataBlock;
	}

	public void publishRecord(SbsPojo rec, String foiUid) {
		try {
			latestRecord = recordToDataBlock(rec);
			latestRecordTime = System.currentTimeMillis();
			eventHandler
				.publish(new DataEvent(latestRecordTime, rec.hexIdent, NAME, foiUid, latestRecord));

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
