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

public class VaisalaWeatherCompositeOutput extends AbstractSensorOutput<VaisalaWeatherSensor>
{
    //private static final Logger log = LoggerFactory.getLogger(FakeWeatherOutput.class);
    DataComponent weatherDataComp;
    DataEncoding weatherEncoding;
    Timer timer;
    BufferedReader reader;
    BufferedWriter writer;
    volatile boolean started;
    String token = null;
    int cnt;
    int supSum;
    int windSum;
    int ptuSum;
    int precipSum;
    int DataRecLen;
    String[] compMessage = null;
    
    public VaisalaWeatherCompositeOutput(VaisalaWeatherSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "weather";
    }


    protected void init(String SupSettings,String WindSettings,String PTUSettings,String PrecipSettings)
    {
    	//System.out.println("");
    	//System.out.println("Configuring Composite Message Data Parameters...");
        SWEHelper fac = new SWEHelper();
        
        // Get total number of measurements being requested to preallocate dataRecord
        supSum = SupSettings.replaceAll("[0]", "").length();
        //System.out.println("No. Sup Parameters = " + supSum);
        
        windSum = WindSettings.replaceAll("[0]", "").length();
        //System.out.println("No. Wind Parameters = " + windSum);
        
        ptuSum = PTUSettings.replaceAll("[0]", "").length();
        //System.out.println("No. PTU Parameters = " + ptuSum);
        
        precipSum = PrecipSettings.replaceAll("[0]", "").length();
        //System.out.println("No. Precip Parameters = " + precipSum);
        
        // Add 1 for time field
        DataRecLen = 1 + supSum + windSum + ptuSum + precipSum;
        //System.out.println("Total No. Parameters = " + DataRecLen);
        
        // build SWE Common record structure
    	weatherDataComp = fac.newDataRecord(DataRecLen);
        weatherDataComp.setName(getName());
        weatherDataComp.setDefinition("http://sensorml.com/ont/swe/property/WeatherData");
        weatherDataComp.setDescription("Weather measurements");
        
        // add time, temperature, pressure, wind speed and wind direction fields
        weatherDataComp.addComponent("time", fac.newTimeStampIsoUTC());
        
        /************************* Add appropriate wind data fields **************************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR0 Wind Sensor Settings...");
        //System.out.println("Dn Bit = " + WindSettings.charAt(0));
        if (WindSettings.charAt(0) == '1')
        {
        	// for wind direction, we also specify a reference frame
        	Quantity q1 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionMin"), "Wind Direction Minimum", null, "deg");
            q1.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q1.setAxisID("z");
            weatherDataComp.addComponent("windDirMin", q1);
        }
        
        //System.out.println("Dm Bit = " + WindSettings.charAt(1));
        if (WindSettings.charAt(1) == '1')
        {
        	// for wind direction, we also specify a reference frame
        	Quantity q2 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionAvg"), "Wind Direction Average", null, "deg");
            q2.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q2.setAxisID("z");
            weatherDataComp.addComponent("windDirAvg", q2);
        }
        
        //System.out.println("Dx Bit = " + WindSettings.charAt(2));
        if (WindSettings.charAt(2) == '1')
        {
        	// for wind direction, we also specify a reference frame
        	Quantity q3 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionMax"), "Wind Direction Maximum", null, "deg");
            q3.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
            q3.setAxisID("z");
            weatherDataComp.addComponent("windDirMax", q3);
        }
        
        //System.out.println("Sn Bit = " + WindSettings.charAt(3));
        if (WindSettings.charAt(3) == '1')
        {
        	weatherDataComp.addComponent("windSpeedMin", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedMin"), "Wind Speed Minumum", null, "mph"));
        }
        
        //System.out.println("Sm Bit = " + WindSettings.charAt(4));
        if (WindSettings.charAt(4) == '1')
        {
        	weatherDataComp.addComponent("windSpeedAvg", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedAvg"), "Wind Speed Average", null, "mph"));
        }
        
        //System.out.println("Sx Bit = " + WindSettings.charAt(5));
        if (WindSettings.charAt(5) == '1')
        {
        	weatherDataComp.addComponent("windSpeedMax", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedMax"), "Wind Speed Maximum", null, "mph"));
        }
        /*************************************************************************************************************************************************************************************/

        /************************* Add appropriate PTU data fields ********************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR0 PTU Sensor Settings...");
        //System.out.println("Air Pressure Bit = " + PTUSettings.charAt(0));
        if (PTUSettings.charAt(0) == '1')
        {
        	weatherDataComp.addComponent("pressure", fac.newQuantity(SWEHelper.getPropertyUri("BarometricPressure"), "Barometric Pressure", null, "inHg"));
        }
        
        //System.out.println("Ext Air Temp Bit = " + PTUSettings.charAt(1));
        if (PTUSettings.charAt(1) == '1')
        {
        	weatherDataComp.addComponent("tempExt", fac.newQuantity(SWEHelper.getPropertyUri("AirTemperature"), "Air Temperature", null, "Fah"));
        }
        
        //System.out.println("Int Air Temp Bit = " + PTUSettings.charAt(2));
        if (PTUSettings.charAt(2) == '1')
        {
        	weatherDataComp.addComponent("tempInt", fac.newQuantity(SWEHelper.getPropertyUri("AirTemperature"), "Internal Temperature", null, "Fah"));
        }
        
        //System.out.println("Rel Humidity Bit = " + PTUSettings.charAt(3));
        if (PTUSettings.charAt(3) == '1')
        {
        	weatherDataComp.addComponent("relHumidity", fac.newQuantity(SWEHelper.getPropertyUri("RelativeHumidity"), "Relative Humidity", null, "%"));
        }
        /*************************************************************************************************************************************************************************************/
     
        /************************* Add appropriate precip data fields ********************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR0 Precip Sensor Settings...");
        //System.out.println("Rain Accum Bit = " + PrecipSettings.charAt(0));
        if (PrecipSettings.charAt(0) == '1')
        {
        	weatherDataComp.addComponent("rainAccum", fac.newQuantity(SWEHelper.getPropertyUri("RainAccumulation"), "Rain Accumulation", null, "in"));
        }
        
        //System.out.println("Rain Duration Bit = " + PrecipSettings.charAt(1));
        if (PrecipSettings.charAt(1) == '1')
        {
        	weatherDataComp.addComponent("rainDur", fac.newQuantity(SWEHelper.getPropertyUri("RainDuration"), "Rain Duration", null, "s"));
        }
        //System.out.println("Rain Intensity Bit = " + PrecipSettings.charAt(2));
        if (PrecipSettings.charAt(2) == '1')
        {
        	weatherDataComp.addComponent("rainIntensity", fac.newQuantity(SWEHelper.getPropertyUri("RainIntensity"), "Rain Intensity", null, "in/h"));
        }
        
        //System.out.println("Hail Accum Bit = " + PrecipSettings.charAt(3));
        if (PrecipSettings.charAt(3) == '1')
        {
        	weatherDataComp.addComponent("hailAccum", fac.newQuantity(SWEHelper.getPropertyUri("HailAccumulation"), "Hail Accumulation", null, "in"));
        }
        
        //System.out.println("Hail Duration Bit = " + PrecipSettings.charAt(4));
        if (PrecipSettings.charAt(4) == '1')
        {
        	weatherDataComp.addComponent("HailDur", fac.newQuantity(SWEHelper.getPropertyUri("HailDuration"), "Hail Duration", null, "s"));
        }
        
        //System.out.println("Hail Intensity Bit = " + PrecipSettings.charAt(5));
        if (PrecipSettings.charAt(5) == '1')
        {
        	weatherDataComp.addComponent("hailIntensity", fac.newQuantity(SWEHelper.getPropertyUri("HailIntensity"), "Hail Intensity", null, "in/h"));
        }
        
        //System.out.println("Rain Peak Bit = " + PrecipSettings.charAt(6));
        if (PrecipSettings.charAt(6) == '1')
        {
        	weatherDataComp.addComponent("rainPeakIntensity", fac.newQuantity(SWEHelper.getPropertyUri("RainPeakIntensity"), "Rain Peak Intensity", null, "in/h"));
        }
        
        //System.out.println("Hail Peak Bit = " + PrecipSettings.charAt(7));
        if (PrecipSettings.charAt(7) == '1')
        {
        	weatherDataComp.addComponent("hailPeakIntensity", fac.newQuantity(SWEHelper.getPropertyUri("HailPeakIntensity"), "Hail Peak Intensity", null, "in/h"));
        }
        /*************************************************************************************************************************************************************************************/
        
        /************************* Add appropriate supervisor data fields ********************************************************************************************************************/
        //System.out.println("");
        //System.out.println("aR0 Supervisor Settings...");
        //System.out.println("Heater Temp Bit = " + SupSettings.charAt(0));
        if (SupSettings.charAt(0) == '1')
        {
        	weatherDataComp.addComponent("heaterTemp", fac.newQuantity(SWEHelper.getPropertyUri("HeaterTemperature"), "Heating Temperature", null, "Fah"));
        }
        
        //System.out.println("Heater Voltage Bit = " + SupSettings.charAt(1));
        if (SupSettings.charAt(1) == '1')
        {
        	weatherDataComp.addComponent("heaterVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Heating Voltage", "Voltage of the internal heating element", "V"));
        }
        
