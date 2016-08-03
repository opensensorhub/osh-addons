package org.sensorhub.impl.sensor.vaisala;

import java.util.StringTokenizer;
import java.util.Timer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEHelper;

public class VaisalaWeatherOutput extends AbstractSensorOutput<VaisalaWeatherSensor>
{
    //private static final Logger log = LoggerFactory.getLogger(FakeWeatherOutput.class);
    DataComponent weatherData;
    DataEncoding weatherEncoding;
    Timer timer;
    BufferedReader reader;
    BufferedWriter writer;
    volatile boolean started;
    String token = null;
    int cnt;
    
    VaisalaWeatherSensor flags = new VaisalaWeatherSensor();
    /********************* Wind Measurement Flags ********************/
    boolean DnFlag = flags.getDn(); // Direction Minimum
    boolean DmFlag = flags.getDm(); // Direction Average
    boolean DxFlag = flags.getDx(); // Direction Maximum
    boolean SnFlag = flags.getSn(); // Speed Minimum
    boolean SmFlag = flags.getSm(); // Speed Average
    boolean SxFlag = flags.getSx(); // Speed Maximum
    /*****************************************************************/
    
    /********************* PTU Measurement Flags *********************/
    boolean PaFlag = flags.getPa(); // Air Pressure
    boolean TaFlag = flags.getTa(); // Air Temperature
    boolean TpFlag = flags.getTp(); // Internal Temperature
    boolean UaFlag = flags.getUa(); // Air Humidity
    /*****************************************************************/
    
    /******************* Precip Measurement Flags ********************/
    boolean RcFlag = flags.getRc(); // Rain Amount
    boolean RdFlag = flags.getRd(); // Rain Duration
    boolean RiFlag = flags.getRi(); // Rain Intensity
    boolean HcFlag = flags.getHc(); // Hail Amount
    boolean HdFlag = flags.getHd(); // Hail Duration
    boolean HiFlag = flags.getHi(); // Hail Intensity
    boolean RpFlag = flags.getRp(); // Rain Peak
    boolean HpFlag = flags.getHp(); // Hail Peak
    /*****************************************************************/
    
    /***************** Supervisor Measurement Flags ******************/
    boolean ThFlag = flags.getTh(); // Heating Temperature
    boolean VhFlag = flags.getVh(); // Heating Voltage
    boolean VsFlag = flags.getVs(); // Supply Voltage
    boolean VrFlag = flags.getVr(); // Reference Voltage
    boolean IdFlag = flags.getId(); // Information Field
    /*****************************************************************/
    
    public VaisalaWeatherOutput(VaisalaWeatherSensor parentSensor)
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
        SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
    	weatherData = fac.newDataRecord(20);
        weatherData.setName(getName());
        weatherData.setDefinition("http://sensorml.com/ont/swe/property/Weather");
        weatherData.setDescription("Weather measurements");
        
        // add time, temperature, pressure, wind speed and wind direction fields
        weatherData.addComponent("time", fac.newTimeStampIsoUTC());
        
        // for wind direction, we also specify a reference frame
        Quantity q1 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionMin"), "Wind Direction Minimum", null, "deg");
        q1.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        q1.setAxisID("z");
        Quantity q2 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionAvg"), "Wind Direction Average", null, "deg");
        q2.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        q2.setAxisID("z");
        Quantity q3 = fac.newQuantity(SWEHelper.getPropertyUri("WindDirectionMax"), "Wind Direction Maximum", null, "deg");
        q3.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        q3.setAxisID("z");
        
