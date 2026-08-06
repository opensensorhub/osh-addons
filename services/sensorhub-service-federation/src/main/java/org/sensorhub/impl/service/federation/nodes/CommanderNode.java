package org.sensorhub.impl.service.federation.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sensorhub.impl.service.federation.events.Event;
import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.Node;
import org.sensorhub.impl.service.federation.oshconnect.System;

import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of nodes.CommanderNode.
 */
public class CommanderNode extends BrokeredNode
{
    private final Map<String, Node> remoteNodes = new HashMap<>();
    private final List<Map.Entry<System, System>> systemMap = new ArrayList<>();
    private final List<Map.Entry<Datastream, Datastream>> datastreamMap = new ArrayList<>();
    private final List<Map.Entry<ControlStream, ControlStream>> controlstreamMap = new ArrayList<>();

    public CommanderNode(Node node)
    {
        super(node);
    }

    public void publishEvent(Event event)
    {
        for (var listener : listeners)
            listener.handleEvents(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleEvents(Event event)
    {
        String[] parsed = parseTopic(event.topic);
        String nodeId = parsed[0];

        // Handle specific types first
        switch (event.type)
        {
            case ADD_REMOTE_NODE:
                log.debug("CommanderNode received ADD_REMOTE_NODE event: {}", nodeId);
                remoteNodes.put(nodeId, (Node) ((Map<String, Object>) event.data).get("node"));
                break;
            case NEW_OBSERVATION:
                log.debug("CommanderNode received NEW_OBSERVATION event: {}", event);
                break;
            case ADD_DATASTREAM:
                log.debug("CommanderNode received ADD_DATASTREAM event: {}", event);
                break;
            case ADD_COMMANDER:
                return;
            case ADD_CONTROLSTREAM:
                log.debug("CommanderNode received ADD_CONTROLSTREAM event: {}", event);
                break;
            case ADD_SYSTEM:
                log.debug("CommanderNode received ADD_SYSTEM event: {}", event);
                handleNewSystem(event);
                break;
            default:
                return;
        }
    }

    @SuppressWarnings("unchecked")
    public void handleNewSystem(Event event)
    {
        // parse event topic to find target remote node
        String[] parsed = parseTopic(event.topic);
        String nodeId = parsed[0];
        Node targetNode = remoteNodes.get(nodeId);
        if (targetNode == null)
            return;
        // The Python source deep-copies the system here; the wrapper carries no
        // clone, so the reference is reused. (insert_resource=True POSTs it.)
        System sysCopy = (System) ((Map<String, Object>) event.data).get("system");
        targetNode.addSystem(sysCopy, true);
    }

    public Map<String, Node> getRemoteNodes()
    {
        return remoteNodes;
    }

    public List<Map.Entry<System, System>> getSystemMap()
    {
        return systemMap;
    }

    public List<Map.Entry<Datastream, Datastream>> getDatastreamMap()
    {
        return datastreamMap;
    }

    public List<Map.Entry<ControlStream, ControlStream>> getControlstreamMap()
    {
        return controlstreamMap;
    }
}
