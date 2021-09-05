/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.usgs.water;

import java.util.concurrent.Callable;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IProcedureObsDatabaseModule;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.impl.datastore.DataStoreUtils;
import org.sensorhub.impl.datastore.ReadOnlyDataStore;
import org.sensorhub.impl.module.AbstractModule;
import org.vast.util.Bbox;
import com.google.common.collect.Sets;
import net.opengis.sensorml.v20.PhysicalSystem;


/**
 * <p>
 * Database implementation used to access data from USGS water data web
 * archive. This implementation requests data from the USGS web services
 * on-the-fly when a query is received by the database.
 * </p>
 * <p>
 * See the <a href="https://waterservices.usgs.gov/rest">documentation</a>
 * of the REST web services used.
 * </p>
 *
 * @author Alex Robin
 * @since Mar 13, 2017
 */
public class USGSWaterDataArchive extends AbstractModule<USGSWaterDataConfig> implements IProcedureObsDatabaseModule<USGSWaterDataConfig>
{
    static final String BASE_USGS_URL = "https://waterservices.usgs.gov/nwis/";
    static final String UID_PREFIX = "urn:usgs:water:";

    IProcedureStore procStore;
    IFoiStore foiStore;
    IObsStore obsStore;
    IParamDatabase paramDb;

    Bbox foiExtent = new Bbox();
    PhysicalSystem systemDesc;
    
    
    @Override
    protected void doInit() throws SensorHubException
    {
    }
    

    @Override
    protected void doStart() throws SensorHubException
    {   
        this.paramDb = new InMemoryParamDb(getLogger());
        ((InMemoryParamDb)paramDb).preloadParams(
            Sets.newHashSet("Physical", "Biological", "Organics", "Inorganics", "Nutrient", "Radiochemical"), null);
        
        this.procStore = new USGSProcedureStore(config.exposeFilter, paramDb, getLogger());
        this.foiStore = new USGSFoiStore(config.exposeFilter, paramDb,getLogger());
        this.obsStore = new USGSObsStore(config.exposeFilter, paramDb, getLogger());
        
        procStore.linkTo(obsStore.getDataStreams());
        foiStore.linkTo(procStore);
        foiStore.linkTo(obsStore);
        obsStore.linkTo(foiStore);
        obsStore.getDataStreams().linkTo(procStore);
    }


    @Override
    protected void doStop() throws SensorHubException
    {
    }
    
    
    @Override
    protected void afterStart()
    {
        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().register(this);
    }
    
    
    @Override
    protected void beforeStop()
    {
        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().unregister(this);
    }


    /*@Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        final String recType = filter.getRecordType();
        USGSWaterDataStream rs = dataStores.get(recType);

        // prepare loader to fetch data from USGS web service
        final ObsRecordLoader loader = new ObsRecordLoader(this, rs.getRecordDescription());
        final USGSDataFilter usgsFilter = getUsgsFilter(filter);

        // request observations by batch and iterates through them sequentially
        final long endTime = usgsFilter.endTime.getTime();
        final long batchLength = 8 * 3600 * 1000; // 8 hours
        class BatchIterator implements Iterator<IDataRecord>
        {
            Iterator<WaterDataRecord> batchIt;
            IDataRecord next;
            long nextBatchStartTime = usgsFilter.startTime.getTime();

            BatchIterator()
            {
                preloadNext();
            }


            protected IDataRecord preloadNext()
            {
                IDataRecord current = next;
                next = null;

                // retrieve next batch if needed
                if ((batchIt == null || !batchIt.hasNext()) && nextBatchStartTime <= endTime)
                {
                    usgsFilter.startTime = new Date(nextBatchStartTime);

                    // adjust batch length to avoid a very small batch at the end
                    long timeGap = 1000; // gap to avoid duplicated obs
                    long adjBatchLength = batchLength;
                    long timeLeft = endTime - nextBatchStartTime;
                    if (((double) timeLeft) / batchLength < 1.5)
                        adjBatchLength = timeLeft + timeGap;
                    usgsFilter.endTime = new Date(Math.min(nextBatchStartTime + adjBatchLength - timeGap, endTime));
                    batchIt = nextBatch(loader, usgsFilter, recType).iterator();
                    nextBatchStartTime += adjBatchLength;
                }

                if (batchIt != null && batchIt.hasNext())
                    next = batchIt.next();

                return current;
            }


            @Override
            public boolean hasNext()
            {
                return (next != null);
            }


            @Override
            public IDataRecord next()
            {
                if (!hasNext())
                    throw new NoSuchElementException();
                return preloadNext();
            }
        }
        ;

        return new BatchIterator();
    }*/


    /*protected Collection<WaterDataRecord> nextBatch(ObsRecordLoader loader, USGSDataFilter filter, String recType)
    {
        try
        {
            ArrayList<WaterDataRecord> records = new ArrayList<WaterDataRecord>();

            // log batch time range
            if (getLogger().isDebugEnabled())
            {
                DateTimeFormat timeFormat = new DateTimeFormat();
                getLogger().debug("Next batch is {} - {}", timeFormat.formatIso(filter.startTime.getTime() / 1000., 0),
                    timeFormat.formatIso(filter.endTime.getTime() / 1000., 0));
            }

            // request and parse next batch
            loader.sendRequest(filter);
            while (loader.hasNext())
            {
                DataBlock data = loader.next();
                if (data == null)
                    break;
                DataKey key = new DataKey(recType, data.getDoubleValue(0));
                records.add(new WaterDataRecord(key, data));
            }

            // sort by timestamps
            Collections.sort(records);
            return records;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while sending request for instantaneous values");
        }
    }*/
    

    @Override
    public Integer getDatabaseNum()
    {
        return config.databaseNum;
    }


    @Override
    public boolean isOpen()
    {
        return isStarted();
    }


    @Override
    public boolean isReadOnly()
    {
        return true;
    }


    @Override
    public IProcedureStore getProcedureStore()
    {
        return procStore;
    }


    @Override
    public IFoiStore getFoiStore()
    {
        return foiStore;
    }


    @Override
    public IObsStore getObservationStore()
    {
        return obsStore;
    }


    @Override
    public ICommandStore getCommandStore()
    {
        return DataStoreUtils.EMPTY_COMMAND_STORE;
    }


    @Override
    public <T> T executeTransaction(Callable<T> transaction) throws Exception
    {
        throw new UnsupportedOperationException(ReadOnlyDataStore.READ_ONLY_ERROR_MSG);
    }


    @Override
    public void commit()
    {
        throw new UnsupportedOperationException(ReadOnlyDataStore.READ_ONLY_ERROR_MSG);
    }
}
