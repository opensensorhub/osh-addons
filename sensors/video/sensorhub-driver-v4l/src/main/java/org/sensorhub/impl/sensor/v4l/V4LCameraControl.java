/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.AllowedValues;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import org.sensorhub.api.command.CommandAck;
import org.sensorhub.api.command.CommandException;
import org.sensorhub.api.command.ICommandAck;
import org.sensorhub.api.command.ICommandData;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.task.ITaskStatus;
import org.sensorhub.api.task.TaskStatus;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.DataValue;
import org.vast.data.SWEFactory;
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
 * @author Alex Robin
 * @since Sep 5, 2013
 */
public class V4LCameraControl extends AbstractSensorControl<V4LCameraDriver>
{
    V4LCameraParams camParams;
    DataComponent commandData;
    
    
    protected V4LCameraControl(V4LCameraDriver driver)
    {
        super("camParams", driver);
    }
    
    
    protected void init(DeviceInfo deviceInfo)
    {
        this.camParams = parentSensor.camParams;
        SWEFactory fac = new SWEFactory();
        
        // build command message structure from V4L info
        this.commandData = fac.newDataRecord();
        commandData.setName(getName());
        commandData.setUpdatable(true);
        AllowedTokens tokenConstraint;
        AllowedValues numConstraint;
        
        // choice of image format
        Category formatVal = fac.newCategory();
        tokenConstraint = fac.newAllowedTokens();
        List<ImageFormat> v4lImgFormats = deviceInfo.getFormatList().getNativeFormats();
        for (int i=0; i<v4lImgFormats.size(); i++)
            tokenConstraint.addValue(v4lImgFormats.get(i).getName());
        formatVal.setConstraint(tokenConstraint);
        commandData.addComponent("imageFormat", formatVal);
        
        // choice of resolutions (enum or range)
        ResolutionInfo v4lResInfo = v4lImgFormats.get(0).getResolutionInfo();
        if (v4lResInfo.getType() == ResolutionInfo.Type.DISCRETE)
        {
            Category resVal = fac.newCategory();       
            tokenConstraint = fac.newAllowedTokens();
            List<DiscreteResolution> v4lResList = v4lResInfo.getDiscreteResolutions();
            for (int i=0; i<v4lResList.size(); i++)
            {
                String resText = v4lResList.get(i).width + "x" + v4lResList.get(i).height; 
                tokenConstraint.addValue(resText);
            }
            resVal.setConstraint(tokenConstraint);
            commandData.addComponent("imageSize", resVal);
        }
        else if (v4lResInfo.getType() == ResolutionInfo.Type.STEPWISE)
        {
            double minWidth = v4lResInfo.getStepwiseResolution().minWidth;
            double maxWidth = v4lResInfo.getStepwiseResolution().maxWidth;
            double minHeight = v4lResInfo.getStepwiseResolution().minHeight;
            double maxHeight = v4lResInfo.getStepwiseResolution().maxHeight;
            
            Count widthVal = fac.newCount(DataType.INT);
            numConstraint = fac.newAllowedValues();
            numConstraint.addInterval(new double[] {minWidth, maxWidth});
            widthVal.setConstraint(numConstraint);
            commandData.addComponent("imageWidth", widthVal);
            
            Count heightVal = fac.newCount(DataType.INT);
            numConstraint = fac.newAllowedValues();
            numConstraint.addInterval(new double[] {minHeight, maxHeight});
            heightVal.setConstraint(numConstraint); 
            commandData.addComponent("imageHeight", heightVal);
        }
        
        // choice of frame rate (enum or range)
        FrameInterval v4lFrameIntervals = null;
        if (v4lResInfo.getType() == ResolutionInfo.Type.DISCRETE)
            v4lFrameIntervals = v4lResInfo.getDiscreteResolutions().get(0).getFrameInterval();
        else if (v4lResInfo.getType() == ResolutionInfo.Type.STEPWISE)
            v4lFrameIntervals = v4lResInfo.getStepwiseResolution().getMinResFrameInterval();
        
        if (v4lFrameIntervals != null)
        {
            Quantity rateVal = fac.newQuantity(DataType.FLOAT);
            rateVal.getUom().setCode("Hz");
            numConstraint = fac.newAllowedValues();
            
            if (v4lFrameIntervals.getType() == FrameInterval.Type.DISCRETE)
            {
                List<DiscreteInterval> v4lIntervalList = v4lFrameIntervals.getDiscreteIntervals();
                for (int i=0; i<v4lIntervalList.size(); i++)
                    numConstraint.addValue(v4lIntervalList.get(i).denominator / v4lIntervalList.get(i).numerator);                
            }
            else if (v4lFrameIntervals.getType() == FrameInterval.Type.STEPWISE)
            {
                DiscreteInterval minInterval = v4lFrameIntervals.getStepwiseInterval().minIntv;
                DiscreteInterval maxInterval = v4lFrameIntervals.getStepwiseInterval().maxIntv;
                double minRate = (double)minInterval.denominator / (double)minInterval.numerator;
                double maxRate = (double)maxInterval.denominator / (double)maxInterval.numerator;                
                numConstraint.addInterval(new double[] {minRate, maxRate});
            }
            
            rateVal.setConstraint(numConstraint);
            commandData.addComponent("frameRate", rateVal);
        }
    }


    @Override
    public DataComponent getCommandDescription()
    {
        return commandData;
    }


    @Override
    public CompletableFuture<Void> executeCommand(ICommandData command, Consumer<ICommandAck> callback)
    {
        // associate command data to msg structure definition
        DataComponent commandMsg = commandData.copy();
        commandMsg.setData(command.getParams());
        
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
        try
        {
            parentSensor.updateParams(camParams);
            callback.accept(CommandAck.success(command));
            return CompletableFuture.completedFuture(null);
        }
        catch (SensorException e)
        {
            callback.accept(CommandAck.fail(command));
            return CompletableFuture.failedFuture(e);
        }
    }


    @Override
    public void validateCommand(ICommandData command) throws CommandException
    {
        // TODO Auto-generated method stub
        
    }


    public void stop()
    {
        
    }

}
