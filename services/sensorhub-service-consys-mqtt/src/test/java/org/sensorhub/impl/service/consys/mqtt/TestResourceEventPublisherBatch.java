/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.impl.service.consys.mqtt.ResourceEventPublisher.ObsBatchState;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.opengis.swe.v20.DataBlock;


/**
 * Unit tests for {@link ResourceEventPublisher} batch observation behavior.
 *
 * <p>The publisher's hot path (obs arrival → counter increment) and slow path
 * (scheduler tick → flush + MQTT publish) are exercised independently of the
 * OSH event bus by driving {@link ObsBatchState#recordObs} and
 * {@link ResourceEventPublisher#flushAllBatches} directly. A capturing
 * {@link IMqttServer} stub records each publish call so the emitted CloudEvent
 * JSON can be asserted against the OGC CS API Part 3 §"Batch Resource Events"
 * spec.</p>
 */
public class TestResourceEventPublisherBatch
{
    static final String NODE_ID    = "test-node";
    static final String CSAPI_BASE = "https://example.org/api";
    static final String SYS_ID     = "sys42";
    static final String DS_ID      = "ds7";


    // -------------------------------------------------------------------------
    // ObsBatchState — accumulator semantics
    // -------------------------------------------------------------------------

    @Test
    public void recordObs_accumulatesCount()
    {
        var state = newState(List.of());
        var t0 = Instant.parse("2026-01-01T00:00:00Z");
        state.recordObs(3, t0);
        state.recordObs(2, t0.plusSeconds(1));
        state.recordObs(5, t0.plusSeconds(2));
        var snap = state.snapAndReset();
        assertNotNull("snapshot must be non-null when observations were recorded", snap);
        assertEquals(10L, snap.count());
    }


    @Test
    public void recordObs_tracksEarliestPhenomenonTime()
    {
        var state = newState(List.of());
        var t0 = Instant.parse("2026-01-01T00:00:00Z");
        // out-of-order arrivals — snapshot must keep the earliest as window start
        state.recordObs(1, t0.plusSeconds(10));
        state.recordObs(1, t0);
        state.recordObs(1, t0.plusSeconds(5));
        var snap = state.snapAndReset();
        assertEquals(t0, snap.windowStart());
    }


    @Test
    public void snapAndReset_emptyWindow_returnsNull()
    {
        var state = newState(List.of());
        assertNull("fresh state with no observations snapshots to null", state.snapAndReset());
    }


    @Test
    public void snapAndReset_clearsCountAndWindowStart()
    {
        var state = newState(List.of());
        state.recordObs(7, Instant.parse("2026-01-01T00:00:00Z"));
        var first = state.snapAndReset();
        assertEquals(7L, first.count());

        // Subsequent snapshot with no new obs must return null (state was cleared)
        assertNull("state must be cleared after snapshot", state.snapAndReset());
    }


    // -------------------------------------------------------------------------
    // flushAllBatches — end-to-end emission
    // -------------------------------------------------------------------------

    @Test
    public void flushAllBatches_emitsOneCloudEventPerDatastreamWithCorrectPayload()
    {
        var stub = new CapturingMqttServer();
        var publisher = newPublisher(stub);

        // Inject a single datastream with no ancestors so we get exactly one publish
        var dsBigId = BigId.fromLong(1, 100L);
        var state = new ObsBatchState(noopSubscription(),
            SYS_ID, DS_ID, CSAPI_BASE + "/systems/" + SYS_ID + "/datastreams/" + DS_ID,
            List.of());
        publisher.obsBatches.put(dsBigId, state);

        // Record a few observations
        var t0 = Instant.parse("2026-01-01T00:00:00Z");
        state.recordObs(40, t0);
        state.recordObs(60, t0.plusSeconds(15));

        publisher.flushAllBatches();

        assertEquals("expected exactly one MQTT publish for one datastream with no ancestors",
            1, stub.published.size());

        var pub = stub.published.get(0);
        assertEquals(NODE_ID + "/systems/" + SYS_ID + "/datastreams/" + DS_ID + "/observations",
            pub.topic);

        var ce = JsonParser.parseString(pub.payloadString()).getAsJsonObject();
        assertEquals("1.0", ce.get("specversion").getAsString());
        assertEquals(CloudEventsTypes.TYPE_OBS_CREATE, ce.get("type").getAsString());
        assertEquals(CSAPI_BASE, ce.get("source").getAsString());
        assertEquals(CSAPI_BASE + "/systems/" + SYS_ID + "/datastreams/" + DS_ID + "/observations",
            ce.get("subject").getAsString());
        assertTrue("CloudEvent should declare an id", ce.has("id"));
        assertEquals("application/json", ce.get("datacontenttype").getAsString());

        var data = ce.getAsJsonObject("data");
        assertEquals(100L, data.get("count").getAsLong());

        JsonArray timerange = data.getAsJsonArray("timerange");
        assertEquals("timerange must be a 2-element array", 2, timerange.size());
        assertEquals(t0.toString(), timerange.get(0).getAsString());
        // window end is wall-clock at flush time — must be at-or-after the last recorded obs
        var endStr = timerange.get(1).getAsString();
        assertFalse("window end must be at or after window start",
            Instant.parse(endStr).isBefore(t0));

        // After flush the state should be empty — a second flush must not publish
        publisher.flushAllBatches();
        assertEquals("empty window must not publish a zero-count CloudEvent",
            1, stub.published.size());
    }


