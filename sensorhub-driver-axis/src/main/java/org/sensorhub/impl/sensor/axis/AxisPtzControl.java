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

import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;

import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.DataChoiceImpl;
import org.vast.data.SWEFactory;
import org.vast.sweCommon.SWEConstants;

/**
 * <p>
 * Implementation of sensor interface for generic Axis Cameras using IP
 * protocol. This particular class provides control of the Pan-Tilt-Zoom
 * (PTZ) capabilities.
 * </p>
 *
 * <p>
 * Copyright (c) 2014
 * </p>
 * 
 * @author Mike Botts <mike.botts@botts-inc.com>
 * @since October 30, 2014
 */



public class AxisPtzControl extends AbstractSensorControl<AxisCameraDriver>
{
	DataChoice commandData; 
		
	String ipAddress;

    // define and set default values
    double minPan = -180.0;
    double maxPan = 180.0;
    double minTilt = -180.0;
    double maxTilt = 0.0;
    double maxZoom = 9999;

    
    
    protected AxisPtzControl(AxisCameraDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return "ptzControl";
    }
    
    
    protected void init()
    {
    	
        ipAddress = parentSensor.getConfiguration().ipAddress;

        // NOTE: command are individual and supported using DataChoice
   	
        // build command message structure
        // PTZ Command Options will consist of DataChoice with items:
        // pan, tilt, zoom, relPan, relTilt, relZoom, presetPos (?)

        
        SWEFactory fac = new SWEFactory();
        this.commandData = fac.newDataChoice();
        
         this.commandData.setName(getName());
        
        // get PTZ limits
        try
        {    	         
            URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=PTZ.Limit");
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


        AllowedValues constraints;
        
        // Pan
        Quantity q = fac.newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition("http://sensorml.com/ont/swe/property/Pan");
        constraints = fac.newAllowedValues();
        constraints.addInterval(new double[] {minPan, maxPan});
        q.setConstraint(constraints);
        q.setLabel("Pan");
        commandData.addItem("pan",q);

        // Tilt
        q = fac.newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition("http://sensorml.com/ont/swe/property/Tilt");
        constraints = fac.newAllowedValues();
        constraints.addInterval(new double[] {minTilt, maxTilt});
        q.setConstraint(constraints);
        q.setLabel("Tilt");
        commandData.addItem("tilt", q);

        // Zoom Factor
        Count c = fac.newCount();
        c.setDefinition("http://sensorml.com/ont/swe/property/AxisZoomFactor");
        constraints = fac.newAllowedValues();
        constraints.addInterval(new double[] {1, maxZoom});
        c.setConstraint(constraints);
        c.setLabel("Zoom Factor");
        commandData.addItem("zoom", c);
         
        // Relative Pan
        q = fac.newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition("http://sensorml.com/ont/swe/property/relativePan");
        //constraints = fac.newAllowedValues();
        //constraints.addInterval(new double[] {minPan, maxPan});
        //relPan.setConstraint(constraints);
        q.setLabel("Relative Pan");
        commandData.addItem("rpan", q);

        // Relative Tilt
        q = fac.newQuantity(DataType.FLOAT);
        q.getUom().setCode("deg");
        q.setDefinition("http://sensorml.com/ont/swe/property/relativeTilt");
        //constraints = fac.newAllowedValues();
        //constraints.addInterval(new double[] {minTilt, maxTilt});
        //relTilt.setConstraint(constraints);
        q.setLabel("Relative Tilt");
        commandData.addItem("rtilt", q);

        // Relative Zoom
        c = fac.newCount();
        c.setDefinition("http://sensorml.com/ont/swe/property/relativeAxisZoomFactor");
        //constraints = fac.newAllowedValues();
        //constraints.addInterval(new double[] {0, maxZoom});
        //relZoom.setConstraint(constraints);
        c.setLabel("Relative Zoom Factor");
        commandData.addItem("rzoom", c);

        // PTZ preset positions
        Text preset = fac.newText();
        preset.setDefinition("http://sensorml.com/ont/swe/property/cameraPresetPositionName");
        preset.setLabel("Preset Camera Position");
        AllowedTokens presetNames = fac.newAllowedTokens();               
        // get preset position names from camera
        // e.g. root.PTZ.Preset.P0.Position.P1.Name=back door
        try
        {    	         
            URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/view/param.cgi?action=list&group=root.PTZ.Preset.P0.Position.*.Name");
            InputStream is = optionsURL.openStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(is));

            String line;
            
            while ((line = bReader.readLine()) != null)
            {
                String[] tokens = line.split("=");
                presetNames.addValue(tokens[1]);
            }
	    }
	    catch (Exception e)
	    {
	        e.printStackTrace();
	    }
        
        preset.setConstraint(presetNames);
        commandData.addItem("gotoserverpresetname",preset);
        
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
              
        DataComponent component = ((DataChoiceImpl) commandMsg).getSelectedComponent();
        String itemID = component.getName();
        String itemValue = component.getData().getStringValue();
        
        // NOTE: you can use validate() method in DataComponent
        // component.validateData(errorList);  // give it a list so it can store the errors
        // if (errorList != empty)  //then you have an error
        
        
        try
        {
        	         
            // set parameter value on camera 
            // NOTE: the item IDs are labeled the same as the Axis parameters so just use those in the command
        	
            URL optionsURL = new URL("http://" + ipAddress + "/axis-cgi/com/ptz.cgi?" + itemID + "=" + itemValue);
            InputStream is = optionsURL.openStream();
            
            
            // add BufferReader and read first line; if "Error", read second line and log error
            is.close();

	    }
	    catch (Exception e)
	    {
	    	
	        throw new SensorException("Error connecting to Axis PTZ control", e);
	    }
        
       
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;    
        
        return cmdStatus;
    }


	public void stop()
	{
		// TODO Auto-generated method stub
		
	}

}
