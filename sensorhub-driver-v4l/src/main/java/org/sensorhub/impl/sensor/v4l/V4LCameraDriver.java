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
        // preload libvideo so it is extracted from JAR
        System.loadLibrary("video");
    }
    
    public V4LCameraDriver()
    {
        
    }
    
    
    @Override
    public void init(V4LCameraConfig config) throws SensorHubException
    {
        super.init(config);
        
        this.dataInterface = new V4LCameraOutput(this);
        this.controlInterface = new V4LCameraControl(this);
    }


    @Override
    public void updateConfig(V4LCameraConfig config) throws SensorHubException
    {
        // cleanup previously used device
        stop();
        init(config);
        
        // restart if enabled
        if (config.enabled)
            start();
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
        
        // init data and control interfaces
        dataInterface.init();
        addOutput(dataInterface, false);
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
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            if (AbstractSensorModule.DEFAULT_ID.equals(sensorDescription.getId()))
                sensorDescription.setId("V4L_CAMERA");
            
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Video4Linux camera on port " + videoDevice.getDevicefile());
        }
    }


    @Override
    public boolean isConnected()
    {
        try
        {
            new VideoDevice(config.deviceName);            
        }
        catch (V4L4JException e)
        {
            return false;
        }
        
        return true;
    }
    

    @Override
    public void cleanup()
    {
        
    }
    
    
    @Override
    public void finalize()
    {
        stop();
    }
}
