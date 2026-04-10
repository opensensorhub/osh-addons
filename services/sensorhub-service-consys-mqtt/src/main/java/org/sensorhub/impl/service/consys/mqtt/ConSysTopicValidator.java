/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import java.util.List;
import java.util.Optional;
import static org.sensorhub.impl.service.consys.mqtt.ConSysTopicValidator.ResourceType.*;


/**
 * <p>
 * Validates MQTT topic patterns against the OGC Connected Systems API Part 3
 * (Pub/Sub) topic hierarchy, with support for MQTT wildcards ({@code +} and
 * {@code #}).
 * </p>
 *
 * <p>
 * The key constraint from the spec: wildcards may only appear in
 * <em>resource ID</em> positions — never in resource type segments
 * (e.g. {@code systems}, {@code datastreams}). For example,
 * {@code systems/+/datastreams/+} is valid; {@code +/134} is not.
 * </p>
 *
 * <p>
 * Usage: strip the nodeId/endpoint prefix from a topic first, then call
 * {@link #matchEventTopic(String)} to validate structure and identify the
 * resource type (for permission mapping).
 * </p>
 *
 * <p>
 * Wildcard semantics:
 * <ul>
 *   <li>{@code +} — matches exactly one resource ID segment</li>
 *   <li>{@code #} — matches a resource ID segment and everything below it
 *       (must be the last segment; permission is based on the depth at which
 *       {@code #} appears)</li>
 * </ul>
 * </p>
 *
 * @author Ian Patterson
 * @since Apr 7, 2026
 */
public final class ConSysTopicValidator
{
    private ConSysTopicValidator() {}


    /**
     * CS API resource types, used for permission mapping after topic matching.
     */
    public enum ResourceType
    {
        SYSTEM, DATASTREAM, CONTROLSTREAM, OBSERVATION, COMMAND, DEPLOYMENT, PROCEDURE, PROPERTY
    }


    /**
     * A structural pattern for a CS API topic.
     * Each element in {@code segments} is either a literal string (resource type
     * keyword) or {@code null} (marks a resource ID position where {@code +} or
     * {@code #} wildcards are valid).
     */
    private record TopicPattern(ResourceType type, String[] segments)
    {
        // Convenience constructor — parses "{keyword}/{id}/..." strings where
        // "{id}" becomes null in the segments array.
        static TopicPattern of(ResourceType type, String template)
        {
            var templateParts = template.split("/", -1);
            var segments = new String[templateParts.length];
            for (int i = 0; i < templateParts.length; i++)
                segments[i] = "{id}".equals(templateParts[i]) ? null : templateParts[i];
            return new TopicPattern(type, segments);
        }
    }


    /**
     * All known CS API Part 3 resource event topic patterns (after stripping
     * the nodeId prefix). Ordered from least-specific to most-specific (shortest
     * to longest) so that when {@code #} appears at an ID position the shallowest
     * — and therefore most semantically correct — ResourceType is returned first.
     * Exact-topic and {@code +}-only subscriptions are unaffected by this order
     * because a shorter pattern never matches a longer exact topic.
     */
    private static final List<TopicPattern> EVENT_PATTERNS = List.of(
        // 2-segment patterns (least specific)
        TopicPattern.of(SYSTEM,        "systems/{id}"),
        TopicPattern.of(DEPLOYMENT,    "deployments/{id}"),
        TopicPattern.of(PROCEDURE,     "procedures/{id}"),
        TopicPattern.of(PROPERTY,      "properties/{id}"),

        // 4-segment patterns
        TopicPattern.of(SYSTEM,        "systems/{id}/subsystems/{id}"),
        TopicPattern.of(SYSTEM,        "systems/{id}/deployments/{id}"),
        TopicPattern.of(DATASTREAM,    "systems/{id}/datastreams/{id}"),
        TopicPattern.of(CONTROLSTREAM, "systems/{id}/controlstreams/{id}"),
        TopicPattern.of(DEPLOYMENT,    "deployments/{id}/subdeployments/{id}"),

        // 6-segment patterns
        TopicPattern.of(OBSERVATION,   "systems/{id}/datastreams/{id}/observations/{id}"),
        TopicPattern.of(COMMAND,       "systems/{id}/controlstreams/{id}/commands/{id}"),

        // 8-segment patterns (most specific)
        TopicPattern.of(COMMAND,       "systems/{id}/controlstreams/{id}/commands/{id}/status/{id}"),
        TopicPattern.of(COMMAND,       "systems/{id}/controlstreams/{id}/commands/{id}/result/{id}")
    );


