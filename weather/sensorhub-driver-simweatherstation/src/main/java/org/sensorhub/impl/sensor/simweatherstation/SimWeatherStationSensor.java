package org.sensorhub.impl.sensor.simweatherstation;

import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.simweatherstation.SimWeatherStationConfig;
import org.sensorhub.impl.sensor.simweatherstation.SimWeatherStationOutput;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;

public class SimWeatherStationSensor extends AbstractSensorModule<SimWeatherStationConfig>
{ 
    SimWeatherStationOutput weatherOut;
    
    public SimWeatherStationSensor()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();

        // add unique ID based on serial number
        this.uniqueID = "urn:simweatherstation:" + config.modelNumber + ":" + config.serialNumber;
        this.xmlID = "SIMWEATHERSTATION_" + config.modelNumber + "_" + config.serialNumber.toUpperCase();
        
        // create data interfaces
        weatherOut = new SimWeatherStationOutput(this);
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
            sensorDescription.setId("SIM_WEATHER_STATION");
            sensorDescription.setDescription("Simulated Weather Station");
          
            IdentifierList identifierList = smlFac.newIdentifierList();
            sensorDescription.addIdentification(identifierList);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Botts Innovative Research, Inc.");
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
            term.setValue("Model " + config.modelNumber + " Simulated Weather Station #" + config.serialNumber);
            identifierList.addIdentifier2(term);

            // Short Name
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
            term.setLabel("Short Name");
            term.setValue("Simulated Weather Station " + config.modelNumber);
            identifierList.addIdentifier2(term);
        }
    }
    
    @Override
    public void start() throws SensorHubException
    {
        weatherOut.start();
    }
    

    @Override
    public void stop() throws SensorHubException
    {
    	weatherOut.stop();
    }
    
    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
}