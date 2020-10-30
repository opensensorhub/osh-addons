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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventPublisher;
import org.sensorhub.api.obs.DataStreamAddedEvent;
import org.sensorhub.api.obs.DataStreamChangedEvent;
import org.sensorhub.api.obs.DataStreamFilter;
import org.sensorhub.api.obs.DataStreamInfo;
import org.sensorhub.api.obs.DataStreamRemovedEvent;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.api.obs.IDataStreamStore;
import org.sensorhub.api.procedure.ProcedureId;
import org.vast.data.TextEncodingImpl;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.IProcedure;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.unit.Unit;
import org.vast.util.Asserts;
import com.github.fge.jsonpatch.JsonPatch;
import de.fraunhofer.iosb.ilt.frostserver.model.Datastream;
import de.fraunhofer.iosb.ilt.frostserver.model.MultiDatastream;
import de.fraunhofer.iosb.ilt.frostserver.model.ObservedProperty;
import de.fraunhofer.iosb.ilt.frostserver.model.Sensor;
import de.fraunhofer.iosb.ilt.frostserver.model.Thing;
import de.fraunhofer.iosb.ilt.frostserver.model.core.AbstractDatastream;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySet;
import de.fraunhofer.iosb.ilt.frostserver.model.core.EntitySetImpl;
import de.fraunhofer.iosb.ilt.frostserver.model.ext.UnitOfMeasurement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityPathElement;
import de.fraunhofer.iosb.ilt.frostserver.path.EntityType;
import de.fraunhofer.iosb.ilt.frostserver.path.NavigationProperty;
import de.fraunhofer.iosb.ilt.frostserver.path.Property;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.util.NoSuchEntityException;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.ScalarComponent;
import net.opengis.swe.v20.TextEncoding;
import net.opengis.swe.v20.Vector;


/**
 * <p>
 * Service handler for Sensor resources
 * </p>
 *
 * @author Alex Robin
 * @date Sep 7, 2019
 */
@SuppressWarnings("rawtypes")
public class DatastreamEntityHandler implements IResourceHandler<AbstractDatastream>
{
    static final String NOT_FOUND_MESSAGE = "Cannot find 'Datastream' entity with ID #";
    static final String NOT_WRITABLE_MESSAGE = "Cannot modify read-only 'Datastream' entity #";
    static final String MISSING_ASSOC = "Missing reference to 'Datastream' or 'MultiDatastream' entity";
    static final String UCUM_URI_PREFIX = "http://unitsofmeasure.org/ucum.html#";
    static final String BAD_LINK_THING = "A new Datastream SHALL link to an Thing entity";

    OSHPersistenceManager pm;
    IDataStreamStore dataStreamReadStore;
    ISTADataStreamStore dataStreamWriteStore;
    STASecurity securityHandler;
    int maxPageSize = 100;


    DatastreamEntityHandler(OSHPersistenceManager pm)
    {
        this.pm = pm;
        this.dataStreamWriteStore = pm.writeDatabase != null ? pm.writeDatabase.getDataStreamStore() : null;
        var federatedDataStreamStore = pm.readDatabase.getObservationStore().getDataStreams();
        this.dataStreamReadStore = new STAFederatedDataStreamStoreWrapper(pm.writeDatabase, federatedDataStreamStore);
        this.securityHandler = pm.service.getSecurityHandler();
    }


    @Override
    public ResourceId create(Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof AbstractDatastream);
        AbstractDatastream<?> dataStream = (AbstractDatastream<?>)entity;

        securityHandler.checkPermission(securityHandler.sta_insert_datastream);
        
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
                
                // handle associations / deep inserts
                ResourceId thingId = pm.thingHandler.handleThingAssoc(dataStream.getThing());
                ResourceId sensorId = pm.sensorHandler.handleSensorAssoc(dataStream.getSensor());
                
                // TODO try to see if a datastream with the same name already exists, and handle versioning
                // when versioning a datastream, the parent procedure description must be versioned too
                
