package org.sensorhub.impl.service.federation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sensorhub.impl.service.federation.oshconnect.ControlStream;
import org.sensorhub.impl.service.federation.oshconnect.ControlStreamResource;
import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.DatastreamResource;
import org.sensorhub.impl.service.federation.oshconnect.Node;
import org.sensorhub.impl.service.federation.oshconnect.System;
import org.sensorhub.impl.service.federation.oshconnect.SystemResource;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

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
                getCsMap().put(existing.getId(), Map.entry(remoteSys, remoteCs));
                mirroredCount++;
                log.info("Adopted existing commander control stream for {} -> {}", csName, existing.getId());
                subscribeToCommanderControlstream(existing, remoteSys, remoteCs);
                continue;
            }

            // Clear remote-specific identifiers/links: cs_id, procedure_link,
            // deployment_link, feature_of_interest_link, sampling_feature_link, links.
            Map<String, JsonElement> update = new HashMap<>();
            update.put("id", JsonNull.INSTANCE);
            update.put("procedureLink@link", JsonNull.INSTANCE);
            update.put("deploymentLink@link", JsonNull.INSTANCE);
            update.put("featureOfInterest@link", JsonNull.INSTANCE);
            update.put("samplingFeature@link", JsonNull.INSTANCE);
            update.put("links", JsonNull.INSTANCE);
            ControlStreamResource csResource = csRes.modelCopy(update);

            try
            {
                ControlStream newCs = cmdSys.addInsertControlstream(csResource);
                String commanderCsId = newCs.getId() != null ? newCs.getId() : "unknown";
                getCsMap().put(commanderCsId, Map.entry(remoteSys, remoteCs));
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
                getDsMap().put(remoteDs.getRemoteKey(), existing);
                mirroredCount++;
                log.info("Adopted existing commander datastream for {} -> {}", dsName, existing.getId());
                continue;
            }

            // Deep-copy and clear remote-specific links; ds_id is set to "default"
            // (the commander assigns its own from the Location header).
            Map<String, JsonElement> update = new HashMap<>();
            update.put("id", new JsonPrimitive("default"));
            update.put("system@id", JsonNull.INSTANCE); // clear remote system id; commander assigns its own parent
            update.put("procedureLink@link", JsonNull.INSTANCE);
            update.put("deploymentLink@link", JsonNull.INSTANCE);
            update.put("featureOfInterest@link", JsonNull.INSTANCE);
            update.put("samplingFeature@link", JsonNull.INSTANCE);
            update.put("links", JsonNull.INSTANCE);
            DatastreamResource dsResource = dsRes.modelCopy(update);

            try
            {
                Datastream newDs = cmdSys.addInsertDatastream(dsResource);
                getDsMap().put(remoteDs.getRemoteKey(), newDs); // node-qualified key (bare ids collide across nodes)
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
