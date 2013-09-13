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
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataType;
import org.vast.data.DataArray;
import org.vast.data.DataBlockByte;
import org.vast.data.DataGroup;
import org.vast.data.DataValue;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.RGBFrameGrabber;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * <p>
 * Implementation of data interface for V4L sensor
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class V4LCameraOutput implements ISensorDataInterface, CaptureCallback
{
    String sensorId;
    VideoDevice videoDevice;
    RGBFrameGrabber frameGrabber;
    V4LCameraParams camParams;
    IEventHandler eventHandler;
    DataComponent camDataStruct;
    
    
    protected V4LCameraOutput()
    {
        this.eventHandler = new BasicEventHandler();
    }
    
    
    protected void init(String sensorId, VideoDevice videoDevice, V4LCameraParams camParams) throws SensorException
    {
        this.sensorId = sensorId;
        this.videoDevice = videoDevice;
        this.camParams = camParams;
        
        // init frame grabber
        try
        {
            frameGrabber = videoDevice.getRGBFrameGrabber(camParams.imgWidth, camParams.imgHeight, 0, V4L4JConstants.STANDARD_WEBCAM);
            frameGrabber.setFrameInterval(1, camParams.frameRate);
            
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
        camDataStruct = new DataArray(camParams.imgHeight);
        camDataStruct.setName("videoFrame");
        DataArray imgRow = new DataArray(camParams.imgWidth);
        imgRow.setName("row");
        ((DataArray)camDataStruct).addComponent(imgRow);        
        DataGroup imgPixel = new DataGroup(3, "pixel");
        imgPixel.addComponent(new DataValue("red", DataType.BYTE));
        imgPixel.addComponent(new DataValue("green", DataType.BYTE));
        imgPixel.addComponent(new DataValue("blue", DataType.BYTE));
        imgRow.addComponent(imgPixel);
    }
    
    
    @Override
    public void exceptionReceived(V4L4JException e)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public void nextFrame(VideoFrame frame)
    {
        DataBlock camData = camDataStruct.createDataBlock();
        ((DataBlockByte)camData).setUnderlyingObject(frame.getBytes());
        frame.recycle();
        eventHandler.publishEvent(new SensorDataEvent(sensorId, camDataStruct, camData));    
    }
    
    
    @Override
    public boolean isStorageSupported()
    {
        return true;
    }


    @Override
    public boolean isPushSupported()
    {
        return true;
    }


    @Override
    public double getAverageSamplingRate()
    {
        return camParams.frameRate;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return camDataStruct;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getStorageCapacity()
    {
        return 0;
    }


    @Override
    public int getNumberOfAvailableRecords()
    {
        return 0;
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int clearAllRecords()
    {
        // TODO Auto-generated method stub
        return 0;
    }
    
    
    public void cleanup()
    {
        if (frameGrabber != null)
        {
            videoDevice.releaseFrameGrabber();
            frameGrabber = null;
        }
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);        
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);        
    }
}
