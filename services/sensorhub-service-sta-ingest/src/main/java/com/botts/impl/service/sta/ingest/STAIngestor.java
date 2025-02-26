package com.botts.impl.service.sta.ingest;

import de.fraunhofer.iosb.ilt.sta.MqttException;
import de.fraunhofer.iosb.ilt.sta.ServiceFailureException;
import de.fraunhofer.iosb.ilt.sta.model.*;
import de.fraunhofer.iosb.ilt.sta.service.MqttConfig;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import net.opengis.OgcProperty;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.impl.system.SystemTransactionHandler;
import org.sensorhub.impl.system.SystemUtils;
import org.sensorhub.impl.system.wrapper.SystemWrapper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
public class STAIngestor
{
    private final SensorThingsService sts;
    SystemDatabaseTransactionHandler transactionHandler;
    boolean usingStateDb;
    boolean isMqttEnabled;

    class SensorData
    {
        AbstractProcess smlDescription;
        Map<String, Datastream> datastreams;
        public SensorData(AbstractProcess smlDescription)
        {
            this.smlDescription = smlDescription;
            this.datastreams = new HashMap<>();
        }
    }

    public STAIngestor(URL apiUrl, MqttConfig mqttConfig, boolean usingStateDb, SystemDatabaseTransactionHandler transactionHandler) throws MalformedURLException, MqttException
    {
        if(mqttConfig != null)
        {
            this.sts = new SensorThingsService(apiUrl, mqttConfig);
            this.isMqttEnabled = true;
        }
        else
        {
            this.sts = new SensorThingsService(apiUrl);
            this.isMqttEnabled = false;
        }
        this.transactionHandler = transactionHandler;
        this.usingStateDb = usingStateDb;
    }

    public void ingest()
    {
        try
        {
            // Get all things
            var things = sts.things().query().list();
            Iterator<Thing> i = things.fullIterator();
            while(i.hasNext())
                registerThing(i.next());
            // TODO: Register tasking capabilities/actuators as OSH Control Stream
            // TODO: Use SWE JSON Bindings to convert taskingParameters to SWE command params

        }
        catch (ServiceFailureException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void registerThing(Thing thing)
    {
        Thread thread = new Thread(() ->
        {
            try
            {
                // SensorML-ify the Thing
                var smlThing = STAUtils.toSmlProcess(thing);
                var parentSystem = new SystemWrapper(smlThing);
                // Add system to database
                var handler = transactionHandler.addOrUpdateSystem(parentSystem);

                try
                {
                    // Add Thing's locations as FOIs
                    var thingLocations = thing.locations().query().list();
                    for(Location location : thingLocations.toList())
                        handler.addFoi(STAUtils.toGmlFeature(location, STAUtils.toUid(location.getName(), location.getId())));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                var datastreams = thing.datastreams().query().top(100).list();
                Iterator<Datastream> datastreamIterator = datastreams.fullIterator();

                Map<Id<?>, SensorData> smlSensorMap = new HashMap<>();

                while(datastreamIterator.hasNext())
                {
                    var datastream = datastreamIterator.next();
                    var sensor = datastream.getSensor();

                    // Keep track of Sensors
                    var sensorData = smlSensorMap.get(sensor.getId());
                    if(sensorData == null)
                    {
                        sensorData = new SensorData(STAUtils.toSmlProcess(sensor));
                        smlSensorMap.put(sensor.getId(), sensorData);
                    }

                    // Add datastream output to its sensor description
                    sensorData.smlDescription.addOutput(datastream.getName(), STAUtils.toSweCommon(datastream));

                    // Keep track of the association between current datastream and sensor
                    sensorData.datastreams.put(datastream.getName(), datastream);
                }

                // Register Sensors to parent Thing
                for(SensorData sensorData : smlSensorMap.values())
                    registerSensor(sensorData, handler);

                // TODO: Register tasking capabilities
//                var taskingCapabilities = thing.taskingCapabilities().query().list();
//                Iterator<TaskingCapability> taskingCapabilityIterator = taskingCapabilities.fullIterator();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to register Thing: {}" + thing.toString(), e);
            }
        });
        thread.start();
    }

    private void registerSensor(SensorData sensorData, SystemTransactionHandler parentHandler) throws DataStoreException, ServiceFailureException {
        var smlSensor = sensorData.smlDescription;

        // Add or update member handler
        var system = new SystemWrapper(smlSensor);
        var memberHandler = parentHandler.addOrUpdateMember(system);

        // Create datastreams if we have outputs
        if (smlSensor.getNumOutputs() > 0)
            SystemUtils.addDatastreamsFromOutputs(memberHandler, smlSensor.getOutputList());

        // Add historical observations and create MQTT subscription
        for (OgcProperty<AbstractSWEIdentifiable> output : system.getFullDescription().getOutputList().getProperties())
        {
            // Get datastream handler for output
            var dsHandler = this.transactionHandler.getDataStreamHandler(memberHandler.getSystemUID(), output.getName());
            var datastream = sensorData.datastreams.get(output.getName());

            // Add historical observations
            if(!this.usingStateDb)
            {
                var observations = datastream.observations().query().list();
                Iterator<Observation> i = observations.fullIterator();

                while(i.hasNext())
                {
                    var observation = i.next();
                    // Add FOI if available
                    var feature = observation.getFeatureOfInterest();
                    BigId featureId = null;
                    if(feature != null)
                        featureId = parentHandler.addOrUpdateFoi(STAUtils.toGmlFeature(feature, STAUtils.toUid(feature.getName(), feature.getId()))).getInternalID();
                    var obs = STAUtils.toObsData(observation, dsHandler.getDataStreamKey().getInternalID(), featureId);
                    // Add observation
                    dsHandler.addObs(obs);
                }
            }

            // Subscribe if MQTT available
            if (this.isMqttEnabled)
                new DatastreamSubscriber(datastream, dsHandler, this.sts).doStart();
        }
    }

}
