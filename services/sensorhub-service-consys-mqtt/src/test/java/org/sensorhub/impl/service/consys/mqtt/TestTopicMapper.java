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
        // designated topics from the Part 3 Resource Data Topics table
        var topics = new TopicMapper("node1");
        var sys = "systems/134/subsystems/7";
        for (var path: new String[] {
            "systems",
            "systems/134/subsystems",
            sys + "/subsystems",
            sys + "/datastreams",
            sys + "/controlstreams",
            sys + "/events",
            "systems/134/datastreams/12/observations",
            sys + "/datastreams/12/observations",
            sys + "/controlstreams/3/commands",
            sys + "/controlstreams/3/commands/99/status",
            sys + "/controlstreams/3/commands/99/result"
        })
        {
            assertEquals("/" + path, topics.toResourceUri("node1/" + path + ":data").toString());
        }
    }


    @Test
    public void testRejectedTopics()
    {
        var topics = new TopicMapper("node1");
        for (var topic: new String[] {
            null,
            "node1",                                        // no resource path
            "node1/",                                       // no resource path
            "node2/systems:data",                           // other node
            "/api/systems:data",                            // 1.x topic
            "node1x/systems:data",                          // prefix of a longer level
            "node1/systems/1/datastreams/2/observations:data?f=json", // query string
            "node1/systems/+/datastreams:data",             // wildcard, not supported yet
            "node1/systems/134/#",                          // wildcard, not supported yet
            "node1/systems/1/datastreams/2/observations",   // event topic, not supported yet
            "node1/systems/134",                            // event topic, not supported yet
            "node1/systems/1/datastreams/2/observations:data/swe-json", // format, not supported yet
            "node1/systems:data/subsystems",                // ':data' not on the last level
            "node1/systems/134:data",                       // ':data' on an ID
            "node1/systems/1:x/datastreams:data",           // ':' in an ID
            "node1/systems/1/datastreams/2:data",           // ':data' on an ID
            "node1/observations:data",                      // flat, not allowed
            "node1/commands:data",                          // flat, not allowed
            "node1/datastreams/2/observations:data",        // no system path
            "node1/systems/1/samplingFeatures:data",        // not an allowed type
            "node1/systems/1/datastreams/2/schema:data",    // not an allowed type
            "node1/systems/1/subsystems:data/x",            // trailing level
            "node1/systems/1//datastreams:data",            // empty ID
            "node1/:data"                                   // no collection
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
