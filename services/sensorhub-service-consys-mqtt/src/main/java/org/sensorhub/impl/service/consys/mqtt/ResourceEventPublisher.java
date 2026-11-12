/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2024 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import org.sensorhub.api.command.CommandStreamAddedEvent;
import org.sensorhub.api.command.CommandStreamChangedEvent;
import org.sensorhub.api.command.CommandStreamDisabledEvent;
import org.sensorhub.api.command.CommandStreamEnabledEvent;
import org.sensorhub.api.command.CommandStreamEvent;
import org.sensorhub.api.command.CommandStreamRemovedEvent;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.DataStreamAddedEvent;
import org.sensorhub.api.data.DataStreamChangedEvent;
import org.sensorhub.api.data.DataStreamDisabledEvent;
import org.sensorhub.api.data.DataStreamEnabledEvent;
import org.sensorhub.api.data.DataStreamEvent;
import org.sensorhub.api.data.DataStreamRemovedEvent;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.api.system.SystemAddedEvent;
import org.sensorhub.api.system.SystemChangedEvent;
import org.sensorhub.api.system.SystemDisabledEvent;
import org.sensorhub.api.system.SystemEnabledEvent;
import org.sensorhub.api.system.SystemEvent;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.slf4j.Logger;
import com.google.gson.stream.JsonWriter;


/**
 * <p>
 * Proactively subscribes to OSH resource lifecycle events and publishes them
 * to the MQTT broker as CloudEvents v1.0 JSON (without data), per
 * OGC CS API Part 3 Resource Event Topics specification.
 * </p>
 *
 * <p>
 * Because lifecycle events are published to specific resource topics (e.g.
 * {@code {nodeId}/systems/134}), the MQTT broker handles wildcard routing
 * to subscribers of patterns like {@code {nodeId}/systems/+} automatically.
 * </p>
 *
 * <p>
 * Per spec, DataStream and ControlStream events are also published to all
 * ancestor system topics so that subscribers watching a root system receive
 * events from all nested subsystems' streams.
 * </p>
 *
 * <p>
 * Covered resource types: Systems (including subsystems), DataStreams,
 * ControlStreams (CommandStreams), and Observations.
 * </p>
 */
public class ResourceEventPublisher
{
    final IMqttServer mqttServer;
    final String nodeId;
    final String csApiBaseUrl;
    final IEventBus eventBus;
    final IObsSystemDatabase db;
    final IdEncoders idEncoders;
    final Logger log;

    /** Subscriptions to OSH system/datastream/commandstream lifecycle events. */
    final List<Flow.Subscription> activeSubscriptions = new ArrayList<>();

    /**
     * Per-datastream ObsEvent subscriptions, keyed by datastream internal ID.
     * Each subscription publishes observation CloudEvents to MQTT as new obs arrive.
     */
    final Map<BigId, Flow.Subscription> obsSubscriptions = new ConcurrentHashMap<>();


    public ResourceEventPublisher(
        IMqttServer mqttServer,
        String nodeId,
        String csApiBaseUrl,
        IEventBus eventBus,
        IObsSystemDatabase db,
        IdEncoders idEncoders,
        Logger log)
    {
        this.mqttServer = mqttServer;
        this.nodeId = nodeId;
        this.csApiBaseUrl = csApiBaseUrl;
        this.eventBus = eventBus;
        this.db = db;
        this.idEncoders = idEncoders;
        this.log = log;
    }


    /**
     * Subscribe to the OSH event bus for all resource lifecycle event types,
     * and start obs subscriptions for all already-registered datastreams.
     * Safe to call only once.
     */
    public void start()
    {
        subscribeToEvents(SystemEvent.class, this::handleSystemEvent);
        subscribeToEvents(DataStreamEvent.class, this::handleDataStreamEvent);
        subscribeToEvents(CommandStreamEvent.class, this::handleCommandStreamEvent);

        // Subscribe to obs events for datastreams that already existed at startup
        db.getDataStreamStore()
            .selectEntries(new DataStreamFilter.Builder().build())
            .forEach(e -> subscribeToObsEvents(
                e.getKey().getInternalID(),
                e.getValue().getSystemID().getInternalID(),
                e.getValue().getSystemID().getUniqueID(),
                e.getValue().getOutputName()));
    }


