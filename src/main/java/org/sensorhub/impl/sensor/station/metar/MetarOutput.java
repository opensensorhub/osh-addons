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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.station.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(MetarOutput.class);
    DataRecord metarRecordStruct;
    DataEncoding metarRecordEncoding;
    MetarDataRecord latestMetarRecord;
    MetarDataPoller metarPoller = new MetarDataPoller();
    private static final long POLLING_INTERVAL_SECONDS = 120;
    private static final int AVERAGE_SAMPLING_PERIOD = (int)TimeUnit.MINUTES.toSeconds(20);
    protected Timer timer;

    public MetarOutput(MetarSensor parentSensor)
    {
        super(parentSensor);
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
        metarRecordStruct = fac.newDataRecord(13);
        metarRecordStruct.setName(getName());
        metarRecordStruct.setDefinition("http://sensorml.com/ont/swe/property/Weather/MetarStationRecord");
        
        metarRecordStruct.addField("time", fac.newTimeStampIsoUTC());
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
        metarRecordStruct.addField("weather", fac.newText("http://sensorml.com/ont/swe/property/presentWeather", "Present Weather", null));
        metarRecordStruct.addField("sky", fac.newText("http://sensorml.com/ont/swe/property/skyConditions", "Sky Conditions", null));
        
        // default encoding is text
        metarRecordEncoding = fac.newTextEncoding(",", "\n");
    }


    private DataBlock metarRecordToDataBlock(MetarDataRecord rec)
    {
    	DataBlock dataBlock = metarRecordStruct.createDataBlock();
        Station stn = rec.getStation();
        dataBlock.setDoubleValue(0, rec.getTimeUtc()/1000.); 
        dataBlock.setDoubleValue(1, rec.getTemperature()); 
        dataBlock.setDoubleValue(2, rec.getDewPoint()); 
        dataBlock.setDoubleValue(3, rec.getRelativeHumidity()); 
        dataBlock.setDoubleValue(4, rec.getPressure()); 
        dataBlock.setDoubleValue(5, rec.getWindSpeed()); 
        dataBlock.setDoubleValue(6, rec.getWindDirection()); 
        dataBlock.setDoubleValue(7, rec.getWindGust()); 
        dataBlock.setDoubleValue(8, rec.getHourlyPrecip()); 
        dataBlock.setIntValue(9, rec.getCloudCeiling()); 
        dataBlock.setIntValue(10, rec.getVisibility()); 
        dataBlock.setStringValue(11, rec.getPresentWeather()); 
        dataBlock.setStringValue(12, rec.getSkyConditions()); 
        
        return dataBlock;
    }


    protected void start()
    {
        if (timer != null)
            return;
        timer = new Timer();
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run()
            {
            	MetarDataRecord rec = metarPoller.pullStationData();
            	if(latestMetarRecord == null || rec.getTimeUtc() > latestMetarRecord.getTimeUtc()) {
            		latestMetarRecord = rec; 
            		latestRecordTime = System.currentTimeMillis();
            		latestRecord = metarRecordToDataBlock(rec);
            		eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, MetarOutput.this, latestRecord));
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
