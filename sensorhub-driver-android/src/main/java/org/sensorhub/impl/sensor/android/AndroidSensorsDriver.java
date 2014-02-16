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

package org.sensorhub.impl.sensor.android;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.sensorML.system.SMLSystem;
import org.vast.util.DateTime;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;


public class AndroidSensorsDriver implements ISensorInterface<AndroidSensorsConfig>
{
    AndroidSensorsConfig config;
    IEventHandler eventHandler;
    SensorManager sensorManager;
    Map<String, ISensorDataInterface> dataInterfaces;
    Map<String, ISensorControlInterface> controlInterfaces;
    
    
    public AndroidSensorsDriver(Context androidContext)
    {
        this.eventHandler = new BasicEventHandler();
        this.sensorManager = (SensorManager)androidContext.getSystemService(Context.SENSOR_SERVICE);
        
        // create one data interface per available sensor
        dataInterfaces = new LinkedHashMap<String, ISensorDataInterface>();
        controlInterfaces = new LinkedHashMap<String, ISensorControlInterface>();
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor: deviceSensors)
        {
            dataInterfaces.put(sensor.getName(), new AndroidSensorOutput(sensorManager, sensor));
            controlInterfaces.put(sensor.getName(), new AndroidSensorControl(sensorManager, sensor));
        }
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
    
    
    @Override
    public void init(AndroidSensorsConfig config) throws SensorHubException
    {
        this.config = config;
    }


    @Override
    public void updateConfig(AndroidSensorsConfig config) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void start() throws StorageException
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void stop() throws StorageException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public AndroidSensorsConfig getConfiguration()
    {
        return this.config;
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
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean isSensorDescriptionHistorySupported()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public SMLSystem getCurrentSensorDescription() throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public SMLSystem getSensorDescription(DateTime t) throws SensorException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void updateSensorDescription(SMLSystem systemDesc, boolean recordHistory) throws SensorException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public Map<String, ISensorDataInterface> getAllOutputs() throws SensorException
    {
        return dataInterfaces;
    }


    @Override
    public Map<String, ISensorDataInterface> getStatusOutputs() throws SensorException
    {
        return new HashMap<String, ISensorDataInterface>();
    }


    @Override
    public Map<String, ISensorDataInterface> getObservationOutputs() throws SensorException
    {
        return dataInterfaces;
    }


    @Override
    public Map<String, ISensorControlInterface> getCommandInputs() throws SensorException
    {
        return controlInterfaces;
    }


    @Override
    public boolean isConnected()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public void saveState(IModuleStateSaver saver) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO deactivate sensors        
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
