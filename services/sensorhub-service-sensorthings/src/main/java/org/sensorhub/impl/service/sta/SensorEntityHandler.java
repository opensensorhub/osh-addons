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

import java.time.Instant;
import java.util.stream.Collectors;
import org.isotc211.v2005.gmd.CIOnlineResource;
import org.isotc211.v2005.gmd.impl.GMDFactory;
import org.sensorhub.api.datastore.DataStreamFilter;
import org.sensorhub.api.datastore.DataStreamInfo;
import org.sensorhub.api.datastore.FeatureId;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.sensorhub.api.datastore.ProcedureFilter;
import org.sensorhub.api.procedure.IProcedureDescriptionStore;
import org.sensorhub.api.procedure.IProcedureWithState;
import org.sensorhub.impl.sensor.VirtualSensorProxy;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.base.Strings;
import com.google.common.collect.Range;
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
    static final String NOT_FOUND_MESSAGE = "Cannot find 'Sensor' entity with ID #";
    static final String NOT_WRITABLE_MESSAGE = "Cannot modify read-only 'Sensor' entity #";
    static final String MISSING_ASSOC = "Missing reference to 'Sensor' entity";
    static final String FORMAT_SML2 = "vhttp://www.opengis.net/sensorml-json/2.0";
    
    OSHPersistenceManager pm;
    IProcedureDescriptionStore procReadStore;
    IProcedureDescriptionStore procWriteStore;
    IHistoricalObsDatabase federatedDatabase;
    STASecurity securityHandler;
    int maxPageSize = 100;
    String groupUID;
    
    
    SensorEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.federatedDatabase = pm.obsDbRegistry.getFederatedObsDatabase();
        this.procReadStore = federatedDatabase.getProcedureStore();
        this.procWriteStore = pm.database != null ? pm.database.getProcedureStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.groupUID = pm.service.getProcedureGroupUID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        Asserts.checkArgument(entity instanceof Sensor);        
        securityHandler.checkPermission(securityHandler.sta_insert_sensor);
        Sensor sensor = (Sensor)entity;
        
        // generate unique ID from name
        Asserts.checkArgument(!Strings.isNullOrEmpty(sensor.getName()), "Sensor name must be set");
        String uid = groupUID + ":" + sensor.getName().toLowerCase().replaceAll("\\s+", "_");
        
        // create new sensor
        VirtualSensorProxy proxy = new VirtualSensorProxy(
            toSmlProcess(sensor, uid),
            groupUID);
        
        try
        {
            // store description in DB
            Long sensorID = null;
            if (procWriteStore != null)
            {
                FeatureKey key = procWriteStore.add(proxy.getCurrentDescription());
                sensorID = key.getInternalID();
            }
            
            // register new sensor
            FeatureId procID = pm.procRegistry.register(proxy);
            ResourceId newSensorId = new ResourceId(procID.getInternalID());
            
            // handle associations / deep inserts
            pm.dataStreamHandler.handleDatastreamAssocList(newSensorId, sensor);
            
            return newSensorId;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Sensor with name '" + sensor.getName() + "' already exists");
        }        
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        Asserts.checkArgument(entity instanceof Sensor);
        securityHandler.checkPermission(securityHandler.sta_update_sensor);
        Sensor sensor = (Sensor)entity;
        
        // retrieve existing proxy from registry
        ResourceId id = (ResourceId)entity.getId();
        checkProcedureWritable(id.internalID);
        VirtualSensorProxy proxy = getProcedureProxy(id.internalID);
        
        // update description
        proxy.updateDescription(toSmlProcess((Sensor)entity, proxy.getUniqueIdentifier()));
        
        // also update in datastore
        if (procWriteStore != null)
        {
            procWriteStore.addVersion(proxy.getCurrentDescription());
            
            // handle associations / deep inserts
            pm.dataStreamHandler.handleDatastreamAssocList(id, sensor);
        }
        
        return true;
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_sensor);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_delete_sensor);
        
        // retrieve existing proxy from registry
        checkProcedureWritable(id.internalID);
        VirtualSensorProxy proc = getProcedureProxy(id.internalID);
                
        // remove from registry
        proc.delete();
        
        // delete procedure and all attached datastreams from DB
        if (procWriteStore != null)
        {
            FeatureKey procKey = procWriteStore.remove(proc.getUniqueIdentifier());
            pm.database.getObservationStore().getDataStreams().removeEntries(new DataStreamFilter.Builder()
                .withProcedures(procKey.getInternalID())
                .withAllVersions()
                .build());
        }
        
        return true;
    }
    

    @Override
    public Sensor getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_sensor);
        
        var key = FeatureKey.latest(id.internalID);
        var proc = isSensorVisible(key) ? procReadStore.get(key) : null;
        
        if (proc == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        else
            return toFrostSensor(id.internalID, proc, q);
    }
    

    @Override
    public EntitySet<Sensor> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_sensor);
        
        ProcedureFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);
        
        var entitySet = procReadStore.selectEntries(filter)
            .filter(e -> isSensorVisible(e.getKey()))
            //.peek(e -> System.out.println(e.getValue().getUniqueIdentifier() + ": " + e.getValue().getValidTime()))
            .skip(skip)
            .limit(limit+1) // request limit+1 elements to handle paging
            .map(e -> toFrostSensor(e.getKey().getInternalID(), e.getValue(), q))
            .collect(Collectors.toCollection(EntitySetImpl::new));
        
        return FrostUtils.handlePaging(entitySet, path, q, limit);
    }
    
    
    protected ProcedureFilter getFilter(ResourcePath path, Query q)
    {
        ProcedureFilter.Builder builder = new ProcedureFilter.Builder()
            .validAtTime(Instant.now());
            //.withValidTimeDuring(Instant.MIN, Instant.MAX);
        
        String groupUID = pm.service.getProcedureGroupUID();
        if (groupUID != null)
            builder.withParentGroups(groupUID);
        
        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.DATASTREAM ||
                idElt.getEntityType() == EntityType.MULTIDATASTREAM)
            {
                ResourceId dsId = (ResourceId)idElt.getId();
                DataStreamInfo dsInfo = federatedDatabase.getObservationStore().getDataStreams().get(dsId.internalID);
                builder.withInternalIDs(dsInfo.getProcedure().getInternalID());
            }
            else if (idElt.getEntityType() == EntityType.OBSERVATION)
            {
                CompositeResourceId obsId = (CompositeResourceId)idElt.getId();
                long dataStreamID = obsId.parentIDs[0];
                DataStreamInfo dsInfo = federatedDatabase.getObservationStore().getDataStreams().get(dataStreamID);
                builder.withInternalIDs(dsInfo.getProcedure().getInternalID());
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
        SMLHelper smlHelper = SMLHelper.createPhysicalSystem(uid);
        AbstractProcess proc = smlHelper.getDescription();
        proc.setName(sensor.getName());
        proc.setDescription(sensor.getDescription());
        
        // get documentation link if set
        if (sensor.getMetadata() instanceof String)
        {
            DocumentList docList = smlHelper.newDocumentList();
            CIOnlineResource doc = new GMDFactory().newCIOnlineResource();
            doc.setProtocol(sensor.getEncodingType());
            doc.setLinkage((String)sensor.getMetadata());
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
    protected Sensor toFrostSensor(long internalId, AbstractProcess proc, Query q)
    {
        // TODO add full SensorML doc in metadata
                
        Sensor sensor = new Sensor();
        sensor.setId(new ResourceId(internalId));
        sensor.setName(proc.getName());
        sensor.setDescription(proc.getDescription());
        
        // add metadata link
        if (!proc.getDocumentationList().isEmpty())
        {
            DocumentList docList = proc.getDocumentationList().get("sta_metadata");
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
            Range<Instant> validPeriod = proc.getValidTime();
            metadata.validTimeBegin = validPeriod.lowerEndpoint();
            metadata.validTimeEnd = validPeriod.hasUpperBound() ? validPeriod.upperEndpoint() : Instant.now();
            sensor.setMetadata(metadata);
            sensor.setEncodingType(FORMAT_SML2);
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
    
    
    protected VirtualSensorProxy getProcedureProxy(long publicID) throws NoSuchEntityException
    {
        Asserts.checkArgument(publicID > 0, "IDs must be > 0");
        
        var key = new FeatureKey(publicID);        
        var procID = procReadStore.getFeatureID(key);
        if (procID == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicID);
        
        return getProcedureProxy(procID.getUniqueID());
    }
    
    
    protected VirtualSensorProxy getProcedureProxy(String uniqueID) throws NoSuchEntityException
    {
        IProcedureWithState proxy = pm.procRegistry.get(uniqueID);
        if (proxy == null)
        {
            // retrieve description from database
            AbstractProcess sml = procReadStore.getLatestVersion(uniqueID);
            /*IDataStreamStore dsStore = pm.obsDbRegistry.getObservationStore().getDataStreams();
            dsStore.select(new DataStreamFilter.Builder()
                .withProcedures(uniqueID)
                .build());*/
            
            // create and register new proxy
            proxy = new VirtualSensorProxy(sml, groupUID);
            pm.procRegistry.register(proxy);
        }
        
        if (!(proxy instanceof VirtualSensorProxy))
            throw new IllegalArgumentException("Sensor " + uniqueID + " cannot be modified");
        
        return (VirtualSensorProxy)proxy;
    }
    
    
    protected ResourceId handleSensorAssoc(Sensor sensor) throws NoSuchEntityException
    {
        Asserts.checkArgument(sensor != null, MISSING_ASSOC);
        ResourceId sensorId;        
                
        if (sensor.getName() == null)
        {
            sensorId = (ResourceId)sensor.getId();
            Asserts.checkArgument(sensorId != null, MISSING_ASSOC);
            checkSensorID(sensorId.internalID);
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
        var key = new FeatureKey(publicID);
        boolean hasSensor = procReadStore.containsKey(key) && isSensorVisible(key);
        if (!hasSensor)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicID);
    }
    
    
    protected boolean isSensorVisible(FeatureId fid)
    {
        long publicID = fid.getInternalID();
        
        // TODO also check that current user has the right to read this procedure!
        
        return pm.obsDbRegistry.getDatabaseID(publicID) == pm.database.getDatabaseID() ||
            pm.service.isProcedureExposed(fid);
    }
    
    
    protected void checkProcedureWritable(long publicID)
    {
        // TODO also check that current user has the right to write this procedure!
        
        if (!(pm.obsDbRegistry.getDatabaseID(publicID) == pm.database.getDatabaseID()))
            throw new IllegalArgumentException(NOT_WRITABLE_MESSAGE + publicID);
    }

}
