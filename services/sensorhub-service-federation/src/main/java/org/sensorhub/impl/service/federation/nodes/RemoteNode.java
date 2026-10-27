package org.sensorhub.impl.service.federation.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sensorhub.impl.service.federation.events.Event;
import org.sensorhub.impl.service.federation.events.EventHandler;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.Node;
import org.sensorhub.impl.service.federation.oshconnect.System;

/**
 * Port of nodes.RemoteNode.
 */
public class RemoteNode extends BrokeredNode
{
    private final EventHandler eventHandler = new EventHandler();
    private final List<Map.Entry<System, System>> rmtSystemMap = new ArrayList<>();
    private final List<Map.Entry<Datastream, Datastream>> rmtDatastreamMap = new ArrayList<>();

    public RemoteNode(Node node)
    {
        super(node);
    }

    public void publishEvent(Event event)
    {
        for (var listener : listeners)
            listener.handleEvents(event);
    }

    @Override
    public void handleEvents(Event event)
    {
        String[] parsed = parseTopic(event.topic);
        String nodeId = parsed[0];
        if (nodeId.equals(node.getId()))
            return;
    }

    public EventHandler getEventHandler()
    {
        return eventHandler;
    }

    public List<Map.Entry<System, System>> getRmtSystemMap()
    {
        return rmtSystemMap;
    }

    public List<Map.Entry<Datastream, Datastream>> getRmtDatastreamMap()
    {
        return rmtDatastreamMap;
    }
}
