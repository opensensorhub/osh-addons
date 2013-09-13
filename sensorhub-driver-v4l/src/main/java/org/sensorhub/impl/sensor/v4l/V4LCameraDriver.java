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

import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.sensorML.system.SMLSystem;
import org.vast.util.DateTime;
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
public class V4LCameraDriver implements ISensorInterface<V4LCameraConfig>
{
    IEventHandler eventHandler;
    V4LCameraConfig config;
    V4LCameraParams camParams;
    VideoDevice videoDevice;
    DeviceInfo deviceInfo;
    V4LCameraOutput dataInterface;
    V4LCameraControl controlInterface;
    
    
    public V4LCameraDriver()
    {
        this.eventHandler = new BasicEventHandler();
        this.dataInterface = new V4LCameraOutput();
        this.controlInterface = new V4LCameraControl(this);
    }


    @Override
    public void init(V4LCameraConfig config) throws SensorException
    {
        this.config = config;
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
        dataInterface.init(config.id, videoDevice, camParams);
        controlInterface.init(camParams, deviceInfo);
    }


    @Override
    public void updateConfig(V4LCameraConfig config) throws SensorHubException
    {
        // cleanup previously used device adn reinit
        cleanup();
        init(config);
    }
    
    
    public void updateParams(V4LCameraParams params) throws SensorException
    {
        // cleanup framegrabber and reinit sensor interfaces
        dataInterface.cleanup();
        dataInterface.init(config.id, videoDevice, params);
        controlInterface.init(params, deviceInfo);
    }


    @Override
    public V4LCameraConfig getConfiguration()
    {
        return config;
    }


    @Override
    public String getName()
    {
        return config.name;
    }
    
    
    @Override
    public String getLocalID()
    {
        return config.id;
    }


    @Override
    public boolean isSensorDescriptionUpdateSupported()
    {
        return false;
    }
    
    
    @Override
    public boolean isSensorDescriptionHistorySupported()
    {
        return false;
    }
    
    
    @Override
    public SMLSystem getCurrentSensorDescription()
    {
        // TODO build SML description
        return new SMLSystem();
    }


    @Override
    public SMLSystem getSensorDescription(DateTime t) throws SensorException
    {
        throw new SensorException("History of sensor description is not supported by the V4LCamera driver");
    }


    @Override
    public void updateSensorDescription(SMLSystem systemDesc, boolean recordHistory) throws SensorException
    {
        throw new SensorException("Update of sensor description is not supported by the V4LCamera driver");
    }


    @Override
    public Map<String, ISensorDataInterface> getAllOutputs()
    {
        return getObservationOutputs();
    }


    @Override
    public Map<String, ISensorDataInterface> getStatusOutputs()
    {
        // return empty map = no status outputs
        return new HashMap<String, ISensorDataInterface>();
    }


    @Override
    public Map<String, ISensorDataInterface> getObservationOutputs()
    {
        Map<String, ISensorDataInterface> outputs = new HashMap<String, ISensorDataInterface>();
        outputs.put("camOutput", dataInterface);
        return outputs;
    }


    @Override
    public Map<String, ISensorControlInterface> getCommandInputs()
    {
        Map<String, ISensorControlInterface> commandInputs = new HashMap<String, ISensorControlInterface>();
        commandInputs.put("camParams", controlInterface);        
        return commandInputs;
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
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);        
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }
    
    
    @Override
    public void saveState(IModuleStateSaver saver)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void loadState(IModuleStateLoader loader)
    {
        // TODO Auto-generated method stub
        
    }
    
    
    @Override
    public void cleanup()
    {
        if (videoDevice != null)
        {
            videoDevice.releaseFrameGrabber();
            videoDevice.release();
            videoDevice = null;
        }
    }
    
    
    @Override
    public void finalize()
    {
        cleanup();
    }
}
