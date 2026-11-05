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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;
import org.sensorhub.api.command.CommandStreamAddedEvent;
import org.sensorhub.api.command.CommandStreamChangedEvent;
import org.sensorhub.api.command.CommandStreamEvent;
import org.sensorhub.api.command.CommandStreamRemovedEvent;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.DataStreamAddedEvent;
import org.sensorhub.api.data.DataStreamChangedEvent;
import org.sensorhub.api.data.DataStreamEvent;
import org.sensorhub.api.data.DataStreamRemovedEvent;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventBus;
import org.sensorhub.api.system.SystemAddedEvent;
import org.sensorhub.api.system.SystemChangedEvent;
import org.sensorhub.api.system.SystemEvent;
import org.sensorhub.api.system.SystemRemovedEvent;
import org.slf4j.Logger;
import com.google.gson.stream.JsonWriter;


/**
 * <p>
 * Publishes CS API Part 3 Resource Events when a system, subsystem,
 * datastream or control stream is created, updated or deleted. Each event
 * is a CloudEvents 1.0 JSON message, published on the designated topic of
 * the resource, with the full {@code {systemPath}}.
 * </p><p>
 * The topic uses the nested path of the resource, but {@code subject} and
 * {@code resourceparent} use its canonical URL, for example
 * {@code {apiRoot}/datastreams/12} on topic
 * {@code {nodeId}/systems/134/subsystems/7/datastreams/12}.
 * </p>
 */
class ResourceEventPublisher implements Subscriber<SystemEvent>
{
    static final String TYPE_PREFIX = "org.ogc.api.csapi.";

    /** Looks up systems in the database that the CS API reads */
    interface SystemLookup
    {
        /** @return the internal ID of the parent system, or null for a top-level system */
        BigId parentOf(BigId sysId);

        /** @return the internal ID of the system with this UID, or null if it does not exist */
        BigId idOf(String sysUid);
    }

    static SystemLookup lookup(ISystemDescStore store)
    {
        return new SystemLookup() {
            @Override
            public BigId parentOf(BigId sysId)
            {
                return store.getParent(sysId);
            }

            @Override
            public BigId idOf(String sysUid)
            {
                var key = store.getCurrentVersionKey(sysUid);
                return key != null ? key.getInternalID() : null;
            }
        };
    }

    /** A Resource Event, before it is written as a CloudEvent */
    record ResourceEvent(String topic, String type, String subject, String parent, long time) {}

    final IMqttServer server;
    final String prefix;
    final String apiRoot;
    final SystemLookup systems;
    final IdEncoders ids;
    final Logger log;
    final AtomicReference<Subscription> subscription = new AtomicReference<>();
    volatile boolean stopped;


    /**
     * @param prefix the topic prefix, {@code {nodeId}/}
     * @param apiRoot the absolute API root URL, used as CloudEvents {@code source}
     */
    ResourceEventPublisher(IMqttServer server, String prefix, String apiRoot, SystemLookup systems, IdEncoders ids, Logger log)
    {
        this.server = server;
        this.prefix = prefix;
        this.apiRoot = apiRoot.endsWith("/") ? apiRoot.substring(0, apiRoot.length() - 1) : apiRoot;
        this.systems = systems;
        this.ids = ids;
        this.log = log;
    }


    /** Subscribe to the system registry, where all system, datastream and control stream events go */
    void start(IEventBus eventBus)
    {
        eventBus.newSubscription(SystemEvent.class)
            .withTopicID(EventUtils.getSystemRegistryTopicID())
            .withEventType(SystemEvent.class)
            .subscribe(this);
    }


    void stop()
    {
        stopped = true;
        var sub = subscription.getAndSet(null);
        if (sub != null)
            sub.cancel();
    }


    @Override
    public void onSubscribe(Subscription sub)
    {
        // stop() can run before the event bus calls back
        if (stopped || !subscription.compareAndSet(null, sub))
        {
            sub.cancel();
            return;
        }
        sub.request(Long.MAX_VALUE);
    }


    @Override
    public void onNext(SystemEvent e)
    {
        try
        {
            var event = toResourceEvent(e);
            if (event != null)
            {
                server.publish(event.topic(), ByteBuffer.wrap(toJson(event).getBytes(StandardCharsets.UTF_8)));
                log.debug("Published {} on {}", event.type(), event.topic());
            }
        }
        catch (RuntimeException ex)
        {
            log.error("Cannot publish Resource Event for {}", e.getClass().getSimpleName(), ex);
        }
    }


