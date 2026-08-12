package org.sensorhub.impl.service.federation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.ControlStreamResource;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.DatastreamResource;
import org.sensorhub.impl.service.federation.oshconnect.Node;
import org.sensorhub.impl.service.federation.oshconnect.System;
import org.sensorhub.impl.service.federation.oshconnect.SystemResource;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of broker.mirroring.MirroringMixin.
 */
public interface MirroringMixin extends CommandRoutingMixin
{
    default void mirrorControlstreamsToCommander(Node commander, List<Map.Entry<System, ControlStream>> remoteCsInfo)
    {
        Map<String, System> urnToCmdSys = buildCommanderSystemIndex(commander);
        Map<System, Map<String, ControlStream>> cmdCsIndexes = new HashMap<>();
        int mirroredCount = 0;

        for (Map.Entry<System, ControlStream> entry : remoteCsInfo)
        {
            System remoteSys = entry.getKey();
            ControlStream remoteCs = entry.getValue();

            if (remoteSys.getUrn() == null)
            {
                log.warn("Remote system has no UID; skipping its control streams");
                continue;
            }

            System cmdSys = urnToCmdSys.get(remoteSys.getUrn());
            if (cmdSys == null)
            {
                log.debug("No commander system for urn={}, skipping", remoteSys.getUrn());
                continue;
            }

            ControlStreamResource csRes = remoteCs.getUnderlyingResource();
            String csName = csRes.getName() != null ? csRes.getName() : "unknown";

            if (csRes.getCommandSchema() == null)
            {
                log.warn("Skipping control stream {}: source has no command_schema "
                        + "(oshconnect discovery did not populate it)", csName);
                continue;
            }

            // Restart dedup: adopt an existing commander control stream only when
            // its inputName AND schema both match. Matching the display name alone
            // would merge two distinct streams onto one mirror.
            Map<String, ControlStream> cmdCsIndex =
                    cmdCsIndexes.computeIfAbsent(cmdSys, this::indexCommanderControlstreams);
            ControlStream existing = findExistingMirrorControlstream(cmdCsIndex, csRes.getInputName(),
                    controlstreamSchemaSig(remoteCs));
            if (existing != null)
            {
                if (getCsMap().put(existing.getId(), Map.entry(remoteSys, remoteCs)) != null)
                    log.error("Commander control stream {} is already the mirror for another remote "
                            + "stream; adopting it for {} merges two distinct inputs", existing.getId(), csName);
                mirroredCount++;
                log.info("Adopted existing commander control stream for {} -> {}", csName, existing.getId());
                subscribeToCommanderControlstream(existing, remoteSys, remoteCs);
                continue;
            }

            ControlStreamResource csResource = csRes.modelCopy(remoteResourceStripKeys());

            try
            {
                ControlStream newCs = cmdSys.addInsertControlstream(csResource);
                String commanderCsId = newCs.getId() != null ? newCs.getId() : "unknown";
                if (getCsMap().put(commanderCsId, Map.entry(remoteSys, remoteCs)) != null)
                    log.error("Commander returned control stream {} for {}, but another mirror already "
                            + "claimed it — two distinct inputs were merged into one control stream",
                            commanderCsId, csName);
                mirroredCount++;
                log.info("Mirrored control stream: {} -> {}", csName, commanderCsId);
                subscribeToCommanderControlstream(newCs, remoteSys, remoteCs);
            }
            catch (Exception e)
            {
                log.error("Failed to mirror control stream {}: {}", csName, e.toString());
            }
        }

        log.info("Mirrored {} control stream(s)", mirroredCount);
    }

    default void mirrorSystemsToCommander(Node commander, List<System> systems)
    {
        log.info("Mirroring {} system(s) to commander", systems.size());
        int mirroredCount = 0;

        for (System sys : systems)
        {
            SystemResource srcResource = sys.getSystemResource();
            if (srcResource == null)
            {
                log.warn("Skipping system {}: source has no underlying resource", sys.getUrn());
                continue;
            }

            Map<String, JsonElement> update = new HashMap<>();
            update.put("id", JsonNull.INSTANCE);
            update.put("links", JsonNull.INSTANCE);
            SystemResource copyResource = srcResource.modelCopy(update);

            System sysCopy = System.fromResource(copyResource, commander);
            try
            {
                commander.addSystem(sysCopy, true);
                mirroredCount++;
                log.info("  Mirrored system: {}", sys.getUrn());
            }
            catch (Exception e)
            {
                log.error("  Failed to mirror system {}: {}", sys.getUrn(), e.toString());
            }
        }

        log.info("Mirrored {} system(s)", mirroredCount);
    }

    default void mirrorAllToCommander()
    {
        // Faithful intent of the (unused) Python helper, which chained
        // discover_all() into discover_and_mirror_datastreams().
        ((DiscoveryMixin) this).discoverAndMirrorDatastreams();
    }

