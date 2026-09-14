package org.sensorhub.impl.service.federation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sensorhub.impl.service.federation.nodes.RemoteNode;
import org.sensorhub.impl.service.federation.oshconnect.APIHelper;
import org.sensorhub.impl.service.federation.oshconnect.APIResourceTypes;
import org.sensorhub.impl.service.federation.oshconnect.ApiResponse;
import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.EncodingMode;
import org.sensorhub.impl.service.federation.oshconnect.MqttCommClient;
import org.sensorhub.impl.service.federation.oshconnect.Node;
import org.sensorhub.impl.service.federation.oshconnect.System;

import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of broker.reconcile.ReconcileMixin — periodic re-discovery of remote
 * topology and convergence, so resources that appear or disappear on remotes
 * <em>after</em> startup are federated (or retired) instead of the topology being
 * fixed at run-up only.
 *
 * Each cycle, for datastreams AND control streams:
 * <ul>
 *   <li>per remote node, discover systems + streams (unreachable nodes are
 *       skipped and their registry entries left untouched — the shared comm
 *       client auto-reconnects, so pumps resume when the node returns);</li>
 *   <li>mirror systems/streams that appeared (datastreams: subscribe remote →
 *       mirror → pump; control streams: mirror → subscribe commander →
 *       forwarder);</li>
 *   <li>detect schema drift (record/command schema) on known streams → retire +
 *       re-mirror;</li>
 *   <li>count consecutive absences of known streams on REACHABLE nodes; after
 *       {@code removedAfterCycles} the stream is retired (pump interrupted,
 *       unsubscribed, routing entry dropped; commander mirror kept or deleted
 *       per {@code onRemoved}).</li>
 * </ul>
 *
 * The first call to {@link #reconcileOnce()} doubles as the startup path, so
 * one-shot and steady-state discovery use the same code.
 *
 * <p>Two Python facilities are intentionally not carried over: the ops/telemetry
 * bus ({@code _publish_reachability_transitions} and the {@code _bus_events}
 * publishes) is out of scope for this port; and the Python per-stream
 * {@code threading.Event} stop signal is replaced by interrupting the pump
 * {@link Thread} directly (see {@link PumpEntry}).
 */
public interface ReconcileMixin extends DiscoveryMixin
{
    /** Fresh (System, Datastream/ControlStream) wrappers seen this cycle, plus the reachable node set. */
    class CurrentStreams
    {
        final Map<String, Map.Entry<System, Datastream>> ds;
        final Map<String, Map.Entry<System, ControlStream>> cs;
        final Set<String> reachable;

        CurrentStreams(Map<String, Map.Entry<System, Datastream>> ds,
                       Map<String, Map.Entry<System, ControlStream>> cs, Set<String> reachable)
        {
            this.ds = ds;
            this.cs = cs;
            this.reachable = reachable;
        }
    }

    /**
     * Run one convergence cycle; returns the (System, Datastream) pairs currently
     * visible on reachable remote nodes.
     */
    default List<Map.Entry<System, Datastream>> reconcileOnce()
    {
        if (getCommandNodes().isEmpty())
        {
            log.warn("[RECONCILE] no commander nodes configured; skipping");
            return new ArrayList<>();
        }
        Node commanderNode = getCommandNodes().get(0).getNode();

        int cycle = getStreamRegistry().nextCycle();

        CurrentStreams cur = discoverCurrentStreams();
        mirrorNewSystems(commanderNode, cur);

        Map<String, PumpEntry> entries = new HashMap<>();
        for (PumpEntry e : getStreamRegistry().snapshot())
            entries.put(e.key, e);

        // Datastreams: activate the new, mark the still-present, re-mirror on drift.
        for (Map.Entry<String, Map.Entry<System, Datastream>> ce : cur.ds.entrySet())
        {
            String key = ce.getKey();
            System sysObj = ce.getValue().getKey();
            Datastream dsObj = ce.getValue().getValue();

            PumpEntry entry = entries.get(key);
            if (entry == null)
            {
                activateStream(commanderNode, sysObj, dsObj, cycle);
                continue;
            }
            getStreamRegistry().markSeen(key, cycle);

            // Opaque binary passthrough (e.g. video): frames are relayed verbatim
            // and the record schema is never interpreted; the remote's swe+binary
            // schema is also not guaranteed byte-stable across fetches. Re-mirroring
            // such a stream on a schema-string diff would tear down and re-create
            // the commander video mirror, orphaning any viewer watching the old one.
            // So skip drift for binary — it is still activated when new and retired
            // when it vanishes; only the needless churn of a live stream is avoided.
            if (dsObj.getEncodingMode() == EncodingMode.BINARY)
                continue;

            String newSig = datastreamSchemaSig(dsObj);
            if (entry.schemaSig != null && newSig != null && !newSig.equals(entry.schemaSig))
            {
                log.warn("[RECONCILE] schema drift on {}; re-mirroring", key);
                retireStream(entry, "schema drift");
                activateStream(commanderNode, sysObj, dsObj, cycle);
            }
        }

        // Control streams: same lifecycle against the command schema.
        for (Map.Entry<String, Map.Entry<System, ControlStream>> ce : cur.cs.entrySet())
        {
            String key = ce.getKey();
            System sysObj = ce.getValue().getKey();
            ControlStream csObj = ce.getValue().getValue();

            PumpEntry entry = entries.get(key);
            if (entry == null)
            {
                activateControlstream(commanderNode, sysObj, csObj, cycle);
                continue;
            }
            getStreamRegistry().markSeen(key, cycle);
            String newSig = controlstreamSchemaSig(csObj);
            if (entry.schemaSig != null && newSig != null && !newSig.equals(entry.schemaSig))
            {
                log.warn("[RECONCILE] command schema drift on {}; re-mirroring", key);
                retireStream(entry, "schema drift");
                activateControlstream(commanderNode, sysObj, csObj, cycle);
            }
        }

        // Absence-based retire: only for streams missing on a REACHABLE node (a
        // node that is simply down is not a topology change).
        int removalLimit = getRemovedAfterCycles();
        Set<String> curDsKeys = cur.ds.keySet();
        Set<String> curCsKeys = cur.cs.keySet();
        for (PumpEntry entry : entries.values())
        {
            Set<String> curKeys = entry.kind.equals("cmd") ? curCsKeys : curDsKeys;
            if (curKeys.contains(entry.key))
                continue;
            String nodeAddr = entry.key.substring(0, entry.key.lastIndexOf('/'));
            if (!cur.reachable.contains(nodeAddr))
                continue; // node down, not a topology change
            int absences = getStreamRegistry().markAbsent(entry.key);
            if (absences >= removalLimit)
            {
                PumpEntry live = getStreamRegistry().get(entry.key);
                if (live != null)
                    retireStream(live, "absent " + absences + " cycle(s)");
            }
        }

        return new ArrayList<>(cur.ds.values());
    }

    /**
     * Discover datastreams and control streams on every remote node, returning
     * FRESH wrappers (their resources carry this cycle's schema, used for drift
     * detection) plus the set of reachable {@code addr:port} nodes.
     */
    default CurrentStreams discoverCurrentStreams()
    {
        Map<String, Map.Entry<System, Datastream>> currentDs = new LinkedHashMap<>();
        Map<String, Map.Entry<System, ControlStream>> currentCs = new LinkedHashMap<>();
        Set<String> reachable = new HashSet<>();

        for (RemoteNode rmtNode : getRemoteNodes())
        {
            String nodeAddr = rmtNode.getNode().getAddress() + ":" + rmtNode.getNode().getPort();
            List<System> systems;
            try
            {
                systems = rmtNode.getNode().discoverSystems();
            }
            catch (Exception e)
            {
                log.warn("[RECONCILE] node {} unreachable: {}", nodeAddr, e.toString());
                continue;
            }
            reachable.add(nodeAddr);

            for (System sysObj : systems)
            {
                try
                {
                    for (Datastream dsObj : sysObj.discoverDatastreams())
                    {
                        // Binary (e.g. video) datastreams are only federated when
                        // opted in; otherwise skip so JSON federation is unaffected.
                        // (Java-specific gate; the Python source has no such opt-in.)
                        // Logged (not silent) so a video that vanishes because the
                        // opt-in is off is diagnosable; only fires when a binary
                        // stream actually exists AND the flag is off, so it does not
                        // spam once the flag is enabled (the branch is then skipped).
                        if (dsObj.getEncodingMode() == EncodingMode.BINARY && !isBinaryDatastreamsEnabled())
                        {
                            log.info("[RECONCILE] skipping binary datastream {} (enableBinaryDatastreams is off)",
                                    dsObj.getRemoteKey());
                            continue;
                        }
                        dsObj.initialize();
                        currentDs.put(dsObj.getRemoteKey(), Map.entry(sysObj, dsObj));
                    }
                }
                catch (Exception e)
                {
                    log.warn("[RECONCILE] error discovering datastreams for {}: {}", sysObj.getUrn(), e.toString());
                }
                try
                {
                    for (ControlStream csObj : sysObj.discoverControlstreams())
                        currentCs.put(csObj.getRemoteKey(), Map.entry(sysObj, csObj));
                }
                catch (Exception e)
                {
                    log.warn("[RECONCILE] error discovering control streams for {}: {}", sysObj.getUrn(), e.toString());
                }
            }
        }

        return new CurrentStreams(currentDs, currentCs, reachable);
    }

    /**
     * Mirror systems seen on remotes but absent (by urn) on the commander.
     * Throws if the commander itself is unreachable — the whole cycle is retried
     * next tick.
     */
    default void mirrorNewSystems(Node commanderNode, CurrentStreams cur)
    {
        Map<String, System> urnToCmd = buildCommanderSystemIndex(commanderNode);
        List<System> newSystems = new ArrayList<>();
        Set<String> seenUrns = new HashSet<>();

        List<System> sources = new ArrayList<>();
        for (Map.Entry<System, Datastream> v : cur.ds.values())
            sources.add(v.getKey());
        for (Map.Entry<System, ControlStream> v : cur.cs.values())
            sources.add(v.getKey());

        for (System sysObj : sources)
        {
            String urn = sysObj.getUrn();
            if (!urnToCmd.containsKey(urn) && !seenUrns.contains(urn))
            {
                seenUrns.add(urn);
                newSystems.add(sysObj);
            }
        }
        if (!newSystems.isEmpty())
        {
            log.info("[RECONCILE] mirroring {} new system(s)", newSystems.size());
            mirrorSystemsToCommander(commanderNode, newSystems);
        }
    }

    /**
     * Subscribe → mirror → pump for one datastream, then register it. Any failure
     * leaves no pump behind; the stream is retried next cycle.
     */
    default void activateStream(Node commanderNode, System sysObj, Datastream dsObj, int cycle)
    {
        String key = dsObj.getRemoteKey();
        String topic;
        try
        {
            topic = subscribeToRemoteObservationsOne(dsObj);
        }
        catch (Exception e)
        {
            log.error("[RECONCILE] subscribe failed for {}: {}", key, e.toString());
            return;
        }
        try
        {
            mirrorDatastreamsToCommander(commanderNode, List.of(Map.entry(sysObj, dsObj)));
        }
        catch (Exception e)
        {
            log.error("[RECONCILE] mirror failed for {}: {}", key, e.toString());
        }
        Datastream mirror = getDsMap().get(key);
        if (mirror == null)
        {
            unsubscribeStream(dsObj.getMqttClient(), topic);
            return;
        }

        Thread thread = startObservationPump(dsObj);
        getStreamRegistry().add(new PumpEntry(key, "obs", thread, dsObj.getMqttClient(), topic,
                mirror.getId(), sysObj.getUrn(), datastreamSchemaSig(dsObj), cycle));
        log.info("[RECONCILE] stream online: {} -> commander {}", key, mirror.getId());
    }

    /**
     * Mirror one control stream on the commander and start the command forwarder,
     * then register the pump. If the forwarder can't be started the fresh mapping
     * is rolled back (cs_map entry dropped); the leftover commander mirror is
     * adopted by the dedup path next cycle rather than being duplicated, so no
     * explicit delete is needed here.
     */
    default void activateControlstream(Node commanderNode, System sysObj, ControlStream csObj, int cycle)
    {
        String key = csObj.getRemoteKey();
        Map<String, System> urnToCmdSys = buildCommanderSystemIndex(commanderNode);
        System cmdSys = urnToCmdSys.get(sysObj.getUrn());
        if (cmdSys == null)
        {
            log.debug("[RECONCILE] no commander system yet for {}; control stream {} deferred to next cycle",
                    sysObj.getUrn(), key);
            return;
        }

        ControlStream commanderCs = mirrorOneControlstream(cmdSys, sysObj, csObj);
        if (commanderCs == null)
            return;

        Thread thread = subscribeToCommanderControlstream(commanderCs, sysObj, csObj);
        if (thread == null)
        {
            getCsMap().remove(commanderCs.getId());
            return;
        }

        getStreamRegistry().add(new PumpEntry(key, "cmd", thread, commanderCs.getMqttClient(),
                commanderCs.getTopic(), commanderCs.getId(), sysObj.getUrn(),
                controlstreamSchemaSig(csObj), cycle));
        log.info("[RECONCILE] control stream online: {} -> commander {}", key, commanderCs.getId());
    }

    /** Stop one pump/forwarder, unsubscribe it, drop its routing entry, and keep or delete the commander mirror. */
    default void retireStream(PumpEntry entry, String reason)
    {
        log.info("[RECONCILE] retiring {} {} ({})", entry.kind, entry.key, reason);
        if (entry.thread != null)
        {
            entry.thread.interrupt();
            try
            {
                entry.thread.join(5000);
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
            }
        }
        unsubscribeStream(entry.mqttClient, entry.subscribeTopic);

        APIResourceTypes resType;
        if (entry.kind.equals("cmd"))
        {
            getCsMap().remove(entry.commanderId);
            resType = APIResourceTypes.CONTROL_CHANNEL;
        }
        else
        {
            getDsMap().remove(entry.key);
            resType = APIResourceTypes.DATASTREAM;
        }
        getStreamRegistry().remove(entry.key);

        if (isDeleteOnRemoved())
            deleteCommanderMirror(resType, entry.commanderId);
        else if (entry.kind.equals("cmd"))
            log.warn("[RECONCILE] commander control stream {} kept (on_removed=keep) but its forwarder is "
                    + "stopped — commands sent to it will NOT reach any remote", entry.commanderId);
        else
            log.info("[RECONCILE] commander mirror {} kept (on_removed=keep)", entry.commanderId);
    }

    /** DELETE a commander-side mirror (datastream or control stream) by id. */
    default void deleteCommanderMirror(APIResourceTypes resType, String resId)
    {
        try
        {
            APIHelper api = getCommandNodes().get(0).getNode().getApiHelper();
            ApiResponse res = api.deleteResource(resType, resId);
            if (res.ok())
                log.info("[RECONCILE] deleted commander mirror {}", resId);
            else
                log.error("[RECONCILE] delete of commander mirror {} rejected: HTTP {} — {}",
                        resId, res.statusCode(), res.text());
        }
        catch (Exception e)
        {
            log.error("[RECONCILE] failed to delete commander mirror {}: {}", resId, e.toString());
        }
    }

    /**
     * Unsubscribe a retired stream's topic. The comm client is shared per node —
     * unsubscribe the topic only, never stop the client (other streams on the
     * node still use it).
     */
    default void unsubscribeStream(MqttCommClient client, String topic)
    {
        if (client == null || topic == null)
            return;
        try
        {
            client.unsubscribe(topic);
        }
        catch (Exception e)
        {
            log.warn("[RECONCILE] unsubscribe failed for {}: {}", topic, e.toString());
        }
    }
}
