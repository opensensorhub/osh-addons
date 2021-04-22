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
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.vast.data.DataBlockMixed;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.DeviceInfo;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * <p>
 * Implementation of H264 video output for V4L sensor
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Oct 17, 2020
 */
public class V4LCameraOutputH264 extends V4LCameraOutput implements CaptureCallback
{
    ImageFormat imgFormat;
    
    
    protected V4LCameraOutputH264(V4LCameraDriver driver, ImageFormat imgFormat)
    {
        super("camOutput_H264", driver);
        this.imgFormat = imgFormat;
    }
    
    
    @Override
    protected void init(DeviceInfo deviceInfo) throws SensorException
    {
        V4LCameraParams camParams = parentSensor.camParams;
        
        // init frame grabber and output
        try
        {
            initFrameGrabber(camParams);
            
            // adjust params to what was actually set up by V4L
            camParams.imgWidth = frameGrabber.getWidth();
            camParams.imgHeight = frameGrabber.getHeight();
            camParams.frameRate = frameGrabber.getFrameInterval().denominator / frameGrabber.getFrameInterval().numerator;
            camParams.imgFormat = frameGrabber.getImageFormat().getName();
            
            // create SWE output structure
            VideoCamHelper fac = new VideoCamHelper();
            dataStream = fac.newVideoOutputH264(getName(), camParams.imgWidth, camParams.imgHeight);
        }
        catch (V4L4JException e)
        {
            throw new SensorException("Error while initializing frame grabber", e);
        }
    }
    
    
    protected void initFrameGrabber(V4LCameraParams camParams) throws V4L4JException
    {        
        if (frameGrabber == null)
        {
            frameGrabber = parentSensor.videoDevice.getRawFrameGrabber(camParams.imgWidth, camParams.imgHeight, 0, V4L4JConstants.STANDARD_WEBCAM, imgFormat);
            //frameGrabber = parentSensor.videoDevice.getJPEGFrameGrabber(camParams.imgWidth, camParams.imgHeight, 0, V4L4JConstants.STANDARD_WEBCAM, 80);
            //frameGrabber.setFrameInterval(1, camParams.frameRate);
        }
    }


    @Override
    public void processFrame(VideoFrame frame)
    {
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
        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }
}
