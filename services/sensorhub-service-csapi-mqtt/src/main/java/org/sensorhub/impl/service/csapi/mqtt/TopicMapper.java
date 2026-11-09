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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;


/**
 * <p>
 * Maps MQTT topics to CS API resource paths, per the OGC CS API Part 3
 * topic grammar: every topic starts with {@code {nodeId}/}, followed by
 * the resource path. Topics do not carry query strings.
 * </p><p>
 * Only Resource Data topics are accepted, for example
 * {@code node1/systems/134/datastreams/12/observations:data}. The plain
 * {@code :data} topic selects the default representation, and
 * {@code :data/{format}} selects another one by its format token. The
 * topic shape is checked here; that each ID belongs to its parent, and
 * that the resource offers the format, is left to the CS API. Resource
 * Event topics and wildcard topic filters are rejected until they are
 * supported.
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

    // formats that the CS API offers without custom formats
    static final List<String> STANDARD_FORMATS = List.of(
        ResourceFormat.JSON.getMimeType(),
        ResourceFormat.GEOJSON.getMimeType(),
        ResourceFormat.SML_JSON.getMimeType(),
        ResourceFormat.SML_XML.getMimeType(),
        ResourceFormat.OM_JSON.getMimeType(),
        ResourceFormat.SWE_JSON.getMimeType(),
        ResourceFormat.SWE_TEXT.getMimeType(),
        ResourceFormat.SWE_XML.getMimeType(),
        ResourceFormat.SWE_BINARY.getMimeType());

    /** A topic mapped to a CS API resource. {@code format} is null for the default. */
    record ResourceTopic(URI uri, String format) {}

    final String prefix;
    final Map<String, String> formats = new TreeMap<>(); // token -> media type


    /**
     * @param nodeId the node identifier, see {@link #checkNodeId(String)}
     * @param mediaTypes the media types that format subtopics can select.
     *        Types with no valid token, or with the same token as another
     *        type, get no subtopic.
     * @throws IllegalArgumentException if the node identifier is not valid
     */
    TopicMapper(String nodeId, Collection<String> mediaTypes)
    {
        this.prefix = checkNodeId(nodeId) + "/";

        var duplicates = new HashSet<String>();
        for (var type: mediaTypes)
        {
            var token = formatToken(type);
            if (token.isEmpty() || token.indexOf('/') >= 0 || token.indexOf('#') >= 0)
                continue;
            var other = formats.put(token, type);
            if (other != null && !other.equals(type))
                duplicates.add(token);
        }
        formats.keySet().removeAll(duplicates);
    }


    /**
     * Make the format token of a media type (CS API Part 3 format name):
     * remove parameters and the {@code application/} prefix, change
     * {@code +} to {@code -}, and use lowercase.
     */
    static String formatToken(String mediaType)
    {
        var token = mediaType;
        var sep = token.indexOf(';');
        if (sep >= 0)
            token = token.substring(0, sep);
        // media types are case-insensitive, so lowercase first
        token = token.trim().toLowerCase(Locale.ROOT);
        if (token.startsWith("application/"))
            token = token.substring("application/".length());
        return token.replace('+', '-');
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
     * @return the CS API resource that the topic identifies. The URI has no
     *         {@code :data}, and has an {@code f} parameter if the topic has
     *         a format subtopic.
     * @throws InvalidTopicException if the topic is not under this node,
     *         has a query string, is a wildcard filter, is not a Resource
     *         Data topic, or has an unknown format token
     */
    ResourceTopic parse(String topic) throws InvalidTopicException
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
        // (no ':data') come later (see #249).
        var marker = path.lastIndexOf(DATA_MARKER);
        if (marker < 0)
            throw new InvalidTopicException("Only Resource Data topics (with '" + DATA_MARKER + "') are supported");
        var tail = path.substring(marker + DATA_MARKER.length());
        path = path.substring(0, marker);
        if (!DATA_PATH.matcher(path).matches())
            throw new InvalidTopicException("Not a Resource Data topic: " + topic);

        // plain ':data' or ':data/{format}', with no fallback for an unknown token
        String format = null;
        if (!tail.isEmpty())
        {
            var token = tail.startsWith("/") ? tail.substring(1) : "";
            format = formats.get(token);
            if (format == null)
                throw new InvalidTopicException("Unknown format subtopic: " + tail);
        }

        try
        {
            var uri = new URI(null, null, "/" + path, format == null ? null : "f=" + format, null);
            return new ResourceTopic(uri, format);
        }
        catch (URISyntaxException e)
        {
            throw new InvalidTopicException("Invalid resource path in topic: " + topic);
        }
    }
}
