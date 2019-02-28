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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.DataBlockByte;
import au.edu.jcu.v4l4j.CaptureCallback;
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
public class V4LCameraOutputRGB extends V4LCameraOutput implements CaptureCallback
{
    DataComponent camDataStruct;
    
    
    protected V4LCameraOutputRGB(V4LCameraDriver driver)
    {
        super(driver);
    }
    
    
    @Override
    public String getName()
    {
        return "camOutput_RGB";
    }
    
    
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
            
            // create SWE output structure
            VideoCamHelper fac = new VideoCamHelper();
            dataStream = fac.newVideoOutputRGB(getName(), camParams.imgWidth, camParams.imgHeight);
            
            // start capture
            frameGrabber.setCaptureCallback(this);
            if (camParams.doCapture)
                frameGrabber.startCapture();
        }
        catch (V4L4JException e)
        {
            throw new SensorException("Error while initializing frame grabber", e);
        }        
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
            DataBlock dataBlock;
            if (latestRecord == null)
                dataBlock = camDataStruct.createDataBlock();
            else
                dataBlock = latestRecord.renew();
            ((DataBlockByte)dataBlock).setUnderlyingObject(frame.getBytes());
            
            // update latest record and send event
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, dataBlock));
            
            frame.recycle();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }    
    }
}
