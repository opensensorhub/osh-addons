/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2016 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.foscam;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;

import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.foscam.ptz.FoscamPTZpreset;
import org.sensorhub.impl.sensor.foscam.ptz.FoscamPTZpresetsHandler;
import org.sensorhub.impl.sensor.foscam.ptz.FoscamPTZrelMove;
import org.sensorhub.impl.sensor.foscam.ptz.FoscamPTZrelMoveHandler;
import org.vast.data.DataChoiceImpl;


/**
 * <p>
 * Implementation of sensor interface for Foscam Cameras using IP
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * @author Lee Butler <labutler10@gmail.com>
 * @since September 2016
 */
public class FoscamPtzControl extends AbstractSensorControl<FoscamDriver>
{
	DataChoice commandData;
	FoscamDriver DriverDataInterface;
    FoscamPTZpresetsHandler presetsHandler;
    FoscamPTZrelMoveHandler relMoveHandler;
    
    
    protected FoscamPtzControl(FoscamDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return "ptzControl";
    }
    
    
	protected void init() throws SensorException
    {
    	System.out.println("In PTZ Control init()");
        FoscamConfig config = parentSensor.getConfiguration();
        presetsHandler = new FoscamPTZpresetsHandler(config.ptz);
        relMoveHandler = new FoscamPTZrelMoveHandler(config.ptz);

        // build SWE data structure for the tasking parameters
        FoscamHelper foscamHelper = new FoscamHelper();
        Collection<String> presetList = presetsHandler.getPresetNames();
        Collection<String> relMoveList = relMoveHandler.getRelMoveNames();
        System.out.println("presetList = " + presetList);
        System.out.println("relMoveList = " + relMoveList);
        commandData = foscamHelper.getPtzTaskParameters(getName(), relMoveList, presetList);
    }
    
    
    protected void start() throws SensorException
    {
        // reset to Pan=0, Tilt=0, Zoom=0
        DataBlock initCmd;
        commandData.setSelectedItem(7);
        initCmd = commandData.createDataBlock();
        execCommand(initCmd);
    }
    
    
    protected void stop()
    {       
    }


    @Override
    public DataComponent getCommandDescription()
    {    
        return commandData;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
    	System.out.println("In execCommand...");
        // associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();
        commandMsg.setData(command);
        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        DataBlock data = component.getData();
        
        System.out.println("itemID = " + component.getName());
              
        try
        {
        	// preset position
        	if (component.getName().equals(FoscamHelper.TASKING_PTZPRESET))
        	{
        		FoscamPTZpreset preset = presetsHandler.getPreset(data.getStringValue());
        		System.out.println("Tasking with PTZ preset " + preset.name);
        		
        		
        		String presetCmd = new String();
        		
        		if (preset.name.equalsIgnoreCase("Reset"))
        			presetCmd = "ptzReset";
        		else
	        	    presetCmd = "ptzGotoPresetPoint&name=" + preset.name;
        		
        		// move to preset
        	    URL optionsURL = new URL("http://" + parentSensor.getConfiguration().http.remoteHost + 
        	    		":" + Integer.toString(parentSensor.getConfiguration().http.remotePort) +
        	    		"/cgi-bin/CGIProxy.fcgi?cmd=" + presetCmd + 
        	    		"&usr=" + parentSensor.getConfiguration().http.user + 
        	    		"&pwd=" + parentSensor.getConfiguration().http.password);
        	    
        	    // add BufferReader and check for error
        	    InputStream is = optionsURL.openStream();
        	    BufferedReader reader = null;
        	    reader = new BufferedReader(new InputStreamReader(is));
        	    String line;
        	    while ((line = reader.readLine()) != null)
        	    {
        	    	String[] tokens = line.split("<|\\>");
        	    	if (tokens[1].trim().equals("runResult"))
        	    	{
        	    		if (!tokens[2].trim().equalsIgnoreCase("0"))
        	    			System.err.println("Unrecognized Command");
        	    		else
        	    			System.out.println("Successful Command");
        	    	}
        	    }
        	    is.close();
        	}
        	
        	// relative movement
        	else if (component.getName().equals(FoscamHelper.TASKING_PTZREL))
        	{
        		System.out.println("Tasking with relative Movement...");
        		FoscamPTZrelMove relMove = relMoveHandler.getRelMove(data.getStringValue());
        		System.out.println("rel move name = " + relMove.name);
        	    URL optionsURL = new URL("http://" + parentSensor.getConfiguration().http.remoteHost + 
        	    		":" + Integer.toString(parentSensor.getConfiguration().http.remotePort) +
        	    		"/cgi-bin/CGIProxy.fcgi?cmd=" + relMove.cgi +
        	    		"&usr=" + parentSensor.getConfiguration().http.user + 
        	    		"&pwd=" + parentSensor.getConfiguration().http.password);

        	    // add BufferReader and read first line; if "Error", read second line and log error
        	    InputStream is = optionsURL.openStream();
        	    BufferedReader reader = null;
        	    reader = new BufferedReader(new InputStreamReader(is));
        	    String line;
        	    while ((line = reader.readLine()) != null)
        	    {
        	    	String[] tokens = line.split("<|\\>");
        	    	if (tokens[1].trim().equals("result"))
        	    	{
        	    		if (!tokens[2].trim().equalsIgnoreCase("0"))
        	    			System.out.println("Unrecognized Command");
        	    		else
        	    			System.out.println("Successful Command");
        	    	}
        	    }
        	    is.close();
        	    // use timing to control amount of movement
        	    System.out.println("moveTime = " + relMove.moveTime);
            	Thread.sleep(relMove.moveTime);
            	
            	URL optionsURLstop = new URL("http://" + parentSensor.getConfiguration().http.remoteHost + 
        	    		":" + Integer.toString(parentSensor.getConfiguration().http.remotePort) + 
        	    		"/cgi-bin/CGIProxy.fcgi?cmd=ptzStopRun" +
        	    		"&usr=" + parentSensor.getConfiguration().http.user + 
        	    		"&pwd=" + parentSensor.getConfiguration().http.password);
            	InputStream isStop = optionsURLstop.openStream();
            	isStop.close();
        	}
        	
        	
        }
        catch (Exception e)
        {
        	throw new SensorException("Error connecting to Foscam PTZ control", e);
        }        
       
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;    
        System.out.println("cmdStatus = " + cmdStatus.status);
        return cmdStatus;
    }
}
