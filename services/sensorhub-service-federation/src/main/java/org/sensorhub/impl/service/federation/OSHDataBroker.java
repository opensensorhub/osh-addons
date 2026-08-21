package org.sensorhub.impl.service.federation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.sensorhub.impl.service.federation.environment.EnvironmentData;
import org.sensorhub.impl.service.federation.environment.NodeEnvData;
import org.sensorhub.impl.service.federation.events.DefaultEventTypes;
import org.sensorhub.impl.service.federation.events.Event;
import org.sensorhub.impl.service.federation.events.EventHandler;
import org.sensorhub.impl.service.federation.nodes.CommanderNode;
import org.sensorhub.impl.service.federation.nodes.RemoteNode;
import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.Node;
import org.sensorhub.impl.service.federation.oshconnect.OSHConnect;
import org.sensorhub.impl.service.federation.oshconnect.System;

import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of broker.core.OSHDataBroker — the class composed from the broker mixins
 * (Discovery, Mirroring, Observation, CommandRouting, SchemaBuilders). Java
 * cannot share mixin state through multiple inheritance, so the mixins are
 * interfaces with default methods that read shared state via {@link BrokerContext},
 * which this class implements.
 *
 * The one structural change from the Python broker (per the porting brief) is
 * that the node environment comes from the service {@link FederatedBrokerConfig}
 * instead of broker-env2.json.
 */
public class OSHDataBroker implements DiscoveryMixin, MirroringMixin, ObservationMixin,
        CommandRoutingMixin, SchemaBuildersMixin, ReconcileMixin
{
    private final EventHandler eventHandler = new EventHandler();
    private EnvironmentData env;
    private List<CommanderNode> commandNodes = new ArrayList<>();
    private List<RemoteNode> remoteNodes = new ArrayList<>();
    private final Map<String, Datastream> dsMap = new ConcurrentHashMap<>();
    private final Map<String, Map.Entry<System, ControlStream>> csMap = new ConcurrentHashMap<>();
    private final List<Thread> workerThreads = new CopyOnWriteArrayList<>();
    private final StreamRegistry streamRegistry = new StreamRegistry();
    private boolean binaryDatastreamsEnabled = false;
    private int removedAfterCycles = 3;
    private boolean deleteOnRemoved = false;

    public OSHDataBroker()
    {
    }

    /**
     * Tear down everything this broker started: interrupt the observation pump
     * and command-forwarding threads, and disconnect every node's MQTT client so
     * no subscription keeps delivering. Called from the service's doStop so a
     * module restart doesn't leave duplicate forwarders behind.
     */
    public void shutdown()
    {
        for (Thread t : workerThreads)
            t.interrupt();
        workerThreads.clear();

        // Interrupt every per-stream reconcile pump/forwarder (these are tracked
        // in the stream registry, not workerThreads) and clear the registry.
        for (PumpEntry e : streamRegistry.snapshot())
        {
            if (e.thread != null)
                e.thread.interrupt();
            streamRegistry.remove(e.key);
        }

        for (CommanderNode n : commandNodes)
            stopMqtt(n.getNode());
        for (RemoteNode n : remoteNodes)
            stopMqtt(n.getNode());
    }

    private static void stopMqtt(Node node)
    {
        if (node != null && node.getMqttClient() != null)
        {
            try
            {
                node.getMqttClient().stop();
            }
            catch (Exception e)
            {
                log.debug("Error stopping MQTT client: {}", e.toString());
            }
        }
    }

    /**
     * Port of {@code load_env_file}, sourcing the node environment from the
     * service config rather than broker-env2.json. Creates the commander and
     * remote nodes and publishes the corresponding add events.
     */
    public void loadFromConfig(FederatedBrokerConfig config)
    {
        this.binaryDatastreamsEnabled = config.enableBinaryDatastreams;
        this.removedAfterCycles = config.removedAfterCycles;
        this.deleteOnRemoved = "delete".equalsIgnoreCase(config.onRemoved);
        this.env = new EnvironmentData(config.nodes);
        OSHConnect oshMain = new OSHConnect("OSH Data Brokerage");

        List<CommanderNode> cmdNodes = new ArrayList<>();
        List<RemoteNode> rmtNodes = new ArrayList<>();

        log.info("Loading environment configuration...");

        for (NodeEnvData node : env.nodes)
        {
            // Choose MQTT port explicitly (HTTP != MQTT)
            int mqttPort = node.mqttPort;

            Node newNode = new Node(
                    node.protocol,
                    node.address,
                    node.port,                  // HTTP API port
                    node.auth.username,
                    node.auth.password,
                    true,                       // Enable MQTT - required for discover_systems()
                    mqttPort);                  // MQTT port

            String role = node.isCommander ? "COMMANDER" : "REMOTE";

            log.info("  {}: {}:{} (MQTT:{})", role, newNode.getAddress(), newNode.getPort(), mqttPort);

            oshMain.addNode(newNode);
            if (node.isCommander)
            {
                CommanderNode cmdNode = new CommanderNode(newNode);
                cmdNodes.add(cmdNode);
                eventHandler.publish(new Event(Instant.now(), DefaultEventTypes.ADD_COMMANDER,
                        DefaultEventTypes.ADD_COMMANDER.value(), Map.of("node", node), this));
            }
            else
            {
                RemoteNode rmtNode = new RemoteNode(newNode);
                rmtNodes.add(rmtNode);
                eventHandler.publish(new Event(Instant.now(), DefaultEventTypes.ADD_REMOTE_NODE,
                        DefaultEventTypes.ADD_REMOTE_NODE.value(), Map.of("node", node), this));
            }
        }

        this.commandNodes = cmdNodes;
        this.remoteNodes = rmtNodes;
        log.info("Loaded {} commander(s), {} remote node(s)", cmdNodes.size(), rmtNodes.size());
    }

    /**
     * High-level orchestrator. Delegates to {@link #reconcileOnce()} so startup
     * and steady-state discovery share one code path: the first cycle discovers +
     * mirrors every system, datastream, and control stream (registering a pump for
     * each), and every subsequent cycle picks up resources that appeared and
     * retires ones that vanished.
     */
    public void discoverAll()
    {
        if (env == null)
            throw new IllegalStateException("Environment not loaded");
        if (commandNodes.isEmpty())
            return;

        log.info("==================================================");
        log.info("Starting discovery and mirroring...");
        log.info("==================================================");

        reconcileOnce();

        log.info("Discovery complete. Waiting 5s for stabilization...");
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    // ---- BrokerContext ------------------------------------------------------

    @Override
    public EventHandler getEventHandler()
    {
        return eventHandler;
    }

    @Override
    public EnvironmentData getEnv()
    {
        return env;
    }

    @Override
    public List<CommanderNode> getCommandNodes()
    {
        return commandNodes;
    }

    @Override
    public List<RemoteNode> getRemoteNodes()
    {
        return remoteNodes;
    }

    @Override
    public Map<String, Datastream> getDsMap()
    {
        return dsMap;
    }

    @Override
    public Map<String, Map.Entry<System, ControlStream>> getCsMap()
    {
        return csMap;
    }

    @Override
    public List<Thread> getWorkerThreads()
    {
        return workerThreads;
    }

    @Override
    public boolean isBinaryDatastreamsEnabled()
    {
        return binaryDatastreamsEnabled;
    }

    @Override
    public StreamRegistry getStreamRegistry()
    {
        return streamRegistry;
    }

    @Override
    public int getRemovedAfterCycles()
    {
        return removedAfterCycles;
    }

    @Override
    public boolean isDeleteOnRemoved()
    {
        return deleteOnRemoved;
    }
}
