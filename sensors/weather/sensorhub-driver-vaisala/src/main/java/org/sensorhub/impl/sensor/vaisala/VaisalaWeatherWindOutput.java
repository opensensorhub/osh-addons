package org.sensorhub.impl.sensor.vaisala;

import java.util.Timer;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class VaisalaWeatherWindOutput extends AbstractSensorOutput<VaisalaWeatherSensor>
{
    //private static final Logger log = LoggerFactory.getLogger(FakeWeatherOutput.class);
    DataComponent weatherDataWind;
    DataEncoding weatherEncoding;
    Timer timer;
    BufferedReader reader;
    BufferedWriter writer;
    volatile boolean started;
    String token = null;
    int cnt;
    int windSum;
    int DataRecLen;
    String[] windMessage = null;
    
    public VaisalaWeatherWindOutput(VaisalaWeatherSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "wind";
    }


    protected void init(String WindSettings)
    {
    	//System.out.println("");
    	//System.out.println("Configuring Wind Message Data Parameters...");
        SWEHelper fac = new SWEHelper();
        
        // Get number of wind measurements being requested to preallocate dataRecord
        
        windSum = WindSettings.replaceAll("[0]", "").length();
        //System.out.println("No. Wind Parameter = " + windSum);
        
        // Add 1 for time field
        DataRecLen = 1 + windSum;
        //System.out.println("Total No. Parameters = " + DataRecLen);
        
        // build SWE Common record structure
    	weatherDataWind = fac.newDataRecord(DataRecLen);
        weatherDataWind.setName(getName());
        weatherDataWind.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        weatherDataWind.setDescription("Wind measurements");
        
        // add time, temperature, pressure, wind speed and wind direction fields
        weatherDataWind.addComponent("time", fac.newTimeStampIsoUTC());
        
        /************************* Add appropriate wind data fields **************************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR1 Wind Sensor Settings...");
        
        //compare wind settings bits and add appropriate data components to block
        //System.out.println("Dn Bit = " + WindSettings.charAt(0));
        if (WindSettings.charAt(0) == '1')
        {
        	// for wind direction, we also specify a reference frame
        	Quantity q1 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionMin"), "Wind Direction Minimum", null, "deg");
            q1.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q1.setAxisID("z");
            weatherDataWind.addComponent("windDirMin", q1);
        }
        
        //System.out.println("Dm Bit = " + WindSettings.charAt(1));
        if (WindSettings.charAt(1) == '1')
        {
        	// for wind direction, we also specify a reference frame
        	Quantity q2 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionAvg"), "Wind Direction Average", null, "deg");
            q2.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q2.setAxisID("z");
            weatherDataWind.addComponent("windDirAvg", q2);
        }
        
        //System.out.println("Dx Bit = " + WindSettings.charAt(2));
        if (WindSettings.charAt(2) == '1')
        {
        	// for wind direction, we also specify a reference frame
        	Quantity q3 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionMax"), "Wind Direction Maximum", null, "deg");
            q3.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q3.setAxisID("z");
            weatherDataWind.addComponent("windDirMax", q3);
        }
        
        //System.out.println("Sn Bit = " + WindSettings.charAt(3));
        if (WindSettings.charAt(3) == '1')
        {
        	weatherDataWind.addComponent("windSpeedMin", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedMin"), "Wind Speed Minumum", null, "mph"));
        }
        
        //System.out.println("Sm Bit = " + WindSettings.charAt(4));
        if (WindSettings.charAt(4) == '1')
        {
        	weatherDataWind.addComponent("windSpeedAvg", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedAvg"), "Wind Speed Average", null, "mph"));
        }
        
        //System.out.println("Sx Bit = " + WindSettings.charAt(5));
        if (WindSettings.charAt(5) == '1')
        {
        	weatherDataWind.addComponent("windSpeedMax", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedMax"), "Wind Speed Maximum", null, "mph"));
        }
        /*************************************************************************************************************************************************************************************/

        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
    }

    
    public void ParseAndSendWindMeasurement(String windInMessage)
    {
    	//System.out.println("Wind Message: " + windInMessage);
    	windMessage = windInMessage.split(","); // split wind message
    	DataBlock dataBlock = weatherDataWind.createDataBlock();
    	dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000);
    	
    	// parse wind message and place data in block
    	for (int cnt = 1; cnt < windMessage.length; cnt++)
    	{
    		/*************************** Wind Messages ****************************/
    		if (windMessage[cnt].startsWith("Dn"))
    			if (windMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Dn = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Dn = " + Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (windMessage[cnt].startsWith("Dm"))
    			if (windMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Dm = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Dm = " + Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (windMessage[cnt].startsWith("Dx"))
    			if (windMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Dx = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Dx = " + Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (windMessage[cnt].startsWith("Sn"))
    			if (windMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Sn = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Sn = " + Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (windMessage[cnt].startsWith("Sm"))
    			if (windMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Sm = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Sm = " + Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (windMessage[cnt].startsWith("Sx"))
    			if (windMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Sx = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Sx = " + Double.parseDouble(windMessage[cnt].replaceAll("[^0-9.]", "")));
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
    	eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VaisalaWeatherWindOutput.this, dataBlock));
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
        return weatherDataWind;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return weatherEncoding;
    }
}
