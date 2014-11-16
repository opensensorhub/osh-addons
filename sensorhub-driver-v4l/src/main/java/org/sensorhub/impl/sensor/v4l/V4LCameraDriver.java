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

import net.opengis.sensorml.v20.AbstractProcess;
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
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class V4LCameraDriver extends AbstractSensorModule<V4LCameraConfig>
{
    V4LCameraParams camParams;
    VideoDevice videoDevice;
    DeviceInfo deviceInfo;
    V4LCameraOutput dataInterface;
    V4LCameraControl controlInterface;
    
    
    public V4LCameraDriver()
    {
        this.dataInterface = new V4LCameraOutput(this);
        obsOutputs.put("camOutput", dataInterface);
        
        this.controlInterface = new V4LCameraControl(this);
        controlInputs.put("camParams", controlInterface);
    }


    @Override
    public void updateConfig(V4LCameraConfig config) throws SensorHubException
    {
        // cleanup previously used device and restart
        stop();
        init(config);
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
        controlInterface.init();
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
    public AbstractProcess getCurrentSensorDescription() throws SensorException
    {
        AbstractProcess sensorDesc = super.getCurrentSensorDescription();
        sensorDesc.setDescription("Video4Linux camera on port " + videoDevice.getDevicefile());
        return sensorDesc;
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
