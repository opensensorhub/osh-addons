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
import java.math.BigInteger;
import java.util.HashMap;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.database.IProcedureObsDatabase;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.impl.procedure.ProcedureObsTransactionHandler;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Id;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityProperty;
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
    ThingEntityHandler thingHandler;
    FoiEntityHandler foiHandler;
    SensorEntityHandler sensorHandler;
    ObservedPropertyEntityHandler obsPropHandler;
    DatastreamEntityHandler dataStreamHandler;
    ObservationEntityHandler observationHandler;
    LocationEntityHandler locationHandler;
    HistoricalLocationEntityHandler historicalLocationHandler;
    IEventBus eventBus;
    IDatabaseRegistry dbRegistry;
    IProcedureObsDatabase readDatabase;
    ISTADatabase writeDatabase;
    ProcedureObsTransactionHandler transactionHandler;
    
    
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
        try
        {
            for (int i = 0; i < path.size(); i++)
            {
                var elt = path.get(i);
                if (elt instanceof EntityPathElement)
                {
                    ResourceId id = (ResourceId) ((EntityPathElement) elt).getId();
                    if (id == null)
                        continue;
                    
                    switch (((EntityPathElement) elt).getEntityType())
                    {
                        case THING:
                            thingHandler.checkThingID(id.asLong());
                            break;
                            
                        case SENSOR:
                            sensorHandler.checkSensorID(id.asLong());
                            break;
                            
                        case DATASTREAM:
                        case MULTIDATASTREAM:
                            dataStreamHandler.checkDatastreamID(id.asLong());
                            break;
                            
                        case LOCATION:
                            locationHandler.checkLocationID(id.asLong());
                            break;
                            
                        case FEATUREOFINTEREST:
                            foiHandler.checkFoiID(id.asLong());
                            break;
                            
                        default:
                            break;
                    }
                }
            }
        }
        catch (NoSuchEntityException e)
        {
            return false;
        }        
        
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
        
        if (entityType == EntityType.THING)
            return thingHandler;
        else if (entityType == EntityType.FEATUREOFINTEREST)
            return foiHandler;
        else if (entityType == EntityType.SENSOR)
            return sensorHandler;
        else if (entityType == EntityType.OBSERVEDPROPERTY)
            return obsPropHandler;
        else if (entityType == EntityType.DATASTREAM || entityType == EntityType.MULTIDATASTREAM)
            return dataStreamHandler;
        else if (entityType == EntityType.OBSERVATION)
            return observationHandler;
        else if (entityType == EntityType.LOCATION)
            return locationHandler;
        else if (entityType == EntityType.HISTORICALLOCATION)
            return historicalLocationHandler;
        
        throw new UnsupportedOperationException("No support for " + entityType.entityName + " resources yet");
    }
    
    
    protected long toPublicID(long internalID)
    {
        if (writeDatabase != null)
            return dbRegistry.getPublicID(writeDatabase.getDatabaseNum(), internalID);
        else
            return internalID;
    }
    
    
    protected BigInteger toPublicID(BigInteger internalID)
    {
        if (writeDatabase != null)
            return dbRegistry.getPublicID(writeDatabase.getDatabaseNum(), internalID);
        else
            return internalID;
    }
    
    
    protected long toLocalID(long publicID)
    {
        if (writeDatabase != null)
            return dbRegistry.getLocalID(writeDatabase.getDatabaseNum(), publicID);
        else
            return publicID;
    }
    
    
    protected BigInteger toLocalID(BigInteger publicID)
    {
        if (writeDatabase != null)
            return dbRegistry.getLocalID(writeDatabase.getDatabaseNum(), publicID);
        else
            return publicID;
    }
    
    
    protected boolean isInWritableDatabase(long publicID)
    {
        return dbRegistry.getDatabaseNum(publicID) == writeDatabase.getDatabaseNum();
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
            Entity<?> entity = get(entityType, id);
            if (entity == null)
                return null;
            
            if (path.isEntityProperty())
            {
                String propName = path.getLastElement().toString();
                Object val = entity.getProperty(EntityProperty.fromString(propName));
            
                if (path.isValue())
                    return val;
                
                var customEntity = new HashMap<>();
                customEntity.put(propName, val);
                return customEntity;
            }
            
            return entity;            
        }
        
        // case of association with a single entity
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
        
        // connect to hub resources
        this.eventBus = service.getParentHub().getEventBus();
        this.dbRegistry = service.getParentHub().getDatabaseRegistry();
        this.readDatabase = service.readDatabase;
        this.writeDatabase = service.writeDatabase;
        this.transactionHandler = new ProcedureObsTransactionHandler(eventBus, writeDatabase);
        
        // setup all entity handlers
        this.securityHandler = service.getSecurityHandler();
        this.thingHandler = new ThingEntityHandler(this);
        this.foiHandler = new FoiEntityHandler(this);
        this.sensorHandler = new SensorEntityHandler(this);
        this.obsPropHandler = new ObservedPropertyEntityHandler(this);
        this.dataStreamHandler = new DatastreamEntityHandler(this);
        this.observationHandler = new ObservationEntityHandler(this);
        this.locationHandler = new LocationEntityHandler(this);
        this.historicalLocationHandler = new HistoricalLocationEntityHandler(this);
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