    /**
     * Match a topic path (after stripping the nodeId or endpoint prefix)
     * against all known CS API resource event topic patterns.
     *
     * <p>Wildcards are accepted only in {@code {id}} positions. A {@code +}
     * matches exactly one ID segment; a {@code #} matches an ID segment and
     * everything below (must be the last segment).</p>
     *
     * @param path the stripped topic path, e.g. {@code "systems/+/datastreams/+"}
     * @return the matched {@link ResourceType}, or empty if no pattern matches
     *         (invalid topic or wildcard in a non-ID position)
     */
    public static Optional<ResourceType> matchEventTopic(String path)
    {
        var topicSegments = path.split("/", -1);
        for (var pattern : EVENT_PATTERNS)
        {
            var result = matchPattern(topicSegments, pattern.segments(), pattern.type());
            if (result.isPresent())
                return result;
        }
        return Optional.empty();
    }


    /**
     * Returns {@code true} if the topic string contains any MQTT wildcard
     * characters ({@code +} or {@code #}).
     */
    public static boolean hasWildcard(String topic)
    {
        return topic.contains("+") || topic.contains("#");
    }


    /**
     * Attempt to match {@code topicSegments} against {@code patternSegments}.
     *
     * <p>Rules:
     * <ul>
     *   <li>A literal pattern segment must equal the corresponding topic segment.</li>
     *   <li>A {@code null} pattern segment (ID position) accepts any concrete ID or {@code +}.</li>
     *   <li>{@code +} is only valid at a {@code null} (ID) pattern position.</li>
     *   <li>{@code #} must always be the last topic segment (standard MQTT). It is valid
     *       in two positions:
     *       <ol>
     *         <li><b>At an ID position</b> ({@code null} pattern slot) — replaces that
     *             resource ID and everything below it.
     *             e.g. {@code systems/+/subsystems/#} or {@code systems/abc123/datastreams/#}</li>
     *         <li><b>Trailing after full pattern exhaustion</b> — subscribes to the
     *             matched resource and all its children.
     *             e.g. {@code systems/abc123/#} or {@code systems/+/#}</li>
     *       </ol>
     *       {@code #} is invalid at a keyword (literal) position,
     *       e.g. {@code systems/#/datastreams/+} or a bare {@code #}.</li>
     * </ul>
     * </p>
     *
     * <p>Patterns are checked in least-specific-first order so that when {@code #}
     * matches at an ID position the shallowest applicable ResourceType wins
     * (e.g. {@code systems/abc123/datastreams/#} returns DATASTREAM, not OBSERVATION).</p>
     */
    private static Optional<ResourceType> matchPattern(
        String[] topicSegments, String[] patternSegments, ResourceType type)
    {
        int topicIdx = 0, patternIdx = 0;

        while (topicIdx < topicSegments.length)
        {
            var topicSeg = topicSegments[topicIdx];

            if ("#".equals(topicSeg))
            {
                // # must be the last topic segment (standard MQTT)
                if (topicIdx != topicSegments.length - 1)
                    return Optional.empty();

                // Valid at an ID (null) position in the pattern — replaces that resource ID
                if (patternIdx < patternSegments.length && patternSegments[patternIdx] == null)
                    return Optional.of(type);

                // Valid as a trailing wildcard after the pattern is fully consumed
                if (patternIdx == patternSegments.length)
                    return Optional.of(type);

                // Invalid: # where a keyword is expected
                return Optional.empty();
            }

            if (patternIdx >= patternSegments.length)
                return Optional.empty(); // more topic segments than pattern, no # to terminate

            var patternSeg = patternSegments[patternIdx]; // null = ID position

            if ("+".equals(topicSeg))
            {
                if (patternSeg == null) { topicIdx++; patternIdx++; }
                else return Optional.empty(); // + in a keyword position
            }
            else
            {
                if (patternSeg == null)                { topicIdx++; patternIdx++; } // concrete ID in ID slot
                else if (topicSeg.equals(patternSeg))  { topicIdx++; patternIdx++; } // keyword matches
                else return Optional.empty();                                          // keyword mismatch
            }
        }

        // Exact match: both pattern and topic fully consumed
        if (patternIdx == patternSegments.length)
            return Optional.of(type);

        return Optional.empty();
    }
}