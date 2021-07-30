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
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.procedure.ProcedureId;
import org.sensorhub.impl.service.sta.filter.ThingFilterVisitor;
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
    static final String NOT_FOUND_MESSAGE = "Cannot find Thing ";
    static final String MISSING_ASSOC = "Missing reference to Thing entity";
    
    OSHPersistenceManager pm;
    ISTAThingStore thingDataStore;
    STASecurity securityHandler;
    int maxPageSize = 100;
    ProcedureId procGroupID;
    
    
    ThingEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.thingDataStore = pm.writeDatabase != null ? pm.writeDatabase.getThingStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.procGroupID = pm.service.getProcedureGroupID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_thing);
        Asserts.checkArgument(entity instanceof Thing);
        Thing thing = (Thing)entity;
        
        // generate unique ID from name
        Asserts.checkArgument(!Strings.isNullOrEmpty(thing.getName()), "Thing name must be set");
        
        if (thingDataStore != null)
        {
            try
            {
                return pm.writeDatabase.executeTransaction(() -> {
                    // store feature description in DB
                    FeatureKey key = thingDataStore.add(toGmlFeature(thing, null));
                    var thingId = new ResourceIdLong(key.getInternalID());
                    
                    // handle associations / deep inserts
                    pm.locationHandler.handleLocationAssocList(thingId, thing);
                    pm.dataStreamHandler.handleDatastreamAssocList(thingId, thing);
                    
                    return thingId;
                });
            }
            catch (RuntimeException | NoSuchEntityException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
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
            var key = thingDataStore.getCurrentVersionKey(id.asLong());
            if (key == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
                
            // store feature description in DB
            thingDataStore.put(key, toGmlFeature(thing, null));
            
            // handle associations / deep inserts with locations
            pm.locationHandler.handleLocationAssocList(id, thing);
            pm.dataStreamHandler.handleDatastreamAssocList(id, thing);
            
            return true;
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
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
            var count = thingDataStore.removeEntries(new STAThingFilter.Builder()
                    .withInternalIDs(id.asLong())
                    .withAllVersions()
                    .build());
            
            if (count <= 0)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return true;
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }
    

    @Override
    public Thing getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_thing);
        GenericFeature thing = null;
        
        if (thingDataStore != null)
        {
            thing = thingDataStore.getCurrentVersion(id.asLong());
        }
        else
        {
            if (id.asLong() == STAService.HUB_THING_ID)
                thing = pm.service.hubThing;
        }
        
        if (thing == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        
        return toFrostThing(id.asLong(), thing, q);
    }
    

    @Override
    public EntitySet<Thing> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_thing);
        
        if (thingDataStore != null)
        {
            STAThingFilter filter = getFilter(path, q);
            int skip = q.getSkip(0);
            int limit = Math.min(q.getTopOrDefault(), maxPageSize);
            
            var entitySet = thingDataStore.selectEntries(filter)
                .skip(skip)
                .limit(limit+1) // request limit+1 elements to handle paging
                .map(e -> toFrostThing(e.getKey().getInternalID(), e.getValue(), q))
                .collect(Collectors.toCollection(EntitySetImpl::new));
            
            return FrostUtils.handlePaging(entitySet, path, q, limit);
        }
        else
        {
             var set = new EntitySetImpl<Thing>();
             set.add(toFrostThing(STAService.HUB_THING_ID, pm.service.hubThing, q));
             return set;
        }
    }
    
    
    protected STAThingFilter getFilter(ResourcePath path, Query q)
    {
        var builder = new STAThingFilter.Builder()
            .validAtTime(Instant.now());
        
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
                    Long thingID = pm.writeDatabase.getDataStreamStore().getAssociatedThing(pm.toLocalID(dsId.asLong()));
                    builder.withInternalIDs(thingID != null ? thingID : STAService.HUB_THING_ID);
                }
                else if (idElt.getEntityType() == EntityType.HISTORICALLOCATION)
                {
                    /*CompositeResourceId historyLocId = (CompositeResourceId)idElt.getId();
                    long thingID = historyLocId.parentIDs[0];
                    builder.withInternalIDs(thingID);*/
                }
                else if (idElt.getEntityType() == EntityType.LOCATION)
                {
                    ResourceId locId = (ResourceId)idElt.getId();
                    builder.withLocations(locId.asLong());
                }
            }
            
            // if direct parent is not identified, need to look it up
            else
            {
                if (parentElt.getEntityType() == EntityType.DATASTREAM ||
                    parentElt.getEntityType() == EntityType.MULTIDATASTREAM)
                {
                    var dataStreamSet = pm.dataStreamHandler.queryCollection(getParentPath(path), q);
                    
                    Long thingID = null;
                    if (!dataStreamSet.isEmpty())
                    {
                        long dataStreamID = ((ResourceId)dataStreamSet.asList().get(0).getId()).asLong();
                        thingID = pm.writeDatabase.getDataStreamStore().getAssociatedThing(pm.toLocalID(dataStreamID));
                    }
                    
                    builder.withInternalIDs(thingID != null ? thingID : STAService.HUB_THING_ID);
                }
            }
        }
        
        ThingFilterVisitor visitor = new ThingFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);
        
        return builder.build();
    }
    
    
    protected GenericFeature toGmlFeature(Thing thing, String uid)
    {
        GenericFeature f = new GenericFeatureImpl(new QName("Thing"));
        
        f.setUniqueIdentifier(uid);
        f.setName(thing.getName());
        f.setDescription(thing.getDescription());
        
        if (thing.getProperties() != null)
        {
            for (var entry: thing.getProperties().entrySet())
               f.setProperty(entry.getKey(), entry.getValue());
        }
        
        return f;
    }
    
    
    protected Thing toFrostThing(long internalId, GenericFeature f, Query q)
    {
        // TODO implement expand
        //Set<Property> select = q != null ? q.getSelect() : Collections.emptySet();
        
        Thing thing = new Thing();
        
        thing.setId(new ResourceIdLong(internalId));
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
    
    
    protected ResourceId handleThingAssoc(Thing thing) throws NoSuchEntityException
    {
        Asserts.checkArgument(thing != null, MISSING_ASSOC);
        ResourceId thingId;
        
        if (thing.getName() == null)
        {
            thingId = (ResourceId)thing.getId();
            Asserts.checkArgument(thingId != null, MISSING_ASSOC);
            checkThingID(thingId.asLong());
        }
        else
        {
            // deep insert
            thingId = create(thing);
        }
        
        return thingId;
    }
    
    
    protected void checkThingID(long thingID) throws NoSuchEntityException
    {
        boolean hasThing = thingID == STAService.HUB_THING_ID;
        
        if (thingDataStore != null)
            hasThing = thingDataStore.containsKey(new FeatureKey(thingID));
        
        if (!hasThing)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + thingID);
    }

}
