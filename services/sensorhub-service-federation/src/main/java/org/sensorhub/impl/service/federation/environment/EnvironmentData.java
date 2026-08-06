package org.sensorhub.impl.service.federation.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Port of environment.EnvironmentData. Holds the list of nodes that, in the
 * Python broker, came from broker-env2.json; here it is populated from the
 * federation service config.
 */
public class EnvironmentData
{
    public List<NodeEnvData> nodes;

    public EnvironmentData()
    {
        this.nodes = new ArrayList<>();
    }

    public EnvironmentData(List<NodeEnvData> nodes)
    {
        this.nodes = nodes;
    }

    /** Deletes authentication data from all nodes in memory. */
    public void deleteAuthInMem()
    {
        for (NodeEnvData node : nodes)
            node.auth = null;
    }

    /** Returns the list of nodes that are commanders. */
    public List<NodeEnvData> getCommanderNodes()
    {
        return nodes.stream().filter(node -> node.isCommander).collect(Collectors.toList());
    }
}
