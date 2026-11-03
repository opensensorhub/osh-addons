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
import org.sensorhub.api.comm.mqtt.InvalidTopicException;


/**
 * <p>
 * Maps MQTT topics to CS API resource paths, per the OGC CS API Part 3
 * topic grammar: every topic starts with {@code {nodeId}/}, followed by
 * the resource path. Topics do not carry query strings. Wildcard topic
 * filters are rejected until wildcard subscriptions are supported.
 * </p>
 */
final class TopicMapper
{
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
     * @return the URI of the CS API resource that the topic identifies
     * @throws InvalidTopicException if the topic is not under this node,
     *         has no resource path, has a query string, or is a wildcard filter
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
