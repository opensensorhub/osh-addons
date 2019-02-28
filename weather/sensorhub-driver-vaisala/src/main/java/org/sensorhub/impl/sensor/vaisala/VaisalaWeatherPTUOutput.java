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

public class VaisalaWeatherPTUOutput extends AbstractSensorOutput<VaisalaWeatherSensor>
{
    //private static final Logger log = LoggerFactory.getLogger(FakeWeatherOutput.class);
    DataComponent weatherDataPTU;
    DataEncoding weatherEncoding;
    Timer timer;
    BufferedReader reader;
    BufferedWriter writer;
    volatile boolean started;
    String token = null;
    int cnt;
    int ptuSum;
    int DataRecLen;
    String[] ptuMessage = null;
    
    public VaisalaWeatherPTUOutput(VaisalaWeatherSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "ptu";
    }


    protected void init(String PTUSettings)
    {
    	//System.out.println("");
    	//System.out.println("Configuring PTU Message Data Parameters...");
        SWEHelper fac = new SWEHelper();
        
        // Get number of wind measurements being requested to preallocate dataRecord
        
        ptuSum = PTUSettings.replaceAll("[0]", "").length();
        //System.out.println("No. PTU Parameters = " + ptuSum);
        
        // Add 1 for time field
        DataRecLen = 1 + ptuSum;
        //System.out.println("Total No. Parameters = " + DataRecLen);
        
        // build SWE Common record structure
    	weatherDataPTU = fac.newDataRecord(DataRecLen);
        weatherDataPTU.setName(getName());
        weatherDataPTU.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        weatherDataPTU.setDescription("PTU measurements");
        
        // add time, temperature, pressure, wind speed and wind direction fields
        weatherDataPTU.addComponent("time", fac.newTimeStampIsoUTC());
        
        /************************* Add appropriate PTU data fields ********************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR2 PTU Sensor Settings...");
        
        // compare ptu settings bits and add appropriate data components to block
        //System.out.println("Air Pressure Bit = " + PTUSettings.charAt(0));

        if (PTUSettings.charAt(0) == '1')
        {
        	weatherDataPTU.addComponent("pressure", fac.newQuantity(SWEHelper.getPropertyUri("BarometricPressure"), "Barometric Pressure", null, "inHg"));
        }
        
        //System.out.println("Ext Air Temp Bit = " + PTUSettings.charAt(1));
        if (PTUSettings.charAt(1) == '1')
        {
        	weatherDataPTU.addComponent("tempExt", fac.newQuantity(SWEHelper.getPropertyUri("AirTemperature"), "Air Temperature", null, "Fah"));
        }
        
        //System.out.println("Int Air Temp Bit = " + PTUSettings.charAt(2));
        if (PTUSettings.charAt(2) == '1')
        {
        	weatherDataPTU.addComponent("tempInt", fac.newQuantity(SWEHelper.getPropertyUri("AirTemperature"), "Internal Temperature", null, "Fah"));
        }
        
        //System.out.println("Rel Humidity Bit = " + PTUSettings.charAt(3));
        if (PTUSettings.charAt(3) == '1')
        {
        	weatherDataPTU.addComponent("relHumidity", fac.newQuantity(SWEHelper.getPropertyUri("RelativeHumidity"), "Relative Humidity", null, "%"));
        }
        /*************************************************************************************************************************************************************************************/
     
        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
    }

    
    public void ParseAndSendPTUMeasurement(String ptuInMessage)
    {
    	//System.out.println("PTU Message: " + ptuInMessage);
    	ptuMessage = ptuInMessage.split(","); // split ptu message
    	DataBlock dataBlock = weatherDataPTU.createDataBlock();
    	dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000);
    	
    	// parse ptu message and place data in block
    	for (int cnt = 1; cnt < ptuMessage.length; cnt++)
    	{
    		/**************************** PTU Messages ****************************/
    		if (ptuMessage[cnt].startsWith("Ta"))
    			if (ptuMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Ta = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Ta = " + Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (ptuMessage[cnt].startsWith("Tp"))
    			if (ptuMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Tp = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Tp = " + Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (ptuMessage[cnt].startsWith("Ua"))
    			if (ptuMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Ua = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Ua = " + Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (ptuMessage[cnt].startsWith("Pa"))
    			if (ptuMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Pa = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Pa = " + Double.parseDouble(ptuMessage[cnt].replaceAll("[^0-9.]", "")));
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
    	eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VaisalaWeatherPTUOutput.this, dataBlock));
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
        return weatherDataPTU;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return weatherEncoding;
    }
}
