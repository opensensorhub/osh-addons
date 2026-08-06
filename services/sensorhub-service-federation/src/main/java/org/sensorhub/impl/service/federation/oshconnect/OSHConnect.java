package org.sensorhub.impl.service.federation.oshconnect;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of oshconnect.oshconnectapi.OSHConnect for the surface the broker uses:
 * a named container that nodes are registered with.
 */
public class OSHConnect
{
    private final String name;
    private final List<Node> nodes = new ArrayList<>();

    public OSHConnect(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void addNode(Node node)
    {
        nodes.add(node);
    }

    public List<Node> getNodes()
    {
        return nodes;
    }
}
