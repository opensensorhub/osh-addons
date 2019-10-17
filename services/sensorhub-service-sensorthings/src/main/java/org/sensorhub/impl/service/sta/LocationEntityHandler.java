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
import javax.xml.namespace.QName;
import org.geojson.GeoJsonObject;
import org.sensorhub.api.datastore.FeatureFilter;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.vast.ogc.gml.GenericFeature;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.base.Strings;
import de.fraunhofer.iosb.ilt.frostserver.model.Location;
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
public class LocationEntityHandler implements IResourceHandler<Location>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find 'Location' entity with ID #";
    static final String MISSING_ASSOC = "Missing reference to 'Location' entity";
        
    OSHPersistenceManager pm;
    ILocationStore locationDataStore;
    IHistoricalObsDatabase federatedDatabase;
    STASecurity securityHandler;
    int maxPageSize = 100;
    String groupUID;
    
    
    LocationEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.federatedDatabase = pm.obsDbRegistry.getFederatedObsDatabase();
        this.locationDataStore = pm.database != null ? pm.database.getThingLocationStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.groupUID = pm.service.getProcedureGroupUID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_location);
        Asserts.checkArgument(entity instanceof Location);
        Location location = (Location)entity;
        
        // generate unique ID from name
        Asserts.checkArgument(!Strings.isNullOrEmpty(location.getName()), "Location name must be set");
        String uid = groupUID + ":location:" + location.getName().toLowerCase().replaceAll("\\s+", "_");
        
        // store feature description in DB
        if (locationDataStore != null)
        {
            FeatureKey key = locationDataStore.add(toGmlFeature(location, uid));
            return new ResourceId(key.getInternalID());
        }
        
        throw new UnsupportedOperationException("Cannot insert new locations if no database was configured");
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_location);
        Asserts.checkArgument(entity instanceof Location);
        Location location = (Location)entity;
        ResourceId id = (ResourceId)entity.getId();
        
        if (locationDataStore != null)
        {
            // retrieve UID of existing feature
            var fid = locationDataStore.getFeatureID(new FeatureKey(id.internalID));
            if (fid == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
                
            // store feature description in DB
            String uid = fid.getUniqueID();
            var key = new FeatureKey(fid.getInternalID(), uid, Instant.EPOCH);
            locationDataStore.put(key, toGmlFeature(location, uid));
            return true;
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
            var f = locationDataStore.remove(new FeatureKey(id.internalID));
            if (f == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return true;
        }
        
        return false;
    }
    

    @Override
    public Location getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_location);
        
        if (locationDataStore != null)
        {
            var location = locationDataStore.get(new FeatureKey(id.internalID));
            if (location == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return toFrostLocation(id.internalID, location, q);
        }
        
        return null;
    }
    

    @Override
    public EntitySet<Location> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_location);
        
        if (locationDataStore != null)
        {
            FeatureFilter filter = getFilter(path, q);
            int skip = q.getSkip(0);
            int limit = Math.min(q.getTopOrDefault(), maxPageSize);
            
            var entitySet = locationDataStore.selectEntries(filter)
                .skip(skip)
                .limit(limit+1) // request limit+1 elements to handle paging
                .map(e -> toFrostLocation(e.getKey().getInternalID(), e.getValue(), q))
                .collect(Collectors.toCollection(EntitySetImpl::new));
            
            return FrostUtils.handlePaging(entitySet, path, q, limit);
        }
        
        return null;
    }
    
    
    protected FeatureFilter getFilter(ResourcePath path, Query q)
    {
        var builder = new STALocationFilter.Builder()
            .validAtTime(Instant.now());
        
        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.THING)
            {
                ResourceId thingId = (ResourceId)idElt.getId();
                builder.withThings(thingId.internalID)
                    .withLatestVersion();
            }
        }
        
        /*SensorFilterVisitor visitor = new SensorFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);*/
        
        return builder.build();
    }
    
    
    protected AbstractFeature toGmlFeature(Location location, String uid)
    {
        Asserts.checkArgument(GEOJSON_FORMAT.equals(location.getEncodingType()),
            "Unsupported location format: %s", location.getEncodingType());
        GeoJsonObject geojson = (GeoJsonObject)location.getLocation();
        
        GenericFeature f = new GenericFeatureImpl(new QName("Location"));
        f.setUniqueIdentifier(uid);
        f.setName(location.getName());
        f.setDescription(location.getDescription());
        if (geojson != null)
            f.setGeometry(FrostUtils.toGmlGeometry(geojson));        
        return f;
    }
    
    
    protected Location toFrostLocation(long internalId, AbstractFeature f, Query q)
    {
        Location location = new Location();
        location.setId(new ResourceId(internalId));
        location.setName(f.getName());
        location.setDescription(f.getDescription());
        location.setEncodingType(GEOJSON_FORMAT);
        if (f.isSetGeometry())
            location.setLocation(FrostUtils.toGeoJsonGeom(f.getGeometry()));        
        return location;
    }
    
    
    protected void handleLocationAssoc(long thingID, EntitySet<Location> locations) throws NoSuchEntityException
    {
        if (locations == null)
            return;
        
        ResourceId locationId;
        Instant now = Instant.now();
        for (Location location: locations)
        {        
            if (location.getName() == null)
            {
                locationId = (ResourceId)location.getId();
                Asserts.checkArgument(locationId != null, MISSING_ASSOC);
                checkLocationID(locationId.internalID);
            }
            else
            {
                // deep insert
                locationId = create(location);
            }
            
            locationDataStore.addAssociation(thingID, locationId.internalID, now);
        }
    }
    
    
    protected void checkLocationID(long locationID) throws NoSuchEntityException
    {
        boolean hasLocation = locationDataStore.containsKey(new FeatureKey(locationID));        
        if (!hasLocation)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + locationID);
    }

}
