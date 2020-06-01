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
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.FoiFilter;
import org.sensorhub.api.datastore.IFoiStore;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.ogc.gml.IGeoFeature;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.base.Strings;
import de.fraunhofer.iosb.ilt.frostserver.model.FeatureOfInterest;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySetImpl;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.util.NoSuchEntityException;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;


/**
 * <p>
 * Handler for FeatureOfInterest resources
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class FoiEntityHandler implements IResourceHandler<FeatureOfInterest>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find 'FeatureOfInterest' entity with ID #";
    static final String NOT_WRITABLE_MESSAGE = "Cannot modify read-only 'FeatureOfInterest' entity #";
        
    OSHPersistenceManager pm;
    IFoiStore foiReadStore;
    IFoiStore foiWriteStore;
    STASecurity securityHandler;
    int maxPageSize = 100;
    String groupUID;
    
    
    FoiEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.foiReadStore = pm.obsDbRegistry.getFederatedObsDatabase().getFoiStore();
        this.foiWriteStore = pm.database != null ? pm.database.getFoiStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.groupUID = pm.service.getProcedureGroupUID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_foi);
        Asserts.checkArgument(entity instanceof FeatureOfInterest);
        FeatureOfInterest foi = (FeatureOfInterest)entity;
        
        // generate unique ID from name
        Asserts.checkArgument(!Strings.isNullOrEmpty(foi.getName()), "Feature name must be set");
        String uid = groupUID + ":foi:" + foi.getName().toLowerCase().replaceAll("\\s+", "_");
        
        // store feature description in DB
        if (foiWriteStore != null)
        {
            FeatureKey key = foiWriteStore.add(toGmlFeature(foi, uid));
            return new ResourceIdLong(pm.toPublicID(key.getInternalID()));
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_foi);
        Asserts.checkArgument(entity instanceof FeatureOfInterest);
        FeatureOfInterest foi = (FeatureOfInterest)entity;
        
        // retrieve UID of existing feature
        ResourceId id = (ResourceId)entity.getId();
        var fEntry = foiReadStore.getLatestVersionEntry(id.asLong());
        if (fEntry == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
                
        // store feature description in DB
        if (foiWriteStore != null)
        {
            var key = fEntry.getKey();
            var uid = fEntry.getValue().getUniqueIdentifier();
            foiWriteStore.put(key, toGmlFeature(foi, uid));
            return true;
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_foi);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_delete_foi);
        
        if (foiWriteStore != null)
        {
            var key = foiWriteStore.removeEntries(new FoiFilter.Builder()
                    .withInternalIDs(pm.toLocalID(id.asLong()))
                    .withAllVersions()
                    .build())
                .findFirst();
            
            if (key.isEmpty())
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return true;
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }
    

    @Override
    public FeatureOfInterest getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_foi);
        
        var foi = isFoiVisible(id.asLong()) ? foiReadStore.getLatestVersion(id.asLong()) : null;
        
        if (foi == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        else
            return toFrostFoi(id.asLong(), foi, q);
    }
    

    @Override
    public EntitySet<FeatureOfInterest> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_foi);
        
        FoiFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);
        
        var entitySet = foiReadStore.selectEntries(filter)
            .filter(e -> isFoiVisible(e.getKey().getInternalID()))
            .skip(skip)
            .limit(limit+1) // request limit+1 elements to handle paging
            .map(e -> toFrostFoi(e.getKey().getInternalID(), e.getValue(), q))
            .collect(Collectors.toCollection(EntitySetImpl::new));
        
        return FrostUtils.handlePaging(entitySet, path, q, limit);
    }
    
    
    protected FoiFilter getFilter(ResourcePath path, Query q)
    {
        FoiFilter.Builder builder = new FoiFilter.Builder()
            .validAtTime(Instant.now());
        
        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.OBSERVATION)
            {
                ResourceIdBigInt obsId = (ResourceIdBigInt)idElt.getId();
                builder.withObservations()
                    .withInternalIDs(obsId.internalID)
                    .done();
            }
        }
        
        /*SensorFilterVisitor visitor = new SensorFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);*/
        
        return builder.build();
    }
    
    
    protected AbstractFeature toGmlFeature(FeatureOfInterest foi, String uid)
    {
        Asserts.checkArgument(GEOJSON_FORMAT.equals(foi.getEncodingType()), "Unsupported feature format: %s", foi.getEncodingType());
        GeoJsonObject geojson = (GeoJsonObject)foi.getFeature();
        
        AbstractFeature f;
        if (geojson != null)
            f = FrostUtils.toSamplingFeature(geojson);
        else
            f = new GenericFeatureImpl(new QName("Feature"));
        
        f.setUniqueIdentifier(uid);
        f.setName(foi.getName());
        f.setDescription(foi.getDescription());
        return f;
    }
    
    
    protected FeatureOfInterest toFrostFoi(long internalId, IGeoFeature f, Query q)
    {
        // TODO implement select and expand
        //Set<Property> select = q != null ? q.getSelect() : Collections.emptySet();
        
        FeatureOfInterest foi = new FeatureOfInterest();
        foi.setId(new ResourceIdLong(internalId));
        foi.setName(f.getName());
        foi.setDescription(f.getDescription());
        foi.setEncodingType(GEOJSON_FORMAT);
        
        AbstractGeometry geom = f.getGeometry();
        if (geom != null)
        {
            /*Feature geojson = new Feature();
            geojson.setGeometry(FrostUtils.toGeoJsonGeom(geom));
            foi.setFeature(geojson);*/
            foi.setFeature(FrostUtils.toGeoJsonGeom(geom));
        }
        
        return foi;
    }
    
    
    /*
     * Check that foiID is present in database and exposed by service
     */
    protected void checkFoiID(long publicID) throws NoSuchEntityException
    {
        boolean hasFoi = isFoiVisible(publicID) && foiReadStore.getLatestVersionKey(publicID) != null;
        if (!hasFoi)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicID);
    }
    
    
    protected boolean isFoiVisible(long publicID)
    {
        // TODO also check that current user has the right to read this procedure!
        
        // TODO check that feature is either in writable database 
        // or associated with a visible sensor
        
        return true;
    }
    
    
    protected void checkFoiWritable(long publicID)
    {
        // TODO also check that current user has the right to write this procedure!
        
        if (!(pm.obsDbRegistry.getDatabaseID(publicID) == pm.database.getDatabaseID()))
            throw new IllegalArgumentException(NOT_WRITABLE_MESSAGE + publicID);
    }

}
