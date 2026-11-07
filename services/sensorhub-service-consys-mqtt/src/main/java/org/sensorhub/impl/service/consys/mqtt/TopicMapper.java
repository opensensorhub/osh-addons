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
import java.util.Collection;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;


/**
 * <p>
 * Maps MQTT topics to CS API resource paths, per the OGC CS API Part 3
 * topic grammar: every topic starts with {@code {nodeId}/}, followed by
 * the resource path. Topics do not carry query strings.
 * </p><p>
 * Resource Data topics end with {@code :data}, for example
 * {@code node1/systems/134/datastreams/12/observations:data}. The plain
 * {@code :data} topic selects the default representation, and
 * {@code :data/{format}} selects another one by its format token.
 * Resource Event topics have no {@code :data}, for example
 * {@code node1/systems/134/datastreams/12}. They are accepted for
 * systems, subsystems, datastreams and control streams. The Batch Resource
 * Event topic of the observations of a datastream also has no
 * {@code :data}: {@code node1/systems/134/datastreams/12/observations}.
 * The topic shape
 * is checked here; that each ID belongs to its parent, and that the
 * resource offers the format, is left to the CS API.
 * </p><p>
 * Wildcard topic filters ({@code +}, {@code #}) are accepted for Resource
 * Event and Batch Resource Event topics only (see {@link #parseFilter}).
 * </p>
 */
final class TopicMapper
{
    static final String DATA_MARKER = ":data";
    static final String SYSTEMS_PATH = "/systems";

    // Resource Data topic templates (OGC CS API Part 3, Resource Data Topics
    // table), without '{nodeId}/' and ':data'. An ID is one level with no ':'.
    static final Pattern DATA_PATH = Pattern.compile(
        "systems(/[^/:]+(/subsystems/[^/:]+)*/("
        + "subsystems|datastreams|controlstreams|events"
        + "|datastreams/[^/:]+/observations"
        + "|controlstreams/[^/:]+/commands(/[^/:]+/(status|result))?))?");

    // Resource Event topic templates (OGC CS API Part 3, Resource Event Topics
    // table), without '{nodeId}/', for the resource types that have events
    static final Pattern EVENT_PATH = Pattern.compile(
        "systems/[^/:]+(/subsystems/[^/:]+)*(/(datastreams|controlstreams)/[^/:]+)?");

    // Batch Resource Event topic of the observations of one datastream
    static final Pattern BATCH_PATH = Pattern.compile(
        "systems/[^/:]+(/subsystems/[^/:]+)*/datastreams/[^/:]+/observations");
    static final String BATCH_SUFFIX = "/observations";

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

    /**
     * A topic mapped to a CS API resource. {@code format} is null for the
     * default. {@code event} is true for a Resource Event or Batch Resource
     * Event topic. For a Batch Resource Event topic, {@code uri} is the
     * datastream, which a subscriber must be able to read.
     */
    record ResourceTopic(URI uri, String format, boolean event) {}

    /**
     * A wildcard topic filter. {@code uri} is the deepest resource that the
     * filter names before its first wildcard, or the systems collection if it
     * names no resource. A subscriber must be able to read it.
     */
    record TopicFilter(URI uri) {}

    // States of the Resource Event and Batch Resource Event topic grammar,
    // one MQTT level at a time (see EVENT_PATH and BATCH_PATH)
    static final int SYSTEMS = 0, SYSTEM_ID = 1, SYSTEM = 2, SUBSYSTEM_ID = 3,
        DATASTREAM_ID = 4, DATASTREAM = 5, CONTROLSTREAM_ID = 6, OBSERVATIONS = 7, CONTROLSTREAM = 8;
    static final Set<Integer> EVENT_STATES = Set.of(SYSTEM, DATASTREAM, OBSERVATIONS, CONTROLSTREAM);

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
     *         Data or Resource Event topic, or has an unknown format token
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

        // Wildcard filters are parsed by parseFilter().
        // Reject them here: URI would read '#' as a fragment, and map
        // 'node1/systems/134/#' to the parent resource '/systems/134/'.
        if (isFilter(topic))
            throw new InvalidTopicException("Wildcard topic filters are not allowed here");

        // a topic without ':data' is a Resource Event or Batch Resource Event topic
        var marker = path.lastIndexOf(DATA_MARKER);
        if (marker < 0)
        {
            if (BATCH_PATH.matcher(path).matches())
                path = path.substring(0, path.length() - BATCH_SUFFIX.length());
            else if (!EVENT_PATH.matcher(path).matches())
                throw new InvalidTopicException("Not a Resource Data or Resource Event topic: " + topic);
            return new ResourceTopic(toUri(topic, path, null), null, true);
        }
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

