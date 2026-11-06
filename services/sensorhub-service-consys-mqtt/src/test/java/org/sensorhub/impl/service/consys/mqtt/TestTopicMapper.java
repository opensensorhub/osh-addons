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
import java.util.List;
import org.junit.Test;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;


public class TestTopicMapper
{
    static TopicMapper mapper()
    {
        return new TopicMapper("node1", TopicMapper.STANDARD_FORMATS);
    }


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
        assertEquals("node1/", mapper().getPrefix());
    }


    @Test
    public void testToResourceUri() throws InvalidTopicException
    {
        // designated topics from the Part 3 Resource Data Topics table
        var topics = mapper();
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
            var res = topics.parse("node1/" + path + ":data");
            assertEquals("/" + path, res.uri().toString());
            assertNull(res.format());
            assertFalse(res.event());
        }
    }


    @Test
    public void testEventTopics() throws InvalidTopicException
    {
        // designated topics from the Part 3 Resource Event Topics table
        var topics = mapper();
        var sys = "systems/134/subsystems/7";
        for (var path: new String[] {
            "systems/134",
            "systems/134/subsystems/7",
            sys + "/subsystems/8",
            "systems/134/datastreams/12",
            sys + "/datastreams/12",
            "systems/134/controlstreams/3",
            sys + "/controlstreams/3"
        })
        {
            var res = topics.parse("node1/" + path);
            assertEquals("/" + path, res.uri().toString());
            assertNull(res.format());
            assertTrue(res.event());
        }
    }


    @Test
    public void testBatchEventTopics() throws InvalidTopicException
    {
        // the subscriber must be able to read the datastream
        var topics = mapper();
        for (var path: new String[] {
            "systems/134/datastreams/12",
            "systems/134/subsystems/7/subsystems/8/datastreams/12"
        })
        {
            var res = topics.parse("node1/" + path + "/observations");
            assertEquals("/" + path, res.uri().toString());
            assertNull(res.format());
            assertTrue(res.event());
        }
    }


    @Test
    public void testFormatTokens()
    {
        assertEquals("json", TopicMapper.formatToken("application/json"));
        assertEquals("om-json", TopicMapper.formatToken("application/om+json"));
        assertEquals("swe-binary", TopicMapper.formatToken("application/swe+binary"));
        assertEquals("swe-csv", TopicMapper.formatToken("application/swe+csv"));
        assertEquals("geo-json", TopicMapper.formatToken("application/geo+json"));
        assertEquals("sml-xml", TopicMapper.formatToken("Application/SML+XML; version=2.0"));
        assertEquals("text/csv", TopicMapper.formatToken("text/csv"));
    }


    @Test
    public void testFormatSubtopics() throws InvalidTopicException
    {
        var topics = mapper();
        var obs = "node1/systems/1/datastreams/2/observations:data/";
        for (var type: TopicMapper.STANDARD_FORMATS)
        {
            var res = topics.parse(obs + TopicMapper.formatToken(type));
            assertEquals(type, res.format());
            assertEquals("/systems/1/datastreams/2/observations", res.uri().getPath());
            assertEquals("f=" + type, res.uri().getQuery());
        }
    }


    @Test
    public void testFormatsWithoutSubtopic()
    {
        // tokens with '/' and tokens shared by two types get no subtopic
        var topics = new TopicMapper("node1", List.of(
            "application/json", "video/x-motion-jpeg", "text/csv", "application/a+b", "application/a-b"));
        assertEquals(List.of("json"), List.copyOf(topics.formats.keySet()));
    }


    @Test
    public void testRejectedTopics()
    {
        var topics = mapper();
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
            "node1/systems",                                // event topic of a collection
            "node1/systems/1/datastreams",                  // event topic of a collection
            "node1/systems/1/datastreams/2/observations/9", // no observation events yet
            "node1/systems/1/controlstreams/3/commands",    // no command batch events yet
            "node1/systems/1/controlstreams/3/commands/9",  // no command events yet
            "node1/systems/1/events/5",                     // no system events yet
            "node1/systems/1/samplingFeatures/5",           // no sampling feature events yet
            "node1/procedures/5",                           // no procedure events yet
            "node1/datastreams/2",                          // no system path
            "node1/systems/1/",                             // empty level
            "node1/systems/1:x",                            // ':' in an ID
            "node1/systems/1/datastreams/2/observations:data/swe-proto", // unknown format
            "node1/systems/1/datastreams/2/observations:data/",  // empty format
            "node1/systems/1/datastreams/2/observations:data/SWE-JSON", // tokens are lowercase
            "node1/systems/1/datastreams/2/observations:data/swe-json/x", // format is one level
            "node1/systems/1/datastreams/2/observations:datax",  // not ':data'
            "node1/systems:data/subsystems",                // ':data' not on the last level
            "node1/systems/134:data",                       // ':data' on an ID
            "node1/systems/1:x/datastreams:data",           // ':' in an ID
            "node1/systems/1/datastreams/2:data",           // ':data' on an ID
            "node1/observations:data",                      // flat, not allowed
            "node1/commands:data",                          // flat, not allowed
            "node1/datastreams/2/observations:data",        // no system path
            "node1/systems/1/samplingFeatures:data",        // not an allowed type
            "node1/systems/1/datastreams/2/schema:data",    // not an allowed type
            "node1/systems/1/subsystems:data/x",            // unknown format
            "node1/systems/1//datastreams:data",            // empty ID
            "node1/:data"                                   // no collection
        })
        {
            try
            {
                topics.parse(topic);
                fail("Accepted invalid topic: " + topic);
            }
            catch (InvalidTopicException e)
            {
                // expected
            }
        }
    }
}