                // get parent sensor
                long localSensorID = pm.toLocalID(sensorId.asLong());
                IProcedure proc = pm.sensorHandler.procWriteStore.getCurrentVersion(localSensorID);
                String procUID = proc.getUniqueIdentifier();
                
                // create data stream object
                DataRecord recordStruct = toSweCommon(dataStream);
                DataStreamInfo dsInfo = new STADataStream.Builder()
                    .withThing(thingId.asLong())
                    .withProcedure(new ProcedureId(localSensorID, procUID))
                    .withRecordDescription(recordStruct)
                    .withRecordEncoding(new TextEncodingImpl())
                    .build();

                // store in DB
                Long localDsID = dataStreamWriteStore.add(dsInfo);
                long publicDsID = pm.toPublicID(localDsID);
                ResourceId newDsId = new ResourceIdLong(publicDsID);
                
                // publish event
                ProcedureId procID = new ProcedureId(sensorId.asLong(), procUID);
                String outputName = recordStruct.getName();
                IEventPublisher publisher = pm.eventBus.getPublisher(EventUtils.getProcedureOutputSourceID(procUID, outputName));
                publisher.publish(new DataStreamAddedEvent(procID, outputName, publicDsID));
                            
                // handle associations / deep inserts
                pm.observationHandler.handleObservationAssocList(newDsId, dataStream);
                
