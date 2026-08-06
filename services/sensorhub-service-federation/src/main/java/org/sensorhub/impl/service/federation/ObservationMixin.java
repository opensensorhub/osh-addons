package org.sensorhub.impl.service.federation;

import org.sensorhub.impl.service.federation.oshconnect.Datastream;

import com.google.gson.JsonObject;

import static org.sensorhub.impl.service.federation.BrokerLogging.DEBUG_VERBOSE;
import static org.sensorhub.impl.service.federation.BrokerLogging.log;

/**
 * Port of broker.observation_routing.ObservationMixin.
 */
public interface ObservationMixin extends SchemaBuildersMixin, BrokerContext
{
    /**
     * Given a remote datastream ID and an OSH-style observation item, forward it
     * to the mirrored commander Datastream via insert_observation_dict().
     */
    default void routeObservationToCommanderDatastream(String remoteDsId, JsonObject observation)
    {
        Datastream commanderDs = getDsMap().get(remoteDsId);
        if (commanderDs == null)
        {
            if (DEBUG_VERBOSE)
                log.debug("No mirrored datastream for {}", remoteDsId);
            return;
        }

        try
        {
            JsonObject obsDict = buildInsertObservationDict(commanderDs, observation);
            // insert_observation_dict internally does an HTTP POST and either
            // returns the new id or raises on a non-OK response.
            commanderDs.insertObservationDict(obsDict);
            log.debug("Inserted observation for {}", remoteDsId);
        }
        catch (Exception e)
        {
            log.error("Failed to insert observation for {}: {}", remoteDsId, e.toString());
        }
    }

    /**
     * Relay a raw swe+binary observation frame to the mirrored commander
     * Datastream verbatim (opaque passthrough — no parse, no re-encode).
     */
    default void routeBinaryObservationToCommanderDatastream(String remoteDsId, byte[] observation)
    {
        Datastream commanderDs = getDsMap().get(remoteDsId);
        if (commanderDs == null)
        {
            if (DEBUG_VERBOSE)
                log.debug("No mirrored datastream for {}", remoteDsId);
            return;
        }

        try
        {
            commanderDs.insertObservationBytes(observation);
            log.debug("Inserted binary observation for {}", remoteDsId);
        }
        catch (Exception e)
        {
            log.error("Failed to insert binary observation for {}: {}", remoteDsId, e.toString());
        }
    }
}