    @Test
    public void flushAllBatches_publishesToEachAncestorSystemTopic()
    {
        var stub = new CapturingMqttServer();
        var publisher = newPublisher(stub);

        var ancestorA = "ancA";
        var ancestorB = "ancB";
        var dsBigId = BigId.fromLong(1, 200L);
        var state = new ObsBatchState(noopSubscription(),
            SYS_ID, DS_ID, CSAPI_BASE + "/systems/" + SYS_ID + "/datastreams/" + DS_ID,
            List.of(ancestorA, ancestorB));
        publisher.obsBatches.put(dsBigId, state);

        state.recordObs(5, Instant.parse("2026-01-01T00:00:00Z"));
        publisher.flushAllBatches();

        // 1 datastream topic + 2 ancestor system topics = 3 publishes
        assertEquals(3, stub.published.size());
        var topics = stub.published.stream().map(p -> p.topic).toList();
        assertTrue(topics.contains(NODE_ID + "/systems/" + SYS_ID + "/datastreams/" + DS_ID + "/observations"));
        assertTrue(topics.contains(NODE_ID + "/systems/" + ancestorA + "/datastreams/" + DS_ID + "/observations"));
        assertTrue(topics.contains(NODE_ID + "/systems/" + ancestorB + "/datastreams/" + DS_ID + "/observations"));

        // All three must report the same count and subject (collection URL of original datastream)
        for (var p : stub.published)
        {
            var ce = JsonParser.parseString(p.payloadString()).getAsJsonObject();
            assertEquals(5L, ce.getAsJsonObject("data").get("count").getAsLong());
            assertEquals(CSAPI_BASE + "/systems/" + SYS_ID + "/datastreams/" + DS_ID + "/observations",
                ce.get("subject").getAsString());
        }
    }


    // -------------------------------------------------------------------------
    // recordObsBatch — closes the lambda-coverage gap from the bus-driven path
    // -------------------------------------------------------------------------

    @Test
    public void recordObsBatch_emptyArray_isNoOp()
    {
        var publisher = newPublisher(new CapturingMqttServer());
        var dsBigId = BigId.fromLong(1, 400L);
        var state = newState(List.of());
        publisher.obsBatches.put(dsBigId, state);

        publisher.recordObsBatch(dsBigId, new IObsData[0]);
        assertNull("empty array must not start a window", state.snapAndReset());
    }


    @Test
    public void recordObsBatch_unknownDatastream_isNoOp()
    {
        var publisher = newPublisher(new CapturingMqttServer());
        // dsId is not present in obsBatches — record must silently drop
        publisher.recordObsBatch(BigId.fromLong(1, 999L),
            new IObsData[] { stubObs(Instant.parse("2026-01-01T00:00:00Z")) });
        // No assertions on internal state — just ensuring no NPE / crash
    }


    @Test
    public void recordObsBatch_picksEarliestPhenomenonTime()
    {
        var publisher = newPublisher(new CapturingMqttServer());
        var dsBigId = BigId.fromLong(1, 401L);
        var state = newState(List.of());
        publisher.obsBatches.put(dsBigId, state);

        var t0 = Instant.parse("2026-01-01T00:00:00Z");
        // unsorted: middle, earliest, latest, ...
        publisher.recordObsBatch(dsBigId, new IObsData[] {
            stubObs(t0.plusSeconds(3)),
            stubObs(t0),
            stubObs(t0.plusSeconds(7)),
            stubObs(t0.plusSeconds(1))
        });

        var snap = state.snapAndReset();
        assertEquals(4L, snap.count());
        assertEquals(t0, snap.windowStart());
    }


