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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;


/**
 * <p>
 * Maps MQTT topics to CS API resource paths, per the OGC CS API Part 3
 * topic grammar: every topic starts with {@code {nodeId}/}, followed by
 * the resource path. Topics do not carry query strings.
 * </p><p>
 * Only Resource Data topics are accepted, for example
 * {@code node1/systems/134/datastreams/12/observations:data}. The topic
 * shape is checked here; that each ID belongs to its parent is left to the
 * CS API. Resource Event topics, format subtopics and wildcard topic
 * filters are rejected until they are supported.
 * </p>
 */
final class TopicMapper
{
    static final String DATA_MARKER = ":data";

    // Resource Data topic templates (OGC CS API Part 3, Resource Data Topics
    // table), without '{nodeId}/' and ':data'. An ID is one level with no ':'.
    static final Pattern DATA_PATH = Pattern.compile(
        "systems(/[^/:]+(/subsystems/[^/:]+)*/("
        + "subsystems|datastreams|controlstreams|events"
        + "|datastreams/[^/:]+/observations"
        + "|controlstreams/[^/:]+/commands(/[^/:]+/(status|result))?))?");

    final String prefix;


    /**
     * @param nodeId the node identifier, see {@link #checkNodeId(String)}
     * @throws IllegalArgumentException if the node identifier is not valid
     */
    TopicMapper(String nodeId)
    {
        this.prefix = checkNodeId(nodeId) + "/";
    }


    /**
     * Check that {@code nodeId} is one MQTT topic level that can be used in
     * both topic names and topic filters.
     * @return the node identifier
     * @throws IllegalArgumentException if it is empty, contains {@code /},
     *         {@code +}, {@code #} or NUL, or starts with {@code $}
     */
    static String checkNodeId(String nodeId)
    {
        if (nodeId == null || nodeId.isEmpty())
            throw new IllegalArgumentException("nodeId must not be empty");
        if (nodeId.charAt(0) == '$')
            throw new IllegalArgumentException("nodeId must not start with '$'");
        for (var c: nodeId.toCharArray())
        {
            if (c == '/' || c == '+' || c == '#' || c == '\0')
                throw new IllegalArgumentException("nodeId must not contain '" + (c == '\0' ? "NUL" : c) + "'");
        }
        return nodeId;
    }


    /**
     * @return the topic prefix, {@code {nodeId}/}
     */
    String getPrefix()
    {
        return prefix;
    }


    /**
     * @param topic an MQTT topic name
     * @return the URI of the CS API resource that the topic identifies,
     *         without {@code :data}
     * @throws InvalidTopicException if the topic is not under this node,
     *         has a query string, is a wildcard filter, or is not a
     *         Resource Data topic
     */
    URI toResourceUri(String topic) throws InvalidTopicException
    {
        if (topic == null || !topic.startsWith(prefix))
            throw new InvalidTopicException("Topic must start with '" + prefix + "'");

        var path = topic.substring(prefix.length());
        if (path.isEmpty())
            throw new InvalidTopicException("Topic has no resource path");
        if (path.indexOf('?') >= 0)
            throw new InvalidTopicException("Topics must not have a query string");

        // Wildcard filters are not supported yet (see #249, wildcard subscriptions).
        // Reject them here: URI would read '#' as a fragment, and map
        // 'node1/systems/134/#' to the parent resource '/systems/134/'.
        if (path.indexOf('+') >= 0 || path.indexOf('#') >= 0)
            throw new InvalidTopicException("Wildcard topic filters are not supported");

        // Only Resource Data topics are supported. Resource Event topics
        // (no ':data') and format subtopics come later (see #249).
        if (path.contains(DATA_MARKER + "/"))
            throw new InvalidTopicException("Format subtopics are not supported");
        if (!path.endsWith(DATA_MARKER))
            throw new InvalidTopicException("Only Resource Data topics (ending with '" + DATA_MARKER + "') are supported");
        path = path.substring(0, path.length() - DATA_MARKER.length());
        if (!DATA_PATH.matcher(path).matches())
            throw new InvalidTopicException("Not a Resource Data topic: " + topic);

        try
        {
            return new URI("/" + path);
        }
        catch (URISyntaxException e)
        {
            throw new InvalidTopicException("Invalid resource path in topic: " + topic);
        }
    }
}
