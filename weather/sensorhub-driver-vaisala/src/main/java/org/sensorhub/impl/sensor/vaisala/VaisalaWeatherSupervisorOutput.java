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

public class VaisalaWeatherSupervisorOutput extends AbstractSensorOutput<VaisalaWeatherSensor>
{
    //private static final Logger log = LoggerFactory.getLogger(FakeWeatherOutput.class);
    DataComponent weatherDataSup;
    DataEncoding weatherEncoding;
    Timer timer;
    BufferedReader reader;
    BufferedWriter writer;
    volatile boolean started;
    String token = null;
    int cnt;
    int supSum;
    int DataRecLen;
    String[] supMessage = null;
    
    public VaisalaWeatherSupervisorOutput(VaisalaWeatherSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "supervisor";
    }


    protected void init(String SupSettings)
    {
    	//System.out.println("");
    	//System.out.println("Configuring Supervisor Message Data Parameters...");
        SWEHelper fac = new SWEHelper();
        
        // Get total number of measurements being requested to preallocate dataRecord
        supSum = SupSettings.replaceAll("[0]", "").length();
        //System.out.println("No. Sup Parameters = " + supSum);
        
        // Add 1 for time field
        DataRecLen = 1 + supSum;
        //System.out.println("Total No. Parameters = " + DataRecLen);
        
        // build SWE Common record structure
    	weatherDataSup = fac.newDataRecord(DataRecLen);
        weatherDataSup.setName(getName());
        weatherDataSup.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        weatherDataSup.setDescription("Supervisor measurements");
        
        // add time, temperature, pressure, wind speed and wind direction fields
        weatherDataSup.addComponent("time", fac.newTimeStampIsoUTC());
        
        /************************* Add appropriate supervisor data fields ********************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR5 Supervisor Settings...");
        
        // compare sup settings bits and add appropriate data components to block
        //System.out.println("Heater Temp Bit = " + SupSettings.charAt(0));
        
        if (SupSettings.charAt(0) == '1')
        {
        	weatherDataSup.addComponent("heaterTemp", fac.newQuantity(SWEHelper.getPropertyUri("HeaterTemperature"), "Heating Temperature", null, "Fah"));
        }
        
        //System.out.println("Heater Voltage Bit = " + SupSettings.charAt(1));
        if (SupSettings.charAt(1) == '1')
        {
        	weatherDataSup.addComponent("heaterVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Heating Voltage", "Voltage of the internal heating element", "V"));
        }
        
        //System.out.println("Supply Voltage Bit = " + SupSettings.charAt(2));
        if (SupSettings.charAt(2) == '1')
        {
        	weatherDataSup.addComponent("supplyVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Supply Voltage", null, "V"));
        }
        
        //System.out.println("Reference Voltage Bit = " + SupSettings.charAt(3));
        if (SupSettings.charAt(3) == '1')
        {
        	weatherDataSup.addComponent("referenceVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Reference Voltage", null, "V"));
        }
        
        //System.out.println("Id Bit = " + SupSettings.charAt(4));
        if (SupSettings.charAt(4) == '1')
        {
        	weatherDataSup.addComponent("information", fac.newQuantity(SWEHelper.getPropertyUri("Text"), "Information", null, null));
        }
        /*************************************************************************************************************************************************************************************/
        
        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
    }

    
    public void ParseAndSendSupMeasurement(String supInMessage)
    {
    	//System.out.println("Supervisor Message: " + supInMessage);
    	supMessage = supInMessage.split(","); // split sup message
    	DataBlock dataBlock = weatherDataSup.createDataBlock();
    	dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000);
    	
    	// parse sup message and place data in block
    	for (int cnt = 1; cnt < supMessage.length; cnt++)
    	{
    		/************************ Supervisor Messages *************************/
    		if (supMessage[cnt].startsWith("Th"))
    			if (supMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Th = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Th = " + Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (supMessage[cnt].startsWith("Vh"))
    			if (supMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Vh = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Vh = " + Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (supMessage[cnt].startsWith("Vs"))
    			if (supMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Vs = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Vs = " + Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (supMessage[cnt].startsWith("Vr"))
    			if (supMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Vr = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Vr = " + Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (supMessage[cnt].startsWith("Id"))
    			if (supMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Id = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Id = " + Double.parseDouble(supMessage[cnt].replaceAll("[^0-9.]", "")));
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
    	eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VaisalaWeatherSupervisorOutput.this, dataBlock));
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
        return weatherDataSup;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return weatherEncoding;
    }
}
