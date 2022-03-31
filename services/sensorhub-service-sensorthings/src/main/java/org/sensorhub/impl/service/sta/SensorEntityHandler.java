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

import java.security.AccessControlException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Flow.Subscriber;
import java.util.stream.Collectors;
import org.isotc211.v2005.gmd.CIOnlineResource;
import org.isotc211.v2005.gmd.impl.GMDFactory;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.api.system.SystemAddedEvent;
import org.sensorhub.api.system.SystemChangedEvent;
import org.sensorhub.api.system.SystemEvent;
import org.sensorhub.api.system.SystemId;
import org.sensorhub.impl.event.DelegatingSubscriberAdapter;
import org.sensorhub.impl.service.sta.filter.SensorFilterVisitor;
import org.sensorhub.impl.system.SystemSubscriptionHandler;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.sensorhub.utils.SWEDataUtils;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;
import org.vast.util.TimeExtent;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.base.Strings;
import de.fraunhofer.iosb.ilt.frostserver.model.Datastream;
import de.fraunhofer.iosb.ilt.frostserver.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.model.Sensor;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySetImpl;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.NavigationProperty;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.query.Expand;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.util.NoSuchEntityException;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.DocumentList;


/**
 * <p>
 * Handler for Sensor resources
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class SensorEntityHandler implements IResourceHandler<Sensor>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find Sensor ";
    static final String NOT_WRITABLE_MESSAGE = "Cannot modify read-only Sensor ";
    static final String MISSING_ASSOC = "Missing reference to Sensor entity";
    static final String FORMAT_SML2 = "http://www.opengis.net/sensorml-json/2.0";

    OSHPersistenceManager pm;
    ISystemDescStore procReadStore;
    ISystemDescStore procWriteStore;
    IDataStreamStore dataStreamReadStore;
    STASecurity securityHandler;
    int maxPageSize = 100;
    SystemId procGroupID;


    SensorEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.procReadStore = pm.readDatabase.getSystemDescStore();
        this.procWriteStore = pm.writeDatabase != null ? pm.writeDatabase.getSystemDescStore() : null;
        this.dataStreamReadStore = pm.readDatabase.getDataStreamStore();
        this.securityHandler = pm.service.getSecurityHandler();
        this.procGroupID = pm.service.getSystemGroupID();
    }


    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof Sensor);
        Sensor sensor = (Sensor)entity;
        
        securityHandler.checkPermission(securityHandler.sta_insert_sensor);
        
        // generate unique ID from name
        Asserts.checkArgument(!Strings.isNullOrEmpty(sensor.getName()), "Sensor name must be set");        
        String sysUID = procGroupID.getUniqueID() + ":" + SWEDataUtils.toNCName(sensor.getName());
        
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
                
                // generate sensorML description
                AbstractProcess procDesc = toSmlProcess(sensor, sysUID);
                
                // cleanup sml description before storage
                var procWrapper = new SystemWrapper(procDesc)
                    .hideOutputs()
                    .hideTaskableParams()
                    .defaultToValidFromNow();
                
                // store in DB + send event
                FeatureKey procKey;
                if (procGroupID != null)
                {
                    var groupHandler = pm.transactionHandler.getSystemHandler(procGroupID.getInternalID());
                    procKey = groupHandler.addOrUpdateMember(procWrapper).getLocalSystemKey();
                }
                else
                    procKey = pm.transactionHandler.addOrUpdateSystem(procWrapper).getLocalSystemKey();
                
                // handle associations / deep inserts
                var publicSensorID = pm.toPublicID(procKey.getInternalID());
                ResourceId newSensorId = new ResourceIdLong(publicSensorID);
                pm.dataStreamHandler.handleDatastreamAssocList(newSensorId, sensor);
    
                return newSensorId;
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (DataStoreException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error creating sensor", e);
        }
    }


    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof Sensor);
        
        securityHandler.checkPermission(securityHandler.sta_update_sensor);
        
        ResourceId id = (ResourceId)entity.getId();
        long publicSensorID = ((ResourceId)entity.getId()).asLong();
        checkSensorWritable(publicSensorID);
        
        // get transaction handler for existing sensor
        long localSensorID = pm.toLocalID(publicSensorID);
        var procHandler = pm.transactionHandler.getSystemHandler(localSensorID);
        if (procHandler == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
                
        // update description
        try
        {
            // generate sensorML description
            String sysUID = procHandler.getSystemUID();
            AbstractProcess procDesc = toSmlProcess((Sensor)entity, sysUID);
            
            // cleanup sml description before storage
            var procWrapper = new SystemWrapper(procDesc)
                .hideOutputs()
                .hideTaskableParams()
                .defaultToValidFromNow();
            
            return procHandler.update(procWrapper);
        }
        catch (DataStoreException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error updating sensor " + id, e);
        }
    }


    @Override
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_sensor);
        throw new UnsupportedOperationException("Patch not supported");
    }


    @Override
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        securityHandler.checkPermission(securityHandler.sta_delete_sensor);
        
        long publicSensorID = id.asLong();
        checkSensorWritable(publicSensorID);
        
        // get transaction handler for existing sensor
        long localSensorID = pm.toLocalID(publicSensorID);
        var procHandler = pm.transactionHandler.getSystemHandler(localSensorID);
        if (procHandler == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        
        try
        {
            return procHandler.delete();            
        }
        catch (DataStoreException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error deleting sensor " + id, e);
        }
    }


    @Override
    public Sensor getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_sensor);

        var proc = procReadStore.getCurrentVersion(id.asLong());
        if (proc == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        
        return toFrostSensor(id.asLong(), proc, q);
    }


    @Override
    public EntitySet<Sensor> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_sensor);

        SystemFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);

        var entitySet = procReadStore.selectEntries(filter)
            //.peek(e -> System.out.println(e.getValue().getUniqueIdentifier() + ": " + e.getValue().getValidTime()))
            .skip(skip)
            .limit(limit+1) // request limit+1 elements to handle paging
            .map(e -> toFrostSensor(e.getKey().getInternalID(), e.getValue(), q))
            .collect(Collectors.toCollection(EntitySetImpl::new));

        return FrostUtils.handlePaging(entitySet, path, q, limit);
    }
    
    
    @Override
    public void subscribeToCollection(ResourcePath path, Query q, Subscriber<Entity<?>> subscriber)
    {
        securityHandler.checkPermission(securityHandler.sta_read_obs);
        
        // create obs filter
        SystemFilter filter = getFilter(path, q);
        
        // subscribe for obs using filter
        var eventHelper = new SystemSubscriptionHandler(pm.readDatabase, pm.eventBus);
        var ok = eventHelper.subscribe(filter, new DelegatingSubscriberAdapter<SystemEvent, Sensor>(subscriber) {
            public void onNext(SystemEvent item)
            {
                if (item instanceof SystemAddedEvent || item instanceof SystemChangedEvent)
                {
                    var proc = procReadStore.getCurrentVersion(item.getSystemUID());
                    if (proc != null)
                    {
                        var staSensor = toFrostSensor(1, proc, q);
                        subscriber.onNext(staSensor);
                    }
                }
            }           
        });
        
        // if subscribe failed, it means topic was not available
        if (!ok)
            throw new AccessControlException("Resource unavailable: " + path);
    }


    protected SystemFilter getFilter(ResourcePath path, Query q)
    {
        SystemFilter.Builder builder = new SystemFilter.Builder()
            .withCurrentVersion();

        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            var parentElt = (EntityPathElement)path.getMainElement().getParent();

            // if direct parent is identified
            if (idElt == parentElt)
            {
                if (idElt.getEntityType() == EntityType.DATASTREAM ||
                    idElt.getEntityType() == EntityType.MULTIDATASTREAM)
                {
                    ResourceId dsId = (ResourceId)idElt.getId();
                    var dsKey = new DataStreamKey(dsId.asLong());
                    IDataStreamInfo dsInfo = dataStreamReadStore.get(dsKey);
                    builder.withInternalIDs(dsInfo.getSystemID().getInternalID());
                }
            }

            // if direct parent is not identified, need to look it up
            else
            {
                if (parentElt.getEntityType() == EntityType.DATASTREAM ||
                    parentElt.getEntityType() == EntityType.MULTIDATASTREAM)
                {
                    var dataStreamSet = pm.dataStreamHandler.queryCollection(getParentPath(path), q);
                    builder.withInternalIDs(dataStreamSet.isEmpty() ?
                        Long.MAX_VALUE :
                        ((ResourceId)dataStreamSet.iterator().next().getId()).asLong());
                }
            }
        }

        SensorFilterVisitor visitor = new SensorFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);

        return builder.build();
    }


    protected AbstractProcess toSmlProcess(Sensor sensor, String uid)
    {
        // TODO use provided SensorML doc in metadata

        // create simple SensorML instance
        var proc = new SMLHelper().createPhysicalSystem()
            .uniqueID(uid)
            .name(sensor.getName())
            .description(sensor.getDescription())
            .validFrom(OffsetDateTime.now())
            .build();

        // get documentation link if set
        if (sensor.getMetadata() instanceof String)
        {
            CIOnlineResource doc = new GMDFactory().newCIOnlineResource();
            doc.setProtocol(sensor.getEncodingType());
            doc.setLinkage((String)sensor.getMetadata());
            
            DocumentList docList = new SMLHelper().createDocumentList()
                .add(doc)
                .build();
                
            docList.addDocument(doc);
            proc.getDocumentationList().add("sta_metadata", docList);
        }

        return proc;
    }

    public static class SensorMLMetadata
    {
        String uid;
        Instant validTimeBegin;
        Instant validTimeEnd;
    }


    @SuppressWarnings("unchecked")
    protected Sensor toFrostSensor(long internalId, ISystemWithDesc proc, Query q)
    {
        // TODO add full SensorML doc in metadata

        Sensor sensor = new Sensor();
        sensor.setId(new ResourceIdLong(internalId));
        sensor.setName(proc.getName());
        sensor.setDescription(proc.getDescription());
        
        //if (q.getSelect().contains("metadata"))
        {
            AbstractProcess fullDesc = proc.getFullDescription();
            if (fullDesc != null)
            {    
                // add metadata link
                if (!fullDesc.getDocumentationList().isEmpty())
                {
                    DocumentList docList = fullDesc.getDocumentationList().get("sta_metadata");
                    if (!docList.getDocumentList().isEmpty())
                    {
                        CIOnlineResource doc = docList.getDocumentList().get(0);
                        sensor.setEncodingType(doc.getProtocol());
                        sensor.setMetadata(doc.getLinkage());
                    }
                }
                else
                {
                    var metadata = new SensorMLMetadata();
                    metadata.uid = proc.getUniqueIdentifier();
                    TimeExtent validPeriod = proc.getValidTime();
                    if (validPeriod != null)
                    {
                        metadata.validTimeBegin = validPeriod.begin();
                        metadata.validTimeEnd = validPeriod.hasEnd() ? validPeriod.end() :
                            Instant.now().truncatedTo(ChronoUnit.SECONDS);
                    }
                    sensor.setMetadata(metadata);
                    sensor.setEncodingType(FORMAT_SML2);
                }
            }
        }

        // expand navigation links
        if (q != null && q.getExpand() != null)
        {
            for (Expand exp: q.getExpand())
            {
                NavigationProperty prop = exp.getPath().get(0);
                if (prop == NavigationProperty.DATASTREAMS)
                {
                    ResourcePath linkedPath = FrostUtils.getNavigationLinkPath(sensor.getId(), EntityType.SENSOR, EntityType.DATASTREAM);
                    EntitySet<?> linkedEntities = pm.dataStreamHandler.queryCollection(linkedPath, exp.getSubQuery());
                    sensor.setDatastreams((EntitySet<Datastream>)linkedEntities);
                }

                if (prop == NavigationProperty.MULTIDATASTREAMS)
                {
                    ResourcePath linkedPath = FrostUtils.getNavigationLinkPath(sensor.getId(), EntityType.SENSOR, EntityType.MULTIDATASTREAM);
                    EntitySet<?> linkedEntities = pm.dataStreamHandler.queryCollection(linkedPath, exp.getSubQuery());
                    sensor.setMultiDatastreams((EntitySet<MultiDatastream>)linkedEntities);
                }
            }
        }

        return sensor;
    }


    protected ResourceId handleSensorAssoc(Sensor sensor) throws NoSuchEntityException
    {
        Asserts.checkArgument(sensor != null, MISSING_ASSOC);
        ResourceId sensorId;

        if (sensor.getName() == null)
        {
            sensorId = (ResourceId)sensor.getId();
            Asserts.checkArgument(sensorId != null, MISSING_ASSOC);
            checkSensorIDInWriteStore(sensorId.asLong());
        }
        else
        {
            // deep insert
            sensorId = create(sensor);
        }

        return sensorId;
    }


    /*
     * Check that sensorID is present in database and exposed by service
     */
    protected void checkSensorID(long publicID) throws NoSuchEntityException
    {
        if (procReadStore.getCurrentVersionKey(publicID) == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicID);
    }
    
    
    /*
     * Check that sensorID is present in writable database
     */
    protected void checkSensorIDInWriteStore(long publicID) throws NoSuchEntityException
    {
        checkSensorID(publicID);
        checkSensorWritable(publicID);
    }


    protected void checkSensorWritable(long publicID)
    {
        checkTransactionsEnabled();
        
        // TODO also check that current user has the right to write this sensor!

        if (!pm.isInWritableDatabase(publicID))
            throw new IllegalArgumentException(NOT_WRITABLE_MESSAGE + publicID);
    }
    
    
    protected void checkTransactionsEnabled()
    {
        if (procWriteStore == null)
            throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }

}
