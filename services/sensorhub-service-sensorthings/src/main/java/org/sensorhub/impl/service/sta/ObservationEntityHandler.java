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

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.concurrent.Flow.Subscriber;
import java.util.stream.Collectors;
import org.joda.time.DateTimeZone;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.event.IEventPublisher;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.api.obs.IObsData;
import org.sensorhub.api.obs.ObsData;
import org.sensorhub.api.obs.ObsEvent;
import org.sensorhub.impl.event.DelegatingSubscriberAdapter;
import org.sensorhub.impl.procedure.DataStreamTransactionHandler;
import org.sensorhub.impl.procedure.ProcedureSubscriptionHandler;
import org.sensorhub.impl.service.sta.filter.ObsFilterVisitor;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataBlockInt;
import org.vast.data.DataBlockLong;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockString;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.fraunhofer.iosb.ilt.frostserver.model.Datastream;
import de.fraunhofer.iosb.ilt.frostserver.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.frostserver.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.model.Observation;
import de.fraunhofer.iosb.ilt.frostserver.model.core.AbstractDatastream;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySetImpl;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInstant;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.util.NoSuchEntityException;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * Handler for Observation resources.<br/>
 *
 * The truncate flag is used to remove the nanoseconds part of observations IDs
 * so that we can pass the CITE tests that are limited to 34-bits integer IDs.
 * However this will only work if observation time stamps are "whole seconds"
 * with the fractional part set to 0.
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
@SuppressWarnings("rawtypes")
public class ObservationEntityHandler implements IResourceHandler<Observation>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find Observation ";

    OSHPersistenceManager pm;
    STASecurity securityHandler;
    IObsStore obsReadStore;
    IObsStore obsWriteStore;
    int maxPageSize = 100;
    boolean truncateIds = false;
    Cache<Long, DataStreamTransactionHandler> dsHandlerCache;
    Cache<Long, Boolean> dsResultHasTsCache;
    
    
    static class EventPublisherInfo
    {
        IDataStreamInfo dsInfo;
        IEventPublisher publisher;
        
        EventPublisherInfo(IDataStreamInfo dsInfo, IEventPublisher publisher)
        {
            this.dsInfo = dsInfo;
            this.publisher = publisher;
        }
    }


    ObservationEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.obsReadStore = pm.readDatabase.getObservationStore();
        this.obsWriteStore = pm.writeDatabase != null ? pm.writeDatabase.getObservationStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        
        this.dsHandlerCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .concurrencyLevel(4)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
        
        /*this.dsResultHasTsCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .concurrencyLevel(4)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();*/
    }


    @Override
    public ResourceId create(Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof Observation);
        Observation obs = (Observation)entity;

        securityHandler.checkPermission(securityHandler.sta_insert_obs);
        Asserts.checkArgument(obs.getPhenomenonTime() != null, "Missing phenomenonTime");
        
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
                
                // handle associations / deep inserts
                var ds = obs.getDatastream() != null ? obs.getDatastream() : obs.getMultiDatastream();
                ResourceId dsId = pm.dataStreamHandler.handleDatastreamAssoc(ds);
                
                // get transaction handler for existing datastream
                long localDsID = pm.toLocalID(dsId.asLong());
                var dsHandler = dsHandlerCache.get(localDsID, () -> {
                    return pm.transactionHandler.getDataStreamHandler(localDsID);
                });
                if (dsHandler == null)
                    throw new NoSuchEntityException(DatastreamEntityHandler.NOT_FOUND_MESSAGE + dsId);
                //var oldDsInfo = dsHandler.getDataStreamInfo();
        
                // check linked FOI exists
                ResourceId foiId = null;
                String foiUri = null;
                if (obs.getFeatureOfInterest() != null)
                {
                    foiId = (ResourceId)obs.getFeatureOfInterest().getId();
                    long localFoiID = pm.toLocalID(foiId.asLong());
                    var foi = pm.foiHandler.foiWriteStore.getCurrentVersion(localFoiID);
                    if (foi == null)
                        throw new NoSuchEntityException(FoiEntityHandler.NOT_FOUND_MESSAGE + foiId);
                    foiUri = foi.getUniqueIdentifier();
                }
        
                // generate OSH obs
                var obsData = toObsData(obs, dsId, foiId, foiUri);
                
                // store in DB + send event
                var newObsId = dsHandler.addObs(obsData);
                                
                return new ResourceIdBigInt(newObsId);
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error creating observation", e);
        }
    }


    @Override
    public boolean update(Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof Observation);
        //Observation obs = (Observation)entity;
        
        securityHandler.checkPermission(securityHandler.sta_update_obs);
        throw new UnsupportedOperationException("Update not supported");
    }


    @Override
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_obs);
        throw new UnsupportedOperationException("Patch not supported");
    }


    @Override
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        securityHandler.checkPermission(securityHandler.sta_delete_obs);
        ResourceId obsId = id;

        if (obsWriteStore != null)
        {
            IObsData obs = obsWriteStore.remove(toLocalKey(obsId));
            if (obs == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);

            return true;
        }

        return false;
    }


    @Override
    public Observation getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_obs);

        BigInteger key = id.asBigInt();
        if (truncateIds)
            key = expandPublicId(key);

        IObsData obs = obsReadStore.get(key);
        if (obs == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);

        return toFrostObservation(key, obs, checkResultHasTimeStamp(obs), q);
    }


    @Override
    public EntitySet<?> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_obs);

        // create obs filter
        ObsFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);
        
        // collect result to entity set
        var entitySet = obsReadStore.selectEntries(filter)
            .skip(skip)
            .limit(limit+1) // request limit+1 elements to handle paging
            .map(e -> toFrostObservation(e.getKey(), e.getValue(), checkResultHasTimeStamp(e.getValue()), q))
            .collect(Collectors.toCollection(EntitySetImpl::new));

        return FrostUtils.handlePaging(entitySet, path, q, limit);
    }
    
    
    @Override
    public void subscribeToCollection(ResourcePath path, Query q, Subscriber<Entity<?>> subscriber)
    {
        securityHandler.checkPermission(securityHandler.sta_read_obs);
        
        // create obs filter
        ObsFilter filter = getFilter(path, q);
        
        // subscribe for obs using filter
        var eventHelper = new ProcedureSubscriptionHandler(pm.readDatabase, pm.eventBus);
        var ok = eventHelper.subscribe(filter, new DelegatingSubscriberAdapter<ObsEvent, Observation>(subscriber) {
            public void onNext(ObsEvent item)
            {
                for (var obs: item.getObservations())
                {
                    var staObs = toFrostObservation(BigInteger.ONE, obs, checkResultHasTimeStamp(obs), q);
                    subscriber.onNext(staObs);
                }
            }
        });
        
        // if subscribe failed, it means topic was not available
        if (!ok)
            throw new IllegalArgumentException("Resource unavailable: " + path);
    }
    
    
    protected boolean checkResultHasTimeStamp(IObsData obs)
    {
        /*var dsID = obs.getDataStreamID();
        
        try
        {
            return dsResultHasTsCache.get(dsID, () -> {
                var dsInfo = obsReadStore.getDataStreams().get(new DataStreamKey(dsID));
                return SWEDataUtils.getTimeStampIndexer(dsInfo.getRecordStructure()) != null;
            });
        }
        catch (ExecutionException e)
        {
            throw new IllegalStateException(e.getCause());
        }*/
        return true;
    }


    protected ObsFilter getFilter(ResourcePath path, Query q)
    {
        ObsFilter.Builder builder = new ObsFilter.Builder();

        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.DATASTREAM ||
                idElt.getEntityType() == EntityType.MULTIDATASTREAM)
            {
                ResourceId dsId = (ResourceId)idElt.getId();
                builder.withDataStreams()
                    .withInternalIDs(dsId.asLong())
                    .withAllVersions()
                    .done();
            }
            if (idElt.getEntityType() == EntityType.FEATUREOFINTEREST)
            {
                ResourceId foiId = (ResourceId)idElt.getId();
                builder.withFois(foiId.asLong());
            }
        }

        ObsFilterVisitor visitor = new ObsFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);

        return builder.build();
    }


    /*
     * Create a local DB obs key from the entity ID
     */
    protected BigInteger toLocalKey(ResourceId obsId)
    {
        BigInteger publicId = obsId.asBigInt();
        if (truncateIds)
            publicId = expandPublicId(publicId);

        return pm.toLocalID(publicId);
    }


    protected ObsData toObsData(Observation obs, ResourceId dsId, ResourceId foiId, String foiUri)
    {
        // phenomenon time
        Instant phenomenonTime = Instant.parse(obs.getPhenomenonTime().asISO8601()).truncatedTo(ChronoUnit.MILLIS);

        // result time
        Instant resultTime = null;
        if (obs.getResultTime() != null)
            resultTime = Instant.parse(obs.getResultTime().asISO8601()).truncatedTo(ChronoUnit.MILLIS);

        // result
        DataBlock dataBlk = createDataBlock(phenomenonTime, obs.getResult());

        return new ObsData.Builder()
            .withDataStream(pm.toLocalID(dsId.asLong()))
            .withFoi(foiId == null ? IObsData.NO_FOI : new FeatureId(pm.toLocalID(foiId.asLong()), foiUri))
            .withPhenomenonTime(phenomenonTime)
            .withResultTime(resultTime)
            .withResult(dataBlk)
            .build();
    }
    
    
    protected DataBlock createDataBlock(Instant timeStamp, Object val)
    {
        var tsBlk = new DataBlockDouble(1);
        tsBlk.setDoubleValue(timeStamp.toEpochMilli() / 1000.);
        
        var dataBlk = createDataBlock(val);
        if (dataBlk instanceof DataBlockMixed)
        {
            ((DataBlockMixed)dataBlk).getUnderlyingObject()[0] = tsBlk;
            return dataBlk;
        }
        else
        {
            var wrapBlk = new DataBlockMixed(2, 2);
            ((DataBlockMixed)wrapBlk).getUnderlyingObject()[0] = tsBlk;
            ((DataBlockMixed)wrapBlk).getUnderlyingObject()[1] = (AbstractDataBlock)dataBlk;
            return wrapBlk;
        }
    }


    protected DataBlock createDataBlock(Object val)
    {
        DataBlock dataBlk;
        
        if (val instanceof Integer)
        {
            dataBlk = new DataBlockInt(1);
            dataBlk.setIntValue((Integer)val);
        }
        else if (val instanceof Long)
        {
            dataBlk = new DataBlockLong(1);
            dataBlk.setLongValue((Long)val);
        }
        else if (val instanceof Number)
        {
            dataBlk = new DataBlockDouble(1);
            dataBlk.setDoubleValue(((Number)val).doubleValue());
        }
        else if (val instanceof String)
        {
            dataBlk = new DataBlockString();
            dataBlk.setStringValue((String)val);
        }
        else if (val instanceof ArrayList)
        {
            var elts = (ArrayList)val;
            var numElts = elts.size();
            var blkSize = numElts + 1;
            dataBlk = new DataBlockMixed(blkSize, blkSize);
            for (int i = 0; i < numElts; i++)
            {
                var childBlk = (AbstractDataBlock)createDataBlock(elts.get(i));
                ((DataBlockMixed)dataBlk).getUnderlyingObject()[i+1] = childBlk;
            }
        }
        else
            throw new IllegalArgumentException("Unsupported result type: " + val.getClass().getSimpleName());
        
        return dataBlk;
    }


    protected Observation toFrostObservation(BigInteger key, IObsData obsData, boolean resultHasTimeStamp, Query q)
    {
        Observation obs = new Observation();

        // composite ID
        if (key != null)
            obs.setId(new ResourceIdBigInt(truncateIds ? truncatePublicId(key) : key));

        // phenomenon time
        obs.setPhenomenonTime(TimeInstant.create(obsData.getPhenomenonTime().toEpochMilli(), DateTimeZone.UTC));

        // result time
        if (obsData.getResultTime() == null)
            obs.setResultTime((TimeInstant)obs.getPhenomenonTime());
        else
            obs.setResultTime(TimeInstant.create(obsData.getResultTime().toEpochMilli(), DateTimeZone.UTC));

        // FOI
        if (obsData.getFoiID().getInternalID() != 0)
        {
            FeatureOfInterest foi = new FeatureOfInterest(new ResourceIdLong(obsData.getFoiID().getInternalID()));
            foi.setExportObject(false);
            obs.setFeatureOfInterest(foi);
        }

        // result
        DataBlock data = obsData.getResult();
        int resultStartIdx = resultHasTimeStamp ? 1 : 0;
        if ((resultHasTimeStamp && data.getAtomCount() == 2) || data.getAtomCount() == 1)
        {   
            Datastream ds = new Datastream(new ResourceIdLong(obsData.getDataStreamID()));
            ds.setExportObject(false);
            obs.setDatastream(ds);
            obs.setResult(getResultValue(data, resultStartIdx));
        }
        else
        {
            MultiDatastream ds = new MultiDatastream(new ResourceIdLong(obsData.getDataStreamID()));
            ds.setExportObject(false);
            obs.setMultiDatastream(ds);
            Object[] result = new Object[data.getAtomCount()-resultStartIdx];
            for (int i = resultStartIdx, j = 0; i < data.getAtomCount(); i++, j++)
                result[j] = getResultValue(data, i);
            obs.setResult(result);
        }

        return obs;
    }


    protected Object getResultValue(DataBlock data, int index)
    {
        switch (data.getDataType(index))
        {
            case BOOLEAN:
                return data.getBooleanValue(index);
                
            case DOUBLE:
            case FLOAT:
                return data.getDoubleValue(index);

            case BYTE:
            case UBYTE:
            case SHORT:
            case USHORT:
            case INT:
                return data.getIntValue(index);
                
            case UINT:
            case LONG:            
            case ULONG:
                return data.getLongValue(index);

            case ASCII_STRING:
            case UTF_STRING:
                return data.getStringValue(index);

            default:
                return null;
        }
    }


    protected void handleObservationAssocList(ResourceId dataStreamId, AbstractDatastream<?> dataStream) throws NoSuchEntityException
    {
        if (dataStream.getObservations() == null)
            return;

        boolean isMultiDatastream = dataStream instanceof MultiDatastream;

        for (Observation obs: dataStream.getObservations())
        {
            if (obs.getResult() != null)
            {
                // also set/override mandatory datastream ID
                if (isMultiDatastream)
                    obs.setMultiDatastream(new MultiDatastream(dataStreamId));
                else
                    obs.setDatastream(new Datastream(dataStreamId));

                create(obs);
            }
        }
    }
    
    
    protected void checkTransactionsEnabled()
    {
        if (obsWriteStore == null)
            throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }


    protected BigInteger expandPublicId(BigInteger id)
    {
        // insert the 0 nanoseconds part (32-bits) in the ID
        var localId = pm.toLocalID(id).shiftLeft(32);
        return pm.toPublicID(localId);
    }


    protected BigInteger truncatePublicId(BigInteger id)
    {
        // remove the nanoseconds part (32-bits) of the ID
        var localId = pm.toLocalID(id).shiftRight(32);
        return pm.toPublicID(localId);
    }

}