        weatherData.addComponent("windDirMin", q1);
        weatherData.addComponent("windDirAvg", q2);
        weatherData.addComponent("windDirMax", q3);
        weatherData.addComponent("windSpeedMin", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedMin"), "Wind Speed Minumum", null, "mph"));
        weatherData.addComponent("windSpeedAvg", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedAvg"), "Wind Speed Average", null, "mph"));
        weatherData.addComponent("windSpeedMax", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeedMax"), "Wind Speed Maximum", null, "mph"));
        weatherData.addComponent("tempExt", fac.newQuantity(SWEHelper.getPropertyUri("AirTemperature"), "Air Temperature", null, "Fah"));
        weatherData.addComponent("tempInt", fac.newQuantity(SWEHelper.getPropertyUri("AirTemperature"), "Internal Temperature", null, "Fah"));
        weatherData.addComponent("relHumidity", fac.newQuantity(SWEHelper.getPropertyUri("RelativeHumidity"), "Relative Humidity", null, "%"));
        weatherData.addComponent("pressure", fac.newQuantity(SWEHelper.getPropertyUri("BarometricPressure"), "Barometric Pressure", null, "inHg"));
        weatherData.addComponent("rainAccum", fac.newQuantity(SWEHelper.getPropertyUri("RainAccumulation"), "Rain Accumulation", null, "in"));
        weatherData.addComponent("rainIntensity", fac.newQuantity(SWEHelper.getPropertyUri("RainIntensity"), "Rain Intensity", null, "in/h"));
        weatherData.addComponent("hailAccum", fac.newQuantity(SWEHelper.getPropertyUri("HailAccumulation"), "Hail Accumulation", null, "in"));
        weatherData.addComponent("hailIntensity", fac.newQuantity(SWEHelper.getPropertyUri("HailIntensity"), "Hail Intensity", null, "in/h"));
        weatherData.addComponent("rainPeakIntensity", fac.newQuantity(SWEHelper.getPropertyUri("RainPeakIntensity"), "Rain Peak Intensity", null, "in/h"));
        weatherData.addComponent("hailPeakIntensity", fac.newQuantity(SWEHelper.getPropertyUri("HailPeakIntensity"), "Hail Peak Intensity", null, "in/h"));
        weatherData.addComponent("heaterTemp", fac.newQuantity(SWEHelper.getPropertyUri("HeaterTemperature"), "Heating Temperature", null, "Fah"));
        weatherData.addComponent("heaterVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Heating Voltage", "Voltage of the internal heating element", "V"));
        weatherData.addComponent("supplyVoltage", fac.newQuantity(SWEHelper.getPropertyUri("Voltage"), "Supply Voltage", null, "V"));
        
     
        // also generate encoding definition
        weatherEncoding = fac.newTextEncoding(",", "\n");
        
        //reader = new BufferedReader(new InputStreamReader(parentSensor.dataIn));
    }

    
    private void getMeasurement()
    {
        /*
        Current Message Order
        Dn = Wind Direction Minimum
        Dm = Wind Direction Average
        Dx = Wind Direction Maximum
        Sn = Wind Speed Minimum
        Sm = Wind Speed Average
        Sx = Wind Speed Maximum
        Ta = Air Temperature
        Tp = Internal Temperature
        Ua = Relative Humidity
        Pa = Air Pressure
        Rc = Rain Accumulation
        Ri = Rain Intensity
        Hc = Hail Accumulation
        Hi = Hail Intensity
        Rp = Rain Peak Intensity
        Hp = Hail Peak Intensity
        Th = Heating Temperature
        Vh = Heating Voltage
        Vs = Supply Voltage
        */
    	
    	String inputLine = null;
    	try {
    		
    		/******** Get Input from Serial and "Tokenize" ************/
            inputLine = parentSensor.dataIn.readLine();
            StringTokenizer st = new StringTokenizer(inputLine, ",");
            /**********************************************************/
            
            
            /*********************************** Build and Publish dataBlock ****************************************/
            DataBlock dataBlock = weatherData.createDataBlock();
            cnt = 0;
            while(st.hasMoreTokens())
            {
            	// Get next token
            	token = st.nextToken();
            	
            	// Compare last two characters of first token to expected message type indicator
            	// In this case, the expected indicator is of type aR0
            	if (cnt == 0 && !token.substring(token.length()-2, token.length()).equals("R0"))
            	{
            		System.err.println("Message type = " + token + ", Looking for type aR0");
            		return;
            	}
                dataBlock.setDoubleValue(cnt, Double.parseDouble(token.replaceAll("[^0-9.]", "")));
                cnt ++;
            }
            
            // Replace First Entry (address) with Current Time
            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000);
            
            // Update Latest Record and Send Event
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, VaisalaWeatherOutput.this, dataBlock));
            /********************************************************************************************************/
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


    protected void start()
    {   
        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
                    getMeasurement();
                }
                
                reader = null;
            }
        });
        
        started = true;
        t.start();
    }


    protected void stop()
    {
    	started = false;
        
        if (reader != null)
        {
            try { reader.close(); }
            catch (IOException e) { }
        }
        
        if (parentSensor.commProvider != null)
        {
        	try {
				parentSensor.commProvider.stop();
			} catch (SensorHubException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	parentSensor.commProvider = null;
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
        return weatherData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return weatherEncoding;
    }
}
