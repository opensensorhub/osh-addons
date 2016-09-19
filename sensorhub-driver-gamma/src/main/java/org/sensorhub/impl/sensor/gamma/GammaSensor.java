package org.sensorhub.impl.sensor.gamma;

import java.io.IOException;
import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.gamma.GammaOutput;
import org.sensorhub.impl.sensor.gamma.GammaConfig;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GammaSensor extends AbstractSensorModule<GammaConfig>
{ 
    ICommProvider<?> commProvider;
    BufferedReader dataIn;
    GammaOutput gammaOut;
    String inputLine = null;
    String deviceAddress = null;
    String[] msgToken = null;
    volatile boolean started;
    int cnt;
    public final static char CR = (char) 0x0D;
    public final static char LF = (char) 0x0A;
    public final static String CRLF = "" + CR + LF;
    
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
        	
        	// set identifiers in SensorML
            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setId("GAMMA_DETECTOR");
            sensorDescription.setDescription("Gamma Detector Module");
                
          
            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Health Physics Intruments");
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
            term.setValue("Model " + config.modelNumber + " Gamma Detector #" + config.serialNumber);
            identifierList.addIdentifier2(term);

            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Gamma Detector Model " + config.modelNumber);
            identifierList.addIdentifier2(term);
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
