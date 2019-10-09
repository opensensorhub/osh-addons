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
import java.util.List;
import java.util.stream.Collectors;
import org.isotc211.v2005.gmd.CIOnlineResource;
import org.isotc211.v2005.gmd.impl.GMDFactory;
import org.sensorhub.api.datastore.DataStreamFilter;
import org.sensorhub.api.datastore.DataStreamInfo;
import org.sensorhub.api.datastore.FeatureId;
import org.sensorhub.api.datastore.FeatureKey;
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
    OSHPersistenceManager pm;
    IProcedureDescriptionStore procReadStore;
    IProcedureDescriptionStore procWriteStore;
    STASecurity securityHandler;
    int maxPageSize = 100;
    String groupUID;
    
    
    SensorEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.procReadStore = pm.obsDbRegistry.getProcedureStore();
        this.procWriteStore = pm.obsDatabase != null ? pm.obsDatabase.getProcedureStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.groupUID = pm.service.getProcedureGroupUID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity)
    {
        securityHandler.checkPermission(securityHandler.sta_insert_sensor);
        Asserts.checkArgument(entity instanceof Sensor);
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
            
            // parse attached data streams if any
            for (Datastream ds: sensor.getDatastreams())
                pm.dataStreamHandler.addDatastream(sensorID, proxy, ds);
            for (MultiDatastream ds: sensor.getMultiDatastreams())
                pm.dataStreamHandler.addDatastream(sensorID, proxy, ds);
            
            // register new sensor
            FeatureId procID = pm.procRegistry.register(proxy);
            return new ResourceId(procID.getInternalID());
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Sensor with name " + sensor.getName() + " already exists");
        }        
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity)
    {
        Asserts.checkArgument(entity instanceof Sensor);
        securityHandler.checkPermission(securityHandler.sta_update_sensor);
        
        // retrieve existing proxy from registry
        ResourceId id = (ResourceId)entity.getId();
        VirtualSensorProxy proxy = getProcedureProxy(id.internalID);
        
        // update description
        proxy.updateDescription(toSmlProcess((Sensor)entity, proxy.getUniqueIdentifier()));
        
        // also update in datastore
        if (procWriteStore != null)
            procWriteStore.addVersion(proxy.getCurrentDescription());
        
        return true;
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch)
    {
        securityHandler.checkPermission(securityHandler.sta_update_sensor);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id)
    {
        securityHandler.checkPermission(securityHandler.sta_delete_sensor);
        
        // retrieve existing proxy from registry
        VirtualSensorProxy proc = getProcedureProxy(id.internalID);
                
        // remove from registry
        proc.delete();
        
        // also delete procedure and all attached datastreams from DB
        if (procWriteStore != null)
        {
            FeatureKey procKey = procWriteStore.remove(proc.getUniqueIdentifier());
            pm.obsDbRegistry.getObservationStore().getDataStreams().removeEntries(DataStreamFilter.builder()
                .withProcedures(procKey.getInternalID())
                .build());
        }
        
        return true;
    }
    

    @Override
    public Sensor getById(ResourceId id, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_sensor);
        
        Asserts.checkArgument(id.internalID > 0, "IDs must be > 0");
        
        FeatureKey key = FeatureKey.builder()
            .withInternalID(id.internalID)
            .withLatestValidTime()
            .build();
        
        AbstractProcess proc = procReadStore.get(key);
        return proc != null ? toFrostSensor(id.internalID, proc, q) : null;
    }
    

    @Override
    public EntitySet<Sensor> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_sensor);
        
        ProcedureFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);
        
        return procReadStore.selectEntries(filter)
            .peek(e -> System.out.println(e.getValue().getUniqueIdentifier() + ": " + e.getValue().getValidTime()))
            .skip(skip)
            .limit(limit)
            .map(e -> toFrostSensor(e.getKey().getInternalID(), e.getValue(), q))
            .collect(Collectors.toCollection(EntitySetImpl::new));
    }
    
    
    protected ProcedureFilter getFilter(ResourcePath path, Query q)
    {
        ProcedureFilter.Builder builder = ProcedureFilter.builder()
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
                DataStreamInfo dsInfo = pm.obsDbRegistry.getObservationStore().getDataStreams().get(dsId.internalID);
                builder.withInternalIDs(dsInfo.getProcedure().getInternalID());
            }
            else if (idElt.getEntityType() == EntityType.OBSERVATION)
            {
                ObsResourceId obsId = (ObsResourceId)idElt.getId();
                DataStreamInfo dsInfo = pm.obsDbRegistry.getObservationStore().getDataStreams().get(obsId.dataStreamID);
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
        }
        
        // expand navigation links
        List<Expand> expand = q.getExpand();
        if (!expand.isEmpty())
        {
            for (Expand exp: expand)
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
    
    
    protected VirtualSensorProxy getProcedureProxy(long internalID)
    {
        Asserts.checkArgument(internalID > 0, "IDs must be > 0");
        
        FeatureKey key = FeatureKey.builder()
            .withInternalID(internalID)
            .build();
        
        FeatureId procID = procReadStore.getFeatureID(key);
        if (procID == null)
            throw new IllegalArgumentException("Sensor " + internalID + " not found");
        
        return getProcedureProxy(procID.getUniqueID());
    }
    
    
    protected VirtualSensorProxy getProcedureProxy(String uniqueID)
    {
        IProcedureWithState proxy = pm.procRegistry.get(uniqueID);
        if (proxy == null)
        {
            // retrieve description from database
            AbstractProcess sml = procReadStore.getLatestVersion(uniqueID);
            
            // create and register new proxy
            proxy = new VirtualSensorProxy(sml, groupUID);
            pm.procRegistry.register(proxy);
        }
        
        if (!(proxy instanceof VirtualSensorProxy))
            throw new IllegalArgumentException("Sensor " + uniqueID + " cannot be modified");
        
        return (VirtualSensorProxy)proxy;
    }

}
