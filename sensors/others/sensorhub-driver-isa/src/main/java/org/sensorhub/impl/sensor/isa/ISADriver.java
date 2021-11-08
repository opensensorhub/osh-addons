package org.sensorhub.impl.sensor.isa;

import java.io.IOException;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.system.ISystemGroupDriver;
import org.sensorhub.impl.sensor.AbstractSensorModule;
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
public class ISADriver extends AbstractSensorModule<ISAConfig> implements ISystemGroupDriver<ISASensor>
{ 
    ICommProvider<?> commProvider;
    BufferedReader dataIn;
    String[] msgToken = null;
    Timer timer;
    volatile boolean started;
    
    Map<String, ISASensor> allSensors = new TreeMap<>();
    ISASimulation simulation;
    
    
    public ISADriver()
    {
    }
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        // add unique ID based on serial number
        this.uniqueID = "urn:osh:sensor:isa:" + config.networkID;
        this.xmlID = "ISA_" + config.networkID.toUpperCase();
        
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
        
        // init simulation
        simulation = new ISASimulation(this);
        simulation.init();
    }
    
    
    protected void registerSensor(ISASensor sensor)
    {
        allSensors.put(sensor.getUniqueIdentifier(), sensor);
        //getParentHub().getSystemDriverRegistry().register(sensor);
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
                .description("Network of sensors connected via the Integrated Sensor Architecture (ISA) protocol")
                .definition(SWEConstants.DEF_SENSOR_NETWORK)
                .build();
        }
    }
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
        if (started)
            return;
        
        // start simulation
        simulation.start();
        
        // start simulated measurement timer
        timer = new Timer("ISA Driver");
        timer.schedule(new TimerTask() {
        
            @Override
            public void run()
            {
                for (var sensor: allSensors.values())
                {
                    if (sensor.getCurrentTime() > 0)
                    {
                        // send sensor pos if it has changed
                        sensor.sendLocation(sensor.getCurrentTime());
                        
                        // send data for all other outputs
                        for (var output: sensor.getOutputs().values())
                        {
                            if (output instanceof ISAOutput)
                                ((ISAOutput)output).sendSimulatedMeasurement();
                        }
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
        
        if (simulation != null)
        {
            simulation.stop();
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
                getLogger().error("Error closing comm provider", e);
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
