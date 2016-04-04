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

package org.sensorhub.impl.sensor.dahua;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
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
 * Implementation of sensor interface for generic Dahua Cameras using IP
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */
public class DahuaPtzControl extends AbstractSensorControl<DahuaCameraDriver>
{
	DataChoice commandData;

    // define and set default values
    double minPan = 0.0;
    double maxPan = 360.0;
    double minTilt = 0.0;
    double maxTilt = 90.0;
    double minZoom = 1.0;
    double maxZoom = 100.0;   //TODO: Determine max zoom for Dahua cameras
    
    // Since Dahua doesn't allow you to retrieve current PTZ positions, save the last state here and push to the output module
    double pan = 0.0;
    double tilt = 0.0;
    double zoom = 1.0;    
    PtzPresetsHandler presetsHandler;
    
    
    protected DahuaPtzControl(DahuaCameraDriver driver)
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
        DahuaCameraConfig config = parentSensor.getConfiguration();
        presetsHandler = new PtzPresetsHandler(config.ptz);
        
        // get PTZ limits
        try
        {    	         
            URL optionsURL = new URL("http://" + parentSensor.getHostName() + "/cgi-bin/ptz.cgi?action=getCurrentProtocolCaps&channel=0");
            InputStream is = optionsURL.openStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(is));

            // get limit values from IP stream
            String line;
            while ((line = bReader.readLine()) != null)
            {
                // parse response
                String[] tokens = line.split("=");

                if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.HorizontalAngle[0]"))
                    minPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.HorizontalAngle[1]"))
                    maxPan = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.VerticalAngle[0]"))
                    minTilt = Double.parseDouble(tokens[1]);
                else if (tokens[0].trim().equalsIgnoreCase("caps.PtzMotionRange.VerticalAngle[1]"))
                    maxTilt = Double.parseDouble(tokens[1]);
                //else if (tokens[0].trim().equalsIgnoreCase("root.PTZ.Limit.L1.MaxZoom"))
                //    maxZoom = Double.parseDouble(tokens[1]);
            }
	    }
	    catch (IOException e)
	    {
	        e.printStackTrace();
	    }

        // build SWE data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();
        Collection<String> presetList = presetsHandler.getPresetNames();
        commandData = videoHelper.getPtzTaskParameters(getName(), minPan, maxPan, minTilt, maxTilt, minZoom, maxZoom, presetList);
        
        // reset to Pan=0, Tilt=0, Zoom=0
        try
        {
            DataBlock initCmd;
            commandData.setSelectedItem(0);
            initCmd = commandData.createDataBlock();
            execCommand(initCmd);
            commandData.setSelectedItem(1);
            initCmd = commandData.createDataBlock();
            execCommand(initCmd);
            commandData.setSelectedItem(2);
            initCmd = commandData.createDataBlock();
            execCommand(initCmd);
        }
        catch (SensorException e)
        {
            e.printStackTrace();
        }
    }
    
    
    protected void start() throws SensorException
    {
        // reset to Pan=0, Tilt=0, Zoom=0
        DataBlock initCmd;
        commandData.setSelectedItem(0);
        initCmd = commandData.createDataBlock();
        execCommand(initCmd);
        commandData.setSelectedItem(1);
        initCmd = commandData.createDataBlock();
        execCommand(initCmd);
        commandData.setSelectedItem(2);
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
        // associate command data to msg structure definition
        DataChoice commandMsg = (DataChoice) commandData.copy();
        commandMsg.setData(command);
              
        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedItem();
        String itemID = component.getName();
        DataBlock data = component.getData();
        
        // NOTE: you can use validate() method in DataComponent
        // component.validateData(errorList);  // give it a list so it can store the errors
        // if (errorList != empty)  //then you have an error
              
        try
        {
        	// if preset position, act on that with
        	if (itemID.equals(VideoCamHelper.TASKING_PTZPRESET))
        	{
        	    PtzPreset preset = presetsHandler.getPreset(data.getStringValue());
        	    setPan(preset.pan);
        	    setTilt(preset.tilt);
        	    setZoom(preset.zoom);
        	}
        	
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PAN))
        	    setPan(data.getDoubleValue());
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_TILT))
        	    setTilt(data.getDoubleValue());
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_ZOOM))
        	    setZoom(data.getDoubleValue());
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_RPAN))
        	    setPan(data.getDoubleValue() + pan);
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_RTILT))
        	    setTilt(data.getDoubleValue() + tilt);
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_RTILT))
        	    setZoom(data.getDoubleValue() + zoom);
        	else if (itemID.equalsIgnoreCase(VideoCamHelper.TASKING_PTZ_POS))
        	{
        		// Set all components of PTZ
        	    setPan(data.getDoubleValue(0));
        	    setTilt(data.getDoubleValue(1));
        	    setZoom(data.getDoubleValue(2));    		
        	}
        	
        	// send request to absolute pan/tilt/zoom position
            URL optionsURL = new URL("http://" + parentSensor.getHostName() + 
            		"/cgi-bin/ptz.cgi?action=start&channel=0&code=PositionABS&arg1=" + pan + "&arg2=" + tilt + "&arg3=" + zoom);
                   	         
            // add BufferReader and read first line; if "Error", read second line and log error
            InputStream is = optionsURL.openStream();
            is.close();
	    }
	    catch (Exception e)
	    {
	    	throw new SensorException("Error connecting to Dahua PTZ control", e);
	    }        
       
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;    
        
        return cmdStatus;
    }
    
    
    // set pan and notify DahuaPtzOutput
    public void setPan(double value)
    {
    	pan = value;
    	parentSensor.ptzDataInterface.setPan(value);
    }
    

    // set tilt and notify DahuaPtzOutput
    public void setTilt(double value)
    {
    	tilt = value;
    	parentSensor.ptzDataInterface.setTilt(value);
    }
    

    // set zoom and notify DahuaPtzOutput
    public void setZoom(double value)
    {
    	zoom = value;
    	parentSensor.ptzDataInterface.setZoom(value);
    }
}
