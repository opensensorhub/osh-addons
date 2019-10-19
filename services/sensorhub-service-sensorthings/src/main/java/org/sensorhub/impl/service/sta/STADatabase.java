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

import java.util.Set;
import java.util.concurrent.Callable;
import org.h2.mvstore.MVStore;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IDatabaseRegistry;
import org.sensorhub.api.datastore.IFeatureStore;
import org.sensorhub.api.datastore.IFoiStore;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.sensorhub.api.datastore.IObsStore;
import org.sensorhub.api.procedure.IProcedureDescriptionStore;
import org.sensorhub.impl.datastore.h2.H2Utils;
import org.sensorhub.impl.datastore.h2.MVDataStoreInfo;
import org.sensorhub.impl.datastore.h2.MVFeatureStoreImpl;
import org.sensorhub.impl.datastore.h2.MVHistoricalObsDatabase;
import org.vast.ogc.gml.GenericFeature;
import com.google.common.collect.Sets;


/**
 * <p>
 * Main SensorThings Database implementation.<br/>
 * Depending on the configuration, this class can either include its own instance
 * of {@link IHistoricalObsDatabase} or link to one that already exists as a
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
    IHistoricalObsDatabase obsDatabase;
    MVFeatureStoreImpl thingStore;
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
                obsDatabase = new MVHistoricalObsDatabase();
                ((MVHistoricalObsDatabase)obsDatabase).init(config);
                ((MVHistoricalObsDatabase)obsDatabase).start();
                mvStore = ((MVHistoricalObsDatabase)obsDatabase).getMVStore();
                externalObsDatabaseUsed = false;
            }
            else
            {
                // get database module used for writing obs/sensor/datastream/obs/foi entities
                obsDatabase = (IHistoricalObsDatabase)service.getParentHub().getModuleRegistry().getModuleById(config.externalObsDatabaseID);
                externalObsDatabaseUsed = true;
            }
        }
        catch (SensorHubException e)
        {
            throw new IllegalArgumentException("Cannot find STA Observation database", e);
        }
        
        // register obs database with hub
        Set<String> wildcardUID = Sets.newHashSet(service.getProcedureGroupUID()+"*");
        dbRegistry = service.getParentHub().getDatabaseRegistry();
        dbRegistry.register(wildcardUID, obsDatabase);
        
        // create separate MV Store if an external obs database is used
        if (externalObsDatabaseUsed)
            mvStore = initMVStore();
        
        // open thing data store
        if (H2Utils.getDataStoreInfo(mvStore, THING_STORE_NAME) == null)
        {
            thingStore = MVFeatureStoreImpl.create(mvStore, MVDataStoreInfo.builder()
                .withName(THING_STORE_NAME)
                .build());
        }
        else
            thingStore = MVFeatureStoreImpl.open(mvStore, THING_STORE_NAME);
        
        // open location data store
        if (H2Utils.getDataStoreInfo(mvStore, LOCATION_STORE_NAME) == null)
        {
            locationStore = STALocationStoreImpl.create(this, MVDataStoreInfo.builder()
                .withName(LOCATION_STORE_NAME)
                .build());
        }
        else
            locationStore = STALocationStoreImpl.open(this, LOCATION_STORE_NAME);
        
        // open observed property data store
        if (H2Utils.getDataStoreInfo(mvStore, OBS_PROP_STORE_NAME) == null)
        {
            obsPropStore = STAObsPropStoreImpl.create(this, MVDataStoreInfo.builder()
                .withName(OBS_PROP_STORE_NAME)
                .build());
        }
        else
            obsPropStore = STAObsPropStoreImpl.open(this, OBS_PROP_STORE_NAME);

        // init datastream store wrapper
        this.dataStreamStore = new STADataStreamStoreImpl(this,
            obsDatabase.getObservationStore().getDataStreams());
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
    
    
    public long toPublicID(long internalID)
    {
        return dbRegistry.getPublicID(getDatabaseID(), internalID);
    }
    
    
    public long toLocalID(long publicID)
    {
        return dbRegistry.getLocalID(getDatabaseID(), publicID);
    }
    
        
    @Override
    public int getDatabaseID()
    {
        return obsDatabase.getDatabaseID();
    }


    @Override
    public IProcedureDescriptionStore getProcedureStore()
    {
        return obsDatabase.getProcedureStore();
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public IFeatureStore<FeatureKey, GenericFeature> getThingStore()
    {
        return (IFeatureStore)thingStore;
    }


    @Override
    public ILocationStore getThingLocationStore()
    {
        return locationStore;
    }


    @Override
    public IFeatureStore<FeatureKey, ObsPropDef> getObservedPropertyDataStore()
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
        mvStore.close();        
    }

}
