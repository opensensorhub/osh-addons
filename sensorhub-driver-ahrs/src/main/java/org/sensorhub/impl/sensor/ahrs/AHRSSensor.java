
package org.sensorhub.impl.sensor.ahrs;

import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;
import org.sensorhub.impl.comm.RS232Config;

//import gnu.io.*;

import net.opengis.sensorml.v20.ClassifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.Term;

public class AHRSSensor extends AbstractSensorModule<AHRSConfig> 
{
    static final Logger log = LoggerFactory.getLogger(AHRSSensor.class);
    protected final static String CRS_ID = "SENSOR_FRAME";
        
    ICommProvider<? super CommConfig> commProvider;
    AHRSOutput dataInterface;

    public AHRSSensor()
    {   

    }

    // Mike mod - for debugging purposes - check on parent classes
//    public void superclassCheck()
//    {
//    	Class C = getClass();
//    	while (C != null) 
//    	{
//    		System.out.println(C.getName());
//    		C = C.getSuperclass();
//    	}    	
//    }
    
    @Override
    public void init(AHRSConfig config) throws SensorHubException
    {
    	super.init(config);

    	dataInterface = new AHRSOutput(this);
    	addOutput(dataInterface, false);
    	dataInterface.init();
    }

    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setId("AHRS");
            sensorDescription.setDescription("Microstrain Attitude & Heading Reference System - AHRS");
            
            ClassifierList classif = smlFac.newClassifierList();
            sensorDescription.getClassificationList().add(classif);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Microstrain");
            classif.addClassifier(term);
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
            term.setLabel("Model Number");
            term.setValue("3DM-GX2");
            classif.addClassifier(term);
            
            SpatialFrame localRefFrame = smlFac.newSpatialFrame();
            localRefFrame.setId(CRS_ID);
            localRefFrame.setOrigin("Position of Accelerometers (as marked on the plastic box of the device)");
            localRefFrame.addAxis("X", "The X axis is in the plane of the aluminum mounting plate, parallel to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Y", "The Y axis is in the plane of the aluminum mounting plate, orthogonal to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Z", "The Z axis is orthogonal to the aluminum mounting plate, so that the frame is direct (as marked on the plastic box of the device)");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
        }
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
    	if (commProvider == null)
    	{
          // we need to recreate comm provider here because it can be changed by UI
          // TODO do that in updateConfig
          try
          {
    		if (config.commSettings == null)
                  throw new SensorHubException("No communication settings specified");

              commProvider = config.commSettings.getProvider();
              commProvider.start();
              
              if (config.commSettings instanceof RS232Config)
                  dataInterface.decimFactor = 10;
          }
          catch (Exception e)
          {
              commProvider = null;
              throw e;
          }
    	}

    	dataInterface.start(commProvider);

    }

    @Override
    public void stop() throws SensorHubException
    {
    	if (dataInterface != null)
    	{
    		dataInterface.stop();
//    		System.out.println("Stopping dataInterface ...\n");
    	}
    	else
    	{
//    		System.out.println("dataInterface is null ...\n");
    	}
                  
    	if (commProvider != null)
    	{
    		commProvider.stop();
    		commProvider = null;
//    		System.out.println("Stopping commProvider ...\n");
    	}
    	else
    	{
//    		System.out.println("commProvider is null ...\n");
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
