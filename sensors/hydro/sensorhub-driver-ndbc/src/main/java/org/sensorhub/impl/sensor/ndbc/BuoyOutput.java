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

package org.sensorhub.impl.sensor.ndbc;

import java.util.List;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;

/**
 * 
 * @author Tony Cook
 *
 */
public class BuoyOutput extends AbstractSensorOutput<BuoySensor> { 
	private static final int AVERAGE_SAMPLING_PERIOD = 300; // 5 minutes
	DataRecord recordStruct;
	DataEncoding recordEncoding;
	static final String NAME = "buoyOutput";

	Logger logger;

	public BuoyOutput(BuoySensor parentSensor) {
		super(NAME, parentSensor);
		logger = parentSensor.getLogger();
		init();
	}

	protected void init() {
		SWEHelper sweHelper = new SWEHelper();
        GeoPosHelper geoFac = new GeoPosHelper();
 		// SWE Common data structure
		recordStruct = geoFac.createRecord()
			.name(getName())
			.label("Buoy Record Output")
			.definition(SWEHelper.getPropertyUri("buoyOutput"))
			.addField("time", sweHelper.createTime()
		            .asSamplingTimeIsoUTC()
		            .build())
			.addField("id", geoFac.createText()
				.label("id")
				.description("Five-digit WMO Station Identifier")
				.definition(SWEHelper.getPropertyUri("wmoId"))
				.build())
			.addField("location", geoFac.newLocationVectorLLA(SWEHelper.getPropertyUri("location")))			
			.addField("windSpeed", geoFac.createQuantity()
				.description("Wind speed (m/s) averaged over an eight-minute period for buoys" + ""
						+   "and a two-minute period for land stations")
				.definition(SWEConstants.CF_URI_PREFIX + "wind_speed")
				.uomCode("m/s")
				.build())
			.addField("windDirection", geoFac.createQuantity()
				.description("Wind direction (the direction the wind is coming from in degrees clockwis" 
					+" from true N) during the same period used for WSPD")
				.definition(SWEConstants.CF_URI_PREFIX + "wind_from_direction")
				.uomCode("deg")
				.build())
			.addField("windGust", geoFac.createQuantity()
				.description("Peak 5 or 8 second gust speed (m/s) measured during the eight-minute or"
						+ "two-minute period") 
				.definition(SWEConstants.CF_URI_PREFIX + "wind_gust")
				.uomCode("m/s")
				.build())
			.addField("significantWaveHeight", geoFac.createQuantity()
				.description("Significant wave height (meters) is calculated as the average"
						+ "of the highest one-third of all of the wave heights during the 20-minute sampling period")
				.definition(SWEConstants.CF_URI_PREFIX + "significant_wave_height")
				.uomCode("m")
				.build())
			.addField("dominantWavePeriod", geoFac.createQuantity()
				.description("Dominant wave period (seconds) is the period with the maximum wave energy.")
				.definition("https://mmisw.org/ont/mx_testing/mxparms1/dominant_wave_period")
				.uomCode("s")
				.build())
			.addField("averageWavePeriod", geoFac.createQuantity()
				.description("Average wave period (seconds) of all waves during the 20-minute period")
				.definition("https://mmisw.org/ont/mx_testing/mxparms1/dominant_wave_period")
				.uomCode("s")
				.build())			
			.addField("waveDirection", geoFac.createQuantity()
				.description("The direction from which the waves at the dominant period (DPD) are coming."
						+ "The units are degrees from true North")
				.definition(SWEConstants.SWE_PROP_URI_PREFIX + "waveDirectopn")
				.uomCode("deg")
				.build())
			.addField("seaLevelPressure", geoFac.createQuantity()
				.description("Sea level pressure (hPa). For C-MAN sites and Great Lakes buoys")
				.definition("https://mmisw.org/ont/mx_testing/mxparms1/sea_level_pressure")
				.uomCode("hPa")
				.build())
			.addField("pressureTendency", geoFac.createQuantity()
				.description("Pressure Tendency is the direction (plus or minus) and the amount of"
						+ "pressure change (hPa)for a three hour period ending at the time of observation")
				.definition("https://mmisw.org/ont/mx_testing/mxparms1/pressure_tendency")
				// .uomCode() hPa/3 hours- not sure how to encode
				.build())
			.addField("airTemperature", geoFac.createQuantity()
				.description("Air temperature (Celsius) at sensor height")
				.definition(SWEConstants.CF_URI_PREFIX + "air_temperature")
				.uomCode("Cel")
				.build())
			.addField("waterTemperature", geoFac.createQuantity()
				.description("Sea surface temperature (Celsius).")
				.definition(SWEConstants.CF_URI_PREFIX + "sea_water_temperature")
				.uomCode("Cel")
				.build())
			.addField("dewPoint", geoFac.createQuantity()
				.description("Dewpoint temperature taken at the same height as the air temperature measurement")
				.definition(SWEConstants.CF_URI_PREFIX + "dew_point")
				.uomCode("Cel") // ?? Check this one
				.build())
			.addField("visibility", geoFac.createQuantity()
				.description("Station visibility (nautical miles).")
				.definition(SWEConstants.CF_URI_PREFIX + "visibility")
				.uomCode("[nmi_i]")
				.build())
			.addField("tideWaterLevel", geoFac.createQuantity()
				.description("he water level in feet above or below Mean Lower Low Water (MLLW).")
				.definition(SWEConstants.SWE_PROP_URI_PREFIX + "tideWaterLevel")
				.uomCode("m") // Check this
				.build())
		.build();
			
		// default encoding is text
		recordEncoding = geoFac.newTextEncoding(",", "\n");
	}
	
