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
    default void routeObservationToCommanderDatastream(String remoteDsId, JsonObject observation,
                                                       Datastream sourceDs)
    {
        Datastream commanderDs = getDsMap().get(remoteDsId);
        if (commanderDs == null)
        {
            if (DEBUG_VERBOSE)
                log.debug("No mirrored datastream for {}", remoteDsId);
            return;
        }

        ObsEcho echo = getObsEcho();
        String echoKey = echoKey(sourceDs, echo.phenomenonTimeMs(observation));
        if (echo.seen(echoKey))
        {
            log.debug("Skipping observation already forwarded for {}", remoteDsId);
            return;
        }

        // Marked before the insert, not after: the destination publishes the
        // observation as part of storing it, so the echo can arrive while this
        // call is still in flight.
        echo.markForwarded(echoKey);
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
            echo.forget(echoKey); // a dropped insert must stay retryable
            log.error("Failed to insert observation for {}: {}", remoteDsId, e.toString());
        }
    }

    /** Fleet-wide fingerprint of an observation on this source datastream, or null if unidentifiable. */
    default String echoKey(Datastream sourceDs, Long phenTimeMs)
    {
        if (sourceDs == null)
            return null;
        return getObsEcho().key(sourceDs.getSystemUid(), sourceDs.getResource().getOutputName(), phenTimeMs);
    }

    /**
     * Relay a raw swe+binary observation frame to the mirrored commander
     * Datastream verbatim (opaque passthrough — no parse, no re-encode).
     */
    default void routeBinaryObservationToCommanderDatastream(String remoteDsId, byte[] observation,
                                                             Datastream sourceDs)
    {
        Datastream commanderDs = getDsMap().get(remoteDsId);
        if (commanderDs == null)
        {
            if (DEBUG_VERBOSE)
                log.debug("No mirrored datastream for {}", remoteDsId);
            return;
        }

        // The relay never parses the payload, but a SWE binary frame opens with
        // its sampling time, which is enough to fingerprint a video frame.
        ObsEcho echo = getObsEcho();
        String echoKey = echoKey(sourceDs, echo.binaryFrameTimeMs(observation));
        if (echo.seen(echoKey))
        {
            log.debug("Skipping binary observation already forwarded for {}", remoteDsId);
            return;
        }

        echo.markForwarded(echoKey);
        try
        {
            commanderDs.insertObservationBytes(observation);
            log.debug("Inserted binary observation for {}", remoteDsId);
        }
        catch (Exception e)
        {
            echo.forget(echoKey);
            log.error("Failed to insert binary observation for {}: {}", remoteDsId, e.toString());
        }
    }
}
