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
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.DataBlockMixed;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * <p>
 * Implementation of video output for V4L sensor
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class V4LCameraOutputMJPEG extends V4LCameraOutput implements CaptureCallback
{
    ImageFormat imgFormat;
    boolean firstFrame;
    
    
    protected V4LCameraOutputMJPEG(V4LCameraDriver driver, ImageFormat imgFormat)
    {
        super(driver);
        this.imgFormat = imgFormat;
    }
    
    
    @Override
    public String getName()
    {
        return "camOutput_MJPEG";
    }
    
    
    @Override
    protected void init() throws SensorException
    {
        V4LCameraParams camParams = parentSensor.camParams;
        
        // init frame grabber
        try
        {
            frameGrabber = parentSensor.videoDevice.getRawFrameGrabber(camParams.imgWidth, camParams.imgHeight, 0, V4L4JConstants.STANDARD_WEBCAM, imgFormat);
            //frameGrabber = parentSensor.videoDevice.getJPEGFrameGrabber(camParams.imgWidth, camParams.imgHeight, 0, V4L4JConstants.STANDARD_WEBCAM, 80);
            //frameGrabber.setFrameInterval(1, camParams.frameRate);
            parentSensor.getLogger().debug("V4L frame grabber created");
            
            // adjust params to what was actually set up by V4L
            camParams.imgWidth = frameGrabber.getWidth();
            camParams.imgHeight = frameGrabber.getHeight();
            camParams.frameRate = frameGrabber.getFrameInterval().denominator / frameGrabber.getFrameInterval().numerator;
            camParams.imgFormat = frameGrabber.getImageFormat().getName();
            
            // create SWE output structure
            VideoCamHelper fac = new VideoCamHelper();
            dataStream = fac.newVideoOutputMJPEG(getName(), camParams.imgWidth, camParams.imgHeight);
            
            // start capture
            if (camParams.doCapture)
            {
                parentSensor.getLogger().debug("Starting V4L capture");
                frameGrabber.setCaptureCallback(this);
                processingFrame = false;
                firstFrame = true;
                frameGrabber.startCapture();
                parentSensor.getLogger().debug("V4L capture started");
            }            
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
            // discard first frame because capture time is wrong
            if (firstFrame)
            {
                frame.recycle();
                firstFrame = false;
                return;
            }
            
            // skip frame if we're lagging behind
            if (processingFrame)
            {
                parentSensor.getLogger().debug("Frame skipped @ " + frame.getCaptureTime());
                return;
            }
            
            processingFrame = true;
            DataBlock dataBlock;
            if (latestRecord == null)
                dataBlock = dataStream.getElementType().createDataBlock();
            else
                dataBlock = latestRecord.renew();
            
            // time stamp
            dataBlock.setDoubleValue(getJulianTimeStamp(frame.getCaptureTime()));
            
            // either compressed or RGB data
            byte[] frameData = new byte[frame.getFrameLength()];
            System.arraycopy(frame.getBytes(), 0, frameData, 0, frameData.length);
            ((DataBlockMixed)dataBlock).getUnderlyingObject()[1].setUnderlyingObject(frameData);
            
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
        finally
        {
            processingFrame = false;
        }
    }
}
