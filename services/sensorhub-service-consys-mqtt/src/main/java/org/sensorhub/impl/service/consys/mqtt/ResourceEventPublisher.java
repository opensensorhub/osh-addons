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
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.sensorhub.api.data.IObsData;
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
    final int batchIntervalSeconds;

    /** Subscriptions to OSH system/datastream/commandstream lifecycle events. */
    final List<Flow.Subscription> activeSubscriptions = new ArrayList<>();

    /**
     * Per-datastream observation batching state, keyed by datastream internal ID.
     * Each entry holds a counter, the start of the current window, and the precomputed
     * topic/URL strings. Observations are accumulated in {@link #recordObsBatch} and
     * flushed by the scheduler in {@link #flushAllBatches}.
     */
    final Map<BigId, ObsBatchState> obsBatches = new ConcurrentHashMap<>();

    /** Single-thread scheduler that drives periodic batch flushes. */
    ScheduledExecutorService scheduler;


    public ResourceEventPublisher(
        IMqttServer mqttServer,
        String nodeId,
        String csApiBaseUrl,
        IEventBus eventBus,
        IObsSystemDatabase db,
        IdEncoders idEncoders,
        Logger log,
        int batchIntervalSeconds)
    {
        this.mqttServer = mqttServer;
        this.nodeId = nodeId;
        this.csApiBaseUrl = csApiBaseUrl;
        this.eventBus = eventBus;
        this.db = db;
        this.idEncoders = idEncoders;
        this.log = log;
        // Defensive clamp: scheduler refuses zero/negative periods.
        this.batchIntervalSeconds = Math.max(1, batchIntervalSeconds);
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

        // Schedule the periodic batch flusher. The first run fires after one full
        // interval so the initial window collects a full batch.
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "consys-mqtt-obs-batch-flusher");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flushAllBatches,
            batchIntervalSeconds, batchIntervalSeconds, TimeUnit.SECONDS);
        log.info("Observation batch CloudEvents publisher started; interval = {}s", batchIntervalSeconds);
    }


    /**
     * Cancel all active event bus subscriptions and stop the batch flusher.
     * Pending (un-flushed) observations in the current window are dropped — stop
     * is intended to be predictable and quick rather than to drain in-flight state.
     */
    public void stop()
    {
        if (scheduler != null)
        {
            scheduler.shutdownNow();
            try
            {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
                    log.warn("Batch flusher did not terminate within 5s");
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }

        for (var sub : activeSubscriptions)
            sub.cancel();
        activeSubscriptions.clear();

        for (var state : obsBatches.values())
            state.subscription.cancel();
        obsBatches.clear();
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
            var state = obsBatches.remove(event.getDataStreamID());
            if (state != null)
                state.subscription.cancel();
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
     * accumulate counts for periodic batch CloudEvent publishing per OGC CS API
     * Part 3 §"Batch Resource Events".
     *
     * <p>Observations are not published one-by-one; they are counted into a
     * per-datastream window and the scheduler in {@link #flushAllBatches} emits
     * one summary CloudEvent per topic per interval.</p>
     *
     * <p>If a subscription for this datastream already exists the call is a no-op.</p>
     */
    private void subscribeToObsEvents(BigId dsId, BigId sysId, String sysUID, String outputName)
    {
        // Guard against duplicate subscriptions (e.g. startup scan racing with DataStreamAddedEvent)
        if (obsBatches.containsKey(dsId))
            return;

        var encodedSysId    = idEncoders.getSystemIdEncoder().encodeID(sysId);
        var encodedDsId     = idEncoders.getDataStreamIdEncoder().encodeID(dsId);
        var ancestorSysIds  = getAncestorIds(sysId).stream()
            .map(id -> idEncoders.getSystemIdEncoder().encodeID(id))
            .toList();
        var parentUrl       = csApiBaseUrl + "/systems/" + encodedSysId + "/datastreams/" + encodedDsId;

        var obsTopicId = EventUtils.getDataStreamDataTopicID(sysUID, outputName);

        eventBus.newSubscription(ObsEvent.class)
            .withTopicID(obsTopicId)
            .withEventType(ObsEvent.class)
            .subscribe(new Flow.Subscriber<>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription)
                {
                    // Atomically register; cancel if a duplicate raced ahead.
                    var state = new ObsBatchState(
                        subscription, encodedSysId, encodedDsId, parentUrl, ancestorSysIds);
                    if (obsBatches.putIfAbsent(dsId, state) != null)
                        subscription.cancel();
                    else
                        subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ObsEvent event)
                {
                    recordObsBatch(dsId, event.getObservations());
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


    /**
     * Drain every per-datastream window and publish one batch CloudEvent per
     * (datastream + ancestor system) topic for any window that received at least
     * one observation since the previous flush. Empty windows are skipped — no
     * zero-count events are emitted.
     */
    void flushAllBatches()
    {
        var flushEnd = Instant.now();
        for (var entry : obsBatches.entrySet())
        {
            try
            {
                flushOne(entry.getValue(), flushEnd);
            }
            catch (Exception e)
            {
                log.error("Error flushing obs batch for datastream {}", entry.getValue().encodedDsId, e);
            }
        }
    }


    /**
     * Add the given observation array to the batching state for {@code dsId}.
     * Empty arrays are a no-op. The earliest phenomenon time across the array
     * is used as the candidate window start.
     */
    void recordObsBatch(BigId dsId, IObsData[] obsArr)
    {
        if (obsArr.length == 0)
            return;

        Instant earliest = obsArr[0].getPhenomenonTime();
        for (int i = 1; i < obsArr.length; i++)
        {
            var t = obsArr[i].getPhenomenonTime();
            if (t.isBefore(earliest))
                earliest = t;
        }

        var state = obsBatches.get(dsId);
        if (state != null)
            state.recordObs(obsArr.length, earliest);
    }


    private static final String OBSERVATIONS_PATH = "/observations";


    private void flushOne(ObsBatchState state, Instant flushEnd)
    {
        var snap = state.snapAndReset();
        if (snap == null)
            return;

        // Spec: subject is the canonical URL of the resource collection.
        var subjectUrl = state.parentUrl + OBSERVATIONS_PATH;

        // Direct parent topic
        publishBatchObsCloudEvent(
            nodeId + "/systems/" + state.encodedSysId + "/datastreams/" + state.encodedDsId + OBSERVATIONS_PATH,
            subjectUrl, state.parentUrl,
            snap.windowStart(), flushEnd, snap.count());

        // Ancestor system topics (recursive — same propagation as the per-obs path used)
        for (var ancestorSysId : state.ancestorSysIds)
        {
            var ancestorParent = csApiBaseUrl + "/systems/" + ancestorSysId + "/datastreams/" + state.encodedDsId;
            publishBatchObsCloudEvent(
                nodeId + "/systems/" + ancestorSysId + "/datastreams/" + state.encodedDsId + OBSERVATIONS_PATH,
                subjectUrl, ancestorParent,
                snap.windowStart(), flushEnd, snap.count());
        }
    }


    /**
     * Serialize and publish a Batch Resource Event CloudEvent v1.0 JSON message
     * to MQTT, per OGC CS API Part 3 §"Batch Resource Events". The {@code data}
     * field carries {@code {timerange:[start,end], count:N}}.
     */
    private void publishBatchObsCloudEvent(String topic, String subject, String parentId,
                                           Instant windowStart, Instant windowEnd, long count)
    {
        try
        {
            var sw = new StringWriter();
            try (var w = new JsonWriter(sw))
            {
                w.beginObject();
                w.name("specversion").value(CloudEventsTypes.SPECVERSION);
                w.name("id").value(UUID.randomUUID().toString());
                w.name("type").value(CloudEventsTypes.TYPE_OBS_CREATE);
                w.name("source").value(csApiBaseUrl);
                w.name("subject").value(subject);
                w.name("time").value(windowEnd.toString());
                if (parentId != null)
                    w.name("parentid").value(parentId);
                w.name("datacontenttype").value("application/json");
                w.name("data").beginObject();
                w.name("timerange").beginArray()
                    .value(windowStart.toString())
                    .value(windowEnd.toString())
                    .endArray();
                w.name("count").value(count);
                w.endObject();
                w.endObject();
            }
            var bytes = sw.toString().getBytes(StandardCharsets.UTF_8);
            mqttServer.publish(topic, ByteBuffer.wrap(bytes));
            log.debug("Published batch CloudEvent count={} to MQTT topic '{}'", count, topic);
        }
        catch (Exception e)
        {
            log.error("Failed to publish batch CloudEvent to MQTT topic '{}'", topic, e);
        }
    }


    /**
     * Per-datastream batching state. The {@code count} and {@code windowStart}
     * fields are mutated by both observation arrivals and the scheduled flusher,
     * so all reads/writes of those two fields go through the synchronized
     * methods here.
     */
    static final class ObsBatchState
    {
        final Flow.Subscription subscription;
        final String encodedSysId;
        final String encodedDsId;
        final String parentUrl;
        final List<String> ancestorSysIds;

        // Guarded by `this`.
        long count;
        Instant windowStart;

        ObsBatchState(Flow.Subscription subscription, String encodedSysId,
                      String encodedDsId, String parentUrl, List<String> ancestorSysIds)
        {
            this.subscription = subscription;
            this.encodedSysId = encodedSysId;
            this.encodedDsId = encodedDsId;
            this.parentUrl = parentUrl;
            this.ancestorSysIds = ancestorSysIds;
        }

        synchronized void recordObs(int n, Instant earliestPhenomenonTime)
        {
            count += n;
            if (windowStart == null || earliestPhenomenonTime.isBefore(windowStart))
                windowStart = earliestPhenomenonTime;
        }

        /** Snapshot and reset; returns null when the window held no observations. */
        synchronized Snapshot snapAndReset()
        {
            if (count == 0)
                return null;
            var snap = new Snapshot(count, windowStart);
            count = 0;
            windowStart = null;
            return snap;
        }

        record Snapshot(long count, Instant windowStart) {}
    }
}