package org.sensorhub.impl.service.federation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class ObsEchoTest
{
    ObsEcho echo;

    @Before
    public void setUp()
    {
        echo = new ObsEcho();
    }


    private static JsonObject obs(String json)
    {
        return JsonParser.parseString(json).getAsJsonObject();
    }


    @Test
    public void aForwardedObservationIsRecognizedOnItsWayBack()
    {
        String k = echo.key("urn:axis:cam:1", "video1", 1786563907716L);
        assertFalse(echo.seen(k));
        echo.markForwarded(k);
        assertTrue(echo.seen(k));
    }


    @Test
    public void theKeyIsNodeIndependent()
    {
        // datastream ids differ per node; the same logical observation must
        // fingerprint identically wherever it resurfaces
        assertEquals(echo.key("urn:sys:1", "out", 42L), echo.key("urn:sys:1", "out", 42L));
    }


    @Test
    public void differentOutputsOfOneSystemDoNotCollide()
    {
        String video = echo.key("urn:axis:cam:1", "video1", 42L);
        String ptz = echo.key("urn:axis:cam:1", "ptzOutput", 42L);
        echo.markForwarded(video);
        assertFalse("a video frame must not suppress a PTZ observation", echo.seen(ptz));
    }


    @Test
    public void failsOpenWhenTheObservationCannotBeIdentified()
    {
        assertNull(echo.key(null, "out", 1L));
        assertNull(echo.key("urn:sys:1", null, 1L));
        assertNull(echo.key("urn:sys:1", "out", null));
        // a null key is never a hit, so the caller forwards
        assertFalse(echo.seen(null));
    }


    @Test
    public void aFailedInsertIsNotSuppressedOnRetry()
    {
        String k = echo.key("urn:sys:1", "out", 7L);
        echo.markForwarded(k);
        echo.forget(k);
        assertFalse(echo.seen(k));
    }


    @Test
    public void theCacheIsBounded()
    {
        for (int i = 0; i < ObsEcho.RECENT_CACHE_SIZE + 500; i++)
            echo.markForwarded(echo.key("urn:sys:1", "out", (long) i));
        assertTrue("cache must not grow without bound", echo.size() <= ObsEcho.RECENT_CACHE_SIZE);
    }


    @Test
    public void readsPhenomenonTimeFromJson()
    {
        assertEquals(Long.valueOf(1786563907716L),
            echo.phenomenonTimeMs(obs("{\"phenomenonTime\":\"2026-08-12T19:45:07.716Z\"}")));
    }


    @Test
    public void unparseableJsonTimeFailsOpen()
    {
        assertNull(echo.phenomenonTimeMs(obs("{}")));
        assertNull(echo.phenomenonTimeMs(obs("{\"phenomenonTime\":null}")));
        assertNull(echo.phenomenonTimeMs(obs("{\"phenomenonTime\":\"not-a-time\"}")));
        assertNull(echo.phenomenonTimeMs(null));
    }


    @Test
    public void readsTheTimestampFromASweBinaryFrame()
    {
        // frame layout: 8-byte big-endian double (epoch seconds), 4-byte length, payload
        double epochSeconds = 1786563907.716d;
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putDouble(epochSeconds);
        buf.putInt(4);
        byte[] frame = buf.array();

        Long ms = echo.binaryFrameTimeMs(frame);
        assertEquals(1786563907716L, ms.longValue());
    }


    @Test
    public void shortOrNonsenseBinaryFramesFailOpen()
    {
        assertNull(echo.binaryFrameTimeMs(null));
        assertNull(echo.binaryFrameTimeMs(new byte[] {1, 2, 3}));
        // all-zero header is not a plausible sampling time
        assertNull(echo.binaryFrameTimeMs(new byte[16]));
    }
}
