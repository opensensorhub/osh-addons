package org.sensorhub.impl.sensor.uahweather;

import java.io.IOException;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.uahweather.UAHweatherConfig;
import org.sensorhub.impl.sensor.uahweather.UAHweatherOutput;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class UAHweatherSensor extends AbstractSensorModule<UAHweatherConfig>
{ 
    ICommProvider<?> commProvider;
    BufferedReader dataIn;
    UAHweatherOutput weatherOut;
    String[] msgToken = null;
    volatile boolean started;
    
    public UAHweatherSensor()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // init comm provider
        if (commProvider == null)
        {
            // we need to recreate comm provider here because it can be changed by UI
            try
            {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");
                
                commProvider = config.commSettings.getProvider();
                commProvider.start();
            }
            catch (Exception e)
            {
                commProvider = null;
                throw e;
            }
        }
        
        // connect to data stream
        try
        {
            dataIn = new BufferedReader(new InputStreamReader(commProvider.getInputStream(), StandardCharsets.US_ASCII));
            getLogger().info("Connected to weather station data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }
        
        
        // add unique ID based on serial number
        this.uniqueID = "urn:uahweather:" + config.modelNumber + ":" + config.serialNumber;
        this.xmlID = "UAHWEATHER_" + config.modelNumber + "_" + config.serialNumber.toUpperCase();
        
        // create data interfaces
        weatherOut = new UAHweatherOutput(this);
        addOutput(weatherOut, false);
        
        // execute initialization
        weatherOut.init();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
        	super.updateSensorDescription();
        	
        	// set identifiers in SensorML
            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setId("UAH_WEATHER");
            sensorDescription.setDescription("UAH Home Weather Station");
                
          
            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("UAH Atmospheric Science");
            identifierList.addIdentifier2(term);
            
            if (config.modelNumber != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
                term.setLabel("Model Number");
                term.setValue(config.modelNumber);
                identifierList.addIdentifier2(term);
            }
            
            if (config.serialNumber != null)
            {
                term = smlFac.newTerm();
                term.setDefinition(SWEHelper.getPropertyUri("SerialNumber"));
                term.setLabel("Serial Number");
                term.setValue(config.serialNumber);
                identifierList.addIdentifier2(term);
            }
            
            // Long Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("LongName"));
            term.setLabel("Long Name");
            term.setValue("Model " + config.modelNumber + " UAH Weather Station #" + config.serialNumber);
            identifierList.addIdentifier2(term);

            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("UAH Weather Station " + config.modelNumber);
            identifierList.addIdentifier2(term);
        }
    }


    private void getMeasurement()
    {	
    	String msg = null;
    	double BaroPres = -9999.99;
    	float Temp = -9999;
    	float Humid = -9999;
    	float windSpeed = -9999;
    	int windDirADC = -9999;
    	double windDirDeg = -9999.99;
    	byte rainCnt = -99;
    	long timeMillis = System.currentTimeMillis(); 
    	try
    	{
            msg = dataIn.readLine();
            System.out.println("Message: " + msg);
            msgToken = msg.split("\t");
            BaroPres = Double.parseDouble(msgToken[0]);
            Temp = Float.parseFloat(msgToken[1]);
            Humid = Float.parseFloat(msgToken[2]);
            rainCnt = Byte.parseByte(msgToken[3]);
            windSpeed = Float.parseFloat(msgToken[4]);
            windDirADC = Integer.parseInt(msgToken[5]);
            
            if (windDirADC != -9999)
            	windDirDeg = windDirADC*360/1024;
            
		}
    	catch (Exception e)
    	{
			e.printStackTrace();
		}
    	weatherOut.sendOutput(timeMillis, BaroPres, Temp, Humid, rainCnt, windSpeed, windDirDeg);
    }
    
    @Override
    public void start() throws SensorHubException
    {
    	if (started)
            return;
        
        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
                    getMeasurement();
                }
                
                dataIn = null;
            }
        });
        
        started = true;
        t.start();
    }
    

    @Override
    public void stop() throws SensorHubException
    {
    	started = false;
        
        if (dataIn != null)
        {
            try { dataIn.close(); }
            catch (IOException e) { }
        }
        
    	if (commProvider != null)
        {
        	try {
  				commProvider.stop();
  			} catch (SensorHubException e) {
  				// TODO Auto-generated catch block
  				e.printStackTrace();
  			}
        	commProvider = null;
        }
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
    	return (commProvider != null);
    }
    
}