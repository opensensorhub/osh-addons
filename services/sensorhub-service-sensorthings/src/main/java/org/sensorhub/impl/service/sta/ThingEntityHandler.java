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
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.sensorhub.api.datastore.DataStreamInfo;
import org.sensorhub.api.datastore.FeatureFilter;
import org.sensorhub.api.datastore.FeatureId;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IFeatureStore;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.vast.ogc.gml.GenericFeature;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.base.Strings;
import de.fraunhofer.iosb.ilt.frostserver.model.Thing;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySetImpl;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.util.NoSuchEntityException;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Handler for Thing resources
 * </p>
 *
 * @author Alex Robin
 * @date Oct 11, 2019
 */
public class ThingEntityHandler implements IResourceHandler<Thing>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find Thing with id #";
    static final String GEOJSON_FORMAT = "application/vnd.geo+json";
        
    OSHPersistenceManager pm;
    IFeatureStore<FeatureKey, GenericFeature> thingDataStore;
    IHistoricalObsDatabase federatedDatabase;
    STASecurity securityHandler;
    int maxPageSize = 100;
    String groupUID;
    
    
    ThingEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.federatedDatabase = pm.obsDbRegistry.getFederatedObsDatabase();
        this.thingDataStore = pm.database != null ? pm.database.getThingStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.groupUID = pm.service.getProcedureGroupUID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_thing);
        Asserts.checkArgument(entity instanceof Thing);
        Thing thing = (Thing)entity;
        
        // generate unique ID from name
        Asserts.checkArgument(!Strings.isNullOrEmpty(thing.getName()), "Thing name must be set");
        String uid = groupUID + ":thing:" + thing.getName().toLowerCase().replaceAll("\\s+", "_");
        
        // store feature description in DB
        if (thingDataStore != null)
        {
            FeatureKey key = thingDataStore.add(toGmlFeature(thing, uid));
            return new ResourceId(key.getInternalID());
        }
        
        throw new UnsupportedOperationException("Cannot insert new features if no database was configured");
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_thing);
        Asserts.checkArgument(entity instanceof Thing);
        Thing thing = (Thing)entity;
        
        if (thingDataStore != null)
        {
            // retrieve UID of existing feature
            ResourceId id = (ResourceId)entity.getId();
            FeatureId fid = thingDataStore.getFeatureID(FeatureKey.builder()
                .withInternalID(id.internalID)
                .build());
            if (fid == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
                
            // store feature description in DB
            String uid = fid.getUniqueID();
            thingDataStore.put(FeatureKey.builder()
                    .withInternalID(fid.getInternalID())
                    .withUniqueID(uid)
                    .build(),
                toGmlFeature(thing, uid));
            return true;
        }
        
        return false;
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_thing);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_delete_thing);
        
        if (thingDataStore != null)
        {
            AbstractFeature f = thingDataStore.remove(FeatureKey.builder()
                .withInternalID(id.internalID)
                .build());
            
            if (f == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return true;
        }
        
        return false;
    }
    

    @Override
    public Thing getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_thing);
        
        if (thingDataStore != null)
        {
            GenericFeature thing = thingDataStore.get(FeatureKey.builder()
                .withInternalID(id.internalID)
                .build());
            
            if (thing == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            else
                return toFrostThing(id.internalID, thing, q);
        }
        
        return null;
    }
    

    @Override
    public EntitySet<Thing> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_thing);
        
        if (thingDataStore != null)
        {
            FeatureFilter filter = getFilter(path, q);
            int skip = q.getSkip(0);
            int limit = Math.min(q.getTopOrDefault(), maxPageSize);
            
            var entitySet = thingDataStore.selectEntries(filter)
                .skip(skip)
                .limit(limit+1) // request limit+1 elements to handle paging
                .map(e -> toFrostThing(e.getKey().getInternalID(), e.getValue(), q))
                .collect(Collectors.toCollection(EntitySetImpl::new));
            
            return FrostUtils.handlePaging(entitySet, path, q, limit);
        }
        
        return null;
    }
    
    
    protected FeatureFilter getFilter(ResourcePath path, Query q)
    {
        var builder = new FeatureFilter.Builder()
            .validAtTime(Instant.now());
        
        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.DATASTREAM ||
                idElt.getEntityType() == EntityType.MULTIDATASTREAM)
            {
                ResourceId dsId = (ResourceId)idElt.getId();
                DataStreamInfo dsInfo = federatedDatabase.getObservationStore().getDataStreams().get(dsId.internalID);
                long thingID = dsInfo instanceof STADataStream ?                    
                    ((STADataStream)dsInfo).getThingID() : STADatabase.HUB_THING_ID;
                builder.withInternalIDs(thingID);
            }
        }
        
        /*SensorFilterVisitor visitor = new SensorFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);*/
        
        return builder.build();
    }
    
    
    protected GenericFeature toGmlFeature(Thing thing, String uid)
    {
        GenericFeature f = new GenericFeatureImpl(new QName("Thing"));
        
        f.setUniqueIdentifier(uid);
        f.setName(thing.getName());
        f.setDescription(thing.getDescription());
        
        for (var entry: thing.getProperties().entrySet())
           f.setProperty(entry.getKey(), entry.getValue());
        
        return f;
    }
    
    
    protected Thing toFrostThing(long internalId, GenericFeature f, Query q)
    {
        // TODO implement expand
        //Set<Property> select = q != null ? q.getSelect() : Collections.emptySet();
        
        Thing thing = new Thing();
        
        thing.setId(new ResourceId(internalId));
        thing.setName(f.getName());
        thing.setDescription(f.getDescription());
        
        if (f.getProperties() != null && !f.getProperties().isEmpty())
        {
            LinkedHashMap<String,Object> props = new LinkedHashMap<>();
            for (var entry: f.getProperties().entrySet())
                props.put(entry.getKey().getLocalPart(), entry.getValue());
            thing.setProperties(props);
        }
        
        return thing;
    }
    
    
    protected void checkThingID(long thingID) throws NoSuchEntityException
    {
        boolean hasThing = thingID == STADatabase.HUB_THING_ID;
        
        if (thingDataStore != null)
        {
            hasThing = thingDataStore.containsKey(FeatureKey.builder()
                .withInternalID(thingID)
                .build());
        }
        if (!hasThing)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + thingID);
    }

}
