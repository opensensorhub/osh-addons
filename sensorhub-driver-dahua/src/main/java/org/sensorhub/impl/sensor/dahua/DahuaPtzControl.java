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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;

import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.DataChoiceImpl;
import org.vast.data.SWEFactory;

/**
 * <p>
 * Implementation of sensor interface for generic Dahua Cameras using IP
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * <p>
 * Copyright (c) 2016
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since March 2016
 */



public class DahuaPtzControl extends AbstractSensorControl<DahuaCameraDriver>
{
	DataChoice commandData; 
	DahuaCameraDriver driver;
		
	String ipAddress;

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

    
    
    protected DahuaPtzControl(DahuaCameraDriver driver)
    {
        super(driver);
        this.driver = driver;
    }
    
    
    @Override
    public String getName()
    {
        return "ptzControl";
    }
    
    
    protected void init()
    {
    	        
        ipAddress = parentSensor.getConfiguration().net.remoteHost;
        
        // get PTZ limits
        try
        {    	         
            URL optionsURL = new URL("http://" + ipAddress + "/cgi-bin/ptz.cgi?action=getCurrentProtocolCaps&channel=0");
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
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }


        // get preset Position Names
        // Unfortunately Dahua IP API v1.0 doesn't support getting preset position names so we'll keep the list empty until we find a way

        List<String> presetList = new LinkedList<>();
        
//        try
//        {    	         
//            URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=root.PTZ.Preset.P0.Position.*.Name");
//            InputStream is = optionsURL.openStream();
//            BufferedReader bReader = new BufferedReader(new InputStreamReader(is));
//
//            String line;           
//            while ((line = bReader.readLine()) != null)
//            {
//                String[] tokens = line.split("=");
//                presetList.add(tokens[1]);
//            }
//	    }
//	    catch (Exception e)
//	    {
//	        e.printStackTrace();
//	    }

        
        // get the SWE Common Data structure for the tasking parameters
        VideoCamHelper videoHelper = new VideoCamHelper();
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
        String itemValue = component.getData().getStringValue();
        
        // NOTE: you can use validate() method in DataComponent
        // component.validateData(errorList);  // give it a list so it can store the errors
        // if (errorList != empty)  //then you have an error
              
        try
        {
        	         
         	// if gotoserverpresetname, act on that with
        	// http://192.168.0.201/cgi-bin/ptz.cgi?action=start&channel=0&code=GotoPreset&arg1=0&arg2=<presetNumber>&arg3=0

        	// if (itemID.equalsIgnoreCase("gotoPresetPosition")) ..... ;	 
        	// figure out preset number based on name (itemValue) and then
            // URL optionsURL = new URL("http://" + ipAddress + "/cgi-bin/ptz.cgi?action=start&channel=0&code=GotoPreset&arg1=0&0arg2="
        	//	   + presetNumber + "&arg3=0"
            // InputStream is = optionsURL.openStream();
        	// is.close();
        	
        	
        	// for absolute or relative pan-tilt-zoom use:
        	// http://192.168.0.201/cgi-bin/ptz.cgi?action=start&channel=0&code=PositionABS&arg1=0&arg2=0&arg3=0
        	
        	if (itemID.equalsIgnoreCase("pan")) setPan(Double.parseDouble(itemValue));
        	else if (itemID.equalsIgnoreCase("tilt")) setTilt(Double.parseDouble(itemValue));
        	else if (itemID.equalsIgnoreCase("zoom")) setZoom(Double.parseDouble(itemValue));
        	else if (itemID.equalsIgnoreCase("rpan")) setPan(Double.parseDouble(itemValue) + pan);
        	else if (itemID.equalsIgnoreCase("rtilt")) setTilt(Double.parseDouble(itemValue) + tilt);
        	else if (itemID.equalsIgnoreCase("rzoom")) setZoom(Double.parseDouble(itemValue) + zoom);
        	
            URL optionsURL = new URL("http://" + ipAddress + 
            		"/cgi-bin/ptz.cgi?action=start&channel=0&code=PositionABS&arg1=" + pan + "&arg2=" + tilt + "&arg3=" + zoom);
            InputStream is = optionsURL.openStream();
       	         
            // add BufferReader and read first line; if "Error", read second line and log error
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
    public void setPan(double value){
    	pan = value;
    	driver.ptzDataInterface.setPan(value);
    }

    // set tilt and notify DahuaPtzOutput
    public void setTilt(double value){
    	tilt = value;
    	driver.ptzDataInterface.setTilt(value);
    }

    // set zoom and notify DahuaPtzOutput
    public void setZoom(double value){
    	zoom = value;
    	driver.ptzDataInterface.setZoom(value);
    }


	public void stop()
	{
		// TODO Auto-generated method stub
		
	}

}
