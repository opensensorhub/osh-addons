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
import org.sensorhub.api.datastore.FeatureId;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.FoiFilter;
import org.sensorhub.api.datastore.IFoiStore;
import org.vast.ogc.gml.GenericFeatureImpl;
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
            return new ResourceId(pm.toPublicID(key.getInternalID()));
        }
        
        throw new UnsupportedOperationException("Cannot insert new features if no database was configured");
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_foi);
        Asserts.checkArgument(entity instanceof FeatureOfInterest);
        FeatureOfInterest foi = (FeatureOfInterest)entity;
        
        // retrieve UID of existing feature
        ResourceId id = (ResourceId)entity.getId();
        FeatureId fid = foiReadStore.getFeatureID(new FeatureKey(id.internalID));
        if (fid == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
                
        // store feature description in DB
        String uid = fid.getUniqueID();
        if (foiWriteStore != null)
        {
            //foiWriteStore.addVersion(toGmlFeature(foi, uid));
            foiWriteStore.put(
                new FeatureKey(pm.toLocalID(fid.getInternalID()), uid, Instant.EPOCH),
                toGmlFeature(foi, uid));
            return true;
        }
        
        return false;
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
            var key = new FeatureKey(pm.toLocalID(id.internalID));
            var f = foiWriteStore.remove(key);
            
            if (f == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return true;
        }
        
        return false;
    }
    

    @Override
    public FeatureOfInterest getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_foi);
        
        var foi = foiReadStore.get(new FeatureKey(id.internalID));
        
        if (foi == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        else
            return toFrostFoi(id.internalID, foi, q);
    }
    

    @Override
    public EntitySet<FeatureOfInterest> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_foi);
        
        FoiFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);
        
        var entitySet = foiReadStore.selectEntries(filter)
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
                ObsResourceId obsId = (ObsResourceId)idElt.getId();
                if (obsId.foiID == 0) // case of no FOI
                    obsId.foiID = Long.MAX_VALUE;
                builder.withInternalIDs(obsId.foiID);
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
    
    
    protected FeatureOfInterest toFrostFoi(long internalId, AbstractFeature f, Query q)
    {
        // TODO implement select and expand
        //Set<Property> select = q != null ? q.getSelect() : Collections.emptySet();
        
        FeatureOfInterest foi = new FeatureOfInterest();
        foi.setId(new ResourceId(internalId));
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

}