        return new ResourceTopic(toUri(topic, path, format), format, false);
    }


    /**
     * @return true if the topic has a wildcard character, so it is a topic
     *         filter and not a topic name
     */
    static boolean isFilter(String topic)
    {
        return topic.indexOf('+') >= 0 || topic.indexOf('#') >= 0;
    }


    /**
     * Parse a wildcard topic filter. A filter is accepted only if it follows
     * the MQTT wildcard rules, and can match a Resource Event or Batch
     * Resource Event topic. Resource Data topics ({@code :data}) cannot be
     * subscribed to with wildcards, but a filter that ends with {@code #}
     * also matches them: those messages are not sent to the subscriber.
     * @param filter an MQTT topic filter with {@code +} or {@code #}
     * @return the filter, with the resource that a subscriber must be able
     *         to read
     * @throws InvalidTopicException if the filter is not under this node, is
     *         not a valid MQTT filter, names a Resource Data topic, or cannot
     *         match any Resource Event or Batch Resource Event topic
     */
    TopicFilter parseFilter(String filter) throws InvalidTopicException
    {
        if (filter == null || !filter.startsWith(prefix))
            throw new InvalidTopicException("Topic filter must start with '" + prefix + "'");

        var levels = filter.substring(prefix.length()).split("/", -1);
        var states = Set.of(SYSTEMS);
        String resource = null; // deepest resource before the first wildcard
        var path = new StringBuilder();
        var literal = true;

        for (int i = 0; i < levels.length; i++)
        {
            var level = levels[i];
            if (level.indexOf(':') >= 0)
                throw new InvalidTopicException("Wildcard topic filters are not supported on Resource Data topics: " + filter);

            if (level.equals("#"))
            {
                if (i != levels.length - 1)
                    throw new InvalidTopicException("'#' must be the last level of a topic filter: " + filter);
                return toFilter(filter, resource);
            }
            if (level.equals("+"))
                literal = false;
            else if (level.indexOf('+') >= 0 || level.indexOf('#') >= 0)
                throw new InvalidTopicException("A wildcard must be a whole topic level: " + filter);
            else if (level.isEmpty())
                throw new InvalidTopicException("Empty topic level in filter: " + filter);

            states = next(states, level);
            if (states.isEmpty())
                throw new InvalidTopicException("Topic filter matches no Resource Event topic: " + filter);

            if (literal)
            {
                path.append(i == 0 ? "" : "/").append(level);
                if (states.size() == 1 && EVENT_STATES.contains(states.iterator().next()))
                    resource = path.toString();
            }
        }

        if (states.stream().noneMatch(EVENT_STATES::contains))
            throw new InvalidTopicException("Topic filter matches no Resource Event topic: " + filter);

        return toFilter(filter, resource);
    }


    static TopicFilter toFilter(String filter, String resource) throws InvalidTopicException
    {
        // no resource before the first wildcard: check the systems collection
        if (resource == null)
            resource = "systems";

        // a Batch Resource Event topic is checked on its datastream
        else if (BATCH_PATH.matcher(resource).matches())
            resource = resource.substring(0, resource.length() - BATCH_SUFFIX.length());

        return new TopicFilter(toUri(filter, resource, null));
    }


    /**
     * @return the grammar states after one level. {@code +} takes all
     *         transitions; an ID is any level other than {@code +}.
     */
    static Set<Integer> next(Set<Integer> states, String level)
    {
        var any = level.equals("+");
        var next = new HashSet<Integer>();
        for (var state: states)
        {
            switch (state)
            {
                case SYSTEMS:
                    if (any || level.equals("systems")) next.add(SYSTEM_ID);
                    break;
                case SYSTEM_ID:
                case SUBSYSTEM_ID:
                    next.add(SYSTEM);
                    break;
                case SYSTEM:
                    if (any || level.equals("subsystems")) next.add(SUBSYSTEM_ID);
                    if (any || level.equals("datastreams")) next.add(DATASTREAM_ID);
                    if (any || level.equals("controlstreams")) next.add(CONTROLSTREAM_ID);
                    break;
                case DATASTREAM_ID:
                    next.add(DATASTREAM);
                    break;
                case CONTROLSTREAM_ID:
                    next.add(CONTROLSTREAM);
                    break;
                case DATASTREAM:
                    if (any || level.equals("observations")) next.add(OBSERVATIONS);
                    break;
                default:
                    break;
            }
        }
        return next;
    }


    static URI toUri(String topic, String path, String format) throws InvalidTopicException
    {
        try
        {
            return new URI(null, null, "/" + path, format == null ? null : "f=" + format, null);
        }
        catch (URISyntaxException e)
        {
            throw new InvalidTopicException("Invalid resource path in topic: " + topic);
        }
    }
}
