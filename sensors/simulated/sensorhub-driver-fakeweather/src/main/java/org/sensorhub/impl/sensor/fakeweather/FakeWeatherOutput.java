/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakeweather;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.fakeweather.FakeWeatherOutput;
import org.sensorhub.api.data.DataEvent;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


public class FakeWeatherOutput extends AbstractSensorOutput<FakeWeatherSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEncoding;
    Timer timer;
    Random rand = new Random();
    
    // reference values around which actual values vary
    double tempRef = 20.0;
    double pressRef = 1013.0;
    double windSpeedRef = 5.0;
    double directionRef = 0.0;
    
    // initialize then keep new values for each measurement
    double temp = tempRef;
    double press = pressRef;
    double windSpeed = windSpeedRef;
    double windDir = directionRef;

    
    public FakeWeatherOutput(FakeWeatherSensor parentSensor)
    {
        super("weather", parentSensor);
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // create output data structure
        dataStruct = fac.createRecord()
            .name(getName())
            .definition("urn:osh:data:weather")
            .description("Weather measurements")
            
            .addField("time", fac.createTime()
                .asSamplingTimeIsoUTC())
            
            .addField("temperature", fac.createQuantity()
                .definition(SWEHelper.getCfUri("air_temperature"))
                .label("Air Temperature")
                .uomCode("Cel"))
            
            .addField("pressure", fac.createQuantity()
                .definition(SWEHelper.getCfUri("air_pressure"))
                .label("Atmospheric Pressure")
                .uomCode("hPa"))
            
            .addField("windSpeed", fac.createQuantity()
                .definition(SWEHelper.getCfUri("wind_speed"))
                .label("Wind Speed")
                .uomCode("m/s"))
            
            .addField("windDirection", fac.createQuantity()
                .definition(SWEHelper.getCfUri("wind_from_direction"))
                .label("Wind Direction")
                .uomCode("deg")
                .refFrame(SWEConstants.REF_FRAME_NED, "z"))
            
            .build();
     
        // also generate encoding definition
        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    
    private void sendMeasurement()
    {                
        // generate new weather values
        double time = System.currentTimeMillis() / 1000.;
        
        // temperature; value will increase or decrease by less than 0.1 deg
        temp += variation(temp, tempRef, 0.001, 0.1);
        
        // pressure; value will increase or decrease by less than 20 hPa
        press += variation(press, pressRef, 0.001, 0.1);
        
        // wind speed; keep positive
        // vary value between +/- 10 m/s
        windSpeed += variation(windSpeed, windSpeedRef, 0.001, 0.1);
        windSpeed = windSpeed < 0.0 ? 0.0 : windSpeed; 
        
        // wind direction; keep between 0 and 360 degrees
        windDir += 1.0 * (2.0 * Math.random() - 1.0);
        windDir = windDir < 0.0 ? windDir+360.0 : windDir;
        windDir = windDir > 360.0 ? windDir-360.0 : windDir;
        
        parentSensor.getLogger().trace(String.format("temp=%5.2f, press=%4.2f, wind speed=%5.2f, wind dir=%3.1f", temp, press, windSpeed, windDir));
        
        // build and publish datablock
        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, temp);
        dataBlock.setDoubleValue(2, press);
        dataBlock.setDoubleValue(3, windSpeed);
        dataBlock.setDoubleValue(4, windDir);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, FakeWeatherOutput.this, dataBlock));        
    }
    
    
    private double variation(double val, double ref, double dampingCoef, double noiseSigma)
    {
        return -dampingCoef*(val - ref) + noiseSigma*rand.nextGaussian();
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
                sendMeasurement();
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
