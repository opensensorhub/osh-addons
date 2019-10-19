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
import org.sensorhub.api.datastore.DataStreamInfo;
import org.sensorhub.api.datastore.FeatureFilter;
import org.sensorhub.api.datastore.FeatureKey;
import org.sensorhub.api.datastore.IFeatureStore;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.sensorhub.impl.service.sta.ISTADatabase.ObsPropDef;
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
    IFeatureStore<FeatureKey, ObsPropDef> obsPropDataStore;
    IHistoricalObsDatabase federatedDatabase;
    STASecurity securityHandler;
    int maxPageSize = 100;
    String groupUID;
    
    
    ObservedPropertyEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.federatedDatabase = pm.obsDbRegistry.getFederatedObsDatabase();
        this.obsPropDataStore = pm.database != null ? pm.database.getObservedPropertyDataStore() : null;
        this.securityHandler = pm.service.getSecurityHandler();
        this.groupUID = pm.service.getProcedureGroupUID();
    }
    
    
    @Override
    public ResourceId create(@SuppressWarnings("rawtypes") Entity entity) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_insert_obsprop);
        Asserts.checkArgument(entity instanceof ObservedProperty);
        ObservedProperty obsProp = (ObservedProperty)entity;
        
        if (!obsProp.getDatastreams().isEmpty())
            throw new IllegalArgumentException("Cannot deep insert a Datastream with an ObservedProperty");
        if (!obsProp.getMultiDatastreams().isEmpty())
            throw new IllegalArgumentException("Cannot deep insert a MultiDatastream with an ObservedProperty");
                
        if (obsPropDataStore != null)
        {
            // store feature description in DB
            FeatureKey key = obsPropDataStore.add(toGmlDefinition(obsProp));            
            return new ResourceId(key.getInternalID());
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
            var key = new FeatureKey(id.internalID, def.getUniqueIdentifier());
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
            var obsProp = obsPropDataStore.remove(new FeatureKey(id.internalID));
            
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
            var obsProp = obsPropDataStore.get(new FeatureKey(id.internalID));
            
            if (obsProp == null)
                throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
            
            return toFrostObservedProperty(id.internalID, obsProp, q);
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
            
            var entitySet = obsPropDataStore.selectEntries(filter)
                .skip(skip)
                .limit(limit+1) // request limit+1 elements to handle paging
                .map(e -> toFrostObservedProperty(e.getKey().getInternalID(), e.getValue(), q))
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
                    ((STADataStream)dsInfo).getThingID() : STAService.HUB_THING_ID;
                builder.withInternalIDs(thingID);
            }
        }
        
        /*SensorFilterVisitor visitor = new SensorFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);*/
        
        return builder.build();
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
        
        obsProp.setId(new ResourceId(internalId));
        obsProp.setName(f.getName());
        obsProp.setDescription(f.getDescription());
        obsProp.setDefinition(f.getUniqueIdentifier());
        
        return obsProp;
    }
    
    
    protected void handleObsPropertyAssoc(EntitySet<ObservedProperty> obsProps) throws NoSuchEntityException
    {
        if (obsProps == null)
            return;
        
        for (ObservedProperty obsProp: obsProps)
            handleObsPropertyAssoc(obsProp);
    }
    
    
    protected ResourceId handleObsPropertyAssoc(ObservedProperty obsProp) throws NoSuchEntityException
    {
        Asserts.checkArgument(obsProp != null, MISSING_ASSOC);
        ResourceId obsPropId;
        
        if (obsProp.getName() == null)
        {
            obsPropId = (ResourceId)obsProp.getId();
            Asserts.checkArgument(obsPropId != null, MISSING_ASSOC);
            checkObsPropID(obsPropId.internalID);
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
