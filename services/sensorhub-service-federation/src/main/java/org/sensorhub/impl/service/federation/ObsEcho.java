package org.sensorhub.impl.service.federation;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


/**
 * <p>
 * Suppression of observations this broker has already forwarded, so a
 * re-delivery is relayed at most once.
 * </p>
 *
 * <p>
 * An observation is identified fleet-wide by
 * {@code (systemUid, outputName, phenomenonTime @ epoch-ms)}. Datastream ids are
 * assigned per node, so an echo arriving back under a different node's id would
 * defeat an id-based key; the system UID and output name survive mirroring
 * unchanged, which is what makes the same logical observation recognizable
 * wherever it resurfaces. That covers re-mirroring after a restart, an MQTT
 * reconnect replaying an overlap, and a cycle of brokers relaying to each other.
 * </p>
 *
 * <p>
 * Every failure mode <b>fails open</b>: an unparseable or absent time yields a
 * null key and the caller forwards. Dedupe must never lose data — a duplicate
 * observation is a nuisance, a dropped one is a defect.
 * </p>
 *
 * <p>
 * This does <i>not</i> make a self-federating node inert on its own. The cache
 * only knows what this broker already sent, so the first copy of each
 * observation would still be written before its echo could be recognized — a
 * permanent doubling. A node that mirrors onto itself must skip forwarding
 * outright; see {@code DiscoveryMixin}.
 * </p>
 *
 * @author CR31
 */
public class ObsEcho
{
    /** Bound of the recently-forwarded cache. */
    static final int RECENT_CACHE_SIZE = 10_000;

    /**
     * Recently-forwarded fingerprints, access-ordered so the bound evicts the
     * least recently seen rather than the oldest inserted.
     */
    private final Set<String> recentlyForwarded = Collections.newSetFromMap(Collections.synchronizedMap(
        new LinkedHashMap<String, Boolean>(64, 0.75f, true)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest)
            {
                return size() > RECENT_CACHE_SIZE;
            }
        }));


    /**
     * Fingerprint of an observation, or null when it cannot be identified and
     * the caller should forward unconditionally.
     */
    public String key(String systemUid, String outputName, Long phenTimeMs)
    {
        if (systemUid == null || outputName == null || phenTimeMs == null)
            return null;
        return systemUid + '|' + outputName + '|' + phenTimeMs;
    }


    /** True iff this fingerprint was forwarded recently. A null key is never a hit. */
    public boolean seen(String key)
    {
        return key != null && recentlyForwarded.contains(key);
    }


    /**
     * Record a fingerprint as forwarded.
     *
     * Call this BEFORE the insert, not after: the destination publishes the
     * observation to its subscribers as part of storing it, so the echo can
     * arrive while the insert call is still in flight. A mark placed afterwards
     * loses that race. Roll back with {@link #forget} if the insert fails.
     */
    public void markForwarded(String key)
    {
        if (key != null)
            recentlyForwarded.add(key);
    }


    /** Undo a {@link #markForwarded} after a failed insert, so a retry is not suppressed. */
    public void forget(String key)
    {
        if (key != null)
            recentlyForwarded.remove(key);
    }


    /** Test/diagnostic hook. */
    int size()
    {
        return recentlyForwarded.size();
    }


    /**
     * Epoch-ms of an observation's {@code phenomenonTime} from a CS API JSON
     * observation, or null when absent or unparseable.
     */
    public Long phenomenonTimeMs(JsonObject observation)
    {
        if (observation == null)
            return null;

        JsonElement t = observation.get("phenomenonTime");
        if (t == null || t.isJsonNull() || !t.isJsonPrimitive())
            return null;

        try
        {
            return Instant.parse(t.getAsString()).toEpochMilli();
        }
        catch (DateTimeParseException | UnsupportedOperationException | IllegalStateException e)
        {
            return null;
        }
    }


    /**
     * Epoch-ms of a raw SWE binary observation frame, or null when the frame is
     * too short or carries no sane timestamp.
     *
     * A SWE binary observation frame opens with the sampling time as a
     * big-endian IEEE-754 double holding seconds since the Unix epoch, followed
     * by a 4-byte payload length. Reading those 8 bytes is what lets the binary
     * relay path be deduped at all — it never parses the payload itself.
     */
    public Long binaryFrameTimeMs(byte[] frame)
    {
        if (frame == null || frame.length < 8)
            return null;

        double epochSeconds = ByteBuffer.wrap(frame, 0, 8).getDouble();
        if (!Double.isFinite(epochSeconds) || epochSeconds <= 0)
            return null;

        return (long) (epochSeconds * 1000.0);
    }
}
