package org.sensorhub.impl.sensor.zwavedom;

import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.LightInfoArray;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.TempInfoArray;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.UtilityInfoArray;
import org.sensorhub.impl.sensor.zwavedom.ZWaveDomHandler.WeatherInfoArray;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;

public class ZWaveDomDriver extends AbstractSensorModule<ZWaveDomConfig>
{
    RobustConnection connection;
	ZWaveDomHandler LightData, WeatherData, TempData, UtilityData;
	ZWaveDomLightOutput lightOut;
	ZWaveDomWeatherOutput weatherOut;
	ZWaveDomTempOutput tempOut;
	ZWaveDomUtilityOutput utilityOut;
	ZWaveDomLightControl lightControl;
	String hostURL;
	LightInfoArray lightDevices;
	WeatherInfoArray weatherDevices;
	TempInfoArray tempDevices;
	UtilityInfoArray utilityDevices;
	
    public ZWaveDomDriver()
    {
    }

    @Override
    public void setConfiguration(final ZWaveDomConfig config)
    {
        super.setConfiguration(config);
        
        // compute full host URL
        hostURL = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/json.htm?";
    };
    
  @Override
  public void init() throws SensorHubException
  {
	  super.init();

	  System.out.println("Configuring Z-Wave Network");
	  
	  lightOut = new ZWaveDomLightOutput(this);
	  lightOut.init();
	  addOutput(lightOut,false);
	  
	  weatherOut = new ZWaveDomWeatherOutput(this);
	  weatherOut.init();
	  addOutput(weatherOut,false);
	  
	  tempOut = new ZWaveDomTempOutput(this);
	  tempOut.init();
	  addOutput(tempOut,false);
	  
	  utilityOut = new ZWaveDomUtilityOutput(this);
	  utilityOut.init();
	  addOutput(utilityOut,false);
	  
	  lightControl = new ZWaveDomLightControl(this);
	  addControlInput(lightControl);
	  lightControl.init();
	  
      // add unique ID based on serial number
      this.uniqueID = "urn:zwavedom:" + config.modelNumber + ":" + config.serialNumber;
      this.xmlID = "ZWAVEDOM_" + config.modelNumber + "_" + config.serialNumber.toUpperCase();
  }
  
  @Override
  protected void updateSensorDescription()
  {
      synchronized (sensorDescLock)
      {
      	super.updateSensorDescription();
      	
      	// set identifiers in SensorML
          SMLFactory smlFac = new SMLFactory();
          sensorDescription.setId("Z-WaveNetwork");
          sensorDescription.setDescription("Z-Wave Home Sensor Network");
              
        
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
          term.setValue(config.modelNumber + " Z-Wave Network #" + config.serialNumber);
          identifierList.addIdentifier2(term);

          // Short Name
          term = smlFac.newTerm();
          term.setDefinition(SWEHelper.getPropertyUri("ShortName"));
          term.setLabel("Short Name");
          term.setValue("Z-Wave Network " + config.modelNumber);
          identifierList.addIdentifier2(term);
      }
  }

	@Override
	public boolean isConnected()
	{
		return connection.isConnected();
	}

	@Override
	public void start() throws SensorHubException
	{
		if (lightOut != null)
        	lightOut.start();
		
		if (weatherOut != null)
			weatherOut.start();
		
		if (tempOut != null)
			tempOut.start();
		
		if (utilityOut != null)
			utilityOut.start();
	}
	
	@Override
	public void stop() throws SensorHubException
	{
		if (lightOut != null)
        	lightOut.stop();
		
		if (weatherOut != null)
			weatherOut.stop();
		
		if (tempOut != null)
			tempOut.stop();
		
		if (utilityOut != null)
			utilityOut.stop();
	}
	
	@Override
	public void cleanup() throws SensorHubException
	{
	}


    protected String getHostURL()
    {
        return hostURL;
    }
}
