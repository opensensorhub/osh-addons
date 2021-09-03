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
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.procedure.ProcedureId;
import org.sensorhub.impl.service.sta.filter.FoiFilterVisitor;
import org.sensorhub.utils.SWEDataUtils;
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
    static final String NOT_FOUND_MESSAGE = "Cannot find FeatureOfInterest ";
    static final String NOT_WRITABLE_MESSAGE = "Cannot modify read-only FeatureOfInterest ";
    static final String MISSING_ASSOC = "Missing reference to FeatureOfInterest entity";
        
    OSHPersistenceManager pm;
    IFoiStore foiReadStore;
    IFoiStore foiWriteStore;
    STASecurity securityHandler;
    int maxPageSize = 100;
    ProcedureId procGroupID;
    
    
    FoiEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.foiReadStore = pm.readDatabase.getFoiStore();
        this.foiWriteStore = pm.writeDatabase != null ? pm.writeDatabase.getFoiStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.procGroupID = pm.service.getProcedureGroupID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof FeatureOfInterest);
        FeatureOfInterest foi = (FeatureOfInterest)entity;
        
        securityHandler.checkPermission(securityHandler.sta_insert_foi);
        
        // generate unique ID from name
        Asserts.checkArgument(!Strings.isNullOrEmpty(foi.getName()), "Feature name must be set");
        String uid = procGroupID.getUniqueID() + ":foi:" + SWEDataUtils.toNCName(foi.getName());
        
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
                // store feature in DB
                FeatureKey key = foiWriteStore.add(toGmlFeature(foi, uid));
                
                // publish event?                
                // handle associations / deep inserts?
                
                return new ResourceIdLong(pm.toPublicID(key.getInternalID()));
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error creating feature of interest", e);
        }
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof FeatureOfInterest);
        FeatureOfInterest foi = (FeatureOfInterest)entity;
        
        securityHandler.checkPermission(securityHandler.sta_update_foi);
        
        long publicFoiID = ((ResourceId)entity.getId()).asLong();
        checkFoiWritable(publicFoiID);
        
        // retrieve UID of existing feature
        long locaFoiID = pm.toLocalID(publicFoiID);
        var fEntry = foiWriteStore.getCurrentVersionEntry(locaFoiID);
        if (fEntry == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicFoiID);
        
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
                
                // store feature description in DB
                var key = fEntry.getKey();
                var uid = fEntry.getValue().getUniqueIdentifier();
                foiWriteStore.put(key, toGmlFeature(foi, uid));
                return true;
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error updating feature of interest", e);
        }
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_foi);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        securityHandler.checkPermission(securityHandler.sta_delete_foi);
        
        long publicFoiID = id.asLong();
        checkFoiWritable(publicFoiID);
        long locaFoiID = pm.toLocalID(publicFoiID);
        
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
                
                var count = foiWriteStore.removeEntries(new FoiFilter.Builder()
                        .withInternalIDs(locaFoiID)
                        .withAllVersions()
                        .build());
                
                if (count <= 0)
                    throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
                return true;
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error updating feature of interest", e);
        }
    }
    

    @Override
    public FeatureOfInterest getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_foi);
        
        var foi = foiReadStore.getCurrentVersion(id.asLong());
        
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
        
        FoiFilterVisitor visitor = new FoiFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);
        
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
        foi.setFeature(FrostUtils.toGeoJsonFeature(f));
        
        return foi;
    }


    protected ResourceId handleFoiAssoc(FeatureOfInterest foi) throws NoSuchEntityException
    {
        Asserts.checkArgument(foi != null, MISSING_ASSOC);
        ResourceId foiId;

        if (foi.getName() == null)
        {
            foiId = (ResourceId)foi.getId();
            Asserts.checkArgument(foiId != null, MISSING_ASSOC);
            checkFoiIDInWriteStore(foiId.asLong());
        }
        else
        {
            // deep insert
            foiId = create(foi);
        }

        return foiId;
    }
    
    
    /*
     * Check that foi ID is present in database and exposed by service
     */
    protected void checkFoiID(long publicID) throws NoSuchEntityException
    {
        boolean hasFoi = foiReadStore.getCurrentVersionKey(publicID) != null;
        if (!hasFoi)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicID);
    }


    /*
     * Check that foi ID is present in writable database
     */
    protected void checkFoiIDInWriteStore(long publicID) throws NoSuchEntityException
    {
        long localID = pm.toLocalID(publicID);
        if (foiWriteStore.getCurrentVersionKey(localID) == null)
            throw new IllegalArgumentException(NOT_WRITABLE_MESSAGE + publicID);
    }
    
    
    protected void checkFoiWritable(long publicID)
    {
        // TODO also check that current user has the right to write this procedure!
        
        if (!pm.isInWritableDatabase(publicID))
            throw new IllegalArgumentException(NOT_WRITABLE_MESSAGE + publicID);
    }
    
    
    protected void checkTransactionsEnabled()
    {
        if (foiWriteStore == null)
            throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }

}
