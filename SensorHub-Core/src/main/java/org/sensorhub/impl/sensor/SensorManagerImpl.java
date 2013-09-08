/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.ISensorManager;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p><b>Title:</b>
 * SensorMannagerImpl
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Default implementation of the sensor manager interface
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class SensorManagerImpl implements ISensorManager
{

    public SensorManagerImpl()
    {
        
    }
    
    
    @Override
    public List<ISensorInterface<?>> getLoadedModules()
    {
        List<ISensorInterface<?>> enabledSensors = new ArrayList<ISensorInterface<?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: ModuleRegistry.getInstance().getLoadedModules())
        {
            if (module instanceof ISensorInterface)
                enabledSensors.add((ISensorInterface<?>)module);
        }
        
        return enabledSensors;
    }


    @Override
    public List<ModuleConfig> getAvailableModules()
    {
        List<ModuleConfig> configuredSensors = new ArrayList<ModuleConfig>();
        
        // retrieve all modules implementing ISensorInterface
        for (ModuleConfig config: ModuleRegistry.getInstance().getAvailableModules())
        {
            try
            {
                if (Class.forName(config.moduleClass).isInstance(ISensorInterface.class))
                    configuredSensors.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return configuredSensors;
    }


    @Override
    public ISensorInterface<?> getModuleById(String moduleID)
    {
        IModule<?> module = ModuleRegistry.getInstance().getModuleById(moduleID);
        
        if (module instanceof ISensorInterface<?>)
            return (ISensorInterface<?>)module;
        else
            return null;
    }


    @Override
    public ISensorInterface<?> findSensor(String uid)
    {
        List<ISensorInterface<?>> enabledSensors = getLoadedModules();
        for (ISensorInterface<?> sensor: enabledSensors)
        {
            try
            {
                if (uid.equals(sensor.getCurrentSensorDescription().getUniqueID()))
                    return sensor;
            }
            catch (SensorException e)
            {
            }
        }
        
        return null;
    }


    @Override
    public List<ISensorInterface<?>> getConnectedSensors()
    {
        List<ISensorInterface<?>> connectedSensors = new ArrayList<ISensorInterface<?>>();
        
        // scan module list
        for (IModule<?> module: ModuleRegistry.getInstance().getLoadedModules())
        {
            if (module instanceof ISensorInterface && ((ISensorInterface<?>)module).isConnected())
                connectedSensors.add((ISensorInterface<?>)module);
        }
        
        return connectedSensors;
    }


    @Override
    public String installDriver(String driverPackageURL, boolean replace)
    {
        // TODO Auto-generated method stub
        // TODO need to implement generic module software loading in ModuleRegistry
        //  + dynamic classloader handling new uploaded jars (scanning directory at startup)
        return null;
    }


    @Override
    public void uninstallDriver(String driverID)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public List<IModuleProvider> getInstalledSensorDrivers()
    {
        List<IModuleProvider> installedModules = new ArrayList<IModuleProvider>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModuleProvider modType: ModuleRegistry.getInstance().getInstalledModuleTypes())
        {
            try
            {
                if (modType.getModuleClass().isInstance(ISensorInterface.class))
                    installedModules.add(modType);
            }
            catch (Exception e)
            {
            }
        }
        
        return installedModules;
    }

}
