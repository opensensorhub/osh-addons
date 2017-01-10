package org.sensorhub.impl.sensor.domoticz;

import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.domoticz.DomoticzConfig.DeviceTypeEnum;
import org.sensorhub.impl.sensor.domoticz.DomoticzHandler.DomoticzResponse;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;

public class DomoticzDriver extends AbstractSensorModule<DomoticzConfig>
{
    RobustConnection connection;
    
	DomoticzTempOutput domTempOut;
	DomoticzTempHumOutput domTempHumOut;
	DomoticzSwitchOutput domSwitchOut;
	DomoticzSelectorOutput domSelectorOut;
	DomoticzUVOutput domUVOut;
	DomoticzLumOutput domLumOut;
	DomoticzStatusOutput domStatusOut;
	DomoticzEnviroOutput domEnviroOut;
	DomoticzMotionOutput domMotionOut;
	DomoticzAlertOutput domAlertOut;
	
	DomoticzSwitchControl switchControl;
	DomoticzSelectorControl selectorControl;
	
	DomoticzHandler domHandler = new DomoticzHandler();;

	String hostURL;
	Timer timer;
	
	ArrayList<ValidDevice> validDevices = new ArrayList<ValidDevice>();
	ArrayList<ValidDevice> validDeviceInit = new ArrayList<ValidDevice>();
	List<DeviceTypeEnum> valTypes = new ArrayList<DeviceTypeEnum>();
	List<Boolean> valAlert = new ArrayList<Boolean>();
	List<String> valPtlMotionIdx = new ArrayList<String>(); // Potential motion idx
	ArrayList<ValidDevice> validDeviceStart = new ArrayList<ValidDevice>();
	ArrayList<ValidDevice> validDeviceTotal = new ArrayList<ValidDevice>();
	ArrayList<DomoticzDevice> uniqueDevice = new ArrayList<DomoticzDevice>();
	List<Integer> dupRows = new ArrayList<Integer>();
	
	public void setValidDevices() throws SensorHubException
	{
//		System.out.println("Setting Valid Devices");
		validDevices = getValidDevicesTotal();
	}
	
	public ArrayList<ValidDevice> getValidDevices() throws SensorHubException
	{
//		System.out.println("Returning Valid Devices");
		return validDevices;
	}
	
    public DomoticzDriver()
    {
    }

    @Override
    public void setConfiguration(final DomoticzConfig config)
    {
        super.setConfiguration(config);
        
        // compute full host URL
        hostURL = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/json.htm?";
    };
    
