package org.sensorhub.impl.service.federation.nodes;

import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.service.federation.events.IEventListener;
import org.sensorhub.impl.service.federation.oshconnect.Node;

/**
 * Port of nodes.BrokeredNode.
 */
public abstract class BrokeredNode implements IEventListener
{
    protected final Node node;
    protected final List<IEventListener> listeners = new ArrayList<>();

    protected BrokeredNode(Node node)
    {
        this.node = node;
    }

    public Node getNode()
    {
        return node;
    }

    public List<IEventListener> getListeners()
    {
        return listeners;
    }

    /**
     * Parse a topic string into (node_id, resource_id). The resource differs by
     * context — a system topic often has no resource id, a datastream topic has
     * a system parent, an observation topic has a datastream parent.
     */
    public String[] parseTopic(String topic)
    {
        int idx = topic.lastIndexOf('/');
        String nodeId = idx >= 0 ? topic.substring(0, idx) : topic;
        String resourceId = idx >= 0 ? topic.substring(idx + 1) : topic;
        return new String[] { nodeId, resourceId };
    }
}
