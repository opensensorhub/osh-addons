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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.DataStreamAddedEvent;
import org.sensorhub.api.data.DataStreamRemovedEvent;
import org.sensorhub.api.data.ObsEvent;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.api.system.SystemEvent;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.slf4j.Logger;
import com.google.gson.stream.JsonWriter;


/**
 * <p>
 * Publishes CS API Part 3 Batch Resource Events for the observations of
 * each datastream. Each event gives the number of observations created in
 * one time window, on the topic of the observations collection, for example
 * {@code {nodeId}/systems/134/datastreams/12/observations}.
 * </p><p>
 * The publisher listens to the data channel of each datastream in the hub
 * database, and of each new datastream. Each datastream gets at most one
 * event in each window (see {@link BatchWindow}).
 * </p>
 */
class BatchEventPublisher
{
    static final String TYPE = ResourceEventPublisher.TYPE_PREFIX + "observation.create";

    /** Looks up datastreams in the database that the CS API reads */
    interface DataStreamLookup
    {
        /** @return the internal ID of the system of a datastream, or null if it does not exist */
        BigId systemOf(BigId dsId);
    }

    static DataStreamLookup lookup(IDataStreamStore store)
    {
        return dsId -> {
            var dsInfo = store.get(new DataStreamKey(dsId));
            return dsInfo != null ? dsInfo.getSystemID().getInternalID() : null;
        };
    }

    /** The event window of one datastream, and where its events go */
    record Stream(BigId sysId, String topic, String subject, String parent, BatchWindow window) {}

    final IMqttServer server;
    final String prefix;
    final String apiRoot;
    final Function<BigId, String> systemPath;
    final DataStreamLookup dataStreams;
    final IdEncoders ids;
    final long windowMs;
    final Logger log;

    /** Event bus subscriptions, by data channel topic ID. A null value is a pending subscription. */
    final Map<String, AtomicReference<Subscription>> channels = new ConcurrentHashMap<>();
    final Map<BigId, Stream> streams = new ConcurrentHashMap<>();
    final AtomicReference<Subscription> registry = new AtomicReference<>();
    volatile ScheduledExecutorService scheduler;
    volatile IEventBus eventBus;
    volatile boolean stopped;


    /**
     * @param prefix the topic prefix, {@code {nodeId}/}
     * @param apiRoot the absolute API root URL, used as CloudEvents {@code source}
     * @param systemPath gives the full path of a system, {@code systems/{id}/subsystems/{id}...}
     * @param windowMs the window length in milliseconds
     */
    BatchEventPublisher(IMqttServer server, String prefix, String apiRoot, Function<BigId, String> systemPath,
        DataStreamLookup dataStreams, IdEncoders ids, long windowMs, Logger log)
    {
        this.server = server;
        this.prefix = prefix;
        this.apiRoot = apiRoot.endsWith("/") ? apiRoot.substring(0, apiRoot.length() - 1) : apiRoot;
        this.systemPath = systemPath;
        this.dataStreams = dataStreams;
        this.ids = ids;
        this.windowMs = Math.max(0, windowMs);
        this.log = log;
    }