  @Override
  public void init() throws SensorHubException
  {
	  super.init();
	  
	  selectorControl = null;
	  
	  setValidDevices();
	  validDeviceInit = getValidDevices();

	  // Print User Input
//	  System.out.println("############################################################");
//	  System.out.println("#                     User Input                           #");
//	  System.out.println("############################################################");
//	  System.out.println("idx : type : latLonLat : locDesc : Alert");
//	  System.out.println();
//	  for (int i = 0; i < config.domoticzDevice.size(); i++)
//	  {
//		  System.out.println(
//				  config.domoticzDevice.get(i).getDeviceIdx() + ":" + 
//				  config.domoticzDevice.get(i).getDeviceType() + ":[" + 
//				  config.domoticzDevice.get(i).getLocationLLA().getLat() + "," +
//				  config.domoticzDevice.get(i).getLocationLLA().getLon() + "," + 
//				  config.domoticzDevice.get(i).getLocationLLA().getAlt() + "]:" + 
//				  config.domoticzDevice.get(i).getLocDesc() + ":" + 
//				  config.domoticzDevice.get(i).getProvideAlert());
//	  }
//	  System.out.println("############################################################");
//	  System.out.println();

	  // Print Added Devices
//	  System.out.println("############################################################");
//	  System.out.println("#                   Valid Devices                          #");
//	  System.out.println("############################################################");
//	  System.out.println("idx : type : latLonLat : locDesc : Alert");
//	  System.out.println();
	  for (int i = 0; i < validDeviceInit.size(); i++)
	  {
//		  System.out.println(
//				  validDeviceInit.get(i).getValidIdx() + ":" + 
//				  validDeviceInit.get(i).getValidType() + ":[" + 
//				  validDeviceInit.get(i).getValidLocationLLA().getLat() + "," +
//				  validDeviceInit.get(i).getValidLocationLLA().getLon() + "," + 
//				  validDeviceInit.get(i).getValidLocationLLA().getAlt() + "]:" + 
//				  validDeviceInit.get(i).getValidLocDesc() + ":" + 
//				  validDeviceInit.get(i).getValidProvideAlert());
		  
		  valTypes.add(validDeviceInit.get(i).getValidType());
		  valAlert.add(validDeviceInit.get(i).getValidProvideAlert());
		  if (validDeviceInit.get(i).getValidType() == DeviceTypeEnum.Switch)
			  valPtlMotionIdx.add(validDeviceInit.get(i).getValidIdx());
	  }
//	  System.out.println("############################################################");
//	  System.out.println();
	  
	  // Construct SWE outputs
	  if (valTypes.size() > 0)
	  {
		  domStatusOut = new DomoticzStatusOutput(this);
		  try { domStatusOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domStatusOut, false);
	  }
	  
	  if (!Collections.disjoint(valTypes, config.getEnviroTypes()))
	  {
		  domEnviroOut = new DomoticzEnviroOutput(this);
		  try { domEnviroOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domEnviroOut, false);
	  }
	  
	  if (!Collections.disjoint(valPtlMotionIdx, config.getMotionIdx()))
	  {
		  domMotionOut = new DomoticzMotionOutput(this);
		  try { domMotionOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domMotionOut, false);
	  }
	  
	  if (valAlert.contains(true))
	  {
		  domAlertOut = new DomoticzAlertOutput(this);
		  try { domAlertOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domAlertOut, false);
	  }
	  
	  if (valTypes.contains(DeviceTypeEnum.Temperature))
	  {
		  domTempOut = new DomoticzTempOutput(this);
		  try { domTempOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domTempOut, false);
	  }
	  
	  if (valTypes.contains(DeviceTypeEnum.Temperature_Humidity))
	  {
		  domTempHumOut = new DomoticzTempHumOutput(this);
		  try { domTempHumOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domTempHumOut, false);
	  }
	  
	  if (valTypes.contains(DeviceTypeEnum.UV))
	  {
		  domUVOut = new DomoticzUVOutput(this);
		  try { domUVOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domUVOut, false);
	  }
	  
	  if (valTypes.contains(DeviceTypeEnum.Lux))
	  {
		  domLumOut = new DomoticzLumOutput(this);
		  try { domLumOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domLumOut, false);
	  }
	  
	  
	  if (valTypes.contains(DeviceTypeEnum.Selector_Switch))
	  {
		  domSelectorOut = new DomoticzSelectorOutput(this);
		  try { domSelectorOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domSelectorOut, false);

		  // add selector controller
          this.selectorControl = new DomoticzSelectorControl(this);
          addControlInput(selectorControl);
          selectorControl.init();
	  }
	  
	  
	  if (valTypes.contains(DeviceTypeEnum.Switch))
	  {
		  domSwitchOut = new DomoticzSwitchOutput(this);
		  try { domSwitchOut.init(); }
		  catch (IOException e) { e.printStackTrace(); }
		  addOutput(domSwitchOut, false);
	  }
	  
	  if (valTypes.contains(DeviceTypeEnum.Selector_Switch) || valTypes.contains(DeviceTypeEnum.Switch))
	  {
		  // add switch controller
          this.switchControl = new DomoticzSwitchControl(this);
          addControlInput(switchControl);
          switchControl.init();
	  }

      // add unique ID based on serial number
      this.uniqueID = "urn:domoticz:" + config.modelNumber + ":" + config.serialNumber;
      this.xmlID = "DOMOTICZ_" + config.modelNumber + "_" + config.serialNumber.toUpperCase();
  }
  
