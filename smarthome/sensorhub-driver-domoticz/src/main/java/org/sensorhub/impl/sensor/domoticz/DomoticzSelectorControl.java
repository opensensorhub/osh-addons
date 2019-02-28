package org.sensorhub.impl.sensor.domoticz;

import java.io.InputStream;
import java.net.URL;

import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.DataChoiceImpl;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Text;


public class DomoticzSelectorControl extends AbstractSensorControl<DomoticzDriver>
{
	DataChoice commandData;

	public DomoticzSelectorControl(DomoticzDriver driver)
	{
		super(driver);
	}

	
	@Override
	public String getName()
	{
		return "selectorControl";
	}

	
	protected void init()
	{
		SWEHelper sweHelp = new SWEHelper();
		commandData = sweHelp.newDataChoice();
		commandData.setName(getName());

		Text turnOn = sweHelp.newText("http://sensorml.com/ont/swe/property/turnOn", 
        		"On", 
        		"Set switch On");
		commandData.addItem("setOn", turnOn);
		
		Text turnOff = sweHelp.newText("http://sensorml.com/ont/swe/property/turnOff", 
        		"Off", 
        		"Set switch Off");
		commandData.addItem("setOff", turnOff);
		
		Text toggle = sweHelp.newText("http://sensorml.com/ont/swe/property/toggle", 
        		"Toggle", 
        		"Toggle switch");
		commandData.addItem("toggle", toggle);
		
		Text setLevel = sweHelp.newText("http://sensorml.com/ont/swe/property/setLevel", 
        		"Level Selector", 
        		"Value for selector switch");
		commandData.addItem("setLevel", setLevel);
	}
	
	
	@Override
	public DataComponent getCommandDescription() {
		return commandData;
	}


    protected void start() throws SensorException
    {
    }
	
    
    @Override
	public CommandStatus execCommand(DataBlock command) throws SensorException {
    	
    	// associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();
        commandMsg.setData(command); System.out.println("commandMsg = " + commandMsg);
        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        
        String indexName = component.getName();
        String cmd = "";
        String cmdLevel = "0";
        
        DataBlock data = component.getData();
        String[] cmdText = data.getStringValue(0).split(",");
        String idx = cmdText[0].trim();
        if (cmdText.length == 2)
        	cmdLevel = cmdText[1].trim();
        
        if (indexName.equalsIgnoreCase("setOn"))
        	cmd = "On";
        else if (indexName.equalsIgnoreCase("setOff"))
        	cmd = "Off";
        else if (indexName.equalsIgnoreCase("toggle"))
        	cmd = "Toggle";
        else if (indexName.equalsIgnoreCase("setLevel"))
        	cmd = "Set%20Level&level=" + cmdLevel;
    	
        // send request
        try
        {
        	System.out.println("Setting Sensor " + idx + " " + cmd);
			URL optionsURL = new URL(parentSensor.getHostURL() + 
					"type=command&param=switchlight&idx=" + idx + "&switchcmd=" + cmd);
			InputStream is = optionsURL.openStream();
            is.close();
		}
        catch (Exception e)
        {
        	throw new SensorException("Error sending command", e);
		}
        
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;    
        
        return cmdStatus;
	}
    
    
    protected void stop()
    {       
    }

}
