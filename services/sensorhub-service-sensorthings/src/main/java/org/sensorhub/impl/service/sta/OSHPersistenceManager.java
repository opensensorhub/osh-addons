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

import java.io.IOException;
import java.io.Writer;
import org.sensorhub.api.datastore.IHistoricalObsDatabase;
import org.sensorhub.api.datastore.IDatabaseRegistry;
import org.sensorhub.api.procedure.IProcedureRegistry;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManager;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;
import de.fraunhofer.iosb.ilt.frostserver.util.IncompleteEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.NoSuchEntityException;
import de.fraunhofer.iosb.ilt.frostserver.util.UpgradeFailedException;


/**
 * 
 * <p>
 * Implementation of FROST persistence manager backed by OSH data stores.
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
public class OSHPersistenceManager implements PersistenceManager
{
    CoreSettings settings;
    STAService service;
    STASecurity securityHandler;
    ResourceIdManager idManager = new ResourceIdManager();
    SensorEntityHandler sensorHandler;
    FoiEntityHandler foiHandler;
    DatastreamEntityHandler dataStreamHandler;
    ObservationEntityHandler observationHandler;
    IProcedureRegistry procRegistry;
    IDatabaseRegistry obsDbRegistry;
    IHistoricalObsDatabase obsDatabase;
    
    
    public OSHPersistenceManager()
    {        
    }


    @Override
    public ResourceIdManager getIdManager()
    {
        return idManager;
    }


    @Override
    public boolean validatePath(ResourcePath path)
    {
        // TODO Auto-generated method stub
        return true;
    }
    
    
    protected IResourceHandler<?> getHandler(ResourcePath path)
    {
        Asserts.checkNotNull(path, ResourcePath.class);
        return getHandler(path.getMainElementType());
    }
    
    
    protected IResourceHandler<?> getHandler(EntityType entityType)
    {
        Asserts.checkNotNull(entityType, EntityType.class);
        
        if (entityType == EntityType.SENSOR)
            return sensorHandler;
        else if (entityType == EntityType.FEATUREOFINTEREST)
            return foiHandler;
        else if (entityType == EntityType.DATASTREAM || entityType == EntityType.MULTIDATASTREAM)
            return dataStreamHandler;
        else if (entityType == EntityType.OBSERVATION)
            return observationHandler;
        
        throw new UnsupportedOperationException("No support for " + entityType.entityName + " resources yet");
    }
    
    
    protected long toPublicID(long internalID)
    {
        if (obsDatabase != null)
            return obsDbRegistry.getPublicID(obsDatabase.getDatabaseID(), internalID);
        else
            return internalID;
    }
    
    
    protected long toLocalID(long publicID)
    {
        if (obsDatabase != null)
            return obsDbRegistry.getLocalID(obsDatabase.getDatabaseID(), publicID);
        else
            return publicID;
    }


    @Override
    @SuppressWarnings("rawtypes")
    public boolean insert(Entity entity) throws NoSuchEntityException, IncompleteEntityException
    {
        Asserts.checkNotNull(entity, Entity.class);
        
        ResourceId assignedId = getHandler(entity.getEntityType()).create(entity);
        entity.setId(assignedId);
        
        return assignedId != null;
    }


    @Override
    @SuppressWarnings({ "rawtypes" })
    public Entity get(EntityType entityType, Id id)
    {
        Asserts.checkNotNull(id, Id.class);
        Asserts.checkArgument(((ResourceId)id).internalID > 0, "Invalid ID. Entity IDs must be > 0");
        
        try
        {
            Entity result = getHandler(entityType).getById((ResourceId)id, null);
            return (result != null && result.getEntityType() == entityType) ? result : null;
        }
        catch (NoSuchEntityException e)
        {
            return null;
        }
    }


    @Override
    public Object get(ResourcePath path, Query q)
    {
        // case of request by ID
        if (path.getMainElement() == path.getIdentifiedElement())
        {
            EntityType entityType = ((EntityPathElement)path.getMainElement()).getEntityType();
            Id id = ((EntityPathElement)path.getMainElement()).getId();
            return get(entityType, id);
        }
        
        // case of relationship to a single entity
        else if (path.getMainElement() instanceof EntityPathElement)
        {
            EntitySet<?> resultSet = getHandler(path).queryCollection(path, q);
            if (resultSet.isEmpty())
                return null;
            else if (resultSet.size() == 1)
                return resultSet.asList().get(0);
            else
                return resultSet;
        }
        
        // case of collection
        return getHandler(path).queryCollection(path, q);
    } 


    @Override
    public boolean delete(EntityPathElement pathElement) throws NoSuchEntityException
    {
        Asserts.checkNotNull(pathElement, EntityPathElement.class);        
        return getHandler(pathElement.getEntityType()).delete((ResourceId)pathElement.getId());
    }


    @Override
    public void delete(ResourcePath path, Query query) throws NoSuchEntityException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    @SuppressWarnings({ "rawtypes" })
    public boolean update(EntityPathElement pathElement, Entity entity) throws NoSuchEntityException, IncompleteEntityException
    {
        Asserts.checkNotNull(pathElement, EntityPathElement.class);        
        Asserts.checkNotNull(entity, Entity.class);
        entity.setId(pathElement.getId());
        return getHandler(pathElement.getEntityType()).update(entity);
    }


    @Override
    public boolean update(EntityPathElement pathElement, JsonPatch patch) throws NoSuchEntityException, IncompleteEntityException
    {
        Asserts.checkNotNull(pathElement, EntityPathElement.class);        
        Asserts.checkNotNull(patch, JsonPatch.class);        
        return getHandler(pathElement.getEntityType()).patch((ResourceId)pathElement.getId(), patch);
    }


    @Override
    public void init(CoreSettings settings)
    {
        this.settings = settings;
        
        // retrieve service instance
        int serviceId = settings.getPersistenceSettings().getCustomSettings().getInt(STAService.SERVICE_INSTANCE_ID);
        this.service = STAService.serviceInstances.get(serviceId);
        
        // connect to registries
        this.procRegistry = service.getParentHub().getProcedureRegistry();
        this.obsDbRegistry = service.getParentHub().getDatabaseRegistry();
        this.obsDatabase = service.getDatabase();
        
        // setup all handlers
        this.securityHandler = service.getSecurityHandler();
        this.sensorHandler = new SensorEntityHandler(this);
        this.foiHandler = new FoiEntityHandler(this);
        this.dataStreamHandler = new DatastreamEntityHandler(this);
        this.observationHandler = new ObservationEntityHandler(this);
    }


    @Override
    public CoreSettings getCoreSettings()
    {
        return settings;
    }


    @Override
    public void commit()
    {
    }


    @Override
    public void rollback()
    {
    }


    @Override
    public void close()
    {        
    }
    
    
    @Override
    public String checkForUpgrades()
    {
        return null;
    }


    @Override
    public boolean doUpgrades(Writer out) throws UpgradeFailedException, IOException
    {
        return false;
    }

}
