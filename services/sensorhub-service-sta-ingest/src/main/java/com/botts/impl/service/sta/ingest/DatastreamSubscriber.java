package com.botts.impl.service.sta.ingest;

import de.fraunhofer.iosb.ilt.sta.MqttException;
import de.fraunhofer.iosb.ilt.sta.model.Datastream;
import de.fraunhofer.iosb.ilt.sta.model.EntityType;
import de.fraunhofer.iosb.ilt.sta.model.Observation;
import de.fraunhofer.iosb.ilt.sta.model.builder.DatastreamBuilder;
import de.fraunhofer.iosb.ilt.sta.service.MqttSubscription;
import de.fraunhofer.iosb.ilt.sta.service.SensorThingsService;
import org.sensorhub.impl.system.DataStreamTransactionHandler;

public class DatastreamSubscriber implements Runnable {

    Datastream datastream;
    DataStreamTransactionHandler dataStreamTransactionHandler;
    SensorThingsService sts;

    public DatastreamSubscriber(Datastream datastream, DataStreamTransactionHandler dataStreamTransactionHandler, SensorThingsService sts) {
        this.datastream = datastream;
        this.dataStreamTransactionHandler = dataStreamTransactionHandler;
        this.sts = sts;
    }

    public void doStart() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            System.out.println("Starting MQTT subscription on datastream " + datastream.getId());
            MqttSubscription subscription = DatastreamBuilder.builder()
                    .service(this.sts)
                    .id(this.datastream.getId())
                    .build()
                    .<Observation>subscribeRelative(observation -> {
                        System.out.println("Received new observation from " + datastream.getName());
                        dataStreamTransactionHandler.addObs(
                                STAUtils.toObsData(
                                        observation,
                                        dataStreamTransactionHandler.getDataStreamKey().getInternalID(),
                                        null));
                    }, EntityType.OBSERVATIONS);
            this.sts.unsubscribe(subscription);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }
}
