/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.util.concurrent.Callable;
import org.h2.mvstore.MVStore;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.deployment.IDeploymentStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.datastore.h2.MVObsSystemDatabase;


/**
 * <p>
 * Main SensorThings Database implementation.<br/>
 * Depending on the configuration, this class can either include its own instance
 * of {@link IObsSystemDatabase} or link to one that already exists as a
 * separate module.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 14, 2019
 */
public class STADatabase implements ISTADatabase
{
    final static String THING_STORE_NAME = "thing_store";
    final static String LOCATION_STORE_NAME = "location_store";
    final static String OBS_PROP_STORE_NAME = "obsprop_store";
        
    STAService service;
    STADatabaseConfig config;
    MVStore mvStore;
    IDatabaseRegistry dbRegistry;
    IObsSystemDatabase obsDatabase;
    STAThingStoreImpl thingStore;
    STALocationStoreImpl locationStore;
    STAObsPropStoreImpl obsPropStore;
    STADataStreamStoreImpl dataStreamStore;
    boolean externalObsDatabaseUsed;
    
    
    STADatabase(STAService service, STADatabaseConfig config)
    {
        this.service = service;
        this.config = config;
        init();
    }
    

    public void init()
    {
        // init embedded obs database or use external one
        try
        {
            if (config.externalObsDatabaseID == null)
            {        
                obsDatabase = new MVObsSystemDatabase();
                ((MVObsSystemDatabase)obsDatabase).setConfiguration(config);
                ((MVObsSystemDatabase)obsDatabase).init();
                ((MVObsSystemDatabase)obsDatabase).waitForState(ModuleState.INITIALIZED, 5000L);
                ((MVObsSystemDatabase)obsDatabase).start();
                ((MVObsSystemDatabase)obsDatabase).waitForState(ModuleState.STARTED, 5000L);
                mvStore = ((MVObsSystemDatabase)obsDatabase).getMVStore();
                externalObsDatabaseUsed = false;
            }
            else
            {
                // get database module used for writing sensor/datastream/obs/foi entities
                obsDatabase = (IObsSystemDatabase)service.getParentHub().getModuleRegistry().getModuleById(config.externalObsDatabaseID);
                externalObsDatabaseUsed = true;
            }
        }
        catch (SensorHubException e)
        {
            throw new IllegalArgumentException("Cannot find STA Observation database", e);
        }
        
        // enforce same database num as obs DB
        int dbNum = obsDatabase.getDatabaseNum() != null ? obsDatabase.getDatabaseNum() : 0;
        var idType = config.idProviderType;
        
        // register obs database with hub
        dbRegistry = service.getParentHub().getDatabaseRegistry();
        dbRegistry.register(obsDatabase);
        
        // create separate MV Store if an external obs database is used
        if (externalObsDatabaseUsed)
            mvStore = initMVStore();
        
        // open thing data store
        thingStore = STAThingStoreImpl.open(this, dbNum, idType, MVDataStoreInfo.builder()
            .withName(THING_STORE_NAME)
            .build());
        
        // open location data store
        locationStore = STALocationStoreImpl.open(this, dbNum, idType, MVDataStoreInfo.builder()
            .withName(LOCATION_STORE_NAME)
            .build());
        
        // open observed property data store
        obsPropStore = STAObsPropStoreImpl.open(this, dbNum, idType, MVDataStoreInfo.builder()
            .withName(OBS_PROP_STORE_NAME)
            .build());

        // init datastream store wrapper
        dataStreamStore = new STADataStreamStoreImpl(this,
            obsDatabase.getObservationStore().getDataStreams());
        
        thingStore.locationStore = locationStore;
        locationStore.thingStore = thingStore;
    }
    
    
    protected MVStore initMVStore()
    {
        MVStore.Builder builder = new MVStore.Builder().fileName(config.storagePath);
        
        if (config.memoryCacheSize > 0)
            builder = builder.cacheSize(config.memoryCacheSize/1024);
                                  
        if (config.autoCommitBufferSize > 0)
            builder = builder.autoCommitBufferSize(config.autoCommitBufferSize);
        
        if (config.useCompression)
            builder = builder.compress();
        
        MVStore mvStore = builder.open();
        mvStore.setVersionsToKeep(0);
        
        return mvStore;
    }
    
    
    public <T> T executeTransaction(Callable<T> transaction) throws Exception
    {
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                if (externalObsDatabaseUsed)
                    return obsDatabase.executeTransaction(transaction);
                else
                    return transaction.call();
            }
            catch (Exception e)
            {
                mvStore.rollbackTo(currentVersion);
                throw e;
            }
        }
    }
    
        
    @Override
    public Integer getDatabaseNum()
    {
        return obsDatabase.getDatabaseNum();
    }


    @Override
    public ISystemDescStore getSystemDescStore()
    {
        return obsDatabase.getSystemDescStore();
    }


    @Override
    public IDeploymentStore getDeploymentStore()
    {
        throw new UnsupportedOperationException();
    }


    public ISTADataStreamStore getDataStreamStore()
    {
        return dataStreamStore;
    }


    @Override
    public IObsStore getObservationStore()
    {
        return obsDatabase.getObservationStore();
    }


    @Override
    public IFoiStore getFoiStore()
    {
        return obsDatabase.getFoiStore();
    }


    @Override
    public ICommandStore getCommandStore()
    {
        return obsDatabase.getCommandStore();
    }


    @Override
    public ISTAThingStore getThingStore()
    {
        return thingStore;
    }


    @Override
    public ISTALocationStore getThingLocationStore()
    {
        return locationStore;
    }


    @Override
    public ISTAObsPropStore getObservedPropertyDataStore()
    {
        return obsPropStore;
    }


    @Override
    public void commit()
    {
        obsDatabase.commit();
        thingStore.commit();
        obsPropStore.commit();
    }
    
    
    public MVStore getMVStore()
    {
        return mvStore;
    }


    @Override
    public void close()
    {
        if (!mvStore.isClosed())
            mvStore.close();
    }


    @Override
    public boolean isOpen()
    {
        return obsDatabase != null &&
            mvStore != null &&
            !mvStore.isClosed() &&
            obsDatabase.isOpen();
    }


    @Override
    public boolean isReadOnly()
    {
        return obsDatabase.isReadOnly();
    }

}
