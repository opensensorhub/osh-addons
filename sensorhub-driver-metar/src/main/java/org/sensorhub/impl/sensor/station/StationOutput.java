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

package org.sensorhub.impl.sensor.station;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;


/**
 * 
 * @author Tony Cook
 *
 *  ISSUES - we are pushing data here to the bus. How will that be controlled/timed
 *  		 
 */
public class StationOutput extends AbstractSensorOutput<StationSensor>
{
    private static final Logger log = LoggerFactory.getLogger(StationOutput.class);
    DataRecord baseRecordStructure;
    DataEncoding baseRecordEncoding;
    DataBlock latestRecord;
    boolean sendData;
    protected Timer timer;
    StationDataPoller dataPoller; // = new MetarDataPoller();  // how do I get an implementing class into here?

    
    public StationOutput(StationSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "GenericWeatherStation";
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // SWE Common data structure
        baseRecordStructure = fac.newDataRecord(7);
        baseRecordStructure.setName(getName());
        baseRecordStructure.setDefinition("http://sensorml.com/ont/swe/property/Weather/BaseWeatherStationRecord");
        
        // stationName,time,lat,lon,el,Temperature (degreesF),Dewpoint (degreesF),Relative Humididty (%),Wind Speed (mph),Wind Direction (degrees),
        //Air Pressure (inches HG),Precipitation (inches),Heat Index (degreesF),Wind Chill (degreesF), Wind Gust (mph),
        //Rainfaill last 3 hours (inches),Rainfaill last 6 hours (inches),Rainfaill last 24 hours (inches),Max Temperature last 24 hours (degreesF),Min Temperature last 24 hours (degreesF),
        //cloud Ceiling (feet),visibility (feet)
        
        baseRecordStructure.addField("time", fac.newTimeStampIsoUTC());
        baseRecordStructure.addField("temp", fac.newQuantity("http://sensorml.com/ont/swe/property/Temperature", "Air Temperature", null, "degF"));
        baseRecordStructure.addField("dewPoint", fac.newQuantity("http://sensorml.com/ont/swe/property/DewPoint", "Dew Point Temperature", null, "degF"));
        baseRecordStructure.addField("humidity", fac.newQuantity("http://sensorml.com/ont/swe/property/HumidityValue", "Relative Humidity", null, "%"));
        baseRecordStructure.addField("windSpeed", fac.newQuantity("http://sensorml.com/ont/swe/property/WindSpeed", "Wind Speed", null, "[mi_i]/h"));
        baseRecordStructure.addField("windDir", fac.newQuantity("http://sensorml.com/ont/swe/property/WindDirectionAngle", "Wind Direction", null, "deg"));
        baseRecordStructure.addField("press", fac.newQuantity("http://sensorml.com/ont/swe/property/AirPressureValue", "Atmospheric Pressure", null, "[in_i]Hg"));
        
        // default is text encoding
        baseRecordEncoding = fac.newTextEncoding(",", "\n");
    }


    private void sendLatestRecord()
    {
    	StationDataRecord rec = dataPoller.pullStationData("3467");
    	
    	// build and publish datablock
        DataBlock dataBlock = baseRecordStructure.createDataBlock();
        Station stn = rec.getStation();
//        dataBlock.setDoubleValue(0, rec.getTimeUtc()/1000.); 
        dataBlock.setDoubleValue(0, rec.getTimeUtc()); 
        dataBlock.setDoubleValue(1, rec.getTemperature()); 
        dataBlock.setDoubleValue(2, rec.getDewPoint()); 
        dataBlock.setDoubleValue(3, rec.getRelativeHumidity()); 
        dataBlock.setDoubleValue(4, rec.getWindSpeed()); 
        dataBlock.setDoubleValue(5, rec.getWindDirection()); 
        dataBlock.setDoubleValue(6, rec.getPressure()); 
        
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, StationOutput.this, dataBlock));
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
            	sendLatestRecord();
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, TimeUnit.SECONDS.toMillis(3));        
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
        return 30.0;
    }


    @Override 
    public DataComponent getRecordDescription()
    {
        return baseRecordStructure;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return baseRecordEncoding;
    }
}