    /**
     * Build a mapping from system URN -> commander System object. Assumes
     * commander.discover_systems() has been called (it is here).
     */
    default Map<String, System> buildCommanderSystemIndex(Node commander)
    {
        Map<String, System> urnToSys = new HashMap<>();
        commander.discoverSystems(); // ensure its systems list is up-to-date
        for (System sys : commander.systems())
        {
            // A null UID must never become a map key: HashMap accepts it, so every
            // system that fails to expose one would collapse onto a single entry
            // and their streams would all be mirrored into one commander system.
            if (sys.getUrn() == null)
            {
                log.warn("Commander system has no UID; excluding it from the mirror index");
                continue;
            }
            urnToSys.put(sys.getUrn(), sys);
        }
        return urnToSys;
    }

    default void mirrorDatastreamsToCommander(Node commander, List<Map.Entry<System, Datastream>> remoteDsInfo)
    {
        Map<String, System> urnToCmdSys = buildCommanderSystemIndex(commander);
        Map<System, Map<String, Datastream>> cmdDsIndexes = new HashMap<>();
        Set<String> claimedCmdDsIds = new HashSet<>();
        int mirroredCount = 0;

        for (Map.Entry<System, Datastream> entry : remoteDsInfo)
        {
            System remoteSys = entry.getKey();
            Datastream remoteDs = entry.getValue();

            if (remoteSys.getUrn() == null)
            {
                log.warn("Remote system has no UID; skipping its datastreams");
                continue;
            }

            System cmdSys = urnToCmdSys.get(remoteSys.getUrn());
            if (cmdSys == null)
            {
                log.debug("No commander system for urn={}", remoteSys.getUrn());
                continue;
            }

            DatastreamResource dsRes;
            String dsName;
            try
            {
                dsRes = remoteDs.getResource();
                dsName = dsRes.getName() != null ? dsRes.getName() : "unknown";
            }
            catch (Exception e)
            {
                log.debug("Failed to get DatastreamResource: {}", e.toString());
                continue;
            }

            if (dsRes.getRecordSchema() == null)
            {
                log.warn("Skipping datastream {}: source has no record_schema "
                        + "(oshconnect discovery did not populate it)", dsName);
                continue;
            }

            // Restart dedup: adopt an existing commander datastream only when its
            // outputName AND schema both match. Matching the display name alone
            // would merge two distinct streams onto one mirror — which is exactly
            // how binary video frames end up in another output's datastream.
            Map<String, Datastream> cmdDsIndex =
                    cmdDsIndexes.computeIfAbsent(cmdSys, this::indexCommanderDatastreams);
            Datastream existing = findExistingMirrorDatastream(cmdDsIndex, dsRes.getOutputName(),
                    datastreamSchemaSig(remoteDs));
            if (existing != null)
            {
                recordDatastreamMirror(remoteDs, existing, dsName, claimedCmdDsIds);
                mirroredCount++;
                log.info("Adopted existing commander datastream for {} -> {}", dsName, existing.getId());
                continue;
            }

            DatastreamResource dsResource = dsRes.modelCopy(remoteResourceStripKeys());

            try
            {
                Datastream newDs = cmdSys.addInsertDatastream(dsResource);
                recordDatastreamMirror(remoteDs, newDs, dsName, claimedCmdDsIds);
                mirroredCount++;
                log.debug("Mirrored datastream: {}", dsName);
            }
            catch (Exception e)
            {
                log.error("Failed to create datastream {} on commander: {}", dsName, e.toString());
            }
        }

        log.info("Mirrored {} datastream(s)", mirroredCount);
    }

    /**
     * Record a remote datastream's commander mirror in ds_map, reporting the two
     * ways distinct streams can silently end up sharing one mirror:
     *
     * <ul>
     * <li>the routing key was already mapped — two remote datastreams resolved to
     *     the same node-qualified key, so one pump's observations would be lost;</li>
     * <li>the commander returned a datastream id another mirror already claimed
     *     this run — the commander merged two outputs, which it does whenever two
     *     streams share a (system UID, outputName) pair.</li>
     * </ul>
     *
     * Both are reported rather than rejected: the mirror is still recorded so the
     * federation keeps running, but the condition is no longer invisible.
     */
    default void recordDatastreamMirror(Datastream remoteDs, Datastream cmdDs, String dsName,
                                        Set<String> claimedCmdDsIds)
    {
        String cmdDsId = cmdDs.getId();
        if (cmdDsId != null && !claimedCmdDsIds.add(cmdDsId))
        {
            log.error("Commander datastream {} is already the mirror for another remote stream; "
                    + "mirroring {} onto it merges two distinct outputs into one datastream "
                    + "(they likely share an outputName under the same system)", cmdDsId, dsName);
        }

        Datastream prev = getDsMap().put(remoteDs.getRemoteKey(), cmdDs);
        if (prev != null)
        {
            log.error("Routing key {} was already mapped to commander datastream {}; "
                    + "overwriting with {} — observations from one remote stream will be lost",
                    remoteDs.getRemoteKey(), prev.getId(), cmdDsId);
        }
    }

