/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.DeviceInfo;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * <p>
 * Common base for all video outputs (different codecs)
 * </p>
 *
 * @author Alex Robin
 * @since Apr 23, 2016
 */
public abstract class V4LCameraOutput extends AbstractSensorOutput<V4LCameraDriver> implements CaptureCallback
{
    DataStream dataStream;
    FrameGrabber frameGrabber;
    long systemTimeOffset = -1L;
    AtomicBoolean processingFrame = new AtomicBoolean();
    boolean started, firstFrame;
    
    
    public V4LCameraOutput(String name, V4LCameraDriver parentSensor)
    {
        super(name, parentSensor);
    }
    
    
    protected abstract void init(DeviceInfo deviceInfo) throws SensorException;
    
    protected abstract void initFrameGrabber(V4LCameraParams camParams) throws V4L4JException;
    
    protected abstract void processFrame(VideoFrame frame);
    
    
    protected void start() throws SensorException
    {
        try
        {
            initFrameGrabber(parentSensor.camParams);            
            frameGrabber.setCaptureCallback(this);
            processingFrame.set(false);
            started = false;
            firstFrame = true;
            frameGrabber.startCapture();
            parentSensor.getLogger().debug("V4L frame capture started");
        }
        catch (V4L4JException e)
        {
            throw new SensorException("Could not start V4L frame grabber", e);
        }
    }

    
    protected void stop()
    {
        if (frameGrabber != null)
        {
            if (started)
                frameGrabber.stopCapture();
            parentSensor.getLogger().debug("V4L capture stopped");
            
            parentSensor.videoDevice.releaseFrameGrabber();
            parentSensor.videoDevice.releaseControlList();            
            frameGrabber = null;
        }
    }
    
    
    @Override
    public void exceptionReceived(V4L4JException e)
    {
        getLogger().error("Error while streaming V4L video", e);
    }
    
    
    @Override
    public void nextFrame(VideoFrame frame)
    {
        if (processingFrame.compareAndSet(false, true))
        {
            try
            {
                // discard first frame because capture time is wrong
                if (firstFrame)
                    firstFrame = false;
                else            
                    processFrame(frame);
                
                frame.recycle();                
            }
            catch (Exception e)
            {
                getLogger().error("Error decoding frame, ts=" + frame.getCaptureTime());
            }
            finally
            {
                processingFrame.set(false);
            }
        }
        
        // else skip frame if we're lagging behind
        else
        {
            parentSensor.getLogger().debug("Frame skipped, ts=" + frame.getCaptureTime());
        }
    }
        
    
    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataStream.getEncoding();
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return parentSensor.camParams.frameRate;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataStream.getElementType();
    }
    
    
    protected final double getJulianTimeStamp(long v4lTimeStampMicros)
    {
        long sensorTimeMillis = v4lTimeStampMicros / 1000;
        
        if (systemTimeOffset < 0)
            systemTimeOffset = System.currentTimeMillis() - sensorTimeMillis;
            
        return (systemTimeOffset + sensorTimeMillis) / 1000.;
    }

}