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

    @DisplayInfo(label = "Status Report Interval (s)", desc = "How often the service publishes a "
            + "federation status summary (nodes, mirrored datastreams/control streams, active threads). "
            + "Set to 0 to disable status reporting.")
    public int statusReportIntervalSeconds = 30;

    @DisplayInfo(label = "Reconcile Interval (s)", desc = "How often the broker re-discovers remote "
            + "topology to pick up datastreams/control streams that appeared and retire ones that "
            + "vanished. Set to 0 to fix the topology at startup (discover on run-up only).")
    public int reconcileIntervalSeconds = 30;

    @DisplayInfo(label = "Removed After Cycles", desc = "Consecutive reconcile cycles a known stream "
            + "must be absent from a REACHABLE remote before it is retired (its pump stopped and routing "
            + "entry dropped). A node that is simply down does not count against this.")
    public int removedAfterCycles = 3;

    @DisplayInfo(label = "On Removed", desc = "What to do with a retired stream's commander-side mirror: "
            + "'keep' leaves it in place (history preserved), 'delete' removes it from the commander.")
    public String onRemoved = "keep";
}
