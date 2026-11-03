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
import org.junit.Test;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;


public class TestTopicMapper
{
    @Test
    public void testValidNodeIds()
    {
        for (var id: new String[] {"api", "node1", "my-node", "a.b_c", "x"})
            assertEquals(id, TopicMapper.checkNodeId(id));
    }


    @Test
    public void testInvalidNodeIds()
    {
        for (var id: new String[] {null, "", "a/b", "a+b", "a#b", "a\0b", "$sys", "/api"})
        {
            try
            {
                TopicMapper.checkNodeId(id);
                fail("Accepted invalid nodeId: " + id);
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }
        }
    }


    @Test
    public void testPrefix()
    {
        assertEquals("node1/", new TopicMapper("node1").getPrefix());
    }


    @Test
    public void testToResourceUri() throws InvalidTopicException
    {
        var topics = new TopicMapper("node1");
        assertEquals("/systems/134", topics.toResourceUri("node1/systems/134").toString());
        assertEquals("/datastreams/abc/observations", topics.toResourceUri("node1/datastreams/abc/observations").toString());
    }


    @Test
    public void testRejectedTopics()
    {
        var topics = new TopicMapper("node1");
        for (var topic: new String[] {
            null,
            "node1",                                    // no resource path
            "node1/",                                   // no resource path
            "node2/systems/134",                        // other node
            "/api/systems/134",                         // 1.x topic
            "node1x/systems/134",                       // prefix of a longer level
            "node1/datastreams/abc/observations?f=json", // query string
            "node1/systems/+",                          // wildcard, not supported yet
            "node1/systems/134/#",                      // wildcard, not supported yet
            "node1/systems/+/datastreams/+/observations"
        })
        {
            try
            {
                topics.toResourceUri(topic);
                fail("Accepted invalid topic: " + topic);
            }
            catch (InvalidTopicException e)
            {
                // expected
            }
        }
    }
}
