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

import java.nio.ByteOrder;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.BinaryComponentImpl;
import org.vast.data.BinaryEncodingImpl;
import org.vast.data.CountImpl;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockByte;
import org.vast.data.DataRecordImpl;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.RGBFrameGrabber;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * <p>
 * Implementation of data interface for V4L sensor
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class V4LCameraOutput extends AbstractSensorOutput<V4LCameraDriver> implements CaptureCallback
{
    RGBFrameGrabber frameGrabber;
    DataComponent camDataStruct;
    
    
    protected V4LCameraOutput(V4LCameraDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return "camOutput";
    }
    
    
    @Override
    protected void init() throws SensorException
    {
        V4LCameraParams camParams = parentSensor.camParams;
        
        // init frame grabber
        try
        {
            frameGrabber = parentSensor.videoDevice.getRGBFrameGrabber(camParams.imgWidth, camParams.imgHeight, 0, V4L4JConstants.STANDARD_WEBCAM);
            //frameGrabber.setFrameInterval(1, camParams.frameRate);
            
            // adjust params to what was actually set up by V4L
            camParams.imgWidth = frameGrabber.getWidth();
            camParams.imgHeight = frameGrabber.getHeight();
            camParams.frameRate = frameGrabber.getFrameInterval().denominator / frameGrabber.getFrameInterval().numerator;
            camParams.imgFormat = frameGrabber.getImageFormat().getName();
            
            frameGrabber.setCaptureCallback(this);
            if (camParams.doCapture)
                frameGrabber.startCapture();
        }
        catch (V4L4JException e)
        {
            throw new SensorException("Error while initializing frame grabber", e);
        }
        
        // build output structure
        camDataStruct = new DataArrayImpl(camParams.imgHeight);
        camDataStruct.setName(getName());
        camDataStruct.setDefinition("http://sensorml.com/ont/swe/property/VideoFrame");
        DataArray imgRow = new DataArrayImpl(camParams.imgWidth);
        ((DataArray)camDataStruct).addComponent("row", imgRow);        
        DataRecord imgPixel = new DataRecordImpl(3);
        imgPixel.addComponent("red", new CountImpl(DataType.BYTE));
        imgPixel.addComponent("green", new CountImpl(DataType.BYTE));
        imgPixel.addComponent("blue", new CountImpl(DataType.BYTE));
        imgRow.addComponent("pixel", imgPixel);
    }
    
    
    @Override
    public void exceptionReceived(V4L4JException e)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public void nextFrame(VideoFrame frame)
    {
        try
        {
            //double samplingTime = frame.getCaptureTime() / 1000.;
            DataBlock camData = camDataStruct.createDataBlock();
            ((DataBlockByte)camData).setUnderlyingObject(frame.getBytes());
            
            // update latest record and send event
            latestRecord = camData;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, camData));
            
            frame.recycle();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }    
    }
    
    
    @Override
    public DataEncoding getRecommendedEncoding()
    {
        BinaryEncoding dataEnc = new BinaryEncodingImpl();
        dataEnc.setByteEncoding(ByteEncoding.RAW);
        dataEnc.setByteOrder(ByteOrder.BIG_ENDIAN);
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("row/pixel/red", DataType.BYTE));
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("row/pixel/green", DataType.BYTE));
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("row/pixel/blue", DataType.BYTE));
        return dataEnc;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return parentSensor.camParams.frameRate;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return camDataStruct;
    }
    
    
    @Override
    protected void stop()
    {
        if (frameGrabber != null)
        {
            if (parentSensor.camParams.doCapture)
                frameGrabber.stopCapture();
            parentSensor.videoDevice.releaseFrameGrabber();
            frameGrabber = null;
        }
    }

}
