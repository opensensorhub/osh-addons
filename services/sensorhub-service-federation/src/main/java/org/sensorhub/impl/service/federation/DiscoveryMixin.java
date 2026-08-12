package org.sensorhub.impl.service.federation;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.sensorhub.impl.service.federation.nodes.CommanderNode;
import org.sensorhub.impl.service.federation.nodes.RemoteNode;
import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.EncodingMode;
import org.sensorhub.impl.service.federation.oshconnect.StreamableModes;
import org.sensorhub.impl.service.federation.oshconnect.System;

import com.google.gson.JsonObject;

import static org.sensorhub.impl.service.federation.BrokerLogging.DEBUG_VERBOSE;
import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of broker.discovery.DiscoveryMixin.
 *
 * Two Python helpers are intentionally not carried over because they are dead
 * code that depend on facilities outside this port: {@code _pump_inbound_deque}
 * (an asyncio alternative to the threaded {@code test_obs_retrieval} pump, never
 * invoked) and {@code _debug_and_subscribe_default} (a SUBACK-reason-code
 * debugger reaching into paho callback internals, never invoked).
 */
public interface DiscoveryMixin extends MirroringMixin, ObservationMixin, SchemaBuildersMixin
{
    /**
     * Discover control streams on all remote nodes; return a flat list of
     * (System, ControlStream) entries.
     */
    default List<Map.Entry<System, ControlStream>> discoverRemoteControlstreams()
    {
        List<Map.Entry<System, ControlStream>> rmtDiscovered = new ArrayList<>();

        for (RemoteNode rmtNode : getRemoteNodes())
        {
            String nodeAddr = rmtNode.getNode().getAddress() + ":" + rmtNode.getNode().getPort();
            List<System> rmtSys;
            try
            {
                rmtSys = rmtNode.getNode().discoverSystems();
            }
            catch (Exception e)
            {
                log.warn("Failed to connect to remote node {}: {}", nodeAddr, e.toString());
                continue;
            }

            for (System sys : rmtSys)
            {
                try
                {
                    List<ControlStream> controlstreams = sys.discoverControlstreams();
                    if (!controlstreams.isEmpty())
                        log.debug("Found {} control stream(s) for {}", controlstreams.size(), sys.getUrn());
                    for (ControlStream cs : controlstreams)
                        rmtDiscovered.add(Map.entry(sys, cs));
                }
                catch (Exception e)
                {
                    log.warn("Error discovering control streams for {}: {}", sys.getUrn(), e.toString());
                }
            }
        }

        return rmtDiscovered;
    }

    /**
     * Discover control streams on remote nodes and mirror them to the commander.
     */
    default List<Map.Entry<System, ControlStream>> discoverAndMirrorControlstreams()
    {
        List<Map.Entry<System, ControlStream>> remoteCsInfo = discoverRemoteControlstreams();

        if (getCommandNodes().isEmpty())
        {
            log.warn("[CONTROLSTREAM] no commander nodes configured; skipping control streams");
            return remoteCsInfo;
        }

        CommanderNode commander = getCommandNodes().get(0);
        log.info("[CONTROLSTREAM] Mirroring {} control stream(s) to commander", remoteCsInfo.size());
        mirrorControlstreamsToCommander(commander.getNode(), remoteCsInfo);
        return remoteCsInfo;
    }

    /**
     * Discover remote systems and mirror them to the first commander node.
     */
    default List<System> discoverAndMirrorSystems()
    {
        if (getEnv() == null)
            throw new IllegalStateException("Environment not loaded");
        if (getCommandNodes().isEmpty())
        {
            log.warn("[discover] no commander nodes configured; skipping systems");
            return null;
        }

        CommanderNode commander = getCommandNodes().get(0);
        log.info("[DISCOVER] using commander at {}:{}", commander.getNode().getAddress(),
                commander.getNode().getPort());

        List<System> remoteSystems = discoverRemoteSystems();

        log.info("[DISCOVER] discovered {} remote systems", remoteSystems.size());
        mirrorSystemsToCommander(commander.getNode(), remoteSystems);
        return remoteSystems;
    }

    /**
     * Discover systems on all remote nodes; return a flat list of Systems.
     */
    default List<System> discoverRemoteSystems()
    {
        List<System> allSystems = new ArrayList<>();
        for (RemoteNode rmtNode : getRemoteNodes())
        {
            rmtNode.getNode().discoverSystems();
            allSystems.addAll(rmtNode.getNode().systems());
        }
        return allSystems;
    }

    /**
     * Pure read: discover datastreams on every remote node and return
     * (System, Datastream) entries. Safe to call repeatedly.
     */
    default List<Map.Entry<System, Datastream>> discoverRemoteDatastreams()
    {
        List<Map.Entry<System, Datastream>> rmtDiscovered = new ArrayList<>();

        for (RemoteNode rmtNode : getRemoteNodes())
        {
            List<System> rmtSys = rmtNode.getNode().discoverSystems();
            for (System sys : rmtSys)
            {
                for (Datastream datastream : sys.discoverDatastreams())
                {
                    // Binary (e.g. video) datastreams are only federated when opted in;
                    // otherwise skip them entirely so JSON federation is unaffected.
                    if (datastream.getEncodingMode() == EncodingMode.BINARY && !isBinaryDatastreamsEnabled())
                    {
                        log.info("Skipping binary datastream {} (binary datastreams disabled)", datastream.getId());
                        continue;
                    }
                    datastream.initialize();
                    rmtDiscovered.add(Map.entry(sys, datastream));
                }
            }
        }

        log.info("Discovered {} remote datastream(s)", rmtDiscovered.size());
        return rmtDiscovered;
    }