        //System.out.println("Supply Voltage Bit = " + SupSettings.charAt(2));
        if (SupSettings.charAt(2) == '1')
        {
        	weatherDataComp.addComponent("supplyVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Supply Voltage", null, "V"));
        }
        
        //System.out.println("Reference Voltage Bit = " + SupSettings.charAt(3));
        if (SupSettings.charAt(3) == '1')
        {
        	weatherDataComp.addComponent("referenceVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Reference Voltage", null, "V"));
        }
        
        //System.out.println("Id Bit = " + SupSettings.charAt(4));
        if (SupSettings.charAt(4) == '1')
        {
        	weatherDataComp.addComponent("information", fac.newQuantity(SWEHelper.getPropertyUri("Text"), "Information", null, null));
        }
        /*************************************************************************************************************************************************************************************/
        
        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
    }

    
    public void ParseAndSendCompMeasurement(String compInMessage)
    {
    	//System.out.println("Comp Message: " + compInMessage);
    	compMessage = compInMessage.split(",");
    	DataBlock dataBlock = weatherDataComp.createDataBlock();
    	dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000);
    	for (int cnt = 1; cnt < compMessage.length; cnt++)
    	{
    		/*************************** Wind Messages ****************************/
    		if (compMessage[cnt].startsWith("Dn"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Dn = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Dn = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Dm"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Dm = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Dm = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Dx"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Dx = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Dx = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Sn"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Sn = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Sn = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Sm"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Sm = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Sm = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Sx"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Sx = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Sx = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		/**********************************************************************/
    		
    		/**************************** PTU Messages ****************************/
    		else if (compMessage[cnt].startsWith("Ta"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Ta = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Ta = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Tp"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Tp = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Tp = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Ua"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Ua = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Ua = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Pa"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Pa = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Pa = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		/**********************************************************************/
    		
    		/************************** Precip Messages ***************************/
    		else if (compMessage[cnt].startsWith("Rc"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Rc = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Rc = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Rd"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Rd = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Rd = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Ri"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Ri = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Ri = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Hc"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hc = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hc = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Hd"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hd = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hd = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Hi"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hi = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hi = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Rp"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Rp = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Rp = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Hp"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Hp = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Hp = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		/**********************************************************************/
    		
    		/************************ Supervisor Messages *************************/
    		else if (compMessage[cnt].startsWith("Th"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Th = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Th = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Vh"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Vh = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Vh = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Vs"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Vs = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Vs = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Vr"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Vr = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Vr = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				continue;
    			}
    		
    		else if (compMessage[cnt].startsWith("Id"))
    			if (compMessage[cnt].endsWith("#"))
    			{
    				dataBlock.setDoubleValue(cnt, Double.NaN);
    				//System.out.println("Id = " + Double.NaN);
    				continue;
    			}
    			else
    			{
    				dataBlock.setDoubleValue(cnt, Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
    				//System.out.println("Id = " + Double.parseDouble(compMessage[cnt].replaceAll("[^0-9.]", "")));
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
    	eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VaisalaWeatherCompositeOutput.this, dataBlock));
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
        return weatherDataComp;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return weatherEncoding;
    }
}
