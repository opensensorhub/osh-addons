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

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;


/**
 * 
 * @author Tony Cook
 *
 *  	DataPoller queries station service at POLLING_INTERVAL and checks for new record.  
 *      If record time is greater than latestRecord.time, we update latestRecord and latestBlock
 *      and send event to bus.  
 */
public class MetarOutput extends AbstractSensorOutput<MetarSensor> //extends StationOutput
{
    private static final long POLLING_INTERVAL_SECONDS = 120;
    private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(20);
    
    DataRecord metarRecordStruct;
    DataEncoding metarRecordEncoding;
    MetarDataPoller metarPoller = new MetarDataPoller();
    Timer timer;
    Map<String, Long> latestUpdateTimes;
    

    public MetarOutput(MetarSensor parentSensor)
    {
        super(parentSensor);
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
        
        // SWE Common data structure
        metarRecordStruct = fac.newDataRecord(14);
        metarRecordStruct.setName(getName());
        metarRecordStruct.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        
        metarRecordStruct.addField("time", fac.newTimeStampIsoUTC());
        metarRecordStruct.addField("station", fac.newText("http://sensorml.com/ont/swe/property/StationID", "Station ID", null));
        metarRecordStruct.addField("temp", fac.newQuantity("http://sensorml.com/ont/swe/property/Temperature", "Air Temperature", null, "degF"));
        metarRecordStruct.addField("dewPoint", fac.newQuantity("http://sensorml.com/ont/swe/property/DewPoint", "Dew Point Temperature", null, "degF"));
        metarRecordStruct.addField("humidity", fac.newQuantity("http://sensorml.com/ont/swe/property/HumidityValue", "Relative Humidity", null, "%"));
        metarRecordStruct.addField("press", fac.newQuantity("http://sensorml.com/ont/swe/property/AirPressureValue", "Atmospheric Pressure", null, "[in_i]Hg"));
        metarRecordStruct.addField("windSpeed", fac.newQuantity("http://sensorml.com/ont/swe/property/WindSpeed", "Wind Speed", null, "[mi_i]/h"));
        metarRecordStruct.addField("windDir", fac.newQuantity("http://sensorml.com/ont/swe/property/WindDirectionAngle", "Wind Direction", null, "deg"));
        metarRecordStruct.addField("windGust", fac.newQuantity("http://sensorml.com/ont/swe/property/WindGust", "Wind Gust", null, "[mi_i]/h"));
        metarRecordStruct.addField("precip", fac.newQuantity("http://sensorml.com/ont/swe/property/Precipitation", "Hourly Precipitation", null, "[in_i]"));
        metarRecordStruct.addField("cloudH", fac.newQuantity("http://sensorml.com/ont/swe/property/TopCloudHeightDimension", "Cloud Ceiling", null, "[ft_i]"));
        metarRecordStruct.addField("visibilty", fac.newQuantity("http://sensorml.com/ont/swe/property/Visibility", "Visibility", null, "[ft_i]"));
        metarRecordStruct.addField("weather", fac.newText("http://sensorml.com/ont/swe/property/PresentWeather", "Present Weather", null));
        metarRecordStruct.addField("sky", fac.newText("http://sensorml.com/ont/swe/property/SkyConditions", "Sky Conditions", null));
        
        // default encoding is text
        metarRecordEncoding = fac.newTextEncoding(",", "\n");
    }


    private DataBlock metarRecordToDataBlock(String stationID, MetarDataRecord rec)
    {
    	DataBlock dataBlock = metarRecordStruct.createDataBlock();
    	
    	int index = 0;
    	dataBlock.setDoubleValue(index++, rec.getTimeUtc()/1000.);
        dataBlock.setStringValue(index++, stationID);
        dataBlock.setDoubleValue(index++, rec.getTemperature());
        dataBlock.setDoubleValue(index++, rec.getDewPoint());
        dataBlock.setDoubleValue(index++, rec.getRelativeHumidity());
        dataBlock.setDoubleValue(index++, rec.getPressure());
        dataBlock.setDoubleValue(index++, rec.getWindSpeed());
        dataBlock.setDoubleValue(index++, rec.getWindDirection());
        dataBlock.setDoubleValue(index++, rec.getWindGust());
        dataBlock.setDoubleValue(index++, rec.getHourlyPrecip());
        dataBlock.setIntValue(index++, rec.getCloudCeiling());
        dataBlock.setIntValue(index++, rec.getVisibility());
        dataBlock.setStringValue(index++, rec.getPresentWeather());
        dataBlock.setStringValue(index++, rec.getSkyConditions());
        
        return dataBlock;
    }


    protected void start()
    {
        if (timer != null)
            return;
        timer = new Timer();
        
        // start main measurement generation thread
        TimerTask task = new TimerTask()
        {
            public void run()
            {
            	for (String stationID: parentSensor.getConfiguration().stationIDs)
            	{
                    MetarDataRecord rec = metarPoller.pullStationData(stationID);
                    
                    Long lastUpdateTime = latestUpdateTimes.get(stationID);
                	if (lastUpdateTime == null || rec.getTimeUtc() > lastUpdateTime)
                	{
                	    latestUpdateTimes.put(stationID, rec.getTimeUtc());
                		latestRecordTime = System.currentTimeMillis();
                		latestRecord = metarRecordToDataBlock(stationID, rec);
                		String stationUID = MetarSensor.STATION_UID_PREFIX + stationID;
                		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, stationUID, MetarOutput.this, latestRecord));
                	}
                	
                	// wait a bit before querying next station
                	try { Thread.sleep(1000); }
                    catch (InterruptedException e) { }
            	}
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, TimeUnit.SECONDS.toMillis(POLLING_INTERVAL_SECONDS));        
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
}
