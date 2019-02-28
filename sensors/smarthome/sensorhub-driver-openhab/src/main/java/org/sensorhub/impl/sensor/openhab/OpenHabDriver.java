package org.sensorhub.impl.sensor.openhab;

import net.opengis.sensorml.v20.IdentifierList;
import net.opengis.sensorml.v20.Term;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.RobustConnection;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.openhab.OpenHabHandler.OpenHabItems;
import org.sensorhub.impl.sensor.openhab.OpenHabHandler.OpenHabThings;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;

public class OpenHabDriver extends AbstractSensorModule<OpenHabConfig>
{
    RobustConnection connection;
    
	OpenHabTempOutput habTempOut;
	OpenHabHumOutput habHumOut;
	OpenHabSensorBinaryOutput habBinOut;
	OpenHabAlarmEntryOutput habEntryOut;
	OpenHabAlarmBurglarOutput habBurglarOut;
	OpenHabAlarmGenOutput habGenOut;
	OpenHabSwitchOutput habSwitchOut;
	OpenHabDimmerOutput habDimmerOut;
	OpenHabUVOutput habUVOut;
	OpenHabLumOutput habLumOut;
	OpenHabBatteryOutput habBattOut;
	OpenHabStatusOutput habStatusOut;
	OpenHabEnviroOutput habEnviroOut;
	OpenHabAlarmOutput habAlarmOut;
//	OpenHabMotionOutput habMotionOut;
	OpenHabAlertOutput habAlertOut;

	OpenHabSwitchControl switchControl;
	OpenHabDimmerControl dimmerControl;
	
	OpenHabHandler thingsHandler = new OpenHabHandler();
	OpenHabHandler itemsHandler = new OpenHabHandler();

	String hostURL;
	Timer timer;
	
	List<Boolean> isEntryAlarm = new ArrayList<Boolean>();
	List<Boolean> isBurglarAlarm = new ArrayList<Boolean>();
	List<Boolean> isGeneralAlarm = new ArrayList<Boolean>();
	List<Boolean> isSensorBinary = new ArrayList<Boolean>();
	List<Boolean> isSensorLum = new ArrayList<Boolean>();
	List<Boolean> isSensorRelHum = new ArrayList<Boolean>();
	List<Boolean> isSensorTemp = new ArrayList<Boolean>();
	List<Boolean> isSensorUV = new ArrayList<Boolean>();
	List<Boolean> isSwitchBinary = new ArrayList<Boolean>();
	List<Boolean> isSwitchDimmer = new ArrayList<Boolean>();
	List<Boolean> isBatteryLevel = new ArrayList<Boolean>();
	
    public OpenHabDriver()
    {
    }

    @Override
    public void setConfiguration(final OpenHabConfig config)
    {
        super.setConfiguration(config);
        
        // compute full host URL
        hostURL = "http://" + config.http.remoteHost + ":" + config.http.remotePort + "/rest";
    };
    
