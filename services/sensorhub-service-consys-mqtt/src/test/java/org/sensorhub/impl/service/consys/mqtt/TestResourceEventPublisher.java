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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Subscription;
import org.junit.Test;
import org.sensorhub.api.command.CommandStreamRemovedEvent;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamAddedEvent;
import org.sensorhub.api.data.DataStreamEnabledEvent;
import org.sensorhub.api.system.SystemAddedEvent;
import org.sensorhub.api.system.SystemChangedEvent;
import org.sensorhub.api.system.SystemEvent;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.sensorhub.impl.common.IdEncodersBase32;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class TestResourceEventPublisher
{
    static final String API = "http://host.example:8181/sensorhub/api";
    static final String TOP = "urn:test:top", SUB = "urn:test:sub", SUBSUB = "urn:test:subsub";
    static final BigId TOP_ID = BigId.fromLong(1, 10), SUB_ID = BigId.fromLong(1, 20), SUBSUB_ID = BigId.fromLong(1, 30);
    static final BigId DS_ID = BigId.fromLong(1, 40), CS_ID = BigId.fromLong(1, 50);

    final IdEncodersBase32 ids = new IdEncodersBase32();
    final Map<BigId, BigId> parents = new HashMap<>(Map.of(SUB_ID, TOP_ID, SUBSUB_ID, SUB_ID));
    final Map<String, BigId> uids = new HashMap<>(Map.of(TOP, TOP_ID, SUB, SUB_ID, SUBSUB, SUBSUB_ID));
    final List<String[]> published = new ArrayList<>();
    final ResourceEventPublisher publisher = new ResourceEventPublisher(new TestServer(), "node1/", API + "/",
        new ResourceEventPublisher.SystemLookup() {
            @Override public BigId parentOf(BigId sysId) { return parents.get(sysId); }
            @Override public BigId idOf(String sysUid) { return uids.get(sysUid); }
        }, ids, LoggerFactory.getLogger(TestResourceEventPublisher.class));

    final String top = ids.getSystemIdEncoder().encodeID(TOP_ID);
    final String sub = ids.getSystemIdEncoder().encodeID(SUB_ID);
    final String subsub = ids.getSystemIdEncoder().encodeID(SUBSUB_ID);
    final String ds = ids.getDataStreamIdEncoder().encodeID(DS_ID);
    final String cs = ids.getCommandStreamIdEncoder().encodeID(CS_ID);


    @Test
    public void testTopLevelSystem()
    {
        var e = publisher.toResourceEvent(withSystem(new SystemChangedEvent(1000, TOP), TOP_ID));
        assertEquals(new ResourceEventPublisher.ResourceEvent("node1/systems/" + top, "system.update",
            API + "/systems/" + top, null, 1000), e);
    }


    @Test
    public void testSubsystemCreate()
    {
        // osh-core publishes a new subsystem with the ID of its parent
        var e = publisher.toResourceEvent(withSystem(new SystemAddedEvent(1000, SUBSUB, SUB), SUB_ID));
        assertEquals("node1/systems/" + top + "/subsystems/" + sub + "/subsystems/" + subsub, e.topic());
        assertEquals("system.create", e.type());
        assertEquals(API + "/systems/" + subsub, e.subject());
        assertEquals(API + "/systems/" + sub, e.parent());
    }


    @Test
    public void testSubsystemDelete()
    {
        // a deleted system is no longer in the database
        parents.remove(SUBSUB_ID);
        uids.remove(SUBSUB);
        var e = publisher.toResourceEvent(withSystem(new SystemRemovedEvent(1000, SUBSUB, SUB), SUBSUB_ID));
        assertEquals("node1/systems/" + top + "/subsystems/" + sub + "/subsystems/" + subsub, e.topic());
        assertEquals("system.delete", e.type());
        assertEquals(API + "/systems/" + sub, e.parent());
    }


    @Test
    public void testDataStream()
    {
        var event = new DataStreamAddedEvent(1000, SUB, "out1");
        event.assignSystemID(SUB_ID);
        event.assignDataStreamID(DS_ID);
        var e = publisher.toResourceEvent(event);
        assertEquals("node1/systems/" + top + "/subsystems/" + sub + "/datastreams/" + ds, e.topic());
        assertEquals("datastream.create", e.type());
        assertEquals(API + "/datastreams/" + ds, e.subject());
        assertEquals(API + "/systems/" + sub, e.parent());
    }


    @Test
    public void testControlStream()
    {
        var event = new CommandStreamRemovedEvent(1000, TOP, "control1");
        event.assignSystemID(TOP_ID);
        event.assignCommandStreamID(CS_ID);
        var e = publisher.toResourceEvent(event);
        assertEquals("node1/systems/" + top + "/controlstreams/" + cs, e.topic());
        assertEquals("controlstream.delete", e.type());
        assertEquals(API + "/controlstreams/" + cs, e.subject());
        assertEquals(API + "/systems/" + top, e.parent());
    }


    @Test
    public void testNoResourceEvent()
    {
        var enabled = new DataStreamEnabledEvent(1000, TOP, "out1");
        enabled.assignSystemID(TOP_ID);
        enabled.assignDataStreamID(DS_ID);
        assertNull(publisher.toResourceEvent(enabled));
        assertNull(publisher.toResourceEvent(new SystemChangedEvent(1000, TOP))); // no system ID
    }


    @Test
    public void testCloudEvent()
    {
        var json = parse(publisher.toJson(publisher.toResourceEvent(withSystem(new SystemAddedEvent(1000, SUB, TOP), TOP_ID))));
        assertEquals("1.0", json.get("specversion").getAsString());
        assertEquals("org.ogc.api.csapi.system.create", json.get("type").getAsString());
        assertEquals(API, json.get("source").getAsString());
        assertEquals(API + "/systems/" + sub, json.get("subject").getAsString());
        assertEquals(API + "/systems/" + top, json.get("resourceparent").getAsString());
        assertEquals(Instant.ofEpochMilli(1000), Instant.parse(json.get("time").getAsString()));
        UUID.fromString(json.get("id").getAsString());
        assertEquals(7, json.size());

        // no resourceparent for a top-level system, and a new id for each event
        var event = publisher.toResourceEvent(withSystem(new SystemChangedEvent(1000, TOP), TOP_ID));
        var json1 = parse(publisher.toJson(event));
        var json2 = parse(publisher.toJson(event));
        assertFalse(json1.has("resourceparent"));
        assertNotEquals(json1.get("id"), json2.get("id"));
    }


    @Test
    public void testPublishAndStop()
    {
        var sub = new TestSubscription();
        publisher.onSubscribe(sub);
        assertEquals(Long.MAX_VALUE, sub.requested);

        publisher.onNext(withSystem(new SystemChangedEvent(1000, TOP), TOP_ID));
        publisher.onNext(withSystem(new SystemChangedEvent(1000, "urn:test:unknown"), BigId.fromLong(1, 99)));
        publisher.onNext(withSystem(new SystemAddedEvent(1000, "urn:test:orphan", "urn:test:unknown"), BigId.fromLong(1, 98)));
        assertEquals(2, published.size()); // an unknown parent is logged, not published
        assertEquals("node1/systems/" + top, published.get(0)[0]);
        assertEquals("org.ogc.api.csapi.system.update", parse(published.get(0)[1]).get("type").getAsString());

        publisher.stop();
        assertTrue(sub.cancelled);

        // a subscription that arrives after stop is cancelled
        var late = new TestSubscription();
        publisher.onSubscribe(late);
        assertTrue(late.cancelled);
        assertEquals(0, late.requested);
    }


    static SystemEvent withSystem(SystemEvent e, BigId sysId)
    {
        e.assignSystemID(sysId);
        return e;
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


    static class TestSubscription implements Subscription
    {
        long requested;
        boolean cancelled;

        @Override
        public void request(long n) { requested = n; }

        @Override
        public void cancel() { cancelled = true; }
    }
}
