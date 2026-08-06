package org.sensorhub.impl.service.federation;

import java.util.ArrayList;
import java.util.List;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.service.federation.environment.NodeEnvData;

/**
 * Configuration for the federated broker service.
 *
 * This is the one structural change from the Python broker: the information that
 * lived in broker-env2.json (the {@code nodes} array — commander + remotes, with
 * their connection and auth details) is provided here through the OSH admin web
 * page instead of a JSON file.
 */
public class FederatedBrokerConfig extends ServiceConfig
{
    @DisplayInfo(label = "Nodes", desc = "Federated OSH nodes (one commander plus one or more remotes). "
            + "Mirrors the 'nodes' array of the Python broker's broker-env2.json.")
    public List<NodeEnvData> nodes = new ArrayList<>();

    @DisplayInfo(label = "Enable Binary Datastreams", desc = "Federate datastreams that advertise "
            + "application/swe+binary (e.g. video) as an opaque swe+binary passthrough. "
            + "When off, such datastreams are skipped and JSON datastreams are unaffected.")
    public boolean enableBinaryDatastreams = false;
}
