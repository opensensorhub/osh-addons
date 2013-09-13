/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;

import java.util.List;
import java.util.UUID;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.SensorException;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataType;
import org.vast.data.ConstraintList;
import org.vast.data.DataGroup;
import org.vast.data.DataValue;
import org.vast.sweCommon.EnumNumberConstraint;
import org.vast.sweCommon.EnumTokenConstraint;
import org.vast.sweCommon.IntervalConstraint;
import org.vast.sweCommon.SweConstants;
import org.vast.util.DateTime;
import au.edu.jcu.v4l4j.DeviceInfo;
import au.edu.jcu.v4l4j.FrameInterval;
import au.edu.jcu.v4l4j.FrameInterval.DiscreteInterval;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.ResolutionInfo;
import au.edu.jcu.v4l4j.ResolutionInfo.DiscreteResolution;


/**
 * <p>
 * Implementation of control interface for V4L sensor
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class V4LCameraControl implements ISensorControlInterface
{
    private static String ERROR_ASYNC = "Asynchronous commands are not supported by the V4LCamera driver";
    
    V4LCameraDriver driver;
    V4LCameraParams camParams;
    DataComponent commandData;
    
    
    protected V4LCameraControl(V4LCameraDriver driver)
    {
        this.driver = driver;
    }
    
    
    protected void init(V4LCameraParams camParams, DeviceInfo deviceInfo)
    {
        this.camParams = camParams;
        
        // build command message structure from V4L info
        this.commandData = new DataGroup(2, "camParams");
        ConstraintList constraints;
        
        // choice of image format
        List<ImageFormat> v4lImgFormats = deviceInfo.getFormatList().getRGBEncodableFormats();//.getNativeFormats();
        String[] formatList = new String[v4lImgFormats.size()];
        for (int i=0; i<v4lImgFormats.size(); i++)
            formatList[i] = v4lImgFormats.get(i).getName();
        
        DataValue formatVal = new DataValue("imageFormat", DataType.UTF_STRING);
        constraints = new ConstraintList();
        constraints.add(new EnumTokenConstraint(formatList));
        formatVal.setConstraints(constraints);        
        commandData.addComponent(formatVal);
        
        // choice of resolutions
        ResolutionInfo v4lResInfo = v4lImgFormats.get(0).getResolutionInfo();
        if (v4lResInfo.getType() == ResolutionInfo.Type.DISCRETE)
        {
            List<DiscreteResolution> v4lResList = v4lResInfo.getDiscreteResolutions();
            String[] resList = new String[v4lResList.size()];
            for (int i=0; i<v4lResList.size(); i++)
            {
                String resText = v4lResList.get(i).width + "x" + v4lResList.get(i).height; 
                resList[i] = resText;
            }
            
            DataValue resVal = new DataValue("imageSize", DataType.UTF_STRING);
            constraints = new ConstraintList();
            constraints.add(new EnumTokenConstraint(resList));
            resVal.setConstraints(constraints); 
            commandData.addComponent(resVal);
        }
        else if (v4lResInfo.getType() == ResolutionInfo.Type.STEPWISE)
        {
            double minWidth = v4lResInfo.getStepwiseResolution().minWidth;
            double maxWidth = v4lResInfo.getStepwiseResolution().maxWidth;
            double minHeight = v4lResInfo.getStepwiseResolution().minHeight;
            double maxHeight = v4lResInfo.getStepwiseResolution().maxHeight;
            
            DataValue widthVal = new DataValue("imageWidth", DataType.INT);
            constraints = new ConstraintList();
            constraints.add(new IntervalConstraint(minWidth, maxWidth));
            widthVal.setConstraints(constraints); 
            commandData.addComponent(widthVal);
            
            DataValue heightVal = new DataValue("imageHeight", DataType.INT);
            constraints = new ConstraintList();
            constraints.add(new IntervalConstraint(minHeight, maxHeight));
            widthVal.setConstraints(constraints); 
            commandData.addComponent(heightVal);
        }
        
        // choice of frame rate
        FrameInterval v4lFrameIntervals = null;
        if (v4lResInfo.getType() == ResolutionInfo.Type.DISCRETE)
            v4lFrameIntervals = v4lResInfo.getDiscreteResolutions().get(0).interval;
        else if (v4lResInfo.getType() == ResolutionInfo.Type.STEPWISE)
            v4lFrameIntervals = v4lResInfo.getStepwiseResolution().getMinResFrameInterval();
        
        if (v4lFrameIntervals != null)
        {
            if (v4lFrameIntervals.getType() == FrameInterval.Type.DISCRETE)
            {
                List<DiscreteInterval> v4lIntervalList = v4lFrameIntervals.getDiscreteIntervals();
                double[] rateList = new double[v4lIntervalList.size()];
                for (int i=0; i<v4lIntervalList.size(); i++)
                    rateList[i] = v4lIntervalList.get(i).denominator / v4lIntervalList.get(i).numerator;
                
                DataValue resVal = new DataValue("frameRate", DataType.INT);
                constraints = new ConstraintList();
                constraints.add(new EnumNumberConstraint(rateList));
                resVal.setConstraints(constraints);
                resVal.setProperty(SweConstants.UOM_CODE, "Hz");
                commandData.addComponent(resVal);
            }
            else if (v4lFrameIntervals.getType() == FrameInterval.Type.STEPWISE)
            {
                DiscreteInterval minInterval = v4lFrameIntervals.getStepwiseInterval().minIntv;
                DiscreteInterval maxInterval = v4lFrameIntervals.getStepwiseInterval().maxIntv;
                double minRate = (double)minInterval.denominator / (double)minInterval.numerator;
                double maxRate = (double)maxInterval.denominator / (double)maxInterval.numerator;
                
                DataValue widthVal = new DataValue("frameRate", DataType.FLOAT);
                constraints = new ConstraintList();
                constraints.add(new IntervalConstraint(minRate, maxRate));
                widthVal.setConstraints(constraints); 
                commandData.addComponent(widthVal);
            }
        }
    }
    
    
    @Override
    public boolean isAsyncExecSupported()
    {
        return false;
    }


    @Override
    public boolean isSchedulingSupported()
    {
        return false;
    }


    @Override
    public boolean isStatusHistorySupported()
    {
        return false;
    }


    @Override
    public DataComponent getCommandDescription()
    {
        return commandData;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        CommandStatus cmdStatus = new CommandStatus();
        cmdStatus.status = StatusCode.COMPLETED;
        
        // associate command data to msg structure definition
        DataComponent commandMsg = commandData.copy();
        commandMsg.setData(command);
        
        // parse command (TODO should we assume it has already been validated?)        
        // image format
        camParams.imgFormat = commandMsg.getComponent("imageFormat").getData().getStringValue();
        
        // image width and height
        DataValue imgSize = (DataValue)commandMsg.getComponent("imageSize");
        if (imgSize != null)
        {
            String resText = imgSize.getData().getStringValue();
            String[] tokens = resText.split("x");
            camParams.imgWidth = Integer.parseInt(tokens[0]);
            camParams.imgHeight = Integer.parseInt(tokens[1]);
        }
        else
        {
            camParams.imgWidth = commandMsg.getComponent("imageWidth").getData().getIntValue();
            camParams.imgHeight = commandMsg.getComponent("imageHeight").getData().getIntValue();
        }
        
        // frame rate
        camParams.frameRate = commandMsg.getComponent("frameRate").getData().getIntValue();
        
        // update driver with new params        
        driver.updateParams(camParams);
        
        return cmdStatus;
    }


    @Override
    public CommandStatus execCommandGroup(List<DataBlock> commands) throws SensorException
    {
        CommandStatus groupStatus = new CommandStatus();
        groupStatus.id = UUID.randomUUID().toString();
        groupStatus.status = StatusCode.COMPLETED;
        
        // if any of the commands fail, return fail status with
        // error message and don't process more commands
        for (DataBlock cmd: commands)
        {
            CommandStatus cmdStatus = execCommand(cmd);
            if (cmdStatus.status == StatusCode.REJECTED || cmdStatus.status == StatusCode.FAILED)
            {
                groupStatus.status = cmdStatus.status;
                groupStatus.message = cmdStatus.message;
                break;
            }          
        }
        
        return groupStatus;
    }


    @Override
    public CommandStatus sendCommand(DataBlock command) throws SensorException
    {
        throw new SensorException(ERROR_ASYNC);
    }


    @Override
    public CommandStatus sendCommandGroup(List<DataBlock> commands) throws SensorException
    {
        throw new SensorException(ERROR_ASYNC);
    }


    @Override
    public CommandStatus scheduleCommand(DataBlock command, DateTime execTime) throws SensorException
    {
        throw new SensorException(ERROR_ASYNC);
    }


    @Override
    public CommandStatus scheduleCommandGroup(List<DataBlock> commands, DateTime execTime) throws SensorException
    {
        throw new SensorException(ERROR_ASYNC);
    }


    @Override
    public CommandStatus cancelCommand(String commandID) throws SensorException
    {
        throw new SensorException(ERROR_ASYNC);
    }


    @Override
    public CommandStatus getCommandStatus(String commandID) throws SensorException
    {
        throw new SensorException(ERROR_ASYNC);
    }


    @Override
    public List<CommandStatus> getCommandStatusHistory(String commandID) throws SensorException
    {
        throw new SensorException(ERROR_ASYNC);
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // do nothing since we don't deal with async commands
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        // do nothing since we don't deal with async commands
    }

}
