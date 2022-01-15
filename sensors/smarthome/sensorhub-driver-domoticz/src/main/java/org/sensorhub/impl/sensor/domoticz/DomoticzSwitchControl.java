package org.sensorhub.impl.sensor.domoticz;

import java.io.InputStream;
import java.net.URL;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.DataChoiceImpl;
import org.vast.swe.SWEHelper;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Text;


public class DomoticzSwitchControl extends AbstractSensorControl<DomoticzDriver>
{
	DataChoice commandData;

	
	public DomoticzSwitchControl(DomoticzDriver driver)
	{
		super("switchControl", driver);
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
	}
	
	
	@Override
	public DataComponent getCommandDescription() {
		return commandData;
	}


    protected void start() throws SensorException
    {
    }
	
    
    @Override
	protected boolean execCommand(DataBlock command) throws CommandException {
    	
    	// associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();
        commandMsg.setData(command);
        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String indexName = component.getName();
        String cmd = "";
        
        if (indexName.equalsIgnoreCase("setOn"))
        	cmd = "On";
        else if (indexName.equalsIgnoreCase("setOff"))
        	cmd = "Off";
        else if (indexName.equalsIgnoreCase("toggle"))
        	cmd = "Toggle";
        
        DataBlock data = component.getData();
        
        String idx = data.getStringValue();
    	
        // send request
        try
        {
        	System.out.println("Setting Switch " + idx + " " + cmd);
			URL optionsURL = new URL(parentSensor.getHostURL() + 
					"type=command&param=switchlight&idx=" + idx + "&switchcmd=" + cmd);
			InputStream is = optionsURL.openStream();
            is.close();
		}
        catch (Exception e)
        {
        	throw new CommandException("Error sending command", e);
		}
        
        return true;
	}
    
    
    protected void stop()
    {       
    }

}
