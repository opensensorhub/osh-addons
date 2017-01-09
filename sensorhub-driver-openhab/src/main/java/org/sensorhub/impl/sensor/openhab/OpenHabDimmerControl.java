package org.sensorhub.impl.sensor.openhab;

import java.io.OutputStream;
import java.net.HttpURLConnection;
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


public class OpenHabDimmerControl extends AbstractSensorControl<OpenHabDriver>
{
	DataChoice commandData;

	public OpenHabDimmerControl(OpenHabDriver driver)
	{
		super(driver);
	}

	
	@Override
	public String getName()
	{
		return "dimmerControl";
	}

	
	protected void init()
	{
//		System.out.println("Adding Dimmer Control SWE Template");
		
		SWEHelper sweHelp = new SWEHelper();
		commandData = sweHelp.newDataChoice();
		commandData.setName(getName());

		Text turnOn = sweHelp.newText("http://sensorml.com/ont/swe/property/turnOn", 
        		"On", 
        		"Turn Dimmer on to 100%");
		commandData.addItem("setOn", turnOn);
		
		Text turnOff = sweHelp.newText("http://sensorml.com/ont/swe/property/turnOff", 
        		"Off", 
        		"Turn Dimmer Off");
		commandData.addItem("setOff", turnOff);
		
		Text setLevel = sweHelp.newText("http://sensorml.com/ont/swe/property/setLevel", 
        		"Level Selector", 
        		"Value for dimmer switch");
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
        commandMsg.setData(command);
        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        
        String indexName = component.getName();
        String cmd = "";
        String dimmerVal = "0";
        
        DataBlock data = component.getData();
        String[] cmdText = data.getStringValue(0).split(",");
        String name = cmdText[0].trim();
        if (cmdText.length == 2)
        	dimmerVal = cmdText[1].trim();
        
        if (indexName.equalsIgnoreCase("setOn"))
        	cmd = "ON";
        else if (indexName.equalsIgnoreCase("setOff"))
        	cmd = "OFF";
        else if (indexName.equalsIgnoreCase("setLevel"))
        	cmd = dimmerVal;
    	
        
        // make http post
        try
        {
			URL optionsURL = new URL(parentSensor.getHostURL() + 
					"/items/" + name);
			HttpURLConnection conn = (HttpURLConnection) optionsURL.openConnection();
			conn.setDoOutput(true);
			conn.addRequestProperty("Content-Type", "text/plain");
			conn.addRequestProperty("Accept", "application/json");
			conn.setRequestMethod("POST");
			conn.connect();
			
			byte[] outputBytes = cmd.getBytes("UTF-8");
			OutputStream os = conn.getOutputStream();
			
			System.out.println("trying request " + optionsURL + " with " + cmd);
			os.write(outputBytes);
			conn.getResponseCode();
			os.flush();
			os.close();
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
