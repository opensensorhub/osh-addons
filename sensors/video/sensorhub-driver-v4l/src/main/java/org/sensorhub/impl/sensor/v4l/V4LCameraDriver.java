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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import au.edu.jcu.v4l4j.DeviceInfo;
import au.edu.jcu.v4l4j.ImageFormat;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * <p>
 * Generic driver implementation for most camera compatible with Video4Linux.
 * This implementation makes use of the V4L4J library to connect to the V4L
 * native layer via libv4l4j and libvideo.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class V4LCameraDriver extends AbstractSensorModule<V4LCameraConfig>
{
    V4LCameraParams camParams;
    VideoDevice videoDevice;
    DeviceInfo deviceInfo;
    V4LCameraOutput dataInterface;
    V4LCameraControl controlInterface;
    
    
    static
    {
        try
        {
            // preload libvideo so it is extracted from JAR
            System.loadLibrary("video");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    
    public V4LCameraDriver()
    {
        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate IDs
        generateUniqueID("urn:osh:sensor:v4l-cam:", config.serialNumber);
        generateXmlID("V4L_CAMERA_", config.serialNumber);
        
        // create data and control interfaces
        this.dataInterface = new V4LCameraOutputRGB(this);
        this.controlInterface = new V4LCameraControl(this);
    }
    
    
    @Override
    public void start() throws SensorException
    {
        this.camParams = config.defaultParams.clone();
        
        // init video device
        try
        {
            videoDevice = new VideoDevice(config.deviceName);
            deviceInfo = videoDevice.getDeviceInfo();
        }
        catch (V4L4JException e)
        {
            throw new SensorException("Cannot initialize video device " + config.deviceName, e);
        }
        
        // init video output
        for (ImageFormat fmt: deviceInfo.getFormatList().getNativeFormats())
        {
            if ("MJPEG".equals(fmt.getName()))
            {
                getLogger().debug("Creating MJPEG output");
                dataInterface = new V4LCameraOutputMJPEG(this, fmt);
            }
        }
        dataInterface.init();
        addOutput(dataInterface, false);
        
        // init control interfacess
        controlInterface.init();
        addControlInput(controlInterface);
    }
    
    
    @Override
    public void stop()
    {
        if (dataInterface != null)
            dataInterface.stop();
        
        if (controlInterface != null)
            controlInterface.stop();
        
        if (videoDevice != null)
        {
            videoDevice.release();
            videoDevice = null;
        }
    }
    
    
    public void updateParams(V4LCameraParams params) throws SensorException
    {
        // cleanup framegrabber and reinit sensor interfaces
        dataInterface.stop();
        dataInterface.init();
        controlInterface.init();
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Video4Linux camera on port " + videoDevice.getDevicefile());
        }
    }


    @Override
    public boolean isConnected()
    {
        return (videoDevice != null);
    }
    

    @Override
    public void cleanup()
    {
        
    }
}
