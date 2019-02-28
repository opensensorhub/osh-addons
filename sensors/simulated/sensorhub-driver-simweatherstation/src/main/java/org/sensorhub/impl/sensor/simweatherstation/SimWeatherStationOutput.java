package org.sensorhub.impl.sensor.simweatherstation;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class SimWeatherStationOutput extends AbstractSensorOutput<SimWeatherStationSensor>
{
    DataComponent dataStruct;
    DataEncoding dataEnc;
    Timer timer;
    Random rand = new Random();
    
    // reference values around which actual values vary
    double pressRef = 990.0;
    double tempRef = 30.0;
    double humidRef = 35.0;
    double rainRef = 2.0;
    double windSpeedRef = 5.0;
    double windDirRef = 120.0;
    
    // initialize then keep new values for each measurement
    double press = pressRef;
    double temp = tempRef;
    double humid = humidRef;
    double rain = rainRef;
    double windSpeed = windSpeedRef;
    double windDir = windDirRef;
    
    public SimWeatherStationOutput(SimWeatherStationSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "Sim Weather";
    }


    protected void init()
    {
        SWEHelper fac = new SWEHelper();
        dataStruct = fac.newDataRecord(7);
        dataStruct.setName(getName());
        
        // build SWE Common record structure
        dataStruct.setDefinition("http://sensorml.com/ont/swe/property/Weather");
        dataStruct.setDescription("Weather Station Data");
        
        /************************* Add appropriate data fields *******************************************************************************/
        // add time, average, and instantaneous radiation exposure levels
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        dataStruct.addComponent("pressure", fac.newQuantity(SWEHelper.getPropertyUri("BarometricPressure"), "Barometric Pressure", null, "mbar"));
        dataStruct.addComponent("temperature", fac.newQuantity(SWEHelper.getPropertyUri("Temperature"), "Air Temperature", null, "degC"));
        dataStruct.addComponent("relHumidity", fac.newQuantity(SWEHelper.getPropertyUri("RelativeHumidity"), " Relative Humidity", null, "%"));
        dataStruct.addComponent("rainAccum", fac.newQuantity(SWEHelper.getPropertyUri("RainAccumulation"), "Rain Accumulation", null, "tips"));
        dataStruct.addComponent("windSpeed", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeed"), "Wind Speed", null, "m/s"));
        Quantity q = fac.newQuantity(SWEHelper.getPropertyUri("WindDirection"), "Wind Direction", null, "deg");
        q.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        q.setAxisID("z");
        dataStruct.addComponent("windDir", q);
        
        /*************************************************************************************************************************************/
        
        // also generate encoding definition
        dataEnc = fac.newTextEncoding(",", "\n");
    }
    
    private void sendMeasurement()
    {	
    	// generate new weather values
    	long time = System.currentTimeMillis();
        
        // pressure; value will increase or decrease by no more than 1 mbar
    	press = press + Math.random() - Math.random();
    	if (press < 980 || press > 1000)
    		press = pressRef + Math.random() - Math.random();
        
        // temperature; value will increase or decrease by no more than 1 deg
    	temp = temp + Math.random() - Math.random();
    	if (temp < 25 || temp > 35)
    		temp = tempRef + Math.random() - Math.random();
        
        // humidity; value will increase or decrease by no more than 1%
    	humid = humid + Math.random() - Math.random();
    	if (humid < 30 || humid > 40)
    		humid = humidRef + Math.random() - Math.random();
        
        // rain; value will increase or decrease by no more than 1 tip
    	rain = rain + Math.random() - Math.random();
    	if (rain < 0 || rain > 5)
    		rain = rainRef + Math.random() - Math.random();

        // wind speed; keep positive
        // vary value between +/- 1 m/s
    	windSpeed = windSpeed + Math.random() - Math.random();
    	if (windSpeed < 0 || windSpeed > 10)
    		windSpeed = windSpeedRef + Math.random() - Math.random();
        
        // wind direction; keep between 0 and 360 degrees
    	windDir = windDir + Math.random() - Math.random();
    	if (windDir < 0 || windDir >= 360)
    		windDir = windDirRef + Math.random() - Math.random();
        
        parentSensor.getLogger().trace(String.format("press=%4.2f, temp=%5.2f, humid=%5.2f, wind speed=%5.2f, wind dir=%3.1f, rain=%5.1f", press, temp, humid, windSpeed, windDir, rain));
        
        DataBlock dataBlock;
    	if (latestRecord == null)
    	    dataBlock = dataStruct.createDataBlock();
    	else
    	    dataBlock = latestRecord.renew();
    	
    	dataBlock.setDoubleValue(0, time/1000);
    	dataBlock.setDoubleValue(1, Math.round(press*100.0)/100.0);
    	dataBlock.setDoubleValue(2, Math.round(temp*100.0)/100.0);
    	dataBlock.setDoubleValue(3, Math.round(humid*100.0)/100.0);
    	dataBlock.setDoubleValue(4, Math.round(rain));
    	dataBlock.setDoubleValue(5, Math.round(windSpeed*100.0)/100.0);
    	dataBlock.setDoubleValue(6, Math.round(windDir*100.0)/100.0);

    	// update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, SimWeatherStationOutput.this, dataBlock));
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
    	// sample every 5 seconds
        return 15.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEnc;
    }
}