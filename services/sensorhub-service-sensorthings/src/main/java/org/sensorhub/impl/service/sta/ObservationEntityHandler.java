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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Collectors;
import org.joda.time.DateTimeZone;
import org.sensorhub.api.datastore.DataStreamInfo;
import org.sensorhub.api.datastore.FeatureId;
import org.sensorhub.api.datastore.IObsStore;
import org.sensorhub.api.datastore.ObsData;
import org.sensorhub.api.datastore.ObsFilter;
import org.sensorhub.api.datastore.ObsKey;
import org.sensorhub.impl.sensor.VirtualSensorProxy;
import org.vast.data.DataBlockDouble;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
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
 * Handler for Sensor resources
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
@SuppressWarnings("rawtypes")
public class ObservationEntityHandler implements IResourceHandler<Observation>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find observation with id #";
    
    OSHPersistenceManager pm;
    STASecurity securityHandler;
    IObsStore obsReadStore;
    IObsStore obsWriteStore;
    int maxPageSize = 100;
    
    
    ObservationEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.obsReadStore = pm.obsDbRegistry.getFederatedObsDatabase().getObservationStore();
        this.obsWriteStore = pm.obsDatabase != null ? pm.obsDatabase.getObservationStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
    }
    
    
    @Override
    public ResourceId create(Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_obs);
        Asserts.checkArgument(entity instanceof Observation);
        Observation obs = (Observation)entity;
        
        // check data stream is present
        AbstractDatastream<?> dataStream = obs.getDatastream();
        if (dataStream == null)
            dataStream = obs.getMultiDatastream();
        if (dataStream == null)
            throw new IllegalArgumentException("A new Observation SHALL link to a Datastream or MultiDatastream entity");
        
        Asserts.checkArgument(obs.getPhenomenonTime() != null, "Missing phenomenonTime");
        
        // prepare obs key fields and obs data
        ResourceId dsId = (ResourceId)dataStream.getId();
        Instant phenomenonTime = Instant.parse(obs.getPhenomenonTime().asISO8601());
        FeatureId foi = ObsKey.NO_FOI;
        if (obs.getFeatureOfInterest() != null)
        {
            ResourceId foiId = (ResourceId)obs.getFeatureOfInterest().getId();
            foi = new FeatureId(pm.toLocalID(foiId.internalID));
        }
        
        // generate OSH obs
        DataStreamInfo dsInfo = obsReadStore.getDataStreams().get(dsId.internalID);
        if (dsInfo == null)
            throw new NoSuchEntityException(DatastreamEntityHandler.NOT_FOUND_MESSAGE + dsId);
        ObsData obsData = toObsData(obs);
        
        // push obs to proxy
        VirtualSensorProxy proxy = pm.sensorHandler.getProcedureProxy(dsInfo.getProcedure().getUniqueID());
        //proxy.publishNewRecord(dsInfo.getOutputName(), obsData.getResult());
        
        // add observation to data store
        if (obsWriteStore != null)
        {
            ObsKey key = ObsKey.builder()
                .withPhenomenonTime(phenomenonTime)
                .withDataStream(pm.toLocalID(dsId.internalID))
                .withFoi(foi)
                .build();
            obsWriteStore.put(key, obsData);
        }
        
        // generate datastream ID
        return new ObsResourceId(dsId.internalID, foi.getInternalID(), phenomenonTime.toEpochMilli());
    }
    

    @Override
    public boolean update(Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_obs);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_obs);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_delete_obs);
        
        if (obsWriteStore != null)
        {
            ObsData obs = obsWriteStore.remove(toLocalKey(id));
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
                
        ObsKey key = toPublicKey(id);
        ObsData obs = obsReadStore.get(key);
        if (obs == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        
        return toFrostObservation(key, obs, q);
    }
    
    
    /*
     * Create a local DB obs key from the entity ID
     */
    protected ObsKey toLocalKey(ResourceId id)
    {
        ObsResourceId obsId = (ObsResourceId)id;
        
        return ObsKey.builder()
            .withDataStream(pm.toLocalID(obsId.dataStreamID))
            .withFoi(new FeatureId(pm.toLocalID(obsId.foiID)))
            .withPhenomenonTime(Instant.ofEpochMilli(id.internalID))
            .build();
    }
    
    
    /*
     * Create a public obs key from the entity ID
     */
    protected ObsKey toPublicKey(ResourceId id)
    {
        ObsResourceId obsId = (ObsResourceId)id;
        
        return ObsKey.builder()
            .withDataStream(obsId.dataStreamID)
            .withFoi(new FeatureId(obsId.foiID))
            .withPhenomenonTime(Instant.ofEpochMilli(id.internalID))
            .build();
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
        return obsReadStore.selectEntries(filter)
            .skip(skip)
            .limit(limit)
            .map(e -> toFrostObservation(e.getKey(), e.getValue(), q))
            .collect(Collectors.toCollection(EntitySetImpl::new));
    }
    
    
    protected ObsFilter getFilter(ResourcePath path, Query q)
    {
        ObsFilter.Builder builder = ObsFilter.builder();
        
        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.DATASTREAM ||
                idElt.getEntityType() == EntityType.MULTIDATASTREAM)
            {
                ResourceId dsId = (ResourceId)idElt.getId();
                builder.withDataStreams(dsId.internalID);
            }
            if (idElt.getEntityType() == EntityType.FEATUREOFINTEREST)
            {
                ResourceId foiId = (ResourceId)idElt.getId();
                builder.withFois(foiId.internalID);
            }
        }
        
        /*SensorFilterVisitor visitor = new SensorFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);*/
        
        return builder.build();
    }
    
    
    protected ObsData toObsData(Observation obs)
    {
        double val = ((BigDecimal)obs.getResult()).doubleValue();
        DataBlockDouble result = new DataBlockDouble(1);
        result.setDoubleValue(val);
        
        return ObsData.builder()
            .withResult(result)
            .build();
    }
    
    
    protected void addToDataBlock(Object obj)
    {
        
    }
    
    
    protected Observation toFrostObservation(ObsKey key, ObsData obsData, Query q)
    {
        Observation obs = new Observation();
        
        // composite ID
        obs.setId(new ObsResourceId(
            key.getDataStreamID(),
            key.getFoiID().getInternalID(),
            key.getPhenomenonTime().toEpochMilli()));
        
        // phenomenon time
        obs.setPhenomenonTime(TimeInstant.create(key.getPhenomenonTime().toEpochMilli(), DateTimeZone.UTC));
        
        // result time
        if (key.getResultTime() == null)
            obs.setResultTime((TimeInstant)obs.getPhenomenonTime());
        else
            obs.setResultTime(TimeInstant.create(key.getResultTime().toEpochMilli(), DateTimeZone.UTC));
        
        // FOI
        if (!ObsKey.NO_FOI.equals(key.getFoiID()))
        {
            FeatureOfInterest foi = new FeatureOfInterest(new ResourceId(key.getFoiID().getInternalID()));
            foi.setExportObject(false);
            obs.setFeatureOfInterest(foi);
        }
        
        // result
        DataBlock data = obsData.getResult();
        if (data.getAtomCount() == 2)
        {
            Datastream ds = new Datastream(new ResourceId(key.getDataStreamID()));
            ds.setExportObject(false);
            obs.setDatastream(ds);
            obs.setResult(getResultValue(data, 1));
        }
        else
        {
            MultiDatastream ds = new MultiDatastream(new ResourceId(key.getDataStreamID()));
            ds.setExportObject(false);
            obs.setMultiDatastream(ds);
            Object[] result = new Object[data.getAtomCount()-1];
            for (int i = 1; i < data.getAtomCount(); i++)
                result[i-1] = getResultValue(data, i);
            obs.setResult(result);           
        }        
        
        return obs;
    }
    
    
    protected Object getResultValue(DataBlock data, int index)
    {
        switch (data.getDataType(index))
        {
            case DOUBLE:
            case FLOAT:
                return data.getDoubleValue(index);
                
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case UBYTE:
            case USHORT:
            case UINT:
            case ULONG:
                return data.getLongValue(index);
                
            case ASCII_STRING:
            case UTF_STRING:
                return data.getStringValue(index);
                
            default:
                return null;
        }
    }

}
