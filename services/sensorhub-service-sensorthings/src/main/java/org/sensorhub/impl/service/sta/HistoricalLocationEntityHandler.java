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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.stream.Collectors;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.impl.service.sta.filter.HistoricalLocationFilterVisitor;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import de.fraunhofer.iosb.ilt.frostserver.model.HistoricalLocation;
import de.fraunhofer.iosb.ilt.frostserver.model.Thing;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySetImpl;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInstant;
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
public class HistoricalLocationEntityHandler implements IResourceHandler<HistoricalLocation>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find HistoricalLocation ";
    static final String MISSING_ASSOC = "Missing reference to HistoricalLocation";
        
    OSHPersistenceManager pm;
    ISTALocationStore locationDataStore;
    STASecurity securityHandler;
    int idScope;
    int maxPageSize = 100;
    
    
    static class LocationId
    {
        BigId thingID;
        long time;
        
        LocationId(BigId thingID, long time)
        {
            this.thingID = thingID;
            this.time = time;
        }
    }
    
    
    HistoricalLocationEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.idScope = pm.writeDatabase.getDatabaseNum() != null ? pm.writeDatabase.getDatabaseNum() : 0;
        this.locationDataStore = pm.writeDatabase != null ? pm.writeDatabase.getThingLocationStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_location);
        Asserts.checkArgument(entity instanceof HistoricalLocation);
        //HistoricalLocation location = (HistoricalLocation)entity;
        
        // store feature description in DB
        if (locationDataStore != null)
        {
            
        }
        
        throw new UnsupportedOperationException("Cannot insert new historical locations");
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_location);
        Asserts.checkArgument(entity instanceof HistoricalLocation);
        //HistoricalLocation location = (HistoricalLocation)entity;
        ResourceId id = (ResourceId)entity.getId();
        
        if (locationDataStore != null)
        {
            throw new UnsupportedOperationException("Cannot update historical locations");
        }
        
        throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_location);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_delete_location);
        
        if (locationDataStore != null)
        {
            var f = locationDataStore.remove(new FeatureKey(id));
            if (f == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return true;
        }
        
        return false;
    }
    

    @Override
    public HistoricalLocation getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_location);
        
        if (locationDataStore != null)
        {
            var locId = parseId(id);
            
            var filter = new STALocationFilter.Builder()
                .withThings(locId.thingID)
                .validAtTime(Instant.ofEpochSecond(locId.time))
                .build();
            
            return locationDataStore.selectHistoricalLocations(filter)
                .findFirst()
                .map(k -> toFrostHistoricalLocation(k.getThingID(), k.getTime(), q))
                .orElse(null);
        }
        
        return null;
    }
    

    @Override
    public EntitySet<HistoricalLocation> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_thing);
        
        if (locationDataStore != null)
        {
            STALocationFilter filter = getFilter(path, q);
            int skip = q.getSkip(0);
            int limit = Math.min(q.getTopOrDefault(), maxPageSize);
            
            var entitySet = locationDataStore.selectHistoricalLocations(filter)
                .skip(skip)
                .limit(limit+1) // request limit+1 elements to handle paging
                .map(k -> toFrostHistoricalLocation(k.getThingID(), k.getTime(), q))
                .collect(Collectors.toCollection(EntitySetImpl::new));
            
            return FrostUtils.handlePaging(entitySet, path, q, limit);
        }
        
        return null;
    }
    
    
    protected STALocationFilter getFilter(ResourcePath path, Query q)
    {
        var builder = new STALocationFilter.Builder()
            .withAllVersions();
        
        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.THING)
            {
                ResourceId thingId = (ResourceId)idElt.getId();
                builder.withThings(thingId);
            }
            else if (idElt.getEntityType() == EntityType.LOCATION)
            {
                ResourceId locId = (ResourceId)idElt.getId();
                builder.withInternalIDs(locId);
            }
        }
        
        HistoricalLocationFilterVisitor visitor = new HistoricalLocationFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);
        
        return builder.build();
    }
    
    
    protected HistoricalLocation toFrostHistoricalLocation(BigId thingID, Instant time, Query q)
    {
        HistoricalLocation location = new HistoricalLocation();
        location.setId(new ResourceBigId(generateId(thingID, time)));
        location.setTime(TimeInstant.create(time.toEpochMilli()));
        
        Thing thing = new Thing(new ResourceBigId(thingID));
        thing.setExportObject(false);
        location.setThing(thing);
        
        return location;
    }
    
    
    protected BigId generateId(BigId thingID, Instant time)
    {
        WriteBuffer buf = new WriteBuffer(20); // thingID + timestamp seconds
        DataUtils.writeVarLong(buf.getBuffer(), thingID.getIdAsLong());
        DataUtils.writeVarLong(buf.getBuffer(), time.getEpochSecond());
        return BigId.fromBytes(idScope, buf.getBuffer().array(), 0, buf.position());
    }
    
    
    protected LocationId parseId(BigId id)
    {
        ByteBuffer buf = ByteBuffer.wrap(id.getIdAsBytes());
        var thingID = BigId.fromLong(idScope, DataUtils.readVarLong(buf));
        long time = DataUtils.readVarLong(buf);
        return new LocationId(thingID, time);
    }

}
