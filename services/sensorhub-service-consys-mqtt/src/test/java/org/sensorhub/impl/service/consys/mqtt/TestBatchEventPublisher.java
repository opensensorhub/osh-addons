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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamRemovedEvent;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.sensorhub.impl.common.IdEncodersBase32;
import org.sensorhub.impl.event.EventBus;
import org.sensorhub.impl.service.consys.mqtt.TestResourceEventPublisher.TestSubscription;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockDouble;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class TestBatchEventPublisher
{
    static final String API = "http://host.example:8181/sensorhub/api";
    static final String TOP = "urn:test:top", SUB = "urn:test:sub";
    static final BigId TOP_ID = BigId.fromLong(1, 10), SUB_ID = BigId.fromLong(1, 20);
    static final BigId DS1_ID = BigId.fromLong(1, 40), DS2_ID = BigId.fromLong(1, 41);

    final IdEncodersBase32 ids = new IdEncodersBase32();
    final Map<BigId, BigId> dsSystems = new HashMap<>(Map.of(DS1_ID, TOP_ID, DS2_ID, SUB_ID));
    final List<String[]> published = Collections.synchronizedList(new ArrayList<>());
    final String top = ids.getSystemIdEncoder().encodeID(TOP_ID);
    final String sub = ids.getSystemIdEncoder().encodeID(SUB_ID);
    final String ds1 = ids.getDataStreamIdEncoder().encodeID(DS1_ID);
    final String ds2 = ids.getDataStreamIdEncoder().encodeID(DS2_ID);


    BatchEventPublisher publisher(long windowMs)
    {
        return new BatchEventPublisher(new TestServer(), "node1/", API + "/",
            sysId -> sysId.equals(SUB_ID) ? "systems/" + top + "/subsystems/" + sub : "systems/" + top,
            dsSystems::get, ids, windowMs, LoggerFactory.getLogger(TestBatchEventPublisher.class));
    }


    static ObsEvent obsEvent(long time, String sysUid, BigId dsId, int n)
    {
        var obs = new ObsData[n];
        for (int i = 0; i < n; i++)
        {
            obs[i] = new ObsData.Builder()
                .withDataStream(dsId)
                .withPhenomenonTime(Instant.ofEpochMilli(time))
                .withResult(new DataBlockDouble(1))
                .build();
        }
        return new ObsEvent(time, sysUid, "out1", obs);
    }


    @Test
    public void testTopicAndEvent()
    {
        var publisher = publisher(0);
        publisher.onObs(obsEvent(1000, SUB, DS2_ID, 3));

        assertEquals(1, published.size());
        assertEquals("node1/systems/" + top + "/subsystems/" + sub + "/datastreams/" + ds2 + "/observations", published.get(0)[0]);

        var json = parse(published.get(0)[1]);
        assertEquals("1.0", json.get("specversion").getAsString());
        assertEquals("org.ogc.api.csapi.observation.create", json.get("type").getAsString());
        assertEquals(API, json.get("source").getAsString());
        assertEquals(API + "/datastreams/" + ds2 + "/observations", json.get("subject").getAsString());
        assertEquals(API + "/datastreams/" + ds2, json.get("resourceparent").getAsString());
        assertEquals("application/json", json.get("datacontenttype").getAsString());
        Instant.parse(json.get("time").getAsString());
        UUID.fromString(json.get("id").getAsString());
        assertEquals(9, json.size());

        var data = json.getAsJsonObject("data");
        var range = data.getAsJsonArray("timerange");
        assertEquals(2, range.size());
        assertEquals(Instant.ofEpochMilli(1000), Instant.parse(range.get(0).getAsString()));
        assertEquals(Instant.ofEpochMilli(1000), Instant.parse(range.get(1).getAsString()));
        assertEquals(3, data.get("count").getAsLong());
        assertEquals(2, data.size());
    }


    @Test
    public void testEventsFromEventBus() throws Exception
    {
        // the event bus delivers nothing to a subscription that requests no events
        var publisher = publisher(0);
        var bus = new EventBus();
        publisher.eventBus = bus;
        publisher.scheduler = Executors.newSingleThreadScheduledExecutor();
        var topicId = EventUtils.getDataStreamDataTopicID(TOP, "out1");
        publisher.watch(topicId);

        var channel = publisher.channels.get(topicId);
        for (int i = 0; i < 200 && channel.get() == null; i++)
            Thread.sleep(10);
        assertNotNull(channel.get());

        bus.getPublisher(topicId).publish(obsEvent(1000, TOP, DS1_ID, 2));
        bus.getPublisher(topicId).publish(obsEvent(1001, TOP, DS1_ID, 1));
        for (int i = 0; i < 200 && published.size() < 2; i++)
            Thread.sleep(10);
        assertEquals(2, published.size());
        publisher.stop();
    }


    @Test
    public void testNewIdForEachEvent()
    {
        var publisher = publisher(0);
        publisher.onObs(obsEvent(1000, TOP, DS1_ID, 1));
        publisher.onObs(obsEvent(1001, TOP, DS1_ID, 1));
        assertNotEquals(parse(published.get(0)[1]).get("id"), parse(published.get(1)[1]).get("id"));
        assertEquals("node1/systems/" + top + "/datastreams/" + ds1 + "/observations", published.get(0)[0]);
    }


    @Test
    public void testUnknownDataStream()
    {
        var publisher = publisher(0);
        publisher.onObs(obsEvent(1000, TOP, BigId.fromLong(1, 99), 1));
        assertTrue(published.isEmpty());
        assertTrue(publisher.streams.isEmpty());
    }


    @Test
    public void testDataStreamRemoved()
    {
        var publisher = publisher(1000);
        publisher.onObs(obsEvent(1000, TOP, DS1_ID, 1));
        var window = publisher.streams.get(DS1_ID).window();

        var removed = new DataStreamRemovedEvent(2000, TOP, "out1");
        removed.assignDataStreamID(DS1_ID);
        publisher.onRegistryEvent(removed);
        assertFalse(publisher.streams.containsKey(DS1_ID));

        // the closed window publishes nothing more
        window.record(1, 3000);
        window.flush();
        assertEquals(1, published.size());
    }


    @Test
    public void testSystemRemoved()
    {
        var publisher = publisher(0);
        var subChannel = new AtomicReference<Subscription>(new TestSubscription());
        var topChannel = new AtomicReference<Subscription>(new TestSubscription());
        publisher.channels.put(EventUtils.getDataStreamDataTopicID(SUB, "out1"), subChannel);
        publisher.channels.put(EventUtils.getDataStreamDataTopicID(TOP, "out1"), topChannel);
        publisher.onObs(obsEvent(1000, TOP, DS1_ID, 1));
        publisher.onObs(obsEvent(1000, SUB, DS2_ID, 1));

        // a system UID that starts with another one is not removed
        var removed = new SystemRemovedEvent(2000, SUB, TOP);
        removed.assignSystemID(SUB_ID);
        publisher.onRegistryEvent(removed);
        publisher.onRegistryEvent(new SystemRemovedEvent(2000, "urn:test:to", null));

        assertEquals(List.of(DS1_ID), List.copyOf(publisher.streams.keySet()));
        assertEquals(List.of(EventUtils.getDataStreamDataTopicID(TOP, "out1")), List.copyOf(publisher.channels.keySet()));
        assertNull(subChannel.get());
        assertNotNull(topChannel.get());
    }


    @Test
    public void testStop()
    {
        var publisher = publisher(1000);
        publisher.scheduler = Executors.newSingleThreadScheduledExecutor();
        var registry = new TestSubscription();
        var channel = new TestSubscription();
        publisher.keep(publisher.registry, registry);
        publisher.channels.put("topic1", new AtomicReference<>(channel));
        publisher.onObs(obsEvent(1000, TOP, DS1_ID, 1));
        publisher.onObs(obsEvent(1001, TOP, DS1_ID, 1)); // pending in the window
        var window = publisher.streams.get(DS1_ID).window();

        publisher.stop();
        assertTrue(registry.cancelled);
        assertTrue(channel.cancelled);
        assertTrue(publisher.channels.isEmpty());
        assertTrue(publisher.streams.isEmpty());
        assertTrue(publisher.scheduler.isShutdown());

        // pending observations are dropped, and later ones are not counted
        window.flush();
        publisher.onObs(obsEvent(5000, TOP, DS1_ID, 1));
        assertEquals(1, published.size());
        assertTrue(publisher.streams.isEmpty());

        // a subscription that arrives after stop is cancelled
        var late = new TestSubscription();
        publisher.keep(new AtomicReference<>(), late);
        assertTrue(late.cancelled);
    }


    static JsonObject parse(String json)
    {
        return JsonParser.parseString(json).getAsJsonObject();
    }


    class TestServer implements IMqttServer
    {
        @Override
        public void registerHandler(String topicPrefix, IMqttHandler handler) {}

        @Override
        public void unregisterHandler(String topicPrefix, IMqttHandler handler) {}

        @Override
        public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload)
        {
            published.add(new String[] {topic, StandardCharsets.UTF_8.decode(payload).toString()});
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload, ByteBuffer correlData)
        {
            return publish(topic, payload);
        }
    }
}
