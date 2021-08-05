package org.sensorhub.impl.sensor.isa;

import java.io.IOException;
import java.time.Instant;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.procedure.IProcedureGroupDriver;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.isa.ISASensor.StatusType;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEConstants;
import net.opengis.sensorml.v20.PhysicalSystem;
import java.io.BufferedReader;
import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;


/**
 * <p>
 * Driver for one or more sensors compatible with the ISA protocol and data model
 * </p>
 *
 * @author Alex Robin
 * @since May 19, 2021
 */
public class ISADriver extends AbstractSensorModule<ISAConfig> implements IProcedureGroupDriver<ISASensor>
{ 
    ICommProvider<?> commProvider;
    BufferedReader dataIn;
    String[] msgToken = null;
    Timer timer;
    volatile boolean started;
    
    Map<String, ISASensor> allSensors = new TreeMap<>();
    
    
    public ISADriver()
    {        
    }
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        /*// init comm provider
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
            getLogger().info("Connected to ISA data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }*/
        
        
        // add unique ID based on serial number
        this.uniqueID = "urn:osh:sensor:isa:" + config.networkID;
        this.xmlID = "ISA_" + config.networkID.toUpperCase();
        var timeStamp = Instant.ofEpochMilli(config.lastUpdated.getTime() / 1000);
        double[][] sensorLocations;
        
        // bio sensors
        sensorLocations = new double[][] {
            { 34.706226, -86.671218, 193 },
            { 34.700880, -86.671344, 193 },
            { 34.698622, -86.671385, 193 },
            { 34.694515, -86.671524, 193 },
            { 34.669423, -86.679080, 193 }            
        };
        
        for (int i = 0; i < 5; i++)
        {
            var loc = sensorLocations[i];
            registerSensor(new BiologicalSensor(this, String.format("BIO%03d", i+1))
                .setManufacturer("Bio Sensors, Inc.")
                .setModelNumber("BD456")
                .setSoftwareVersion("FW2021.32.156")
                .addStatusOutputs(StatusType.RADIO)
                .setFixedLocation(timeStamp, loc[0], loc[1], loc[2]));
        }
        
        // chem sensors
        sensorLocations = new double[][] {
            { 34.706226, -86.671218, 193 },
            { 34.700880, -86.671344, 193 },
            { 34.698622, -86.671385, 193 },
            { 34.694515, -86.671524, 193 },
            { 34.669423, -86.679080, 193 }            
        };
        
        for (int i = 0; i < 3; i++)
        {
            var loc = sensorLocations[i];
            registerSensor(new ChemicalSensor(this, String.format("CHEM%03d", i+1))
                .setManufacturer("Chem Sensors, Inc.")
                .setModelNumber("CD456")
                .setSoftwareVersion("FW2021.32.156")
                .addStatusOutputs(StatusType.RADIO)
                .setFixedLocation(timeStamp, loc[0], loc[1], loc[2]));
        }
        
        // weather sensors
        sensorLocations = new double[][] {
            { 34.677990, -86.682758, 193 },
            { 34.705657, -86.674131, 193 }            
        };        
        
        for (int i = 0; i < 2; i++)
        {
            var loc = sensorLocations[i];
            registerSensor(new MeteorologicalSensor(this, String.format("ATM%03d", i+1))
                .setManufacturer("Vaisala, Inc.")
                .setModelNumber("AWS310")
                .setSoftwareVersion("V51.458a")
                .addStatusOutputs(StatusType.ELEC_DC, StatusType.RADIO)
                .setFixedLocation(timeStamp, loc[0], loc[1], loc[2]));
        }
    }
    
    
    protected void registerSensor(ISASensor sensor)
    {
        allSensors.put(sensor.getUniqueIdentifier(), sensor);        
        //getParentHub().getProcedureRegistry().register(sensor);
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
        	super.updateSensorDescription();
        	
        	// add description to SensorML
        	new SMLHelper().edit((PhysicalSystem)sensorDescription)
        	    .name("ISA Sensor Network")
        	    .description("Network of sensors connected via the Integrated Sensor Architecture protocols")
        	    .definition(SWEConstants.DEF_SENSOR_NETWORK)
        	    .build();
        }
    }
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
    	if (started)
            return;
        
        // start simulated measurement timer
    	timer = new Timer("ISA Driver");
        timer.schedule(new TimerTask() {
                        
            @Override
            public void run()
            {
                for (var sensor: allSensors.values())
                {
                    for (var output: sensor.getOutputs().values())
                    {
                        if (output instanceof ISAOutput)
                            ((ISAOutput)output).sendRandomMeasurement();
                    }
                }
            }
            
        }, 500L, 1000L);
    }
    

    @Override
    protected void doStop() throws SensorHubException
    {
    	started = false;
        
    	if (timer != null)
    	{
    	    timer.cancel();
    	    timer = null;
    	}
    	
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


    @Override
    public Map<String, ? extends ISASensor> getMembers()
    {
        return Collections.unmodifiableMap(allSensors);
    }
    
}