    /**
     * Listen to the data channel of each datastream in {@code store}, and of
     * each datastream created later
     */
    void start(IEventBus eventBus, IDataStreamStore store)
    {
        this.eventBus = eventBus;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "csapi-mqtt-batch-events");
            t.setDaemon(true);
            return t;
        });

        // listen for new datastreams before the database is read, so none is missed
        eventBus.newSubscription(SystemEvent.class)
            .withTopicID(EventUtils.getSystemRegistryTopicID())
            .withEventType(SystemEvent.class)
            .consume(this::onRegistryEvent)
            .thenAccept(sub -> keep(registry, sub));

        // all versions: a stopped driver can leave its datastream with no current version
        store.select(new DataStreamFilter.Builder().withAllVersions().build())
            .forEach(ds -> watch(EventUtils.getDataStreamDataTopicID(ds)));
    }


    void stop()
    {
        stopped = true;

        var sub = registry.getAndSet(null);
        if (sub != null)
            sub.cancel();

        for (var ref: channels.values())
        {
            sub = ref.getAndSet(null);
            if (sub != null)
                sub.cancel();
        }
        channels.clear();

        // pending observations are dropped
        for (var s: streams.values())
            s.window().close();
        streams.clear();

        var s = scheduler;
        if (s != null)
            s.shutdownNow();
    }


    /** Keep a subscription, or cancel it if the publisher is stopped */
    void keep(AtomicReference<Subscription> ref, Subscription sub)
    {
        ref.set(sub);

        // stop() can run before the event bus calls back
        if (stopped && ref.compareAndSet(sub, null))
            sub.cancel();
    }


    /** Listen to a datastream data channel, once */
    void watch(String topicId)
    {
        var ref = new AtomicReference<Subscription>();
        if (stopped || channels.putIfAbsent(topicId, ref) != null)
            return;

        eventBus.newSubscription(ObsEvent.class)
            .withTopicID(topicId)
            .withEventType(ObsEvent.class)
            .consume(this::onObs)
            .thenAccept(sub -> {
                keep(ref, sub);

                // the system can be removed before the event bus calls back
                if (channels.get(topicId) != ref && ref.compareAndSet(sub, null))
                    sub.cancel();
            });
    }


    void onRegistryEvent(SystemEvent e)
    {
        try
        {
            if (e instanceof DataStreamAddedEvent added)
            {
                watch(EventUtils.getDataStreamDataTopicID(added.getSystemUID(), added.getOutputName()));
            }
            else if (e instanceof DataStreamRemovedEvent removed && removed.getDataStreamID() != null)
            {
                var s = streams.remove(removed.getDataStreamID());
                if (s != null)
                    s.window().close();
            }
            else if (e instanceof SystemRemovedEvent)
            {
                // osh-core removes the datastreams of a system with it, and sends no event for them
                var group = EventUtils.getSystemPublisherGroupID(e.getSystemUID()) + "/";
                channels.entrySet().removeIf(entry -> {
                    if (!entry.getKey().startsWith(group))
                        return false;
                    var sub = entry.getValue().getAndSet(null);
                    if (sub != null)
                        sub.cancel();
                    return true;
                });

                var sysId = e.getSystemID();
                if (sysId != null)
                {
                    streams.values().removeIf(s -> {
                        if (!sysId.equals(s.sysId()))
                            return false;
                        s.window().close();
                        return true;
                    });
                }
            }
        }
        catch (RuntimeException ex)
        {
            log.error("Cannot handle {} for batch events", e.getClass().getSimpleName(), ex);
        }
    }


    void onObs(ObsEvent e)
    {
        try
        {
            var obs = e.getObservations();
            if (stopped || obs.length == 0)
                return;

            var dsId = obs[0].getDataStreamID();
            if (dsId == null)
                return;
            var stream = streams.computeIfAbsent(dsId, this::newStream);
            if (stream == null)
                return;

            // stop() can run while the stream is created
            if (stopped)
            {
                stream.window().close();
                return;
            }

            stream.window().record(obs.length, e.getTimeStamp());
        }
        catch (RuntimeException ex)
        {
            log.error("Cannot count observations for batch events", ex);
        }
    }


    /** @return the stream of a datastream, or null if the datastream does not exist */
    Stream newStream(BigId dsId)
    {
        var sysId = dataStreams.systemOf(dsId);
        if (sysId == null)
            return null;

        var id = ids.getDataStreamIdEncoder().encodeID(dsId);
        var topic = prefix + systemPath.apply(sysId) + "/datastreams/" + id + "/observations";
        var parent = apiRoot + "/datastreams/" + id;
        var subject = parent + "/observations";
        var window = new BatchWindow(windowMs, () -> System.nanoTime() / 1_000_000, this::schedule,
            (start, end, count) -> publish(topic, subject, parent, start, end, count));
        return new Stream(sysId, topic, subject, parent, window);
    }


    void schedule(Runnable task, long delayMs)
    {
        var s = scheduler;
        if (s == null)
            throw new IllegalStateException("Not started");
        s.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }


    void publish(String topic, String subject, String parent, long startMs, long endMs, long count)
    {
        try
        {
            var json = toJson(subject, parent, Instant.now(), startMs, endMs, count);
            server.publish(topic, ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8)));
            log.trace("Published batch of {} observations on {}", count, topic);
        }
        catch (RuntimeException e)
        {
            log.error("Cannot publish Batch Resource Event on {}", topic, e);
        }
    }


    String toJson(String subject, String parent, Instant time, long startMs, long endMs, long count)
    {
        var json = new StringWriter();
        try (var w = new JsonWriter(json))
        {
            w.beginObject()
                .name("specversion").value("1.0")
                .name("id").value(UUID.randomUUID().toString())
                .name("type").value(TYPE)
                .name("source").value(apiRoot)
                .name("subject").value(subject)
                .name("time").value(time.toString())
                .name("resourceparent").value(parent)
                .name("datacontenttype").value("application/json")
                .name("data").beginObject()
                    .name("timerange").beginArray()
                        .value(Instant.ofEpochMilli(startMs).toString())
                        .value(Instant.ofEpochMilli(endMs).toString())
                    .endArray()
                    .name("count").value(count)
                .endObject();
            w.endObject();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return json.toString();
    }
}