    /**
     * Per remote datastream: set PULL mode, then init MQTT + start (subscribes to
     * the OBSERVATION topic; observations land in each datastream's inbound deque).
     */
    default void subscribeToRemoteObservations(List<Map.Entry<System, Datastream>> remoteDsInfo)
    {
        for (Map.Entry<System, Datastream> entry : remoteDsInfo)
        {
            Datastream dsObj = entry.getValue();
            dsObj.setConnectionMode(StreamableModes.PULL);
            dsObj.initMqtt();
            dsObj.start(); // PULL mode -> subscribes to the OBSERVATION topic
        }
        log.info("Subscribed to observations for {} datastream(s)", remoteDsInfo.size());
    }

    /**
     * Per remote datastream: spawn a daemon thread that drains the inbound deque
     * and forwards observations to the commander mirror via ds_map.
     */
    default void startObservationPumps(List<Map.Entry<System, Datastream>> remoteDsInfo)
    {
        for (Map.Entry<System, Datastream> entry : remoteDsInfo)
        {
            Datastream dsObj = entry.getValue();
            Thread obsThread = new Thread(() -> testObsRetrieval(dsObj));
            obsThread.setDaemon(true);
            getWorkerThreads().add(obsThread);
            obsThread.start();
        }
        log.info("Started observation pumps for {} datastream(s)", remoteDsInfo.size());
    }

    /**
     * Orchestrator: discover -> subscribe to obs -> mirror to commander -> start
     * pumps. Subscription happens BEFORE mirroring so observations accumulate
     * while the mirror call runs; the pump runs LAST so ds_map lookups always
     * find a commander mirror.
     */
    default List<Map.Entry<System, Datastream>> discoverAndMirrorDatastreams()
    {
        List<Map.Entry<System, Datastream>> remoteDsInfo = discoverRemoteDatastreams();
        subscribeToRemoteObservations(remoteDsInfo);

        if (getCommandNodes().isEmpty())
        {
            log.warn("No commander nodes configured; skipping datastream mirror");
            return remoteDsInfo;
        }

        CommanderNode commander = getCommandNodes().get(0);
        mirrorDatastreamsToCommander(commander.getNode(), remoteDsInfo);

        startObservationPumps(remoteDsInfo);
        return remoteDsInfo;
    }

    default void testObsRetrieval(Datastream datastream)
    {
        if (datastream == null)
            return;

        String dsId = datastream.getId() != null ? datastream.getId() : "unknown";

        long checkCount = 0;
        while (true)
        {
            if (Thread.currentThread().isInterrupted())
                return;
            try
            {
                Deque<byte[]> inbound = datastream.getInboundDeque();
                checkCount++;

                // Periodic status (every ~20 seconds at 0.2s sleep)
                if (checkCount % 100 == 0 && DEBUG_VERBOSE)
                    log.debug("ds={} deque_len={}", dsId, inbound.size());

                if (!inbound.isEmpty())
                {
                    byte[] obs = inbound.pollFirst();
                    // Route by the node-qualified key so streams from different
                    // nodes that share a bare id never collide in ds_map.
                    String routeKey = datastream.getRemoteKey();
                    if (datastream.getEncodingMode() == EncodingMode.BINARY)
                    {
                        // Opaque passthrough: relay the raw swe+binary frame unchanged.
                        routeBinaryObservationToCommanderDatastream(routeKey, obs);
                    }
                    else
                    {
                        List<JsonObject> obsData = parseObservation(obs, dsId);
                        if (obsData != null)
                        {
                            for (JsonObject item : obsData)
                                routeObservationToCommanderDatastream(routeKey, item);
                        }
                    }
                }
                else
                {
                    Thread.sleep(50);
                }
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                return;
            }
            catch (Exception e)
            {
                log.error("Observation retrieval error for {}: {}", dsId, e.toString());
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    default void discoverControlStreams()
    {
        // Placeholder in the Python source (no body).
    }

    /**
     * For each remote datastream, start MQTT so observations are streamed via
     * MQTT and forwarded.
     */
    default void startRemoteMqttStreams(List<Map.Entry<System, Datastream>> remoteDsInfo)
    {
        for (Map.Entry<System, Datastream> entry : remoteDsInfo)
        {
            System remoteSys = entry.getKey();
            Datastream remoteDs = entry.getValue();
            try
            {
                remoteDs.initMqtt();
                remoteDs.start(); // subscribes to OBSERVATION topic
            }
            catch (Exception e)
            {
                log.error("Failed to start MQTT for datastream on {}: {}", remoteSys.getUrn(), e.toString());
            }
        }
    }
}
