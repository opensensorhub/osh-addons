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
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.system.SystemId;
import org.sensorhub.impl.service.sta.filter.DatastreamFilterVisitor;
import org.sensorhub.utils.SWEDataUtils;
import org.vast.data.DataIterator;
import org.vast.data.TextEncodingImpl;
import org.vast.ogc.om.IObservation;
import org.vast.swe.SWEBuilders.DataComponentBuilder;
import org.vast.swe.SWEBuilders.QuantityBuilder;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.unit.Unit;
import org.vast.util.Asserts;
import org.vast.util.TimeExtent;
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
import de.fraunhofer.iosb.ilt.frostserver.model.ext.TimeInterval;
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
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.ScalarComponent;
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
    static final String NOT_FOUND_MESSAGE = "Cannot find Datastream ";
    static final String NOT_WRITABLE_MESSAGE = "Cannot modify read-only Datastream ";
    static final String MISSING_ASSOC_MESSAGE = "Missing reference to Datastream or MultiDatastream entity";
    static final String WRONG_ASSOC_MESSAGE = "Cannot associate with read-only Datastream ";
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
        var federatedDataStreamStore = pm.readDatabase.getDataStreamStore();
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
                
                // get parent system handler
                var sysHandler = pm.transactionHandler.getSystemHandler(sensorId);
                if (sysHandler == null)
                    throw new NoSuchEntityException(SensorEntityHandler.NOT_FOUND_MESSAGE + sensorId);
                
                // create data stream object
                var sysUID = sysHandler.getSystemUID();
                var dsInfo = toSweDataStream(new SystemId(sensorId, sysUID), dataStream);

                // store in DB + send event
                var dsHandler = sysHandler.addOrUpdateDataStream(dsInfo);
                
                // associate with thing
                var dsID = dsHandler.getDataStreamKey().getInternalID();
                dataStreamWriteStore.putThingAssoc(thingId.getIdAsLong(), dsID.getIdAsLong());
                
                // handle associations / deep inserts
                ResourceId newDsId = new ResourceBigId(dsID);
                pm.observationHandler.handleObservationAssocList(newDsId, dataStream);
                
                return newDsId;
            });
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (DataStoreException e)
        {
            throw new IllegalArgumentException(e.getMessage());
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
        
        var id = (ResourceId)entity.getId();
        checkDatastreamWritable(id);
        
        // get transaction handler for existing datastream
        var dsHandler = pm.transactionHandler.getDataStreamHandler(id);
        if (dsHandler == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + entity.getId());
        var oldDsInfo = dsHandler.getDataStreamInfo();
        
        try
        {
            // create new data stream version
            var dsInfo = toSweDataStream(oldDsInfo.getSystemID(), dataStream);
            
            // update in DB
            return dsHandler.update(dsInfo);
        }
        catch (IllegalArgumentException | NoSuchEntityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error updating datastream " + id, e);
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
        checkDatastreamWritable(id);
        
        // get transaction handler for existing datastream
        var dsHandler = pm.transactionHandler.getDataStreamHandler(id);
        if (dsHandler == null)
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
        
        // delete from DB
        try
        {
            return dsHandler.delete();
        }
        catch (Exception e)
        {
            throw new ServerErrorException("Error deleting datastream " + id, e);
        }
    }


    @Override
    public AbstractDatastream getById(ResourceId id, Query q) throws NoSuchEntityException
    {
        securityHandler.checkPermission(securityHandler.sta_read_datastream);

        var dsKey = new DataStreamKey(id);
        var dsInfo = dataStreamReadStore.get(dsKey);
        if (dsInfo == null || !isDataStreamVisible(dsInfo, true))
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);

        return toFrostDatastream(id, dsInfo, q);
    }


    @Override
    @SuppressWarnings("unchecked")
    public EntitySet<?> queryCollection(ResourcePath path, Query q)
    {
        securityHandler.checkPermission(securityHandler.sta_read_datastream);

        DataStreamFilter filter = getFilter(path, q);
        int skip = q.getSkip(0);
        int limit = Math.min(q.getTopOrDefault(), maxPageSize);

        var multiDsRequest = path.getMainElementType() == EntityType.MULTIDATASTREAM;
        var entitySet = dataStreamReadStore.selectEntries(filter)
            .filter(e -> isDataStreamVisible(e.getValue(), multiDsRequest))
            .skip(skip)
            .limit(limit+1) // request limit+1 elements to handle paging
            .map(e -> toFrostDatastream(e.getKey().getInternalID(), e.getValue(), q))
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
                builder.withThings(thingId);
            }
            else if (idElt.getEntityType() == EntityType.SENSOR)
            {
                ResourceId sensorId = (ResourceId)idElt.getId();
                builder.withSystems(sensorId);
            }
            else if (idElt.getEntityType() == EntityType.OBSERVATION)
            {
                ResourceId obsId = (ResourceId)idElt.getId();
                builder.withObservations()
                    .withInternalIDs(obsId)
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
    
    
    protected boolean isDataStreamVisible(IDataStreamInfo dsInfo, boolean multiDs)
    {
        var dataStruct = dsInfo.getRecordStructure();
        
        if (!multiDs && !isScalarOutput(dataStruct))
            return false;
        
        // skip datastreams that contain a DataArray
        DataIterator it = new DataIterator(dataStruct);
        while (it.hasNext())
        {
            if (it.next() instanceof DataArray)
                return false;
        }
        
        return true;
    }


    protected boolean isScalarOutput(DataComponent rec)
    {
        return (rec.getComponentCount() <= 2 &&
            rec.getComponent(1) instanceof ScalarComponent);
    }
    
    
    protected IDataStreamInfo toSweDataStream(SystemId sysID, AbstractDatastream<?> abstractDs) throws NoSuchEntityException
    {
        var recordStruct = toSweCommon(abstractDs);
        return new DataStreamInfo.Builder()
            .withName(abstractDs.getName())
            .withDescription(abstractDs.getDescription())
            .withSystem(sysID)
            .withRecordDescription(recordStruct)
            .withRecordEncoding(new TextEncodingImpl())
            .build();
    }


    protected DataRecord toSweCommon(AbstractDatastream<?> abstractDs) throws NoSuchEntityException
    {
        SWEHelper fac = new SWEHelper();

        var rec = fac.createRecord()
            .name(SWEDataUtils.toNCName(abstractDs.getName()))
            .label(abstractDs.getName())
            .description(abstractDs.getDescription())
            .addField("time", fac.createTime().asPhenomenonTimeIsoUTC()
                .label("Sampling Time"));

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
                fac);

            rec.addField(comp.getName(), comp);
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
                    fac);
                i++;

                rec.addField(comp.getName(), comp);
            }
        }

        return rec.build();
    }


    protected DataComponent toComponent(String obsType, ObservedProperty obsProp, UnitOfMeasurement uom, SWEHelper fac)
    {
        DataComponentBuilder<? extends DataComponentBuilder<?,?>, ? extends DataComponent> comp = null;

        if (IObservation.OBS_TYPE_MEAS.equals(obsType))
        {
            comp = fac.createQuantity();

            if (uom.getDefinition() != null && uom.getDefinition().startsWith(UCUM_URI_PREFIX))
                ((QuantityBuilder)comp).uomCode(uom.getDefinition().replace(UCUM_URI_PREFIX, ""));
            else
                ((QuantityBuilder)comp).uomUri(uom.getDefinition());
        }
        else if (IObservation.OBS_TYPE_CATEGORY.equals(obsType))
            comp = fac.createCategory();
        else if (IObservation.OBS_TYPE_COUNT.equals(obsType))
            comp = fac.createCount();
        else if (IObservation.OBS_TYPE_RECORD.equals(obsType))
            comp = fac.createRecord();

        if (comp != null)
        {
            return comp.id(obsProp.getId().toString())
                .name(SWEDataUtils.toNCName(obsProp.getName()))
                .label(obsProp.getName())
                .description(obsProp.getDescription())
                .definition(obsProp.getDefinition())
                .build();
        }
        
        return null;
    }


    protected AbstractDatastream toFrostDatastream(BigId id, IDataStreamInfo dsInfo, Query q)
    {
        AbstractDatastream dataStream;
        Set<Property> select = q != null ? q.getSelect() : Collections.emptySet();
        boolean isExternalDatastream = !pm.isInWritableDatabase(id);

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
        dataStream.setId(new ResourceBigId(id));
        dataStream.setName(dsInfo.getName());
        dataStream.setDescription(dsInfo.getDescription());
        
        // time ranges
        dataStream.setPhenomenonTime(toTimeInterval(dsInfo.getPhenomenonTimeRange()));
        dataStream.setResultTime(toTimeInterval(dsInfo.getResultTimeRange()));
        
        // link to Thing
        var thingID =  isExternalDatastream ?
            pm.thingHandler.hubId :
            dataStreamWriteStore.getAssociatedThing(id.getIdAsLong());
        Thing thing = new Thing(new ResourceBigId(thingID));
        thing.setExportObject(false);
        dataStream.setThing(thing);

        // link to Sensor
        var sensorId = new ResourceBigId(dsInfo.getSystemID().getInternalID());
        Sensor sensor = new Sensor(sensorId);
        sensor.setExportObject(false);
        dataStream.setSensor(sensor);

        return dataStream;
    }
    
    
    protected TimeInterval toTimeInterval(TimeExtent te)
    {
        if (te == null)
            return null;
        
        return TimeInterval.create(
            te.begin().toEpochMilli(),
            te.end().toEpochMilli());
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
        Asserts.checkArgument(ds != null, MISSING_ASSOC_MESSAGE);
        ResourceId dsId;

        if (ds.getName() == null)
        {
            dsId = (ResourceId)ds.getId();
            Asserts.checkArgument(dsId != null, MISSING_ASSOC_MESSAGE);
            checkDatastreamID(dsId);
            if (!pm.isInWritableDatabase(dsId))
                throw new IllegalArgumentException(WRONG_ASSOC_MESSAGE + dsId);
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
                handleDatastreamAssoc(ds);
            }
        }

        if (sensor.getMultiDatastreams() != null)
        {
            for (MultiDatastream ds: sensor.getMultiDatastreams())
            {
                ds.setSensor(new Sensor(sensorId));
                handleDatastreamAssoc(ds);
            }
        }
    }


    /*
     * Check that datastream ID is present in database and exposed by service
     */
    protected void checkDatastreamID(ResourceId id) throws NoSuchEntityException
    {
        if (!dataStreamReadStore.containsKey(new DataStreamKey(id)))
            throw new NoSuchEntityException(NOT_FOUND_MESSAGE + id);
    }


    protected void checkDatastreamWritable(ResourceId id) throws NoSuchEntityException
    {
        checkTransactionsEnabled();
        checkDatastreamID(id);
        
        // TODO also check that current user has the right to write this datastream!
        
        if (!pm.isInWritableDatabase(id))
            throw new IllegalArgumentException(NOT_WRITABLE_MESSAGE + id);
    }
    
    
    protected void checkTransactionsEnabled()
    {
        if (dataStreamWriteStore == null)
            throw new UnsupportedOperationException(NO_DB_MESSAGE);
    }

}
