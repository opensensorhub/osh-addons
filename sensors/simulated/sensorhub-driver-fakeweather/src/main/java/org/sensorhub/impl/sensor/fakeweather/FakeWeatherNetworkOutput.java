/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakeweather;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.fakeweather.FakeWeatherNetworkOutput;
import org.sensorhub.api.data.DataEvent;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


public class FakeWeatherNetworkOutput extends AbstractSensorOutput<FakeWeatherNetwork>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    Timer timer;

    
    public FakeWeatherNetworkOutput(FakeWeatherNetwork parentSensor)
    {
        super("weather", parentSensor);
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // create output data structure
        dataStruct = fac.createDataRecord()
            .name(getName())
            .definition("http://sensorml.com/ont/swe/property/Weather")
            .description("Weather measurements")
            
            .addField("time", fac.createTime()
                .asSamplingTimeIsoUTC())
            
            .addField("stationID", fac.createText()
                .definition(SWEConstants.DEF_SYSTEM_ID)
                .pattern("[0-9A-Z]{5-10}"))
            
            .addField("temperature", fac.createQuantity()
                .definition("http://mmisw.org/ont/cf/parameter/air_temperature")
                .label("Air Temperature")
                .uomCode("Cel"))
            
            .addField("pressure", fac.createQuantity()
                .definition("http://mmisw.org/ont/cf/parameter/air_pressure")
                .label("Atmospheric Pressure")
                .uomCode("hPa"))
            
            .addField("windSpeed", fac.createQuantity()
                .definition("http://mmisw.org/ont/cf/parameter/wind_speed")
                .label("Wind Speed")
                .uomCode("m/s"))
            
            .addField("windDirection", fac.createQuantity()
                .definition("http://mmisw.org/ont/cf/parameter/wind_from_direction")
                .label("Wind Direction")
                .uomCode("deg")
                .refFrame("http://sensorml.com/ont/swe/property/NED", "z"))
            
            .build();
     
        // also generate encoding definition
        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    
    private void sendMeasurement(FakeWeatherStation station)
    {                
        // generate latest record
        if (latestRecord == null)
            latestRecord = station.createMeasurement(dataStruct.createDataBlock());
        else
            latestRecord = station.createMeasurement(latestRecord);
        
        // send event
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, FakeWeatherNetworkOutput.this, station.getUniqueIdentifier(), latestRecord));        
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
                for (var station: getParentProducer().stations.values())
                    sendMeasurement(station);
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));        
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
    	// sample every 1 second
        return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
}
