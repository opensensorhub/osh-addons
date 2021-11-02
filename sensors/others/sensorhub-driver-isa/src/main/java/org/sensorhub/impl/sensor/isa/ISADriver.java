package org.sensorhub.impl.sensor.isa;

import java.io.IOException;
import java.time.Instant;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.system.ISystemGroupDriver;
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
        
        simulation = new ISASimulation(this);
        
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
        
        // radio sensors
        sensorLocations = new double[][] {
            { 34.706226, -86.671218, 193 },
            { 34.698848, -86.671382, 193 },
            { 34.694702, -86.671473, 193 },
            { 34.686162, -86.671778, 193 }
        };
        
        for (int i = 0; i < sensorLocations.length; i++)
        {
            var loc = sensorLocations[i];
            registerSensor(new RadiologicalSensor(this, String.format("RADIO%03d", i+1))
                .addTriggerSource("urn:osh:process:vmti", "targetLocation", RadioReadingOutput.RADIO_MATERIAL_CATEGORY_CODE[1], 1200.0f)
                .setManufacturer("Radiological Sensors, Inc.")
                .setModelNumber("RD123")
                .setSoftwareVersion("FW21.23.89")
                .addStatusOutputs(StatusType.RADIO)
                .setFixedLocationWithRadius(timeStamp, loc[0], loc[1], loc[2], 100));
        }
        
        // bio sensors
        sensorLocations = new double[][] {
            { 34.708926, -86.679157, 193 },
            { 34.698967, -86.675548, 193 },
            { 34.699533, -86.664192, 193 },
            { 34.688416, -86.677495, 193 }
        };
        
        for (int i = 0; i < sensorLocations.length; i++)
        {
            var loc = sensorLocations[i];
            var sensor = new BiologicalSensor(this, String.format("BIO%03d", i+1))
                .setManufacturer("Bio Sensors, Inc.")
                .setModelNumber("BD456")
                .setSoftwareVersion("FW2021.32.156")
                .addStatusOutputs(StatusType.RADIO)
                .setFixedLocation(timeStamp, loc[0], loc[1], loc[2]);
            
            registerSensor(sensor);
        }
        
        // chem sensors
        sensorLocations = new double[][] {
            { 34.695782, -86.675593, 193 },
            { 34.691229, -86.666021, 193 }
        };
        
        for (int i = 0; i < sensorLocations.length; i++)
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
