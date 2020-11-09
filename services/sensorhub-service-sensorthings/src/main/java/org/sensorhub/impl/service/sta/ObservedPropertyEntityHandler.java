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
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.sensorhub.api.database.IProcedureObsDatabase;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.impl.service.sta.ISTAObsPropStore.ObsPropDef;
import org.vast.data.ScalarIterator;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import de.fraunhofer.iosb.ilt.frostserver.model.ObservedProperty;
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
public class ObservedPropertyEntityHandler implements IResourceHandler<ObservedProperty>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find 'ObservedProperty' entity with ID #";
    static final String MISSING_ASSOC = "Missing reference to 'ObservedProperty' entity";
    
    OSHPersistenceManager pm;
    ISTAObsPropStore obsPropDataStore;
    IProcedureObsDatabase readDatabase;
    STASecurity securityHandler;
    int maxPageSize = 100;
    
    
    ObservedPropertyEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.readDatabase = pm.readDatabase;
        this.obsPropDataStore = pm.writeDatabase != null ? pm.writeDatabase.getObservedPropertyDataStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_obsprop);
        Asserts.checkArgument(entity instanceof ObservedProperty);
        ObservedProperty obsProp = (ObservedProperty)entity;
        
        if (!obsProp.getDatastreams().isEmpty() || !obsProp.getMultiDatastreams().isEmpty())
            throw new IllegalArgumentException("Cannot deep insert an ObservedProperty containing Datastreams");
                
        if (obsPropDataStore != null)
        {
            // store feature description in DB
            FeatureKey key = obsPropDataStore.add(toGmlDefinition(obsProp));            
            return new ResourceIdLong(key.getInternalID());
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }
    

    @Override
    public boolean update(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_obsprop);
        Asserts.checkArgument(entity instanceof ObservedProperty);
        ObservedProperty obsProp = (ObservedProperty)entity;
        
        if (obsPropDataStore != null)
        {
            ResourceId id = (ResourceId)entity.getId();
            
            // store definition in DB
            var def = toGmlDefinition(obsProp);
            var key = new FeatureKey(id.asLong());
            obsPropDataStore.put(key, def);
            
            return true;
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }
    
    
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_obsprop);
        throw new UnsupportedOperationException("Patch not supported");
    }
    
    
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_delete_obsprop);
        
        if (obsPropDataStore != null)
        {
            var obsProp = obsPropDataStore.remove(new FeatureKey(id.asLong()));
            
            if (obsProp == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return true;
        }
        
        throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }
    

    @Override
    public ObservedProperty getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_obsprop);
        
        if (obsPropDataStore != null)
        {
            var obsProp = obsPropDataStore.get(new FeatureKey(id.asLong()));
            
            if (obsProp == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return toFrostObservedProperty(id.asLong(), obsProp, q);
        }
        
        return null;
    }
    

    @Override
    public EntitySet<ObservedProperty> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_obsprop);
        
        if (obsPropDataStore != null)
        {
            FeatureFilter filter = getFilter(path, q);
            int skip = q.getSkip(0);
            int limit = Math.min(q.getTopOrDefault(), maxPageSize);
            
            if (filter.getParentFilter() != null &&
                filter.getParentFilter().getInternalIDs() != null &&
                !filter.getParentFilter().getInternalIDs().isEmpty())
            {
                // case of external datastream: parent ID is set to datastream ID
                // just extract observed properties from record structure
                // hack: datastream ID is stored in parent ID filter
                long dsId = filter.getParentFilter().getInternalIDs().iterator().next();
                var dsKey = new DataStreamKey(dsId);
                IDataStreamInfo dsInfo = readDatabase.getObservationStore().getDataStreams().get(dsKey);
                return getObservedPropertySet(dsInfo, q);                
            }
            else
            {
                var entitySet = obsPropDataStore.selectEntries(filter)
                    .skip(skip)
                    .limit(limit+1) // request limit+1 elements to handle paging
                    .map(e -> toFrostObservedProperty(e.getKey().getInternalID(), e.getValue(), q))
                    .collect(Collectors.toCollection(EntitySetImpl::new));
                
                return FrostUtils.handlePaging(entitySet, path, q, limit);
            }
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
                long dsId = ((ResourceId)idElt.getId()).asLong();
                
                if (pm.isInWritableDatabase(dsId))
                {
                    // if datastream was created by STA, get IDs of observed properties from record structure
                    var dsKey = new DataStreamKey(dsId);
                    IDataStreamInfo dsInfo = readDatabase.getObservationStore().getDataStreams().get(dsKey);
                    builder.withInternalIDs(getObservedPropertyIds(dsInfo));
                }
                else
                {
                    // else use an empty list and let caller handle the case
                    // hack: store datastream ID in parent ID set
                    builder.withParents().withInternalIDs(dsId).done();
                }
            }
        }
        
        /*SensorFilterVisitor visitor = new SensorFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);*/
        
        return builder.build();
    }
    
    
    protected TreeSet<Long> getObservedPropertyIds(IDataStreamInfo dsInfo)
    {
        var obsPropIDs = new TreeSet<Long>();
        
        ScalarIterator it = new ScalarIterator(dsInfo.getRecordStructure());
        while (it.hasNext())
        {
            String id = it.next().getId();
            if (id != null)
                obsPropIDs.add(Long.parseLong(id));
        }
        
        return obsPropIDs;
    }
    
    
    protected EntitySet<ObservedProperty> getObservedPropertySet(IDataStreamInfo dsInfo, Query q)
    {
        var entitySet = new EntitySetImpl<ObservedProperty>();
        
        ScalarIterator it = new ScalarIterator(dsInfo.getRecordStructure());
        while (it.hasNext())
        {
            var obsProp = pm.dataStreamHandler.toObservedProperty(it.next(), q.getSelect());
            obsProp.setId(new ResourceIdLong(1));
            entitySet.add(obsProp);
        }
        
        return entitySet;
    }
    
    
    protected ObsPropDef toGmlDefinition(ObservedProperty obsProp)
    {
        return new ObsPropDef(
            obsProp.getDefinition(),
            obsProp.getName(),
            obsProp.getDescription());
    }
    
    
    protected ObservedProperty toFrostObservedProperty(long internalId, ObsPropDef f, Query q)
    {
        // TODO implement expand
        //Set<Property> select = q != null ? q.getSelect() : Collections.emptySet();
        
        ObservedProperty obsProp = new ObservedProperty();
        
        obsProp.setId(new ResourceIdLong(internalId));
        obsProp.setName(f.getName());
        obsProp.setDescription(f.getDescription());
        obsProp.setDefinition(f.getUniqueIdentifier());
        
        return obsProp;
    }
    
    
    protected ResourceId handleObsPropertyAssoc(ObservedProperty obsProp) throws NoSuchEntityException
    {
        Asserts.checkArgument(obsProp != null, MISSING_ASSOC);
        ResourceId obsPropId;
        
        if (obsProp.getName() == null)
        {
            obsPropId = (ResourceId)obsProp.getId();
            Asserts.checkArgument(obsPropId != null, MISSING_ASSOC);
            checkObsPropID(obsPropId.asLong());
        }
        else
        {
            // deep insert
            obsPropId = create(obsProp);
        }
        
        return obsPropId;
    }
    
    
    /*
     * Check that sensorID is present in database and exposed by service
     */
    protected void checkObsPropID(long obsPropID) throws NoSuchEntityException
    {
        var key = new FeatureKey(obsPropID);
        boolean hasObsProp = obsPropDataStore.containsKey(key);
        if (!hasObsProp)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + obsPropID);
    }

}