    /**
     * Fields to clear from a resource copied off a remote node before POSTing it
     * to the commander: the remote's own id, its parent-system references, and
     * its links to remote procedure/deployment/feature resources. The commander
     * assigns its own id (read back from the Location header) and its own parent
     * system (taken from the POST URL).
     *
     * These names must match the CS API wire properties exactly — the earlier
     * "procedureLink@link"/"deploymentLink@link" spellings matched nothing, so
     * the real procedure@link/deployment@link values were carried over intact.
     */
    default Map<String, JsonElement> remoteResourceStripKeys()
    {
        Map<String, JsonElement> update = new HashMap<>();
        update.put("id", JsonNull.INSTANCE);
        update.put("system@id", JsonNull.INSTANCE);
        update.put("system@link", JsonNull.INSTANCE);
        update.put("procedure@link", JsonNull.INSTANCE);
        update.put("deployment@link", JsonNull.INSTANCE);
        update.put("featureOfInterest@link", JsonNull.INSTANCE);
        update.put("samplingFeature@link", JsonNull.INSTANCE);
        update.put("links", JsonNull.INSTANCE);
        return update;
    }

    // ---- restart/collision dedup helpers ------------------------------------

    /** Serialized record-schema fingerprint for a datastream (null if none). */
    default String datastreamSchemaSig(Datastream ds)
    {
        JsonElement s = ds.getResource().getRecordSchema();
        return s != null ? s.toString() : null;
    }

    /** Serialized command-schema fingerprint for a control stream (null if none). */
    default String controlstreamSchemaSig(ControlStream cs)
    {
        JsonElement s = cs.getUnderlyingResource().getCommandSchema();
        return s != null ? s.toString() : null;
    }

    /**
     * Reuse an existing commander datastream that has the same {@code outputName}
     * and a positively matching schema signature, instead of inserting a duplicate
     * mirror.
     *
     * Adoption requires BOTH signatures to be present and equal. An absent
     * signature on either side is not evidence of a match, and adopting on the
     * name alone would merge two distinct streams (e.g. a binary video stream onto
     * a text mirror) into one datastream. Failing to adopt is always safe — it
     * just creates a fresh mirror.
     */
    default Datastream findExistingMirrorDatastream(Map<String, Datastream> cmdIndex, String outputName, String remoteSig)
    {
        if (outputName == null || remoteSig == null)
            return null;

        Datastream candidate = cmdIndex.get(outputName);
        if (candidate == null)
            return null;

        String candSig;
        try
        {
            candSig = datastreamSchemaSig(candidate);
        }
        catch (Exception e)
        {
            log.debug("Skipping commander datastream candidate for {}: {}", outputName, e.toString());
            return null;
        }

        if (!remoteSig.equals(candSig))
        {
            log.warn("Commander datastream {} exists but its schema does not match; "
                    + "creating a new mirror instead of adopting it", outputName);
            return null;
        }
        return candidate;
    }

    /**
     * List a commander system's datastreams once and index them by
     * {@code outputName}. Built per commander system rather than per remote
     * datastream: the listing costs one request plus one schema fetch per
     * datastream, so doing it inside the mirror loop is quadratic.
     */
    default Map<String, Datastream> indexCommanderDatastreams(System cmdSys)
    {
        Map<String, Datastream> byOutputName = new HashMap<>();
        try
        {
            for (Datastream ds : cmdSys.discoverDatastreams())
            {
                String outputName = ds.getResource().getOutputName();
                if (outputName != null)
                    byOutputName.putIfAbsent(outputName, ds);
            }
        }
        catch (Exception e)
        {
            log.debug("Could not list commander datastreams for dedup: {}", e.toString());
        }
        return byOutputName;
    }

    /** Control-stream twin of {@link #findExistingMirrorDatastream}, keyed on {@code inputName}. */
    default ControlStream findExistingMirrorControlstream(Map<String, ControlStream> cmdIndex, String inputName, String remoteSig)
    {
        if (inputName == null || remoteSig == null)
            return null;

        ControlStream candidate = cmdIndex.get(inputName);
        if (candidate == null)
            return null;

        String candSig;
        try
        {
            candSig = controlstreamSchemaSig(candidate);
        }
        catch (Exception e)
        {
            log.debug("Skipping commander control stream candidate for {}: {}", inputName, e.toString());
            return null;
        }

        if (!remoteSig.equals(candSig))
        {
            log.warn("Commander control stream {} exists but its schema does not match; "
                    + "creating a new mirror instead of adopting it", inputName);
            return null;
        }
        return candidate;
    }

    /** Control-stream twin of {@link #indexCommanderDatastreams}, keyed on {@code inputName}. */
    default Map<String, ControlStream> indexCommanderControlstreams(System cmdSys)
    {
        Map<String, ControlStream> byInputName = new HashMap<>();
        try
        {
            for (ControlStream cs : cmdSys.discoverControlstreams())
            {
                String inputName = cs.getUnderlyingResource().getInputName();
                if (inputName != null)
                    byInputName.putIfAbsent(inputName, cs);
            }
        }
        catch (Exception e)
        {
            log.debug("Could not list commander control streams for dedup: {}", e.toString());
        }
        return byInputName;
    }
}
