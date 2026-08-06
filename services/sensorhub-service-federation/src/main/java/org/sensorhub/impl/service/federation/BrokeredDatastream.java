package org.sensorhub.impl.service.federation;

import java.nio.charset.StandardCharsets;

import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.DatastreamResource;
import org.sensorhub.impl.service.federation.oshconnect.Node;
import org.sensorhub.impl.service.federation.oshconnect.StreamableModes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of broker.datastreams.BrokeredDatastream.
 *
 * Datastream subclass that forwards MQTT observations to the OSHDataBroker. It
 * subscribes (PULL mode) to the remote datastream's MQTT topic and forwards
 * received observations to the broker.
 */
public class BrokeredDatastream extends Datastream
{
    private final OSHDataBroker broker;

    public BrokeredDatastream(Node parentNode, DatastreamResource datastreamResource, OSHDataBroker broker)
    {
        super(parentNode, datastreamResource);
        // Keep a reference to the broker for routing
        this.broker = broker;
        // Force this datastream to operate in PULL mode so it SUBSCRIBES
        setConnectionMode(StreamableModes.PULL);
    }

    @Override
    protected void mqttSubCallback(String msgTopic, byte[] payload)
    {
        // Keep base behavior (enqueue)
        try
        {
            super.mqttSubCallback(msgTopic, payload);
        }
        catch (Exception e)
        {
            log.debug("Base MQTT callback error: {}", e.toString());
        }

        JsonElement data;
        try
        {
            data = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            return;
        }

        if (!data.isJsonObject())
            return;
        JsonObject obj = data.getAsJsonObject();
        if (!obj.has("items") || !obj.get("items").isJsonArray())
            return;

        for (JsonElement itemEl : obj.getAsJsonArray("items"))
        {
            if (!itemEl.isJsonObject())
                continue;
            JsonObject item = itemEl.getAsJsonObject();
            String remoteDsId = item.has("datastream@id") && !item.get("datastream@id").isJsonNull()
                    ? item.get("datastream@id").getAsString() : getId();
            broker.routeObservationToCommanderDatastream(remoteDsId, item);
        }
    }
}
