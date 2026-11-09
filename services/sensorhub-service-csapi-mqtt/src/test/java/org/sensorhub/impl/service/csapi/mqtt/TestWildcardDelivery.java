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
import org.junit.Test;
import org.sensorhub.impl.service.csapi.mqtt.CsApiMqttConnector.Access;


/**
 * Checks of messages sent through wildcard subscriptions. The CS API read
 * check is replaced by a table of resources and users.
 */
public class TestWildcardDelivery
{
    long now = 1_000_000;
    final Map<String, Access> access = new HashMap<>(); // "user path" -> access
    final List<String> checks = new ArrayList<>();

    final CsApiMqttConnector connector = new CsApiMqttConnector(null, TestTopicMapper.mapper()) {
        @Override
        Access doCheckRead(String userID, URI uri)
        {
            var key = userID + " " + uri;
            checks.add(key);
            return access.getOrDefault(key, Access.DENIED);
        }

        @Override
        Access doCheckDeleted(String userID, URI uri)
        {
            var key = userID + " deleted " + uri;
            checks.add(key);
            return access.getOrDefault(key, Access.DENIED);
        }
    };


    public TestWildcardDelivery()
    {
        connector.clockMs = () -> now;
    }


    @Test
    public void testEventOfReadableResource()
    {
        access.put("alice /systems/134/datastreams/12", Access.ALLOWED);
        assertTrue(connector.canDeliver("alice", "node1/systems/134/datastreams/12"));
        assertFalse(connector.canDeliver("bob", "node1/systems/134/datastreams/12"));
        assertFalse(connector.canDeliver("alice", "node1/systems/134/datastreams/13"));
    }


    @Test
    public void testBatchEventChecksDatastream()
    {
        access.put("alice /systems/134/datastreams/12", Access.ALLOWED);
        assertTrue(connector.canDeliver("alice", "node1/systems/134/datastreams/12/observations"));
        assertEquals(List.of("alice /systems/134/datastreams/12"), checks);
    }


    @Test
    public void testResourceDataIsNotSent()
    {
        access.put("alice /systems/134/datastreams/12/observations", Access.ALLOWED);
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
    public void testDeletedResourceChecksPermission()
    {
        // a deleted resource: the permission is checked without the database
        access.put("alice /systems/134/subsystems/7", Access.NOT_FOUND);
        access.put("alice deleted /systems/134/subsystems/7", Access.ALLOWED);
        assertTrue(connector.canDeliver("alice", "node1/systems/134/subsystems/7"));

        // no permission: the delete event is not sent
        access.put("bob /systems/134/subsystems/7", Access.NOT_FOUND);
        assertFalse(connector.canDeliver("bob", "node1/systems/134/subsystems/7"));
        assertEquals(List.of(
            "alice /systems/134/subsystems/7", "alice deleted /systems/134/subsystems/7",
            "bob /systems/134/subsystems/7", "bob deleted /systems/134/subsystems/7"), checks);
    }


    @Test
    public void testDecisionsAreCached()
    {
        access.put("alice /systems/134", Access.ALLOWED);
        assertTrue(connector.canDeliver("alice", "node1/systems/134"));
        assertTrue(connector.canDeliver("alice", "node1/systems/134"));
        assertEquals(1, checks.size());

        // access removed: still sent until the cached decision expires
        access.put("alice /systems/134", Access.DENIED);
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