	public void publishRecords(List<BuoyDataRecord> recs) {
		for(BuoyDataRecord rec: recs) {
			publishRecord(rec);
		}
	}

	private DataBlock recordToDataBlock(BuoyDataRecord rec) {
		DataBlock dataBlock = recordStruct.createDataBlock();

		int index = 0;
		Double time = rec.getTimeMs();  // need method to computeThis
		
//		Double time = (rec.timeMessageGenerated.doubleValue())/1000.;
		setDoubleValue(dataBlock, index++, (double)rec.timeMs / 1000.);
		setStringValue(dataBlock, index++, rec.id);
		setDoubleValue(dataBlock, index++, rec.lat);
		setDoubleValue(dataBlock, index++, rec.lon);
		setDoubleValue(dataBlock, index++, rec.windSpeed);
		if(rec.windDir == null)
			dataBlock.setDoubleValue(index, Double.NaN);
		else
			setDoubleValue(dataBlock, index++, Double.valueOf(rec.windDir));
		setDoubleValue(dataBlock, index++, rec.windGust);
		setDoubleValue(dataBlock, index++, rec.wvht);
		setDoubleValue(dataBlock, index++, rec.dpd);
		setDoubleValue(dataBlock, index++, rec.apd);
		setDoubleValue(dataBlock, index++, rec.mwd);
		setDoubleValue(dataBlock, index++, rec.pressure);
		setDoubleValue(dataBlock, index++, rec.ptdy);
		setDoubleValue(dataBlock, index++, rec.airTemp);
		setDoubleValue(dataBlock, index++, rec.waterTemp);
		setDoubleValue(dataBlock, index++, rec.dewPt);
		setDoubleValue(dataBlock, index++, rec.visibility);
		setDoubleValue(dataBlock, index++, rec.tide);

		return dataBlock;
	}

	public void publishRecord(BuoyDataRecord rec) {
		try {
			String foiUid = BuoySensor.BUOY_UID_PREFIX + rec.id;
			latestRecord = recordToDataBlock(rec);
			latestRecordTime = System.currentTimeMillis();
			eventHandler.publish(new DataEvent(latestRecordTime, BuoySensor.BUOY_UID, NAME, foiUid, latestRecord));
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
