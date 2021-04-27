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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
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
public class MetarOutput extends AbstractSensorOutput<MetarSensor>
{
	private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(20);
	private static final int AVERAGE_POLLING_PERIOD = (int)TimeUnit.MINUTES.toMillis(1);

	DataRecord metarRecordStruct;
	DataEncoding metarRecordEncoding;
	Timer timer;
	TimerTask aviationTimerTask;
	
	public MetarOutput(MetarSensor parentSensor)
	{
		super("metarWeather", parentSensor);
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

		// mark component providing foi ID
		metarRecordStruct.getFieldList().getProperty(1).setRole(SWEConstants.DEF_SYSTEM_ID);

		// default encoding is text
		metarRecordEncoding = fac.newTextEncoding(",", "\n");
	}
	
	protected void start()
    {
	    aviationTimerTask = new AviationTimerTask(parentSensor.getConfiguration().aviationWeatherUrl);
        timer = new Timer(true);
        timer.scheduleAtFixedRate(aviationTimerTask, 0, AVERAGE_POLLING_PERIOD);
    }


	private DataBlock metarRecordToDataBlock(String stationID, Metar rec)
	{
		DataBlock dataBlock = metarRecordStruct.createDataBlock();

		int index = 0;
		dataBlock.setDoubleValue(index++, rec.timeUtc);
		dataBlock.setStringValue(index++, stationID);
		setDoubleValue(dataBlock, index++, rec.getTempFahrenheit());
		setDoubleValue(dataBlock, index++, rec.getDewpointFahrenheit());
        setDoubleValue(dataBlock, index++, rec.getRelativeHumidity());
		setDoubleValue(dataBlock, index++, rec.pressure);
		setDoubleValue(dataBlock, index++, rec.getWindSpeed());
		setIntValue(dataBlock, index++, rec.windDirection);
		setDoubleValue(dataBlock, index++, rec.windGust);
		setDoubleValue(dataBlock, index++, rec.hourlyPrecipInches);
		setIntValue(dataBlock, index++, 0);  // todo cloudHeight
//		dataBlock.setIntValue(index++, (int)Math.round(rec.getVisibilityMiles() * 5280.0));
		dataBlock.setStringValue(index++, rec.getPresentWeathers());
		dataBlock.setStringValue(index++, rec.getSkyConditionsAsString());
		dataBlock.setStringValue(index++, rec.getRunwayVisualRanges());

		return dataBlock;
	}

	private void setDoubleValue(DataBlock block, int index, Double value) {
		if(value != null)
			block.setDoubleValue(index, value);
		index++;
	}
	
	private void setIntValue(DataBlock block, int index, Integer value) {
		if(value != null)
			block.setIntValue(index, value);
		index++;
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
						boolean keepMetar = false;
						for(String stationId: parentSensor.getConfiguration().stationIds) {
							if(stationId.equalsIgnoreCase(metar.stationId)) {
								keepMetar = true;
								break;
							}
						}
						if(!keepMetar)
							continue;
						metar.timeUtc = MetarUtil.computeTimeUtc(metar.dateString);
						// TODO Fix the time!!
						latestRecordTime = System.currentTimeMillis();
						String stationUID = MetarSensor.STATION_UID_PREFIX + metar.stationId;
						latestRecord = metarRecordToDataBlock(metar.stationId, metar);
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