    /**
     * Cancel all active event bus subscriptions.
     */
    public void stop()
    {
        for (var sub : activeSubscriptions)
            sub.cancel();
        activeSubscriptions.clear();

        for (var sub : obsSubscriptions.values())
            sub.cancel();
        obsSubscriptions.clear();
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private <T extends SystemEvent> void subscribeToEvents(
        Class<T> eventClass,
        java.util.function.Consumer<T> handler)
    {
        eventBus.newSubscription(eventClass)
            .withTopicID(EventUtils.getSystemRegistryTopicID())
            .withEventType(eventClass)
            .subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription)
                {
                    activeSubscriptions.add(subscription);
                    subscription.request(Long.MAX_VALUE);
                    log.debug("CloudEvents publisher subscribed to {} events", eventClass.getSimpleName());
                }

                @Override
                public void onNext(T event)
                {
                    try { handler.accept(event); }
                    catch (Exception e)
                    {
                        log.error("Error handling {} event for CloudEvents publishing", eventClass.getSimpleName(), e);
                    }
                }

                @Override
                public void onError(Throwable e)
                {
                    log.error("Error in CloudEvents publisher subscription for {}", eventClass.getSimpleName(), e);
                }

                @Override
                public void onComplete()
                {
                    log.debug("CloudEvents publisher subscription completed for {}", eventClass.getSimpleName());
                }
            });
    }


    /**
     * Walk up the system ancestor chain from {@code systemId} and return all
     * ancestor internal IDs, nearest first.
     */
    private List<BigId> getAncestorIds(BigId systemId)
    {
        var ancestors = new ArrayList<BigId>();
        var parentId = db.getSystemDescStore().getParent(systemId);
        while (parentId != null)
        {
            ancestors.add(parentId);
            parentId = db.getSystemDescStore().getParent(parentId);
        }
        return ancestors;
    }


    private void handleSystemEvent(SystemEvent event)
    {
        String ceType;
        if      (event instanceof SystemAddedEvent)    ceType = CloudEventsTypes.TYPE_SYSTEM_CREATE;
        else if (event instanceof SystemRemovedEvent)  ceType = CloudEventsTypes.TYPE_SYSTEM_DELETE;
        else if (event instanceof SystemChangedEvent)  ceType = CloudEventsTypes.TYPE_SYSTEM_UPDATE;
        else if (event instanceof SystemEnabledEvent)  ceType = CloudEventsTypes.TYPE_SYSTEM_ENABLE;
        else if (event instanceof SystemDisabledEvent) ceType = CloudEventsTypes.TYPE_SYSTEM_DISABLE;
        else return;

        if (event.getSystemID() == null)
        {
            log.warn("SystemEvent arrived without an assigned system ID; skipping CloudEvent publish");
            return;
        }

        var sysId = idEncoders.getSystemIdEncoder().encodeID(event.getSystemID());
        String mqttTopic;
        String subjectUrl;
        String parentId = null;

        // For subsystem add events, route to subsystems topic and include parentid
        if (event instanceof SystemAddedEvent added && added.getParentGroupUID() != null)
        {
            var parentUid = added.getParentGroupUID();
            var parentInternalId = lookupSystemInternalId(parentUid);
            if (parentInternalId != null)
            {
                var parentEncodedId = idEncoders.getSystemIdEncoder().encodeID(parentInternalId);
                mqttTopic  = nodeId + "/systems/" + parentEncodedId + "/subsystems/" + sysId;
                subjectUrl = csApiBaseUrl + "/systems/" + parentEncodedId + "/subsystems/" + sysId;
                parentId   = csApiBaseUrl + "/systems/" + parentEncodedId;
            }
            else
            {
                // Parent not found — fall back to top-level topic
                log.warn("Could not resolve parent system UID '{}'; publishing subsystem event as top-level", parentUid);
                mqttTopic  = nodeId + "/systems/" + sysId;
                subjectUrl = csApiBaseUrl + "/systems/" + sysId;
            }
        }
        else
        {
            mqttTopic  = nodeId + "/systems/" + sysId;
            subjectUrl = csApiBaseUrl + "/systems/" + sysId;
        }

        publishCloudEvent(mqttTopic, ceType, subjectUrl, event.getTimeStamp(), parentId);
    }


    private void handleDataStreamEvent(DataStreamEvent event)
    {
        // ObsEvent is a DataStreamEvent subtype but is handled separately via obsSubscriptions
        if (event instanceof ObsEvent)
            return;

        String ceType;
        if      (event instanceof DataStreamAddedEvent)    ceType = CloudEventsTypes.TYPE_DATASTREAM_CREATE;
        else if (event instanceof DataStreamRemovedEvent)  ceType = CloudEventsTypes.TYPE_DATASTREAM_DELETE;
        else if (event instanceof DataStreamChangedEvent)  ceType = CloudEventsTypes.TYPE_DATASTREAM_UPDATE;
        else if (event instanceof DataStreamEnabledEvent)  ceType = CloudEventsTypes.TYPE_DATASTREAM_ENABLE;
        else if (event instanceof DataStreamDisabledEvent) ceType = CloudEventsTypes.TYPE_DATASTREAM_DISABLE;
        else return;

        if (event.getDataStreamID() == null || event.getSystemID() == null)
        {
            log.warn("DataStreamEvent arrived without assigned IDs; skipping CloudEvent publish");
            return;
        }

        var dsId  = idEncoders.getDataStreamIdEncoder().encodeID(event.getDataStreamID());
        var sysId = idEncoders.getSystemIdEncoder().encodeID(event.getSystemID());

        // Canonical resource URL — same for all published topics
        var subjectUrl = csApiBaseUrl + "/systems/" + sysId + "/datastreams/" + dsId;

        // Publish to direct parent system topic
        publishCloudEvent(
            nodeId + "/systems/" + sysId + "/datastreams/" + dsId,
            ceType, subjectUrl, event.getTimeStamp(),
            csApiBaseUrl + "/systems/" + sysId);

        // Per spec: "SHALL also include events for all subsystems' datastreams, recursively"
        // Publish to each ancestor system's topic so subscribers watching a root system
        // receive events from all nested subsystems' streams.
        for (var ancestorId : getAncestorIds(event.getSystemID()))
        {
            var ancestorSysId = idEncoders.getSystemIdEncoder().encodeID(ancestorId);
            publishCloudEvent(
                nodeId + "/systems/" + ancestorSysId + "/datastreams/" + dsId,
                ceType, subjectUrl, event.getTimeStamp(),
                csApiBaseUrl + "/systems/" + ancestorSysId);
        }

        // Manage per-datastream observation subscriptions
        if (event instanceof DataStreamAddedEvent)
        {
            subscribeToObsEvents(
                event.getDataStreamID(), event.getSystemID(),
                event.getSystemUID(), event.getOutputName());
        }
        else if (event instanceof DataStreamRemovedEvent)
        {
            var sub = obsSubscriptions.remove(event.getDataStreamID());
            if (sub != null)
                sub.cancel();
        }
    }


    private void handleCommandStreamEvent(CommandStreamEvent event)
    {
        String ceType;
        if      (event instanceof CommandStreamAddedEvent)    ceType = CloudEventsTypes.TYPE_CONTROLSTREAM_CREATE;
        else if (event instanceof CommandStreamRemovedEvent)  ceType = CloudEventsTypes.TYPE_CONTROLSTREAM_DELETE;
        else if (event instanceof CommandStreamChangedEvent)  ceType = CloudEventsTypes.TYPE_CONTROLSTREAM_UPDATE;
        else if (event instanceof CommandStreamEnabledEvent)  ceType = CloudEventsTypes.TYPE_CONTROLSTREAM_ENABLE;
        else if (event instanceof CommandStreamDisabledEvent) ceType = CloudEventsTypes.TYPE_CONTROLSTREAM_DISABLE;
        else return;

        if (event.getCommandStreamID() == null || event.getSystemID() == null)
        {
            log.warn("CommandStreamEvent arrived without assigned IDs; skipping CloudEvent publish");
            return;
        }

        var csId  = idEncoders.getCommandStreamIdEncoder().encodeID(event.getCommandStreamID());
        var sysId = idEncoders.getSystemIdEncoder().encodeID(event.getSystemID());

        // Canonical resource URL — same for all published topics
        var subjectUrl = csApiBaseUrl + "/systems/" + sysId + "/controlstreams/" + csId;

        // Publish to direct parent system topic
        publishCloudEvent(
            nodeId + "/systems/" + sysId + "/controlstreams/" + csId,
            ceType, subjectUrl, event.getTimeStamp(),
            csApiBaseUrl + "/systems/" + sysId);

        // Per spec: "SHALL also include events for all subsystems' controlstreams, recursively"
        for (var ancestorId : getAncestorIds(event.getSystemID()))
        {
            var ancestorSysId = idEncoders.getSystemIdEncoder().encodeID(ancestorId);
            publishCloudEvent(
                nodeId + "/systems/" + ancestorSysId + "/controlstreams/" + csId,
                ceType, subjectUrl, event.getTimeStamp(),
                csApiBaseUrl + "/systems/" + ancestorSysId);
        }
    }


    /**
     * Subscribe to {@link ObsEvent}s on the given datastream's data channel and
     * publish a CloudEvent to MQTT for each arriving observation.
     *
     * <p>The MQTT topic uses the observation phenomenon time (epoch-ms) as the
     * observation ID segment since the internal database ID is not yet assigned
     * when {@link ObsEvent} is published (events are fired before DB storage).</p>
     *
     * <p>If a subscription for this datastream already exists the call is a no-op.</p>
     */
    private void subscribeToObsEvents(BigId dsId, BigId sysId, String sysUID, String outputName)
    {
        // Guard against duplicate subscriptions (e.g. startup scan racing with DataStreamAddedEvent)
        if (obsSubscriptions.containsKey(dsId))
            return;

        var encodedSysId    = idEncoders.getSystemIdEncoder().encodeID(sysId);
        var encodedDsId     = idEncoders.getDataStreamIdEncoder().encodeID(dsId);
        var ancestorSysIds  = getAncestorIds(sysId).stream()
            .map(id -> idEncoders.getSystemIdEncoder().encodeID(id))
            .toList();

        var obsTopicId = EventUtils.getDataStreamDataTopicID(sysUID, outputName);

        eventBus.newSubscription(ObsEvent.class)
            .withTopicID(obsTopicId)
            .withEventType(ObsEvent.class)
            .subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription)
                {
                    // Atomically register; cancel if a duplicate raced ahead
                    if (obsSubscriptions.putIfAbsent(dsId, subscription) != null)
                        subscription.cancel();
                    else
                        subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ObsEvent event)
                {
                    for (var obs : event.getObservations())
                    {
                        // Use phenomenon time as a stable, unique-enough ID since the
                        // internal obs ID is not yet assigned at event publication time.
                        var obsId      = String.valueOf(obs.getPhenomenonTime().toEpochMilli());
                        var obsPath    = "/systems/" + encodedSysId + "/datastreams/" + encodedDsId + "/observations/";
                        var subjectUrl = csApiBaseUrl + obsPath + obsId;
                        var parentUrl  = csApiBaseUrl + "/systems/" + encodedSysId + "/datastreams/" + encodedDsId;

                        // Direct parent topic
                        publishCloudEvent(
                            nodeId + obsPath + obsId,
                            CloudEventsTypes.TYPE_OBS_CREATE,
                            subjectUrl, event.getTimeStamp(), parentUrl);

                        // Ancestor system topics (recursive per spec)
                        for (var ancestorSysId : ancestorSysIds)
                        {
                            publishCloudEvent(
                                nodeId + "/systems/" + ancestorSysId + "/datastreams/" + encodedDsId + "/observations/" + obsId,
                                CloudEventsTypes.TYPE_OBS_CREATE,
                                subjectUrl, event.getTimeStamp(),
                                csApiBaseUrl + "/systems/" + ancestorSysId + "/datastreams/" + encodedDsId);
                        }
                    }
                }

                @Override
                public void onError(Throwable e)
                {
                    log.error("Error in obs CloudEvents subscription for datastream {}", encodedDsId, e);
                }

                @Override
                public void onComplete()
                {
                    log.debug("Obs CloudEvents subscription completed for datastream {}", encodedDsId);
                }
            });

        log.debug("Started obs CloudEvents subscription for datastream {}", encodedDsId);
    }


    /**
     * Look up the internal BigId of a system given its unique identifier (UID string).
     * Used only for subsystem parent resolution. Returns null if not found.
     */
    private BigId lookupSystemInternalId(String uid)
    {
        try
        {
            return db.getSystemDescStore()
                .selectEntries(new SystemFilter.Builder().withUniqueIDs(uid).build())
                .findFirst()
                .map(e -> e.getKey().getInternalID())
                .orElse(null);
        }
        catch (Exception e)
        {
            log.warn("Error looking up system internal ID for UID '{}'", uid, e);
            return null;
        }
    }


    /**
     * Serialize and publish a CloudEvents v1.0 JSON message (without data) to the
     * MQTT broker on the given topic.
     *
     * @param topic       MQTT topic to publish to
     * @param ceType      CloudEvents {@code type} field value
     * @param subject     CloudEvents {@code subject} field value (canonical resource URL)
     * @param timestampMs Event timestamp in milliseconds since epoch
     * @param parentId    Optional {@code parentid} extension value (canonical URL of parent resource)
     */
    private void publishCloudEvent(String topic, String ceType, String subject,
                                   long timestampMs, String parentId)
    {
        try
        {
            var sw = new StringWriter();
            try (var w = new JsonWriter(sw))
            {
                w.beginObject();
                w.name("specversion").value(CloudEventsTypes.SPECVERSION);
                w.name("id").value(UUID.randomUUID().toString());
                w.name("type").value(ceType);
                w.name("source").value(csApiBaseUrl);
                w.name("subject").value(subject);
                w.name("time").value(Instant.ofEpochMilli(timestampMs).toString());
                if (parentId != null)
                    w.name("parentid").value(parentId);
                w.endObject();
            }
            var bytes = sw.toString().getBytes(StandardCharsets.UTF_8);
            mqttServer.publish(topic, ByteBuffer.wrap(bytes));
            log.debug("Published CloudEvent type='{}' to MQTT topic '{}'", ceType, topic);
        }
        catch (Exception e)
        {
            log.error("Failed to publish CloudEvent to MQTT topic '{}'", topic, e);
        }
    }
}