    @Override
    public void onError(Throwable e)
    {
        log.error("Resource Event subscription failed", e);
    }


    @Override
    public void onComplete()
    {
    }


    /**
     * @return the Resource Event for an OSH event, or null if there is none.
     *         Enable and disable events have no Resource Event.
     */
    ResourceEvent toResourceEvent(SystemEvent e)
    {
        var op = operation(e);
        if (op == null || e.getSystemID() == null)
            return null;

        var sysId = e.getSystemID();
        if (e instanceof DataStreamEvent ds)
        {
            var id = ids.getDataStreamIdEncoder().encodeID(ds.getDataStreamID());
            return new ResourceEvent(prefix + systemPath(sysId) + "/datastreams/" + id, "datastream." + op,
                apiRoot + "/datastreams/" + id, systemUrl(sysId), e.getTimeStamp());
        }
        if (e instanceof CommandStreamEvent cs)
        {
            var id = ids.getCommandStreamIdEncoder().encodeID(cs.getCommandStreamID());
            return new ResourceEvent(prefix + systemPath(sysId) + "/controlstreams/" + id, "controlstream." + op,
                apiRoot + "/controlstreams/" + id, systemUrl(sysId), e.getTimeStamp());
        }

        // A deleted system is no longer in the database, so take its parent from
        // the event. A subsystem goes on the subsystems topic of its parent.
        BigId parentId;
        if (e instanceof SystemAddedEvent added)
        {
            parentId = idOf(added.getParentGroupUID());

            // osh-core gives a new subsystem the ID of its parent, so look it up
            if (parentId != null)
                sysId = idOf(e.getSystemUID());
        }
        else if (e instanceof SystemRemovedEvent removed)
            parentId = idOf(removed.getParentGroupUID());
        else
            parentId = systems.parentOf(sysId);

        var id = ids.getSystemIdEncoder().encodeID(sysId);
        var path = parentId == null ? "systems/" + id : systemPath(parentId) + "/subsystems/" + id;
        return new ResourceEvent(prefix + path, "system." + op,
            apiRoot + "/systems/" + id, parentId == null ? null : systemUrl(parentId), e.getTimeStamp());
    }


    static String operation(SystemEvent e)
    {
        if (e instanceof SystemAddedEvent || e instanceof DataStreamAddedEvent || e instanceof CommandStreamAddedEvent)
            return "create";
        if (e instanceof SystemChangedEvent || e instanceof DataStreamChangedEvent || e instanceof CommandStreamChangedEvent)
            return "update";
        if (e instanceof SystemRemovedEvent || e instanceof DataStreamRemovedEvent || e instanceof CommandStreamRemovedEvent)
            return "delete";
        return null;
    }


    /** @return the internal ID of a system, or null if {@code uid} is null */
    BigId idOf(String uid)
    {
        if (uid == null)
            return null;
        var id = systems.idOf(uid);
        if (id == null)
            throw new IllegalStateException("Unknown system " + uid);
        return id;
    }


    /** @return the full path of a system: {@code systems/{id}}, then {@code /subsystems/{id}} for each level */
    String systemPath(BigId sysId)
    {
        var encoder = ids.getSystemIdEncoder();
        var path = encoder.encodeID(sysId);
        for (var id = systems.parentOf(sysId); id != null; id = systems.parentOf(id))
            path = encoder.encodeID(id) + "/subsystems/" + path;
        return "systems/" + path;
    }


    String systemUrl(BigId sysId)
    {
        return apiRoot + "/systems/" + ids.getSystemIdEncoder().encodeID(sysId);
    }


    String toJson(ResourceEvent event)
    {
        var json = new StringWriter();
        try (var w = new JsonWriter(json))
        {
            w.beginObject()
                .name("specversion").value("1.0")
                .name("id").value(UUID.randomUUID().toString())
                .name("type").value(TYPE_PREFIX + event.type())
                .name("source").value(apiRoot)
                .name("subject").value(event.subject())
                .name("time").value(Instant.ofEpochMilli(event.time()).toString());
            if (event.parent() != null)
                w.name("resourceparent").value(event.parent());
            w.endObject();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
        return json.toString();
    }
}
