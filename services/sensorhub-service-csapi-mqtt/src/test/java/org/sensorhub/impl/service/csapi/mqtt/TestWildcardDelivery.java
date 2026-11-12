/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.csapi.mqtt;

import static org.junit.Assert.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.Test;
import com.google.common.base.Ticker;


/**
 * Checks of messages sent through wildcard subscriptions. The permission
 * check is replaced by a table of resources and users.
 */
public class TestWildcardDelivery
{
    long now = 1_000_000; // ms
    final Map<String, Boolean> access = new HashMap<>(); // "user path" -> can read
    final List<String> checks = new ArrayList<>();

    final Ticker ticker = new Ticker() {
        @Override
        public long read()
        {
            return TimeUnit.MILLISECONDS.toNanos(now);
        }
    };

    final CsApiMqttConnector connector = new CsApiMqttConnector(null, TestTopicMapper.mapper(), ticker) {
        @Override
        boolean doCheckRead(String userID, URI uri)
        {
            var key = userID + " " + uri;
            checks.add(key);
            return access.getOrDefault(key, false);
        }
    };


    @Test
    public void testEventOfReadableResource()
    {
        access.put("alice /systems/134/datastreams/12", true);
        assertTrue(connector.canDeliver("alice", "node1/systems/134/datastreams/12"));
        assertFalse(connector.canDeliver("bob", "node1/systems/134/datastreams/12"));
        assertFalse(connector.canDeliver("alice", "node1/systems/134/datastreams/13"));
    }


    @Test
    public void testBatchEventChecksDatastream()
    {
        access.put("alice /systems/134/datastreams/12", true);
        assertTrue(connector.canDeliver("alice", "node1/systems/134/datastreams/12/observations"));
        assertEquals(List.of("alice /systems/134/datastreams/12"), checks);
    }


    @Test
    public void testResourceDataIsNotSent()
    {
        access.put("alice /systems/134/datastreams/12/observations", true);
        assertFalse(connector.canDeliver("alice", "node1/systems/134/datastreams/12/observations:data"));
        assertFalse(connector.canDeliver("alice", "node1/systems/134/datastreams/12/observations:data/swe-csv"));
        assertFalse(connector.canDeliver("alice", "node1/systems:data"));
        assertTrue(checks.isEmpty());
    }


    @Test
    public void testOtherTopicsAreNotSent()
    {
        assertFalse(connector.canDeliver("alice", "node2/systems/134"));
        assertFalse(connector.canDeliver("alice", "node1/systems/134/events/5"));
        assertTrue(checks.isEmpty());
    }


    @Test
    public void testPermissionOnResourceOrSystemAbove()
    {
        var res = CsApiMqttConnector.ResourcePath.of(URI.create("/systems/134/subsystems/7/datastreams/12"));

        // on the resource itself: the systems above it are not checked
        assertTrue(CsApiMqttConnector.canRead(res, "12"::equals, id -> { throw new AssertionError(id); }));

        // on a system above it
        assertTrue(CsApiMqttConnector.canRead(res, id -> false, "134"::equals));
        assertFalse(CsApiMqttConnector.canRead(res, id -> false, id -> false));
    }


    @Test
    public void testDeletedResourceUsesSystemsOfTopic()
    {
        // osh-core cannot find a deleted resource in the database
        Predicate<String> notInDatabase = id -> { throw new IllegalArgumentException(); };
        var res = CsApiMqttConnector.ResourcePath.of(URI.create("/systems/134/datastreams/12"));

        assertTrue(CsApiMqttConnector.canRead(res, notInDatabase, "134"::equals));
        assertFalse(CsApiMqttConnector.canRead(res, notInDatabase, id -> false));

        // the system was deleted too
        assertFalse(CsApiMqttConnector.canRead(res, notInDatabase, notInDatabase));
    }


    @Test
    public void testDecisionsAreCached()
    {
        access.put("alice /systems/134", true);
        assertTrue(connector.canDeliver("alice", "node1/systems/134"));
        assertTrue(connector.canDeliver("alice", "node1/systems/134"));
        assertEquals(1, checks.size());

        // access removed: still sent until the cached decision expires
        access.put("alice /systems/134", false);
        now += CsApiMqttConnector.ACCESS_CACHE_MS - 1;
        assertTrue(connector.canDeliver("alice", "node1/systems/134"));
        now += 1;
        assertFalse(connector.canDeliver("alice", "node1/systems/134"));
        assertEquals(2, checks.size());
    }


    @Test
    public void testCacheSizeIsLimited()
    {
        for (int i = 0; i < CsApiMqttConnector.ACCESS_CACHE_MAX + 10; i++)
            connector.canDeliver("alice", "node1/systems/" + i);
        assertTrue(connector.accessCache.size() <= CsApiMqttConnector.ACCESS_CACHE_MAX);
    }


    @Test
    public void testResourcePath()
    {
        var res = CsApiMqttConnector.ResourcePath.of(URI.create("/systems/134/subsystems/7/datastreams/12"));
        assertEquals("datastreams", res.type());
        assertEquals("12", res.id());
        assertEquals(List.of("7", "134"), res.ancestors());

        res = CsApiMqttConnector.ResourcePath.of(URI.create("/systems/134"));
        assertEquals("systems", res.type());
        assertEquals("134", res.id());
        assertTrue(res.ancestors().isEmpty());

        assertEquals(List.of("134"), CsApiMqttConnector.ResourcePath.of(URI.create("/systems/134/controlstreams/3")).ancestors());
        assertNull(CsApiMqttConnector.ResourcePath.of(URI.create("/systems")));
    }
}
