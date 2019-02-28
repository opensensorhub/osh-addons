package org.sensorhub.impl.sensor.vaisala;

import java.util.Timer;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class VaisalaWeatherPrecipOutput extends AbstractSensorOutput<VaisalaWeatherSensor>
{
    //private static final Logger log = LoggerFactory.getLogger(FakeWeatherOutput.class);
    DataComponent weatherDataPrecip;
    DataEncoding weatherEncoding;
    Timer timer;
    BufferedReader reader;
    BufferedWriter writer;
    volatile boolean started;
    String token = null;
    int cnt;
    int precipSum;
    int DataRecLen;
    String[] precipMessage = null;
    
    public VaisalaWeatherPrecipOutput(VaisalaWeatherSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "precipitation";
    }


    protected void init(String PrecipSettings)
    {
    	//System.out.println("");
    	//System.out.println("Configuring Precip Message Data Parameters...");
        SWEHelper fac = new SWEHelper();
        
        // Get total number of measurements being requested to preallocate dataRecord
        precipSum = PrecipSettings.replaceAll("[0]", "").length();
        //System.out.println("No. Precip Parameters = " + precipSum);
        
        // Add 1 for time field
        DataRecLen = 1 + precipSum;
        //System.out.println("Total No. Parameters = " + DataRecLen);
        
        // build SWE Common record structure
    	weatherDataPrecip = fac.newDataRecord(DataRecLen);
        weatherDataPrecip.setName(getName());
        weatherDataPrecip.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        weatherDataPrecip.setDescription("Precipitation measurements");
        
        // add time, temperature, pressure, wind speed and wind direction fields
        weatherDataPrecip.addComponent("time", fac.newTimeStampIsoUTC());
        
        /************************* Add appropriate precip data fields ********************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR3 Precip Sensor Settings...");
        
        // compare precip settings bits and add appropriate data components to block
        //System.out.println("Rain Accum Bit = " + PrecipSettings.charAt(0));
        if (PrecipSettings.charAt(0) == '1')
        {
        	weatherDataPrecip.addComponent("rainAccum", fac.newQuantity(SWEHelper.getPropertyUri("RainAccumulation"), "Rain Accumulation", null, "in"));
        }
        
        //System.out.println("Rain Duration Bit = " + PrecipSettings.charAt(1));
        if (PrecipSettings.charAt(1) == '1')
        {
        	weatherDataPrecip.addComponent("rainDur", fac.newQuantity(SWEHelper.getPropertyUri("RainDuration"), "Rain Duration", null, "s"));
        }
        //System.out.println("Rain Intensity Bit = " + PrecipSettings.charAt(2));
        if (PrecipSettings.charAt(2) == '1')
        {
        	weatherDataPrecip.addComponent("rainIntensity", fac.newQuantity(SWEHelper.getPropertyUri("RainIntensity"), "Rain Intensity", null, "in/h"));
        }
        
        //System.out.println("Hail Accum Bit = " + PrecipSettings.charAt(3));
        if (PrecipSettings.charAt(3) == '1')
        {
        	weatherDataPrecip.addComponent("hailAccum", fac.newQuantity(SWEHelper.getPropertyUri("HailAccumulation"), "Hail Accumulation", null, "in"));
        }
        
        //System.out.println("Hail Duration Bit = " + PrecipSettings.charAt(4));
        if (PrecipSettings.charAt(4) == '1')
        {
        	weatherDataPrecip.addComponent("HailDur", fac.newQuantity(SWEHelper.getPropertyUri("HailDuration"), "Hail Duration", null, "s"));
        }
        
        //System.out.println("Hail Intensity Bit = " + PrecipSettings.charAt(5));
        if (PrecipSettings.charAt(5) == '1')
        {
        	weatherDataPrecip.addComponent("hailIntensity", fac.newQuantity(SWEHelper.getPropertyUri("HailIntensity"), "Hail Intensity", null, "in/h"));
        }
        
        //System.out.println("Rain Peak Bit = " + PrecipSettings.charAt(6));
        if (PrecipSettings.charAt(6) == '1')
        {
        	weatherDataPrecip.addComponent("rainPeakIntensity", fac.newQuantity(SWEHelper.getPropertyUri("RainPeakIntensity"), "Rain Peak Intensity", null, "in/h"));
        }
        
        //System.out.println("Hail Peak Bit = " + PrecipSettings.charAt(7));
        if (PrecipSettings.charAt(7) == '1')
        {
        	weatherDataPrecip.addComponent("hailPeakIntensity", fac.newQuantity(SWEHelper.getPropertyUri("HailPeakIntensity"), "Hail Peak Intensity", null, "in/h"));
        }
        /*************************************************************************************************************************************************************************************/
        
        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
    }

    
    public void ParseAndSendPrecipMeasurement(String precipInMessage)
    {
    	//System.out.println("Precip Message: " + precipInMessage);
    	precipMessage = precipInMessage.split(","); // split precip message
    	DataBlock dataBlock = weatherDataPrecip.createDataBlock();
    	dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000);
    	
    	// parse precip message and place data in block
    	for (int cnt = 1; cnt < precipMessage.length; cnt++)
    	{
    		/************************** Precip Messages ***************************/
    		if (precipMessage[cnt].startsWith("Rc"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Rc = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Rc = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (precipMessage[cnt].startsWith("Rd"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Rd = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Rd = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (precipMessage[cnt].startsWith("Ri"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Ri = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Ri = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (precipMessage[cnt].startsWith("Hc"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hc = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hc = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (precipMessage[cnt].startsWith("Hd"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hd = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hd = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (precipMessage[cnt].startsWith("Hi"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hi = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hi = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (precipMessage[cnt].startsWith("Rp"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Rp = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Rp = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (precipMessage[cnt].startsWith("Hp"))
    			if (precipMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hp = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hp = " + Double.parseDouble(precipMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		else
    			System.err.println("Unrecognized Parameter");
    		/*********************************************************************/
    	}
    	
    	/*********************************** Build and Publish dataBlock ****************************************/
    	// Update Latest Record and Send Event
    	latestRecord = dataBlock;
    	latestRecordTime = System.currentTimeMillis();
    	eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VaisalaWeatherPrecipOutput.this, dataBlock));
    	/********************************************************************************************************/
	}


    protected void start()
    {   
    }


    protected void stop()
    {
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
        return weatherDataPrecip;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return weatherEncoding;
    }
}