  public ArrayList<ValidDevice> getValidDevicesTotal() throws SensorHubException
  {
//	  System.out.println("Checking Network Devices...");
//	  System.out.println();
	  
	  // Check for duplicate entries and do not include them
	  for (int i = 0; i < config.domoticzDevice.size(); i++)
	  {
		  if (dupRows.contains(i))
			  continue;
		  else
			  uniqueDevice.add(config.domoticzDevice.get(i));
		  
		  for (int j = 0; j < config.domoticzDevice.size(); j++)
		  {
			  if (config.domoticzDevice.get(i).getDeviceIdx().equals(config.domoticzDevice.get(j).getDeviceIdx()) && j > i)
			  {
				  dupRows.add(j);
				  System.out.println("Device idx " + config.domoticzDevice.get(j).getDeviceIdx() + " already exists. Can only have one assignment per idx ");
			  }
		  }
	  }
	  System.out.println();
	  for (int i = 0; i < uniqueDevice.size(); i++)
	  {
//		  System.out.println("uniqueDevice idx = " + uniqueDevice.get(i).getDeviceIdx());
		  DomoticzResponse domData = domHandler.getFromJSON(getHostURL() + 
				  "type=devices&rid=" + uniqueDevice.get(i).getDeviceIdx());
		  
		  if (domData.getResult() != null)
		  {
			  // Check Device Type from User Input and compare to output
			  // If there is a mismatch, device will be deemed "invalid" and not included
			  switch (uniqueDevice.get(i).getDeviceType())
			  {
			  	case Temperature:
			  		if (!Double.isNaN(domData.getResult()[0].getTemp()))
			  		{
//			  			System.out.println("Temp device idx " + uniqueDevice.get(i).getDeviceIdx() + " is OK...adding");
			  			validDeviceTotal.add(new ValidDevice (uniqueDevice.get(i).getDeviceIdx(), uniqueDevice.get(i).getDeviceType(), uniqueDevice.get(i).getLocationLLA(), uniqueDevice.get(i).getLocDesc(), uniqueDevice.get(i).getProvideAlert(), "Temp is out of bounds!"));
			  		}
					else
					{
						System.out.println("Device Type Mismatch - Check Output Type for idx" + uniqueDevice.get(i).getDeviceIdx());
					}
			  		break;
			  		
			  	case Temperature_Humidity:
			  		if (!Double.isNaN(domData.getResult()[0].getTemp()) && domData.getResult()[0].getHumidity() != Integer.MIN_VALUE)
			  		{
//			  			System.out.println("Temp/Hum device idx " + uniqueDevice.get(i).getDeviceIdx() + " is OK...adding");
			  			validDeviceTotal.add(new ValidDevice (uniqueDevice.get(i).getDeviceIdx(), uniqueDevice.get(i).getDeviceType(), uniqueDevice.get(i).getLocationLLA(), uniqueDevice.get(i).getLocDesc(), uniqueDevice.get(i).getProvideAlert(), "Temp and/or RelHum are/is out of bounds!"));
			  		}
					else
					{
						System.out.println("Device Type Mismatch - Check Output Type for idx" + uniqueDevice.get(i).getDeviceIdx());
					}
			  		break;
			  		
			  	case UV:
			  		if (domData.getResult()[0].getUVI() != null)
			  		{
//			  			System.out.println("UV device idx " + uniqueDevice.get(i).getDeviceIdx() + " is OK...adding");
			  			validDeviceTotal.add(new ValidDevice (uniqueDevice.get(i).getDeviceIdx(), uniqueDevice.get(i).getDeviceType(), uniqueDevice.get(i).getLocationLLA(), uniqueDevice.get(i).getLocDesc(), uniqueDevice.get(i).getProvideAlert(), "UVI is out of bounds!"));
			  		}
					else
					{
						System.out.println("Device Type Mismatch - Check Output Type for idx" + uniqueDevice.get(i).getDeviceIdx());
					}
			  		break;
			  		
			  	case Lux:
			  		if (domData.getResult()[0].getData().contains("Lux"))
			  		{
//			  			System.out.println("Luminance device idx " + uniqueDevice.get(i).getDeviceIdx() + " is OK...adding");
			  			validDeviceTotal.add(new ValidDevice (uniqueDevice.get(i).getDeviceIdx(), uniqueDevice.get(i).getDeviceType(), uniqueDevice.get(i).getLocationLLA(), uniqueDevice.get(i).getLocDesc(), uniqueDevice.get(i).getProvideAlert(), "Illuminance is out of bounds!"));
			  		}
					else
					{
						System.out.println("Device Type Mismatch - Check Output Type for idx" + uniqueDevice.get(i).getDeviceIdx());
					}
			  		break;
			  		
			  	case Selector_Switch:
			  		if (domData.getResult()[0].getLevel() != Integer.MIN_VALUE)
			  		{
//			  			System.out.println("Selector device idx " + uniqueDevice.get(i).getDeviceIdx() + " is OK...adding");
			  			validDeviceTotal.add(new ValidDevice (uniqueDevice.get(i).getDeviceIdx(), uniqueDevice.get(i).getDeviceType(), uniqueDevice.get(i).getLocationLLA(), uniqueDevice.get(i).getLocDesc(), uniqueDevice.get(i).getProvideAlert(), "Selector Level is out of bounds!"));
			  		}
					else
					{
						System.out.println("Device Type Mismatch - Check Output Type for idx" + uniqueDevice.get(i).getDeviceIdx());
					}
			  		break;
			  		
			  	case Switch:
			  		if (domData.getResult()[0].getStatus() != null)
			  		{
//			  			System.out.println("Switch device idx " + uniqueDevice.get(i).getDeviceIdx() + " is OK...adding");
			  			validDeviceTotal.add(new ValidDevice (uniqueDevice.get(i).getDeviceIdx(), uniqueDevice.get(i).getDeviceType(), uniqueDevice.get(i).getLocationLLA(), uniqueDevice.get(i).getLocDesc(), uniqueDevice.get(i).getProvideAlert(), "Switch/Detector was triggered!"));
			  		}
					else
					{
						System.out.println("Device Type Mismatch - Check Output Type for idx" + uniqueDevice.get(i).getDeviceIdx());
					}
			  		break;
			  }
		  }
		  else
			  System.out.println("device Idx " + uniqueDevice.get(i).getDeviceIdx() + " has no data...");
		  
//		  System.out.println();
	  }
	return validDeviceTotal;
  }
  
