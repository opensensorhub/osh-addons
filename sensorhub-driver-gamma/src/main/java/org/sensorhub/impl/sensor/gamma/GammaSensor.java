package org.sensorhub.impl.sensor.gamma;

import java.io.IOException;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class GammaSensor extends AbstractSensorModule<GammaConfig>
{ 
    ICommProvider<?> commProvider;
    BufferedReader dataIn;
    GammaOutput gammaOut;
    String[] msgToken = null;
    volatile boolean started;
    
    public GammaSensor()
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
            getLogger().info("Connected to Gamma Detector data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }
        
        
        // add unique ID based on serial number
        this.uniqueID = "urn:gamma:" + config.modelNumber + ":" + config.serialNumber;
        this.xmlID = "GAMMA_" + config.modelNumber + "_" + config.serialNumber.toUpperCase();
        
        // create data interfaces
        gammaOut = new GammaOutput(this);
        addOutput(gammaOut, false);
        
        // execute initialization
        gammaOut.init();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
        	super.updateSensorDescription();
        	SMLHelper helper = new SMLHelper(sensorDescription);
        	
        	// set identifiers in SensorML
            sensorDescription.setId("GAMMA_DETECTOR");
            sensorDescription.setDescription("Gamma Detector Module");
            helper.addIdentifier("Manufacturer Name", SWEHelper.getPropertyUri("Manufacturer"), "Health Physics Intruments");
            
            if (config.modelNumber != null)
            {
                helper.addIdentifier("Short Name", SWEHelper.getPropertyUri("ShortName"), "Gamma Detector " + config.modelNumber);
                helper.addIdentifier("Model Number", SWEHelper.getPropertyUri("ModelNumber"), config.modelNumber);
            }
            
            if (config.serialNumber != null)
                helper.addIdentifier("Serial Number", SWEHelper.getPropertyUri("SerialNumber"), config.serialNumber);
        }
    }


    private void getMeasurement()
    {	
    	String msg = null;
    	int DoseAvg = 0;
    	int DoseIns = 0;
    	long timeMillis = System.currentTimeMillis(); 
    	try
    	{
            msg = dataIn.readLine();
            System.out.println("Message: " + msg);
            msgToken = msg.split(" ");
            DoseAvg = Integer.parseInt(msgToken[0].replace(".", ""),16);
            DoseIns = Integer.parseInt(msgToken[1],16);
		}
    	catch (Exception e)
    	{
			e.printStackTrace();
		}
    	gammaOut.sendOutput(timeMillis, DoseAvg, DoseIns);
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
