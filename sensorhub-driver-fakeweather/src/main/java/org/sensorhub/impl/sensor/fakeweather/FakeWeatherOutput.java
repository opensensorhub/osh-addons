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
import org.sensorhub.impl.sensor.fakeweather.FakeWeatherSensor;
import org.sensorhub.api.sensor.SensorDataEvent;

import java.util.Timer;
import java.util.TimerTask;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;

import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TimeImpl;
import org.vast.sweCommon.SWEConstants;


public class FakeWeatherOutput extends AbstractSensorOutput<FakeWeatherSensor>
{
    //private static final Logger log = LoggerFactory.getLogger(FakeWeatherOutput.class);
    DataComponent weatherData;
    DataBlock latestRecord;
    boolean sendData;
    Timer timer;
    
    // initialize then keep new values for each measurement
    double temp = 26.0;
    double pressure = 1013.0;
    double speed = 4.5;
    double direction = 60.5;

    public FakeWeatherOutput(FakeWeatherSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "weather";
    }


    protected void init()
    {
        // build SWE Common data structure
    	// Time, temperature, pressure, wind speed, wind direction
        weatherData = new DataRecordImpl(5);
        weatherData.setName(getName());
        weatherData.setDefinition("http://sensorml.com/ont/swe/property/Weather");
        
        Time c1 = new TimeImpl();
        c1.getUom().setHref(Time.ISO_TIME_UNIT);
        c1.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        weatherData.addComponent("time", c1);

        Quantity c;
        c = new QuantityImpl();
        c.getUom().setCode("degC");
        c.setDefinition("http://sensorml.com/ont/swe/property/AtmosphericTemperature");
        weatherData.addComponent("temperature",c);

        c = new QuantityImpl();
        c.getUom().setCode("hPa");
        c.setDefinition("http://sensorml.com/ont/swe/property/AtmosphericPressure");
        weatherData.addComponent("pressure", c);

        c = new QuantityImpl();
        c.getUom().setCode("m/s");
        c.setDefinition("http://sensorml.com/ont/swe/property/WindSpeed");
        weatherData.addComponent("windSpeed", c);        

        c = new QuantityImpl();
        c.setDefinition("http://sensorml.com/ont/swe/property/WindDirection");
        c.getUom().setCode("deg");
        // TODO check if this is the best local coordinate frame and where it will be defined
        c.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        c.setAxisID("down");       
        weatherData.addComponent("windSpeed", c);        
}


    
    
    private void sendMeasurement()
    {
                
        // generate new weather values
        double time = System.currentTimeMillis() / 1000.;
        
        // temperature; value will increase or decrease by less than 1.0 deg
        temp += Math.random() - Math.random();
        
        // pressure; value will increase or decrease by less than 20 hPa
        pressure += 20.0*Math.random() - 20.0*Math.random();
        
        // wind speed; keep positive
        speed += 20.0*Math.random() - 20.0*Math.random();
        speed = speed<0.0?0.0:speed; 
        
        // wind direction; keep between 0 and 360 degrees
        direction += 5.0*Math.random() - 5.0*Math.random();
        direction = direction<0.0?direction+360.0:direction;
        direction = direction>360.0?direction-360.0:direction;
        
        
        // build and publish datablock
        DataBlock dataBlock = weatherData.createDataBlock();
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, temp);
        dataBlock.setDoubleValue(2, pressure);
        dataBlock.setDoubleValue(3, speed);
        dataBlock.setDoubleValue(4, direction);
        
        // update latest record and send event
        latestRecord = dataBlock;
        eventHandler.publishEvent(new SensorDataEvent(time, FakeWeatherOutput.this, dataBlock));
        
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
        
        timer.scheduleAtFixedRate(task, 0, 1000);        
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
        return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return weatherData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return new TextEncodingImpl(",", "\n");
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return latestRecord;
    }
    
    
    @Override
    public double getLatestRecordTime()
    {
        if (latestRecord != null)
            return latestRecord.getDoubleValue(0);
        
        return Double.NaN;
    }

}