                return newDsId;
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error creating datastream", e);
        }
    }


    @Override
    public boolean update(Entity entity) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        Asserts.checkArgument(entity instanceof AbstractDatastream);
        AbstractDatastream<?> dataStream = (AbstractDatastream<?>)entity;
        
        securityHandler.checkPermission(securityHandler.sta_update_datastream);
        
        long publicDsID = ((ResourceId)entity.getId()).asLong();
        checkDatastreamWritable(publicDsID);
                
        // get existing data stream
        // we use write store even for reading because only datastreams in write store can be updated anyway!
        long localDsID = pm.toLocalID(publicDsID);
        IDataStreamInfo oldDsInfo = dataStreamWriteStore.get(localDsID);
        if (oldDsInfo == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicDsID);
        
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
        
                // generate new record structure
                DataRecord newRecordStruct = toSweCommon(dataStream);
                
                // handle associations / deep inserts or reuse existing assoc
                long thingID = dataStream.getThing() != null ?
                    pm.thingHandler.handleThingAssoc(dataStream.getThing()).asLong() :
                    dataStreamWriteStore.getAssociatedThing(localDsID);

                //ResourceId sensorId = pm.sensorHandler.handleSensorAssoc(dataStream.getSensor());
                
                // create new data stream version
                IDataStreamInfo dsInfo = new STADataStream.Builder()
                    .withThing(thingID)
                    .withProcedure(oldDsInfo.getProcedureID())
                    .withRecordDescription(newRecordStruct)
                    .withRecordEncoding(oldDsInfo.getRecordEncoding())
                    .build();

                // check name wasn't changed
                if (!dsInfo.getName().equals(oldDsInfo.getName()))
                    throw new IllegalArgumentException("Cannot change a datastream name");
                
                // TODO check that structure hasn't changed 
                // if obs have already been associated with this datastream
                
                // write to store
                dataStreamWriteStore.put(localDsID, dsInfo);
    
                // publish event
                ProcedureId procID = dsInfo.getProcedureID();
                String outputName = newRecordStruct.getName();
                IEventPublisher publisher = pm.eventBus.getPublisher(EventUtils.getProcedureOutputSourceID(procID.getUniqueID(), outputName));
                publisher.publish(new DataStreamChangedEvent(procID, outputName, publicDsID));
    
                return true;
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error updating datastream " + publicDsID, e);
        }
    }


    @Override
    public boolean patch(ResourceId id, JsonPatch patch) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_update_sensor);
        throw new UnsupportedOperationException("Patch not supported");
    }


    @Override
    public boolean delete(ResourceId id) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        securityHandler.checkPermission(securityHandler.sta_delete_datastream);
        
        long publicDsID = id.asLong();
        checkDatastreamWritable(publicDsID);
                
        try
        {
            return pm.writeDatabase.executeTransaction(() -> {
                
                long localDsId = pm.toLocalID(publicDsID);
                IDataStreamInfo dsInfo = dataStreamWriteStore.remove(localDsId);
                if (dsInfo == null)
                    throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
                
                // publish event
                ProcedureId procID = dsInfo.getProcedureID();
                String outputName = dsInfo.getOutputName();
                IEventPublisher publisher = pm.eventBus.getPublisher(EventUtils.getProcedureOutputSourceID(procID.getUniqueID(), outputName));
                publisher.publish(new DataStreamRemovedEvent(procID, outputName, publicDsID));
                
                return true;
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error deleting datastream " + publicDsID, e);
        }
    }


    @Override
    public AbstractDatastream getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_datastream);

        IDataStreamInfo dsInfo = dataStreamReadStore.get(id.asLong());
        if (dsInfo == null || !isDatastreamVisible(id.asLong(), dsInfo))
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);

        return toFrostDatastream(id.asLong(), dsInfo, q);
    }


    @Override
    @SuppressWarnings("unchecked")
    public EntitySet<?> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_datastream);

        DataStreamFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);

        var entitySet = dataStreamReadStore.selectEntries(filter)
            .filter(e -> isDatastreamVisible(e.getKey(), e.getValue()))
            .skip(skip)
            .limit(limit+1) // request limit+1 elements to handle paging
            .map(e -> toFrostDatastream(e.getKey(), e.getValue(), q))
            .filter(ds -> ds.getEntityType() == path.getMainElementType())
            .collect(Collectors.toCollection(EntitySetImpl::new));

        return FrostUtils.handlePaging(entitySet, path, q, limit);
    }


    protected DataStreamFilter getFilter(ResourcePath path, Query q)
    {
        STADataStreamFilter.Builder builder = new STADataStreamFilter.Builder()
            .withAllVersions();

        EntityPathElement idElt = path.getIdentifiedElement();
        if (idElt != null)
        {
            if (idElt.getEntityType() == EntityType.THING)
            {
                ResourceId thingId = (ResourceId)idElt.getId();
                builder.withThings(thingId.asLong());
            }
            else if (idElt.getEntityType() == EntityType.SENSOR)
            {
                ResourceId sensorId = (ResourceId)idElt.getId();
                builder.withProcedures(sensorId.asLong());
            }
            else if (idElt.getEntityType() == EntityType.OBSERVATION)
            {
                ResourceIdBigInt obsId = (ResourceIdBigInt)idElt.getId();
                builder.withObservations()
                    .withInternalIDs(obsId.internalID)
                    .done();
            }
            else if (idElt.getEntityType() == EntityType.OBSERVEDPROPERTY)
            {
                // TODO
            }
        }

        DatastreamFilterVisitor visitor = new DatastreamFilterVisitor(builder);
        if (q.getFilter() != null)
            q.getFilter().accept(visitor);

        return builder.build();
    }


    protected boolean isScalarOutput(DataComponent rec)
    {
        return (rec.getComponentCount() <= 2 &&
            rec.getComponent(1) instanceof ScalarComponent);
    }


    protected DataRecord toSweCommon(AbstractDatastream<?> abstractDs) throws NoSuchEntityException
    {
        SWEHelper helper = new SWEHelper();

        DataRecord rec = helper.newDataRecord();
        rec.setName(toNCName(abstractDs.getName()));
        rec.setLabel(abstractDs.getName());
        rec.setDescription(abstractDs.getDescription());
        rec.addComponent("time", helper.newTimeIsoUTC(SWEConstants.DEF_PHENOMENON_TIME, "Sampling Time", null));

        if (abstractDs instanceof Datastream)
        {
            Datastream ds = (Datastream)abstractDs;
            ObservedProperty obsProp = ds.getObservedProperty();

            //Asserts.checkArgument(obsProp.getName() != null, "Observed properties must be provided inline when creating a datastream");
            ResourceId obsPropId = pm.obsPropHandler.handleObsPropertyAssoc(obsProp);
            if (obsProp.getName() == null)
                obsProp = pm.obsPropHandler.getById(obsPropId, new Query());
            else
                obsProp.setId(obsPropId);

            DataComponent comp = toComponent(
                ds.getObservationType(),
                obsProp,
                ds.getUnitOfMeasurement(),
                helper);

            rec.addComponent(comp.getName(), comp);
        }
        else
        {
            MultiDatastream ds = (MultiDatastream)abstractDs;

            int i = 0;
            for (ObservedProperty obsProp: ds.getObservedProperties())
            {
                //Asserts.checkArgument(obsProp.getName() != null, "Observed properties must be provided inline when creating a datastream");
                ResourceId obsPropId = pm.obsPropHandler.handleObsPropertyAssoc(obsProp);
                if (obsProp.getName() == null)
                    obsProp = pm.obsPropHandler.getById(obsPropId, new Query());
                else
                    obsProp.setId(obsPropId);

                DataComponent comp = toComponent(
                    ds.getMultiObservationDataTypes().get(i),
                    obsProp,
                    ds.getUnitOfMeasurements().get(i),
                    helper);
                i++;

                rec.addComponent(comp.getName(), comp);
            }
        }

        return rec;
    }


    protected DataComponent toComponent(String obsType, ObservedProperty obsProp, UnitOfMeasurement uom, SWEHelper fac)
    {
        DataComponent comp = null;

        if (IObservation.OBS_TYPE_MEAS.equals(obsType))
        {
            comp = fac.newQuantity();

            if (uom.getDefinition() != null && uom.getDefinition().startsWith(UCUM_URI_PREFIX))
                ((Quantity)comp).getUom().setCode(uom.getDefinition().replace(UCUM_URI_PREFIX, ""));
            else
                ((Quantity)comp).getUom().setHref(uom.getDefinition());
        }
        else if (IObservation.OBS_TYPE_CATEGORY.equals(obsType))
            comp = fac.newCategory();
        else if (IObservation.OBS_TYPE_COUNT.equals(obsType))
            comp = fac.newCount();
        else if (IObservation.OBS_TYPE_RECORD.equals(obsType))
            comp = fac.newDataRecord();

        if (comp != null)
        {
            comp.setId(obsProp.getId().toString());
            comp.setName(toNCName(obsProp.getName()));
            comp.setLabel(obsProp.getName());
            comp.setDescription(obsProp.getDescription());
            comp.setDefinition(obsProp.getDefinition());
        }

        return comp;
    }


    protected String toNCName(String name)
    {
        return name.toLowerCase().replaceAll("\\s+", "_");
    }


    protected AbstractDatastream toFrostDatastream(Long publicID, IDataStreamInfo dsInfo, Query q)
    {
        AbstractDatastream dataStream;
        Set<Property> select = q != null ? q.getSelect() : Collections.emptySet();
        boolean isExternalDatastream = !pm.isInWritableDatabase(publicID);

        // convert to simple or multi datastream
        DataComponent rec = dsInfo.getRecordStructure();
        if (isScalarOutput(rec))
        {
            Datastream simpleDs = new Datastream();
            DataComponent comp = rec.getComponent(1);
            simpleDs.setObservationType(toObsType(comp));
            simpleDs.setUnitOfMeasurement(toUom(comp));
            if (select.isEmpty() || select.contains(NavigationProperty.OBSERVEDPROPERTY))
                simpleDs.setObservedProperty(toObservedProperty(comp, Collections.emptySet()));
            dataStream = simpleDs;
            if (!isExternalDatastream)
                simpleDs.getObservedProperty().setExportObject(false);
        }
        else
        {
            MultiDatastream multiDs = new MultiDatastream();
            multiDs.setObservationType(IObservation.OBS_TYPE_RECORD);
            visitComponent(rec, multiDs, select.isEmpty() || select.contains(NavigationProperty.OBSERVEDPROPERTIES));
            dataStream = multiDs;
            if (!isExternalDatastream)
                multiDs.getObservedProperties().setExportObject(false);
        }

        // common properties
        dataStream.setId(new ResourceIdLong(publicID));
        dataStream.setName(rec.getLabel() != null ?
            rec.getLabel() : StringUtils.capitalize(rec.getName()));
        dataStream.setDescription(rec.getDescription());

        // link to Thing
        long thingID =  isExternalDatastream ?
            STAService.HUB_THING_ID :
            dataStreamWriteStore.getAssociatedThing(pm.toLocalID(publicID));
        Thing thing = new Thing(new ResourceIdLong(thingID));
        thing.setExportObject(false);
        dataStream.setThing(thing);

        // link to Sensor
        ResourceIdLong sensorId = new ResourceIdLong(dsInfo.getProcedureID().getInternalID());
        Sensor sensor = new Sensor(sensorId);
        sensor.setExportObject(false);
        dataStream.setSensor(sensor);

        return dataStream;
    }


    protected void visitComponent(DataComponent c, MultiDatastream multiDs, boolean expandObsProps)
    {
        if (c instanceof ScalarComponent)
        {
            String def = c.getDefinition();

            // skip time stamp
            if (def != null && (SWEConstants.DEF_PHENOMENON_TIME.equals(def) ||
                SWEConstants.DEF_SAMPLING_TIME.equals(def)))
                return;

            multiDs.getMultiObservationDataTypes().add(toObsType(c));
            multiDs.getUnitOfMeasurements().add(toUom(c));
            if (expandObsProps)
                multiDs.getObservedProperties().add(toObservedProperty(c, Collections.emptySet()));
        }
        else if (c instanceof DataRecord || c instanceof Vector)
        {
            for (int i = 0; i < c.getComponentCount(); i++)
            {
                DataComponent child = c.getComponent(i);
                visitComponent(child, multiDs, expandObsProps);
            }
        }
    }


    protected String toObsType(DataComponent comp)
    {
        if (comp instanceof Quantity)
            return IObservation.OBS_TYPE_MEAS;
        else if (comp instanceof Category)
            return IObservation.OBS_TYPE_CATEGORY;
        else if (comp instanceof Count)
            return IObservation.OBS_TYPE_COUNT;
        else if (comp instanceof DataRecord)
            return IObservation.OBS_TYPE_RECORD;
        else
            return null;
    }


    protected ObservedProperty toObservedProperty(DataComponent comp, Set<Property> select)
    {
        ObservedProperty obsProp = new ObservedProperty();

        obsProp.setDefinition(comp.getDefinition());
        obsProp.setName(comp.getLabel() != null ?
            comp.getLabel() : StringUtils.capitalize(comp.getName()));
        obsProp.setDescription(comp.getDescription());

        return obsProp;
    }


    protected UnitOfMeasurement toUom(DataComponent comp)
    {
        UnitOfMeasurement uom = new UnitOfMeasurement();

        if (comp instanceof HasUom)
        {
            if (((HasUom)comp).getUom().hasHref())
            {
                uom.setDefinition(((HasUom)comp).getUom().getHref());
            }
            else
            {
                Unit ucumUnit = ((HasUom)comp).getUom().getValue();
                uom.setName(ucumUnit.getName());
                uom.setSymbol(ucumUnit.getPrintSymbol());
                uom.setDefinition(UCUM_URI_PREFIX +
                    (ucumUnit.getCode() != null ? ucumUnit.getCode() : ucumUnit.getExpression()));
            }
        }
        else if (comp instanceof Count)
        {
            uom.setName("Count");
        }
        else
        {
            uom.setName("No Unit");
        }

        return uom;
    }


    protected ResourceId handleDatastreamAssoc(AbstractDatastream ds) throws NoSuchEntityException
    {
        Asserts.checkArgument(ds != null, MISSING_ASSOC);
        ResourceId dsId;

        if (ds.getName() == null)
        {
            dsId = (ResourceId)ds.getId();
            Asserts.checkArgument(dsId != null, MISSING_ASSOC);
            checkDatastreamIDInWriteStore(dsId.asLong());
        }
        else
        {
            // deep insert
            dsId = create(ds);
        }

        return dsId;
    }


    protected void handleDatastreamAssocList(ResourceId thingId, Thing thing) throws NoSuchEntityException
    {
        if (thing.getDatastreams() != null)
        {
            for (Datastream ds: thing.getDatastreams())
            {
                ds.setThing(new Thing(thingId));
                handleDatastreamAssoc(ds);
            }
        }

        if (thing.getMultiDatastreams() != null)
        {
            for (MultiDatastream ds: thing.getMultiDatastreams())
            {
                ds.setThing(new Thing(thingId));
                handleDatastreamAssoc(ds);
            }
        }
    }


    protected void handleDatastreamAssocList(ResourceId sensorId, Sensor sensor) throws NoSuchEntityException
    {
        if (sensor.getDatastreams() != null)
        {
            for (Datastream ds: sensor.getDatastreams())
            {
                ds.setSensor(new Sensor(sensorId));
                pm.dataStreamHandler.handleDatastreamAssoc(ds);
            }
        }

        if (sensor.getMultiDatastreams() != null)
        {
            for (MultiDatastream ds: sensor.getMultiDatastreams())
            {
                ds.setSensor(new Sensor(sensorId));
                pm.dataStreamHandler.handleDatastreamAssoc(ds);
            }
        }
    }


    /*
     * Check that datastream ID is present in database and exposed by service
     */
    protected void checkDatastreamID(long publicID) throws NoSuchEntityException
    {
        if (!dataStreamReadStore.containsKey(publicID))
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicID);
    }


    /*
     * Check that datastream ID is present in writable database
     */
    protected void checkDatastreamIDInWriteStore(long publicID) throws NoSuchEntityException
    {
        long localID = pm.toLocalID(publicID);
        if (!dataStreamWriteStore.containsKey(localID))
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + publicID);
    }


    protected void checkDatastreamWritable(long publicID)
    {
        // TODO also check that current user has the right to write this procedure!

        if (!pm.isInWritableDatabase(publicID))
            throw new UnsupportedOperationException(NOT_WRITABLE_MESSAGE + publicID);
    }


    protected boolean isDatastreamVisible(long publicID, IDataStreamInfo dsInfo)
    {
        if (!(dsInfo.getRecordEncoding() instanceof TextEncoding))
            return false;

        // TODO check that current user has the right to read this entity!
        
        return true;
    }


    protected boolean isDatastreamVisible(long publicID)
    {
        IDataStreamInfo dsInfo = dataStreamReadStore.get(publicID);
        return dsInfo != null && isDatastreamVisible(publicID, dsInfo);
    }
    
    
    protected void checkTransactionsEnabled()
    {
        if (dataStreamWriteStore == null)
            throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }


    /*
     * Helper methods to convert to/from packed numerical IDs
     */
    /*static int MAX_DATASTREAMS_PER_SENSOR = 100;
    static int MAX_VERSIONS_PER_DATASTREAM = 100;
    static int MAX_VERSIONS_PER_SENSOR = MAX_DATASTREAMS_PER_SENSOR * MAX_VERSIONS_PER_DATASTREAM;
    static int LATEST_VERSION = 999;

    static class DatastreamIds
    {
        long sensorId;
        int outputId;
        int version;
    }


    protected ResourceId generateDatastreamId(long sensorId, int outputId, int version)
    {
        long sensorIdPart = sensorId * MAX_VERSIONS_PER_SENSOR;
        long outputIdPart = outputId * MAX_VERSIONS_PER_DATASTREAM;
        return new ResourceId(sensorIdPart + outputIdPart + version);
    }


    protected DatastreamIds parseDatastreamId(ResourceId dsId)
    {
        long internalId = dsId.getValue();
        DatastreamIds ids = new DatastreamIds();
        ids.sensorId = internalId / MAX_VERSIONS_PER_SENSOR;
        ids.outputId = (int)((internalId / MAX_VERSIONS_PER_DATASTREAM) % MAX_DATASTREAMS_PER_SENSOR);
        ids.version = (int)(internalId % MAX_VERSIONS_PER_DATASTREAM);
        return ids;
    }*/

}