  public void ParseAndSendData() throws SensorHubException
  {
	  validDeviceStart = getValidDevices();
	  for (int k = 0; k < validDeviceStart.size(); k++)
	  {
		  DomoticzResponse domDataStart = domHandler.getFromJSON(getHostURL() + 
				  "type=devices&rid=" + validDeviceStart.get(k).getValidIdx());
          			
		  if (domDataStart.getResult() != null)
		  {
			  switch (validDeviceStart.get(k).getValidType())
			  {
			  case Temperature:
				  domTempOut.postTempData(domDataStart, validDeviceStart.get(k));
				  domStatusOut.postStatusData(domDataStart, validDeviceStart.get(k));
				  domEnviroOut.postEnviroData(domDataStart, validDeviceStart.get(k));
				  if (validDeviceStart.get(k).getValidProvideAlert() && (domDataStart.getResult()[0].getTemp() > 27.0 || domDataStart.getResult()[0].getTemp() < 16.0))
				  {
					  validDeviceStart.get(k).setValidAlertMsg("Temp is " + domDataStart.getResult()[0].getTemp() + "\u00b0" + "C!");
					  domAlertOut.postAlertData(domDataStart, validDeviceStart.get(k));
				  }
				  break;
			  
			  case Temperature_Humidity:
				  domTempHumOut.postTempHumData(domDataStart, validDeviceStart.get(k));
				  domStatusOut.postStatusData(domDataStart, validDeviceStart.get(k));
				  domEnviroOut.postEnviroData(domDataStart, validDeviceStart.get(k));
				  if (validDeviceStart.get(k).getValidProvideAlert() && (domDataStart.getResult()[0].getTemp() > 27.0 || domDataStart.getResult()[0].getTemp() < 16.0 || domDataStart.getResult()[0].getHumidity() > 60))
				  {
					  validDeviceStart.get(k).setValidAlertMsg("Temp/Hum is " + domDataStart.getResult()[0].getTemp() + "\u00b0" + "C/ " + domDataStart.getResult()[0].getHumidity() + "%!");
					  domAlertOut.postAlertData(domDataStart, validDeviceStart.get(k));
				  }
				  break;
				  
			  case UV:
				  domUVOut.postUVData(domDataStart, validDeviceStart.get(k));
				  domStatusOut.postStatusData(domDataStart, validDeviceStart.get(k));
				  domEnviroOut.postEnviroData(domDataStart, validDeviceStart.get(k));
				  if (validDeviceStart.get(k).getValidProvideAlert() && Double.parseDouble(domDataStart.getResult()[0].getUVI()) > 2.0)
				  {
					  validDeviceStart.get(k).setValidAlertMsg("UVI is " + domDataStart.getResult()[0].getUVI() + "!");
					  domAlertOut.postAlertData(domDataStart, validDeviceStart.get(k));
				  }
				  break;
				  
			  case Lux:
				  domLumOut.postLumData(domDataStart, validDeviceStart.get(k));
				  domStatusOut.postStatusData(domDataStart, validDeviceStart.get(k));
				  domEnviroOut.postEnviroData(domDataStart, validDeviceStart.get(k));
				  if (validDeviceStart.get(k).getValidProvideAlert() && (Integer.parseInt(domDataStart.getResult()[0].getData().replaceAll("\\s", "").replaceAll("Lux", "")) > 100 || Integer.parseInt(domDataStart.getResult()[0].getData().replaceAll("\\s", "").replaceAll("Lux", "")) < 10))
				  {
					  validDeviceStart.get(k).setValidAlertMsg("Illuminance is " + domDataStart.getResult()[0].getData() + "!");
					  domAlertOut.postAlertData(domDataStart, validDeviceStart.get(k));
				  }
				  break;
				  
			  case Selector_Switch:
				  domSelectorOut.postSelectorData(domDataStart, validDeviceStart.get(k));
				  domStatusOut.postStatusData(domDataStart, validDeviceStart.get(k));
				  if (validDeviceStart.get(k).getValidProvideAlert() && (domDataStart.getResult()[0].getLevel() < 10 || domDataStart.getResult()[0].getLevel() > 90))
				  {
					  validDeviceStart.get(k).setValidAlertMsg("Selector level is " + domDataStart.getResult()[0].getLevel() + "!");
					  domAlertOut.postAlertData(domDataStart, validDeviceStart.get(k));
				  }
				  break;
				  
			  case Switch:
				  domSwitchOut.postSwitchData(domDataStart, validDeviceStart.get(k));
				  domStatusOut.postStatusData(domDataStart, validDeviceStart.get(k));
				  if (config.getMotionIdx().contains(validDeviceStart.get(k).getValidIdx()))
					  domMotionOut.postMotionData(domDataStart, validDeviceStart.get(k));
				  if (validDeviceStart.get(k).getValidProvideAlert() && domDataStart.getResult()[0].getStatus().equalsIgnoreCase("On"))
					  domAlertOut.postAlertData(domDataStart, validDeviceStart.get(k));
				  break;
			  }
		  }
		  else
			  System.out.println("device Idx " + validDeviceStart.get(k).getValidIdx() + " has no data");
	  }
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
  
  class ValidDevice
  {
  	  private String validIdx;
  	  private DeviceTypeEnum validType;
  	  private LocationLLA validLocationLLA;
  	  private String validLocDesc;
  	  private boolean validProvideAlert;
  	  private String validAlertMsg;

  	  public ValidDevice(String validIdx, DeviceTypeEnum validType, LocationLLA validLocationLLA, String validLocDesc, boolean validProvideAlert, String validAlertMsg)
  	  {
  	    this.validIdx = validIdx;
  	    this.validType = validType;
  	    this.validLocationLLA = validLocationLLA;
  	    this.validLocDesc = validLocDesc;
  	    this.validProvideAlert = validProvideAlert;
  	    this.validAlertMsg = validAlertMsg;
  	  }
  
  	  public String getValidIdx()
  	  {
  		  return this.validIdx;
  	  }

  	  public void setValidIdx(String validIdx)
  	  {
  		  this.validIdx = validIdx;
  	  }
  	  
  	  public DeviceTypeEnum getValidType()
  	  {
  		  return this.validType;
  	  }

  	  public void setValidType(DeviceTypeEnum validType)
  	  {
  		  this.validType = validType;
  	  }
  	  
  	  public LocationLLA getValidLocationLLA()
  	  {
  		  return this.validLocationLLA;
  	  }

  	  public void setValidLocationLLA(LocationLLA validLocationLLA)
  	  {
  		  this.validLocationLLA = validLocationLLA;
  	  }
  	  
  	  public String getValidLocDesc()
  	  {
  		  return this.validLocDesc;
  	  }

  	  public void setValidLocDesc(String validLocDesc)
  	  {
  		  this.validLocDesc = validLocDesc;
  	  }
  	  
  	  public boolean getValidProvideAlert()
  	  {
  		  return this.validProvideAlert;
  	  }

  	  public void setValidProvideAlert(boolean validProvideAlert)
  	  {
  		  this.validProvideAlert = validProvideAlert;
  	  }
  	  
  	  public String getValidAlertMsg()
  	  {
  		  return this.validAlertMsg;
  	  }

  	  public void setValidAlertMsg(String validAlertMsg)
  	  {
  		  this.validAlertMsg = validAlertMsg;
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
		if (timer != null)
			return;
		
		try
		{
			TimerTask timerTask = new TimerTask()
			{
				@Override
				public void run()
				{
	          	try
	          	{
	          		ParseAndSendData();
	          	}
	          	
	          	catch (Exception e)
	          	{
	          		getLogger().error("Cannot get Domoticz data", e);
	          	}
	          }
	      };
	      timer = new Timer();
	      timer.scheduleAtFixedRate(timerTask, 0, (long)(getAverageSamplingPeriod()*1000));
	    }
		catch (Exception e)
		{
			e.printStackTrace();
		}  
	}
	
	@Override
	public void stop() throws SensorHubException
	{
	}
	
	@Override
	public void cleanup() throws SensorHubException
	{
	}


    protected String getHostURL()
    {
        return hostURL;
    }
    
    
	public double getAverageSamplingPeriod() {
		return 5.0;
	}
}
