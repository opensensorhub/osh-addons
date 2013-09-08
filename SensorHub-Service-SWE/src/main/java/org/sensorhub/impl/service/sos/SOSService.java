/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.service.IServiceInterface;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.ISOSDataProviderFactory;
import org.vast.ows.sos.SOSServlet;


/**
 * <p><b>Title:</b>
 * SOSService
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Implementation of SensorHub generic SOS service.
 * This service is automatically configured (mostly) from information obtained
 * from the selected data sources (sensors, storages, processes, etc).
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
@SuppressWarnings("serial")
public class SOSService extends SOSServlet implements IServiceInterface<SOSServiceConfig>, ISOSDataProviderFactory
{
    SOSServiceConfig config;
    Map<String, SOSProviderConfig> dataProviderConfigs;
    

    @Override
    public void init(SOSServiceConfig config) throws SensorHubException
    {        
        dataProviderConfigs = new LinkedHashMap<String, SOSProviderConfig>();
        
        for (SOSProviderConfig providerConf: config.dataProviders)
        {
            // TODO deal with null URI ??            
            dataProviderConfigs.put(providerConf.uri, providerConf);
            
            // register to source events
            if (providerConf instanceof SensorDataProviderConfig)
            {
                //((SensorDataProviderConfig)providerConf).sensorID;
                
            }
        }
    }


    @Override
    public void updateConfig(SOSServiceConfig config) throws SensorHubException
    {
        // TODO Auto-generated method stub

    }
    
    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    @Override
    public SOSServiceConfig getConfiguration()
    {
        return (SOSServiceConfig)config.clone();
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
        // TODO Auto-generated method stub
        
    }
    
    /////////////////////////////////////////
    /// methods overriden from SOSServlet ///
    /////////////////////////////////////////
    
    @Override
    protected void sendCapabilities(String section, OutputStream resp)
    {
        // TODO generate capabilities from config + sensor info
    }

}
