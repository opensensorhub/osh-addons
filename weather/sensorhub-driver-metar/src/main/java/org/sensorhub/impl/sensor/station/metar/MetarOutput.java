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

package org.sensorhub.impl.sensor.station.metar;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.data.IMultiSourceDataInterface;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;


/**
 * 
 * @author Tony Cook
 *
 *  	aviationTimerTas polls aviation csv Metar file at POLLING_INTERVAL and checks for new records.  
 */
public class MetarOutput extends AbstractSensorOutput<MetarSensor> implements IMultiSourceDataInterface
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(20);
	private static final int AVERAGE_POLLING_PERIOD = (int)TimeUnit.MINUTES.toMillis(1);

	DataRecord metarRecordStruct;
	DataEncoding metarRecordEncoding;
	Map<String, Long> latestUpdateTimes;
	Map<String, DataBlock> latestRecords = new LinkedHashMap<String, DataBlock>();
	Timer timer;
	TimerTask aviationTimerTask;
	
	public MetarOutput(MetarSensor parentSensor)
	{
		super(parentSensor);
		aviationTimerTask = new AviationTimerTask(parentSensor.getConfiguration().aviationWeatherUrl);
		timer = new Timer(true);
	    timer.scheduleAtFixedRate(aviationTimerTask, 0, AVERAGE_POLLING_PERIOD);
		latestUpdateTimes = new HashMap<String, Long>();
	}


	@Override
	public String getName()
	{
		return "metarWeather";
	}


	protected void init()
	{
		SWEHelper fac = new SWEHelper();

		//  Add top level structure
		//		Location Vector
		//		Observed Props
		
		// SWE Common data structure
		metarRecordStruct = fac.newDataRecord(15);
		metarRecordStruct.setName(getName());
		metarRecordStruct.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");

		metarRecordStruct.addField("time", fac.newTimeStampIsoUTC());
		metarRecordStruct.addField("stationId", fac.newText("http://sensorml.com/ont/swe/property/StationID", "Station ID", null));
		metarRecordStruct.addField("temperature", fac.newQuantity("http://sensorml.com/ont/swe/property/Temperature", "Air Temperature", null, "degF"));
		metarRecordStruct.addField("dewPoint", fac.newQuantity("http://sensorml.com/ont/swe/property/DewPoint", "Dew Point Temperature", null, "degF"));
		metarRecordStruct.addField("relativeHumidity", fac.newQuantity("http://sensorml.com/ont/swe/property/HumidityValue", "Relative Humidity", null, "%"));
		metarRecordStruct.addField("pressure", fac.newQuantity("http://sensorml.com/ont/swe/property/AirPressureValue", "Atmospheric Pressure", null, "[in_i]Hg"));
		metarRecordStruct.addField("windSpeed", fac.newQuantity("http://sensorml.com/ont/swe/property/WindSpeed", "Wind Speed", null, "[mi_i]/h"));
		metarRecordStruct.addField("windDirection", fac.newQuantity("http://sensorml.com/ont/swe/property/WindDirectionAngle", "Wind Direction", null, "deg"));
		metarRecordStruct.addField("windSpeedGust", fac.newQuantity("http://sensorml.com/ont/swe/property/WindGust", "Wind Speed Gust", null, "[mi_i]/h"));
		metarRecordStruct.addField("precipitation", fac.newQuantity("http://sensorml.com/ont/swe/property/Precipitation", "Hourly Precipitation", null, "[in_i]"));
		metarRecordStruct.addField("cloudHeight", fac.newQuantity("http://sensorml.com/ont/swe/property/TopCloudHeightDimension", "Cloud Ceiling", null, "[ft_i]"));
//		metarRecordStruct.addField("visibilty", fac.newQuantity("http://sensorml.com/ont/swe/property/Visibility", "Visibility", null, "[ft_i]"));
		metarRecordStruct.addField("presentWeather", fac.newText("http://sensorml.com/ont/swe/property/PresentWeather", "Present Weather", null));
		metarRecordStruct.addField("skyConditions", fac.newText("http://sensorml.com/ont/swe/property/SkyConditions", "Sky Conditions", null));
		metarRecordStruct.addField("runwayVisualRange", fac.newText("http://sensorml.com/ont/swe/property/RunwayVisualRange", "RunwayVisualRange", null));

		// mark component providing entity ID
		metarRecordStruct.getFieldList().getProperty(1).setRole(ENTITY_ID_URI);

		// default encoding is text
		metarRecordEncoding = fac.newTextEncoding(",", "\n");
	}


	private DataBlock metarRecordToDataBlock(String stationID, Metar rec)
	{
		DataBlock dataBlock = metarRecordStruct.createDataBlock();

		int index = 0;
		dataBlock.setDoubleValue(index++, rec.timeUtc);
		dataBlock.setStringValue(index++, stationID);
		dataBlock.setDoubleValue(index++, rec.getTemperature());
		dataBlock.setDoubleValue(index++, rec.getDewPoint());
        dataBlock.setDoubleValue(index++, rec.getRelativeHumidity());
		dataBlock.setDoubleValue(index++, rec.pressure);
		dataBlock.setDoubleValue(index++, rec.getWindSpeed());
		dataBlock.setDoubleValue(index++, rec.windDirection);
		dataBlock.setDoubleValue(index++, rec.windGust);
		dataBlock.setDoubleValue(index++, rec.hourlyPrecipInches);
		dataBlock.setIntValue(index++, 0);  // todo cloudHeight
//		dataBlock.setIntValue(index++, (int)Math.round(rec.getVisibilityMiles() * 5280.0));
		dataBlock.setStringValue(index++, rec.getPresentWeathers());
		dataBlock.setStringValue(index++, rec.getSkyConditions());
		dataBlock.setStringValue(index++, rec.getRunwayVisualRanges());

		return dataBlock;
	}

	protected void stop()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
	}


	@Override
	public double getAverageSamplingPeriod()
	{
		return AVERAGE_SAMPLING_PERIOD;
	}


	@Override 
	public DataComponent getRecordDescription()
	{
		return metarRecordStruct;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return metarRecordEncoding;
	}

	@Override
	public Collection<String> getEntityIDs()
	{
		return parentSensor.getEntityIDs();
	}


	@Override
	public Map<String, DataBlock> getLatestRecords()
	{
		return Collections.unmodifiableMap(latestRecords);
	}


	@Override
	public DataBlock getLatestRecord(String entityID)
	{
		//    	DataBlock db = latestRecords.get(entityID);
		for(Map.Entry<String, DataBlock> dbe: latestRecords.entrySet()) {
			String key = dbe.getKey();
			DataBlock val = dbe.getValue();
			System.err.println(key + " : " + val);
		}
		return latestRecords.get(entityID);
	}
	
	// Realtime 
	class AviationTimerTask extends TimerTask {
		String serverUrl;
		
		public AviationTimerTask(String url) {
			serverUrl = url;
		}
		
		@Override
		public void run() {
			MetarAviationWeatherReader reader = new MetarAviationWeatherReader(serverUrl);
			try {
				List<Metar> metars = reader.read();
				for(Metar metar: metars) {
					try {
						metar.timeUtc = MetarParserNew.computeTimeUtc(metar.dateString);
						// TODO Fix the time!!
						latestUpdateTimes.put(metar.stationID, metar.timeUtc);
						latestRecordTime = System.currentTimeMillis();
						String stationUID = MetarSensor.STATION_UID_PREFIX + metar.stationID;
						latestRecord = metarRecordToDataBlock(metar.stationID, metar);
						latestRecords.put(stationUID, latestRecord);   
					} catch (Exception e) {
						e.printStackTrace(System.err);
						continue;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
