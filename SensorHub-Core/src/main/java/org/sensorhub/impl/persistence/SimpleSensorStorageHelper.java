/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.processing.IProcess;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.impl.common.AbstractSimpleProcess;
import org.vast.cdm.common.DataComponent;


/**
 * <p><b>Title:</b>
 * SimpleSensorStorageHelper
 * </p>
 *
 * <p><b>Description:</b><br/>
 * This class allows for connecting the output of a sensor to a storage instance for
 * recording all data.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Aug 30, 2013
 */
public class SimpleSensorStorageHelper extends AbstractSimpleProcess implements IProcess
{
    
    public SimpleSensorStorageHelper(String sensorID, List<String> outputNames)
    {
        // register to sensor data interface listener
        
        
        // setup storage (only activate if already created)
        
        // 
    }
    

    @Override
    public DataComponent getInputList()
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataComponent getOutputList()
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataComponent getParameterList()
    {
        // TODO Auto-generated method stub
        return null;
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
    public void updateConfig(ProcessConfig config)
    {
        // TODO Auto-generated method stub
        
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }

}