    @Override
    public void init() throws SensorHubException
    {
    	super.init();
	  
//    	selectorControl = null;
	  
    	int linkCnt = 0;
	  
    	// Get all available things and step through them to get all unique item
    	// names and types. Types are obtained from channel binding types, which should
    	// be consistent and not be changeable
    	OpenHabThings[] habThings = thingsHandler.getThingsFromJSON(getHostURL() + "/things");
//    	System.out.println("things length = " + habThings.length);
	  
    	// Loop through list of "Things"
    	for (int k = 0; k < habThings.length; k++)
    	{
//    		System.out.println(habThings[k].getLabel() + " is " + habThings[k].getStatusInfo().getStatus());
//    		System.out.println("\tNum Channels:" + habThings[k].getChannels().length);
		  
    		// Loop through list of "Channels" attached to each "Thing" 
    		for (int kk = 0; kk < habThings[k].getChannels().length; kk++)
    		{
//    			System.out.println("\tChannel[" + kk + "]: " + habThings[k].getChannels()[kk].getLinkedItems().length + " linked items");
//    			System.out.println("\t\tBindingType: " + habThings[k].getChannels()[kk].getChannelTypeUID());
			  
    			// Loop through each "Item" linked to each "Channel"
    			for (int kkk = 0; kkk < habThings[k].getChannels()[kk].getLinkedItems().length; kkk++)
    			{
//    				System.out.println("\t\tItem[" + kkk + "]: " + habThings[k].getChannels()[kk].getLinkedItems()[kkk]);
    				linkCnt++;
				  
    				switch (habThings[k].getChannels()[kk].getChannelTypeUID())
    				{
    				case "zwave:alarm_burglar":
//    					System.out.println("Found a burglar alarm");
    					isBurglarAlarm.add(true);
    					break;
				  
    				case "zwave:alarm_co":
//    					System.out.println("Found a CO alarm");
    					break;
					  
    				case "zwave:alarm_co2":
//    					System.out.println("Found a CO2 alarm");
					  	break;
					  
    				case "zwave:alarm_entry":
//    					System.out.println("Found an entry alarm");
    					isEntryAlarm.add(true);
    					break;
					  
    				case "zwave:alarm_flood":
//    					System.out.println("Found a flood alarm");
    					break;
					  
    				case "zwave:alarm_general":
//    					System.out.println("Found a general alarm");
    					isGeneralAlarm.add(true);
    					break;
					  
    				case "zwave:alarm_heat":
//    					System.out.println("Found a heat alarm");
    					break;
					  
    				case "zwave:alarm_motion":
//    					System.out.println("Found a motion alarm");
    					break;
					  
    				case "zwave:alarm_smoke":
//    					System.out.println("Found a smoke alarm");
    					break;
					  
    				case "zwave:color_color":
//    					System.out.println("Found a color item");
    					break;
					  
    				case "zwave:color_temperature":
//    					System.out.println("Found a color temp");
    					break;
					  
    				case "zwave:meter_current":
//    					System.out.println("Found a current meter");
    					break;
					  
    				case "zwave:meter_kvah":
//    					System.out.println("Found a kvah meter");
    					break;
					  
    				case "zwave:meter_kwh":
//    					System.out.println("Found a kwh meter");
    					break;
					  
    				case "zwave:meter_powerfactor":
//    					System.out.println("Found a powerfactor meter");
    					break;
					  
    				case "zwave:meter_reset":
//    					System.out.println("Found a meter reset");
    					break;
					  
    				case "zwave:meter_voltage":
//    					System.out.println("Found a voltage meter");
    					break;
					  
    				case "zwave:meter_watts":
//    					System.out.println("Found a power meter");
    					break;
					  
    				case "zwave:scene_number":
//    					System.out.println("Found a scene number");
    					break;
					  
    				case "zwave:sensor_binary":
//    					System.out.println("Found a binary sensor");
    					isSensorBinary.add(true);
    					break;
					  
    				case "zwave:sensor_door":
//    					System.out.println("Found a door sensor");
    					break;
					  
    				case "zwave:sensor_luminance":
//    					System.out.println("Found a luminance sensor");
    					isSensorLum.add(true);
    					break;
					  
    				case "zwave:sensor_power":
//    					System.out.println("Found a power sensor");
    					break;
					  
    				case "zwave:sensor_relhumidity":
//    					System.out.println("Found a relhum sensor");
    					isSensorRelHum.add(true);
    					break;
					  
    				case "zwave:sensor_temperature":
//    					System.out.println("Found a temperature sensor");
    					isSensorTemp.add(true);
    					break;
					  
    				case "zwave:sensor_ultraviolet":
//    					System.out.println("Found an ultraviolet sensor");
    					isSensorUV.add(true);
    					break;
					  
    				case "zwave:switch_binary":
//    					System.out.println("Found a binary switch");
    					isSwitchBinary.add(true);
    					break;
					  
    				case "zwave:switch_dimmer":
//    					System.out.println("Found a dimmer switch");
    					isSwitchDimmer.add(true);
    					break;
					  
    				case "zwave:thermostat_setpoint":
//    					System.out.println("Found a thermostat setpoint");
    					break;
					  
    				case "zwave:thermostat_fan_mode":
//    					System.out.println("Found a thermostat fan mode");
    					break;
					  
    				case "zwave:thermostat_fan_state":
//    					System.out.println("Found a thermostat fan state");
    					break;
					  
    				case "zwave:thermostat_state":
//    					System.out.println("Found a thermostat state");
    					break;
					  
    				case "zwave:thermostat_mode":
//    					System.out.println("Found a thermostat mode");
    					break;
					  
    				case "system:battery-level":
//    					System.out.println("Found a battery level");
    					isBatteryLevel.add(true);
    					break;
    				}
    			}
    		}
//    		System.out.println();
    	}

//    	System.out.println("Any Entry Alarm? = " + isEntryAlarm.contains(true));
//    	System.out.println("Any Burglar Alarm? = " + isBurglarAlarm.contains(true));
//    	System.out.println("Any General Alarm? = " + isGeneralAlarm.contains(true));
//    	System.out.println("Any Binary Sensor? = " + isSensorBinary.contains(true));
//    	System.out.println("Any Luminance? = " + isSensorLum.contains(true));
//    	System.out.println("Any Rel Hum? = " + isSensorRelHum.contains(true));
//    	System.out.println("Any Temperature? = " + isSensorTemp.contains(true));
//    	System.out.println("Any UV? = " + isSensorUV.contains(true));
//    	System.out.println("Any Binary Switch? = " + isSwitchBinary.contains(true));
//    	System.out.println("Any Dimmer Switch? = " + isSwitchDimmer.contains(true));
//    	System.out.println("Any Battery Level? = " + isBatteryLevel.contains(true));
//    	System.out.println();
	  
    	// Construct SWE outputs
    	if (linkCnt > 0)
    	{
    		habStatusOut = new OpenHabStatusOutput(this);
    		try { habStatusOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habStatusOut, false);
    		
    		habAlertOut = new OpenHabAlertOutput(this);
    		try { habAlertOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habAlertOut, false);
    	}
	  
    	if (isSensorLum.contains(true) || isSensorRelHum.contains(true) || isSensorTemp.contains(true) || isSensorUV.contains(true))
    	{
    		habEnviroOut = new OpenHabEnviroOutput(this);
    		try { habEnviroOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habEnviroOut, false);
    	}

    	if (isSensorBinary.contains(true))
    	{
    		habBinOut = new OpenHabSensorBinaryOutput(this);
    		try { habBinOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habBinOut, false);
    	}
    	
    	if (isEntryAlarm.contains(true) || isBurglarAlarm.contains(true) || isGeneralAlarm.contains(true))
    	{
    		habAlarmOut = new OpenHabAlarmOutput(this);
    		try { habAlarmOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habAlarmOut, false);
    	}
    	
    	if (isEntryAlarm.contains(true))
    	{
    		habEntryOut = new OpenHabAlarmEntryOutput(this);
    		try { habEntryOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habEntryOut, false);
    	}
    	
    	if (isBurglarAlarm.contains(true))
    	{
    		habBurglarOut = new OpenHabAlarmBurglarOutput(this);
    		try { habBurglarOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habBurglarOut, false);
    	}
    	
    	if (isGeneralAlarm.contains(true))
    	{
    		habGenOut = new OpenHabAlarmGenOutput(this);
    		try { habGenOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habGenOut, false);
    	}
    	
    	if (isSensorTemp.contains(true))
    	{
    		habTempOut = new OpenHabTempOutput(this);
    		try { habTempOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habTempOut, false);
    	}
	  
    	if (isSensorRelHum.contains(true))
    	{
    		habHumOut = new OpenHabHumOutput(this);
    		try { habHumOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habHumOut, false);
    	}
	  
    	if (isSensorUV.contains(true))
    	{
    		habUVOut = new OpenHabUVOutput(this);
    		try { habUVOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habUVOut, false);
    	}
	  
    	if (isSensorLum.contains(true))
    	{
    		habLumOut = new OpenHabLumOutput(this);
    		try { habLumOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habLumOut, false);
    	}
	  
	  
    	if (isSwitchDimmer.contains(true))
    	{
    		habDimmerOut = new OpenHabDimmerOutput(this);
    		try { habDimmerOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habDimmerOut, false);

    		// add dimmer controller
    		this.dimmerControl = new OpenHabDimmerControl(this);
    		addControlInput(dimmerControl);
    		dimmerControl.init();
    	}
	  
	  
    	if (isSwitchBinary.contains(true))
    	{
    		habSwitchOut = new OpenHabSwitchOutput(this);
    		try { habSwitchOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habSwitchOut, false);
    		
    		// add switch controller
    		this.switchControl = new OpenHabSwitchControl(this);
    		addControlInput(switchControl);
    		switchControl.init();
    	}
	  
    	if (isBatteryLevel.contains(true))
    	{
    		habBattOut = new OpenHabBatteryOutput(this);
    		try { habBattOut.init(); }
    		catch (IOException e) { e.printStackTrace(); }
    		addOutput(habBattOut, false);
    	}
	  
//	  if (valTypes.contains(21) || valTypes.contains(24))
//	  {
//		  // add switch controller
//          this.switchControl = new OpenHabSwitchControl(this);
//          addControlInput(switchControl);
//          switchControl.init();
//	  }

    	// add unique ID based on serial number
    	this.uniqueID = "urn:openhab:" + config.modelNumber + ":" + config.serialNumber;
    	this.xmlID = "OPENHAB_" + config.modelNumber + "_" + config.serialNumber.toUpperCase();
    }
  
  
    public void ParseAndSendData() throws SensorHubException
    {  
    	// Get all available things and step through them to get all unique item
    	// names and types. Types are obtained from channel binding types, which should
    	// be consistent and not be changeable
    	OpenHabThings[] habThingsStart = thingsHandler.getThingsFromJSON(getHostURL() + "/things");
//    	System.out.println("things length = " + habThingsStart.length);

    	// Loop through list of "Things"
    	for (int k = 0; k < habThingsStart.length; k++)
    	{
    		// Loop through list of "Channels" attached to each "Thing" 
    		for (int kk = 0; kk < habThingsStart[k].getChannels().length; kk++)
    		{
    			// Loop through each "Item" linked to each "Channel"
    			for (int kkk = 0; kkk < habThingsStart[k].getChannels()[kk].getLinkedItems().length; kkk++)
    			{
    				OpenHabItems habItemsStart = itemsHandler.getItemsFromJSON(getHostURL() + "/items/" + habThingsStart[k].getChannels()[kk].getLinkedItems()[kkk]);
//    				System.out.println("habItem: " + habItemsStart.getName());
    				switch (habThingsStart[k].getChannels()[kk].getChannelTypeUID())
    				{
    				case "zwave:alarm_burglar":
    					habBurglarOut.postBurglarData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					habAlarmOut.postAlarmData(habThingsStart[k], habItemsStart);
    					
    					if (habItemsStart.getState().equalsIgnoreCase("ON"))
    						habAlertOut.postAlertData(habThingsStart[k], habItemsStart, "Triggered!");
    					break;

    				case "zwave:alarm_co":
    					break;

    				case "zwave:alarm_co2":
    					break;

    				case "zwave:alarm_entry":
    					habEntryOut.postEntryData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					habAlarmOut.postAlarmData(habThingsStart[k], habItemsStart);
    					
    					if (habItemsStart.getState().equalsIgnoreCase("ON"))
    						habAlertOut.postAlertData(habThingsStart[k], habItemsStart, "Triggered!");
    					break;

    				case "zwave:alarm_flood":
    					break;

    				case "zwave:alarm_general":
    					habGenOut.postGeneralData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					habAlarmOut.postAlarmData(habThingsStart[k], habItemsStart);
    					
    					if (habItemsStart.getState().equalsIgnoreCase("ON"))
    						habAlertOut.postAlertData(habThingsStart[k], habItemsStart, "Triggered!");
    					break;

    				case "zwave:alarm_heat":
    					break;

    				case "zwave:alarm_motion":
    					break;

    				case "zwave:alarm_smoke":
    					break;

    				case "zwave:color_color":
    					break;

    				case "zwave:color_temperature":
    					break;

    				case "zwave:meter_current":
    					break;

    				case "zwave:meter_kvah":
    					break;

    				case "zwave:meter_kwh":
    					break;

    				case "zwave:meter_powerfactor":
    					break;

    				case "zwave:meter_reset":
    					break;

    				case "zwave:meter_voltage":
    					break;

    				case "zwave:meter_watts":
    					break;

    				case "zwave:scene_number":
    					break;

    				case "zwave:sensor_binary":
    					habBinOut.postBinData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					
    					if (habItemsStart.getState().equalsIgnoreCase("ON"))
    						habAlertOut.postAlertData(habThingsStart[k], habItemsStart, "Triggered!");
    					break;

    				case "zwave:sensor_door":
    					break;

    				case "zwave:sensor_luminance":
    					habLumOut.postLumData(habThingsStart[k], habItemsStart);
    					habEnviroOut.postEnviroData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					break;

    				case "zwave:sensor_power":
    					break;

    				case "zwave:sensor_relhumidity":
    					habHumOut.postHumData(habThingsStart[k], habItemsStart);
    					habEnviroOut.postEnviroData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					break;

    				case "zwave:sensor_temperature":
    					habTempOut.postTempData(habThingsStart[k], habItemsStart);
    					habEnviroOut.postEnviroData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					
    					if (!habItemsStart.getState().equalsIgnoreCase("NULL") && (Double.parseDouble(habItemsStart.getState()) < 18.0 || Double.parseDouble(habItemsStart.getState()) > 20.0))
    						habAlertOut.postAlertData(habThingsStart[k], habItemsStart, "Temp is " + Double.parseDouble(habItemsStart.getState()) + "\u00b0" + "C!");
    					break;

    				case "zwave:sensor_ultraviolet":
    					habUVOut.postUVData(habThingsStart[k], habItemsStart);
    					habEnviroOut.postEnviroData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					break;

    				case "zwave:switch_binary":
    					habSwitchOut.postSwitchData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					break;

    				case "zwave:switch_dimmer":
    					habDimmerOut.postDimmerData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					break;

    				case "zwave:thermostat_setpoint":
    					break;

    				case "zwave:thermostat_fan_mode":
    					break;

    				case "zwave:thermostat_fan_state":
    					break;

    				case "zwave:thermostat_state":
    					break;

    				case "zwave:thermostat_mode":
    					break;

    				case "system:battery-level":
    					habBattOut.postBatteryData(habThingsStart[k], habItemsStart);
    					habStatusOut.postStatusData(habThingsStart[k], kk, habItemsStart);
    					
    					if (!habItemsStart.getState().equalsIgnoreCase("NULL") && Integer.parseInt(habItemsStart.getState()) < 10)
    						habAlertOut.postAlertData(habThingsStart[k], habItemsStart, "Low Battery!");
    					break;
    				}
    			}
    		}
    		System.out.println();
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
    					getLogger().error("Cannot get OpenHAB data", e);
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
