/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.axis;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.net.MalformedURLException;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPreset;
import org.sensorhub.impl.sensor.videocam.ptz.PtzPresetsHandler;
import org.vast.data.DataChoiceImpl;


/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */
public class AxisPtzControl extends AbstractSensorControl<AxisCameraDriver>
{
	DataChoice commandData;

    // define and set default values
    double minPan = -180.0;
    double maxPan = 180.0;
    double minTilt = -180.0;
    double maxTilt = 0.0;
    double minZoom = 1.0;
    double maxZoom = 9999;

    PtzPresetsHandler presetsHandler;
    URL optionsURL = null;
    
    protected AxisPtzControl(AxisCameraDriver driver)
    {
        super(driver);
        
        try {
            optionsURL = new URL(parentSensor.getHostUrl() + driver.VAPIX_QUERY_PARAMS_LIST_GROUP_PTZ);
        
        } catch (MalformedURLException e) {
           
            e.printStackTrace();
        }
    }
    
    
    @Override
    public String getName()
    {
        return "ptzControl";
    }
    
    
    protected void init()
    {
        AxisCameraConfig config = parentSensor.getConfiguration();
        presetsHandler = new PtzPresetsHandler(config.ptz);
        
        // get PTZ limits
        try
        {    	  
            InputStream is = optionsURL.openStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(is));

            // get limit values from IP stream
            String line;
            while ((line = bReader.readLine()) != null)
            {
                // parse response
                String[] tokens = line.split("=");

                if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MinPan"))
                    minPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxPan"))
                    maxPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MinTilt"))
                    minTilt = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxTilt"))
                    maxTilt = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxZoom"))
                    maxZoom = Double.parseDouble(tokens[1]);
            }
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }

        // build SWE data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();
        Collection<String> presetList = presetsHandler.getPresetNames();
        commandData = videoHelper.getPtzTaskParameters(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom, presetList);      
    }
    
    
    protected void start()
    {
        
    }
    

    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
    	// associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();
        commandMsg.setData(command);
              
        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String itemID = component.getName();
        DataBlock data = component.getData();
        String itemValue = data.getStringValue();
          
        // NOTE: you can use validate() method in DataComponent
        // component.validateData(errorList);  // give it a list so it can store the errors
        // if (errorList != empty)  //then you have an error
          
        try
        {
            // set parameter value on camera 
            // NOTE: except for "presets", the item IDs are labeled the same as the Axis parameters so just use those in the command
        	if (itemID.equals(VideoCamHelper.TASKING_PTZPRESET))
        	{
        	    PtzPreset preset = presetsHandler.getPreset(data.getStringValue());
        	    
                // pan + tilt + zoom (supported since v2 at least)
        	    optionsURL = new URL(parentSensor.getHostUrl() + "/com/ptz.cgi?pan=" + preset.pan
        	    		+ "&tilt=" + preset.tilt + "&zoom=" + preset.zoom);
                InputStream is = optionsURL.openStream();
                is.close();
       	    
        	}
        	
        	// Act on full PTZ Position
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PTZ_POS))
        	{

        	    optionsURL = new URL(parentSensor.getHostUrl() + "/com/ptz.cgi?pan=" + data.getStringValue(0)
        	    		+ "&tilt=" + data.getStringValue(1) + "&zoom=" + data.getStringValue(2));
                InputStream is = optionsURL.openStream();
                is.close();

        	}
     	
        	else
        	{
        		String cmd = " ";
        		if (itemID.equals(VideoCamHelper.TASKING_PAN)) 
        			cmd = "pan";
        		else if (itemID.equals(VideoCamHelper.TASKING_RPAN)) 
        			cmd = "rpan";
        		else if (itemID.equals(VideoCamHelper.TASKING_TILT)) 
        			cmd = "tilt";
        		else if (itemID.equals(VideoCamHelper.TASKING_RTILT)) 
        			cmd = "rtilt";
        		else if (itemID.equals(VideoCamHelper.TASKING_ZOOM)) 
        			cmd = "zoom";
        		else if (itemID.equals(VideoCamHelper.TASKING_RZOOM)) 
        			cmd = "rzoom";
        			      			
                optionsURL = new URL(parentSensor.getHostUrl() + "/com/ptz.cgi?" + cmd + "=" + itemValue);
                InputStream is = optionsURL.openStream();
                is.close();       		
        	}
        	
	    }
	    catch (Exception e)
	    {	    	
	        throw new SensorException("Error connecting to Axis PTZ control", e);
	    }        
       
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;        
        return cmdStatus;
    }
    
    
    @Override
    public DataComponent getCommandDescription()
    {    
        return commandData;
    }


	public void stop()
	{

	}

}