    @Test
    public void recordObsBatch_acrossMultipleEvents_keepsEarliestAndAccumulates()
    {
        var publisher = newPublisher(new CapturingMqttServer());
        var dsBigId = BigId.fromLong(1, 402L);
        var state = newState(List.of());
        publisher.obsBatches.put(dsBigId, state);

        var t0 = Instant.parse("2026-01-01T00:00:00Z");
        publisher.recordObsBatch(dsBigId, new IObsData[] { stubObs(t0.plusSeconds(10)) });
        publisher.recordObsBatch(dsBigId, new IObsData[] { stubObs(t0) });   // earlier — should win
        publisher.recordObsBatch(dsBigId, new IObsData[] { stubObs(t0.plusSeconds(5)) });

        var snap = state.snapAndReset();
        assertEquals(3L, snap.count());
        assertEquals(t0, snap.windowStart());
    }


    @Test
    public void flushAllBatches_idleDatastream_emitsNothing()
    {
        var stub = new CapturingMqttServer();
        var publisher = newPublisher(stub);

        var dsBigId = BigId.fromLong(1, 300L);
        publisher.obsBatches.put(dsBigId, new ObsBatchState(noopSubscription(),
            SYS_ID, DS_ID, CSAPI_BASE + "/systems/" + SYS_ID + "/datastreams/" + DS_ID,
            List.of()));

        publisher.flushAllBatches();
        assertTrue("an idle datastream must not produce any publish", stub.published.isEmpty());
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ObsBatchState newState(List<String> ancestors)
    {
        return new ObsBatchState(noopSubscription(),
            SYS_ID, DS_ID, CSAPI_BASE + "/systems/" + SYS_ID + "/datastreams/" + DS_ID,
            ancestors);
    }


    private static ResourceEventPublisher newPublisher(IMqttServer mqttServer)
    {
        return new ResourceEventPublisher(
            mqttServer, NODE_ID, CSAPI_BASE,
            null, null, null,
            LoggerFactory.getLogger(TestResourceEventPublisherBatch.class),
            30);
    }


    /**
     * Lightweight {@link IObsData} stub that only fills in {@code phenomenonTime}.
     * The other fields are not exercised by the batching code path under test.
     */
    private static IObsData stubObs(Instant phenomenonTime)
    {
        return new IObsData() {
            @Override public BigId getDataStreamID()         { return BigId.NONE; }
            @Override public BigId getFoiID()                { return BigId.NONE; }
            @Override public Instant getPhenomenonTime()     { return phenomenonTime; }
            @Override public Instant getResultTime()         { return phenomenonTime; }
            @Override public Map<String, Object> getParameters() { return Map.of(); }
            @Override public Geometry getPhenomenonLocation() { return null; }
            @Override public DataBlock getResult()           { return null; }
        };
    }


    /**
     * Subscription stub for state objects whose subscription is never exercised
     * by the test (we bypass {@code subscribeToObsEvents} entirely).
     */
    private static Flow.Subscription noopSubscription()
    {
        return new Flow.Subscription() {
            @Override public void request(long n) {}
            @Override public void cancel() {}
        };
    }


    /** {@link IMqttServer} stub that records every publish call for assertions. */
    static class CapturingMqttServer implements IMqttServer
    {
        final List<Captured> published = new ArrayList<>();

        @Override
        public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload)
        {
            // Copy the bytes — the publisher reuses ByteBuffer wrappers, and we
            // want a stable snapshot for later assertions.
            var copy = new byte[payload.remaining()];
            payload.duplicate().get(copy);
            published.add(new Captured(topic, copy));
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload, ByteBuffer correlData)
        {
            return publish(topic, payload);
        }

        @Override public void registerHandler(String topicPrefix, IMqttHandler handler) {}
        @Override public void unregisterHandler(String topicPrefix, IMqttHandler handler) {}
    }


    record Captured(String topic, byte[] payload)
    {
        String payloadString() { return new String(payload, StandardCharsets.UTF_8); }
    }
}