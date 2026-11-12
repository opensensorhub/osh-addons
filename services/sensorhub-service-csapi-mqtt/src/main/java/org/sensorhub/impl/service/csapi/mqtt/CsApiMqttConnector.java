/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.csapi.mqtt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.InvalidPayloadException;
import org.sensorhub.api.comm.mqtt.IMqttServer.IMqttHandler;
import org.sensorhub.api.comm.mqtt.ImplSpecificException;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.api.comm.mqtt.MqttException;
import org.sensorhub.api.comm.mqtt.MqttOutputStream;
import org.sensorhub.impl.service.consys.InvalidRequestException;
import org.sensorhub.impl.service.consys.InvalidRequestException.ErrorCode;
import org.sensorhub.impl.security.ItemWithIdPermission;
import org.sensorhub.impl.security.ItemWithParentPermission;
import org.sensorhub.impl.service.consys.ConSysApiSecurity;
import org.sensorhub.impl.service.consys.ConSysApiServlet;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;
import org.sensorhub.impl.service.csapi.mqtt.TopicMapper.ResourceTopic;
import org.sensorhub.impl.service.consys.stream.StreamHandler;
import org.vast.util.Asserts;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;


/**
 * <p>
 * This class handles communication with the embedded MQTT server and transfers
 * messages to/from the Connected Systems API servlet for processing. 
 * </p>
 *
 * @author Alex Robin
 * @since May 9, 2023
 */
public class CsApiMqttConnector implements IMqttHandler
{
    ConSysApiServlet servlet;
    TopicMapper topics;
    Map<String, MqttSubscriber> subscribers = new ConcurrentHashMap<>();
    
    
    class MqttSubscriber implements StreamHandler
    {
        IMqttServer server;
        String topic;
        MqttOutputStream os;
        Runnable onStart, onClose;
        AtomicInteger numSubscribers = new AtomicInteger(0);
        AtomicBoolean started = new AtomicBoolean();
        
        MqttSubscriber(IMqttServer server, String topic)
        {
            this.server = server;
            this.topic = topic;
            this.os = new MqttOutputStream(server, topic, 1024, false);
        }

        @Override
        public void sendPacket() throws IOException
        {
            os.send();
        }
        
        @Override
        public void sendPacket(long correlId) throws IOException
        {
            os.send(correlId);
        }

        @Override
        public OutputStream getOutputStream()
        {
            return os;
        }

        @Override
        public void setStartCallback(Runnable onStart)
        {
            this.onStart = Asserts.checkNotNull(onStart, "onStart");
        }

        @Override
        public void setCloseCallback(Runnable onClose)
        {
            this.onClose = Asserts.checkNotNull(onClose, "onClose");
        }
        
        public void maybeStart()
        {
            if (onStart != null && started.compareAndSet(false, true))
                onStart.run();
        }
        
        @Override
        public void close()
        {
            if (onClose != null)
                onClose.run();
        }
    }
    
    
    /**
     * Discards the response of a GET by ID. A Resource Event subscription
     * only checks that the GET succeeds. A GET by ID is never a stream
     * request, so the CS API only writes the resource to this output.
     */
    static final StreamHandler DISCARD = new StreamHandler() {
        @Override public void setStartCallback(Runnable onStart) {}
        @Override public void setCloseCallback(Runnable onClose) {}
        @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        @Override public void sendPacket() {}
        @Override public void close() {}
    };
    
    
    public CsApiMqttConnector(ConSysApiServlet servlet, TopicMapper topics)
    {
        this(servlet, topics, Ticker.systemTicker());
    }


    /** @param ticker the clock of the access cache */
    CsApiMqttConnector(ConSysApiServlet servlet, TopicMapper topics, Ticker ticker)
    {
        this.servlet = servlet;
        this.topics = topics;
        this.accessCache = CacheBuilder.newBuilder()
            .expireAfterWrite(ACCESS_CACHE_MS, TimeUnit.MILLISECONDS)
            .maximumSize(ACCESS_CACHE_MAX)
            .ticker(ticker)
            .build();
    }


    @Override
    public void onSubscribe(String userID, String topic, IMqttServer server) throws InvalidTopicException
    {
        // A wildcard filter can match the topics of many resources, also of
        // resources created later. Check only that the user can read the
        // resource it names. Each message is checked by canDeliver().
        if (TopicMapper.isFilter(topic))
        {
            var uri = topics.parseFilter(topic).uri();
            if (uri.getPath().equals(TopicMapper.SYSTEMS_PATH))
                checkListSystems(userID);
            else
                checkSubscribe(userID, topic, uri);
            return;
        }

        // reject invalid topics before a subscriber is created
        var resource = topics.parse(topic);

        // ResourceEventPublisher publishes the events of all resources, so
        // only check that the resource exists and that the user can read it
        if (resource.event())
        {
            checkSubscribe(userID, topic, resource.uri());
            return;
        }

        var accepted = false;

        try
        {
            // register new subscription if needed
            var sub = subscribers.compute(topic, (k, v) -> {
                // create subscriber if needed
                if (v == null)
                    v = new MqttSubscriber(server, topic);
                return v;
            });
            
            // always evaluate request because we need to check for permissions
            // even if subscriber was already created
            var ctx = new RequestContext(servlet, resource.uri(), sub);
            servlet.getSecurityHandler().setCurrentUser(userID);
            servlet.getRootHandler().doGet(ctx);
            
            // start stream if all went well
            sub.numSubscribers.incrementAndGet();
            accepted = true;
            sub.maybeStart();
        }
        catch (SecurityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throwSubscribeError(topic, e);
        }
        finally
        {
            servlet.getSecurityHandler().clearCurrentUser();

            // do not keep a subscriber that no client uses
            if (!accepted)
            {
                subscribers.computeIfPresent(topic, (k, sub) -> {
                    if (sub.numSubscribers.get() > 0)
                        return sub;
                    sub.close();
                    return null;
                });
            }
        }
    }
    
    
    /** Check that the user can read a resource, with a GET as the user */
    void checkSubscribe(String userID, String topic, URI uri) throws InvalidTopicException
    {
        try
        {
            servlet.getSecurityHandler().setCurrentUser(userID);
            servlet.getRootHandler().doGet(new RequestContext(servlet, uri, DISCARD));
        }
        catch (SecurityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throwSubscribeError(topic, e);
        }
        finally
        {
            servlet.getSecurityHandler().clearCurrentUser();
        }
    }


    /**
     * Check that the user can list the systems collection. This is the check
     * of a collection GET. A collection GET is not used here: through a
     * stream handler, the CS API reads it as a streaming request.
     */
    void checkListSystems(String userID)
    {
        if (!(servlet.getSecurityHandler() instanceof ConSysApiSecurity security))
            throw new AccessControlException("Cannot check access to the systems collection");

        try
        {
            security.setCurrentUser(userID);
            if (!security.hasPermission(security.system_permissions.list))
                throw new AccessControlException("User " + userID + " cannot list systems");
        }
        finally
        {
            security.clearCurrentUser();
        }
    }


    @Override
    public boolean supportsWildcards()
    {
        return true;
    }


    /**
     * Check a message that a client gets through a wildcard filter. Only
     * Resource Events and Batch Resource Events are sent, and only if the
     * user has the permission to read the resource, directly or from a
     * system above it. Decisions are kept for {@link #ACCESS_CACHE_MS}.
     */
    @Override
    public boolean canDeliver(String userID, String topic)
    {
        ResourceTopic resource;
        try
        {
            resource = topics.parse(topic);
        }
        catch (InvalidTopicException e)
        {
            return false;
        }

        // Resource Data is sent only to exact subscriptions
        if (!resource.event())
            return false;

        return checkRead(userID, resource.uri());
    }


    /** How long a read check of a wildcard delivery is kept */
    static final long ACCESS_CACHE_MS = 10_000;
    static final int ACCESS_CACHE_MAX = 10_000;
    final Cache<String, Boolean> accessCache;


    /** @return if the user can read the resource, from the cache if possible */
    boolean checkRead(String userID, URI uri)
    {
        try
        {
            return accessCache.get(userID + '\0' + uri, () -> doCheckRead(userID, uri));
        }
        catch (ExecutionException | UncheckedExecutionException e)
        {
            return false;
        }
    }


    /**
     * Check the read permission of a resource, as the CS API does for a GET,
     * but without a read of the resource. Thus the check also works for a
     * resource that was deleted.
     */
    boolean doCheckRead(String userID, URI uri)
    {
        if (!(servlet.getSecurityHandler() instanceof ConSysApiSecurity security))
            return false;

        var res = ResourcePath.of(uri);
        if (res == null)
            return false;
        var perms = switch (res.type()) {
            case "systems", "subsystems" -> security.system_permissions;
            case "datastreams" -> security.datastream_permissions;
            case "controlstreams" -> security.commandstream_permissions;
            default -> null;
        };
        if (perms == null)
            return false;

        try
        {
            security.setCurrentUser(userID);
            return canRead(res,
                id -> security.hasPermission(new ItemWithIdPermission(perms.get, id)),
                parentId -> security.hasPermission(new ItemWithParentPermission(perms.get, parentId)));
        }
        catch (RuntimeException e)
        {
            // for example, an unknown user
            return false;
        }
        finally
        {
            security.clearCurrentUser();
        }
    }


    /**
     * The permission can be given for the resource itself, or for a system
     * above it. osh-core finds these systems in the database. This lookup
     * fails for a deleted resource. Then the systems of the topic path are
     * used.
     * @param idAllowed checks the permission on a resource ID, with the
     * systems above it in the database
     * @param parentAllowed checks the permission on the resources of a system
     */
    static boolean canRead(ResourcePath res, Predicate<String> idAllowed, Predicate<String> parentAllowed)
    {
        try
        {
            if (idAllowed.test(res.id()))
                return true;
        }
        catch (IllegalArgumentException e)
        {
            // the resource is not in the database
        }

        for (var ancestor: res.ancestors())
        {
            try
            {
                if (parentAllowed.test(ancestor))
                    return true;
            }
            catch (IllegalArgumentException e)
            {
                // the system is not in the database
            }
        }
        return false;
    }


    /**
     * A resource in a topic path: its type and ID, and the IDs of the
     * systems above it, nearest first
     */
    record ResourcePath(String type, String id, List<String> ancestors)
    {
        static ResourcePath of(URI uri)
        {
            var levels = uri.getPath().substring(1).split("/");
            if (levels.length < 2 || levels.length % 2 != 0)
                return null;
            var ancestors = new ArrayList<String>();
            for (int i = levels.length - 3; i > 0; i -= 2)
                ancestors.add(levels[i]);
            return new ResourcePath(levels[levels.length - 2], levels[levels.length - 1], ancestors);
        }
    }


    static void throwSubscribeError(String topic, Exception e) throws InvalidTopicException
    {
        if (e instanceof InvalidRequestException)
            throw new InvalidTopicException(e.getMessage());

        // some resources cannot be streamed yet (for example command results)
        if (e instanceof UnsupportedOperationException)
            throw new InvalidTopicException("Streaming not supported on this resource");

        // the CS API can wrap a request error, for example an unsupported format
        for (var cause = e.getCause(); cause != null; cause = cause.getCause())
        {
            if (cause instanceof InvalidRequestException)
                throw new InvalidTopicException(cause.getMessage());
        }
        throw new IllegalStateException("Internal error subscribing to topic " + topic, e);
    }
    
    
    @Override
    public void onUnsubscribe(String userID, String topic, IMqttServer server) throws InvalidTopicException
    {
        subscribers.computeIfPresent(topic, (k, sub) -> {
            var numSub = sub.numSubscribers.decrementAndGet();
            if (numSub <= 0)
            {
                servlet.getLogger().debug("No more clients listening on topic {}. Cancelling eventbus subscription.", topic);
                sub.close();
                sub = null;
            }
            else
                servlet.getLogger().debug("{} client(s) still listening on topic {}", numSub, topic);
            return sub;
        });
    }


    @Override
    public void onPublish(String userID, String topic, ByteBuffer payload, ByteBuffer correlData) throws MqttException
    {
        try
        {
            // only the server publishes Resource Events
            var resource = topics.parse(topic);
            if (resource.event())
                throw new AccessControlException("Clients cannot publish on Resource Event topics");

            // the format subtopic selects the payload format, else detect it
            var ctx = new RequestContext(servlet, resource.uri(), new ByteBufferInputStream(payload));
            ctx.setRequestContentType(resource.format() != null ? resource.format() : ResourceFormat.AUTO.getMimeType());
            
            if (correlData != null)
            {
                long cmdID = 0;
                if (correlData.remaining() == 8)
                    cmdID = correlData.getLong();
                if (correlData.remaining() == 4)
                    cmdID = correlData.getInt();
                if (cmdID == 0)
                    throw new InvalidRequestException(ErrorCode.BAD_PAYLOAD, "Invalid correlation data");
                //var idStr = StandardCharsets.UTF_8.decode(correlData).toString();
                //var cmdID = Long.parseLong(idStr);
                ctx.setCorrelationID(cmdID);
            }
            
            servlet.getSecurityHandler().setCurrentUser(userID);
            servlet.getRootHandler().doPost(ctx);
        }
        catch (SecurityException | InvalidTopicException e)
        {
            throw e;
        }
        catch (InvalidRequestException e)
        {
            handleInvalidRequestException(e);
        }
        catch (NullPointerException e)
        {
            // Resource builders reject a missing required field with an NPE,
            // for example a command without 'parameters'. Report it as a bad
            // payload, not an internal error.
            throw new InvalidPayloadException(e.getMessage());
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Internal error publishing to topic " + topic, e);
        }
        finally
        {
            servlet.getSecurityHandler().clearCurrentUser();
        }
    }
    
    
    private void handleInvalidRequestException(InvalidRequestException e) throws MqttException, SecurityException
    {
        switch (e.getErrorCode())
        {
            case NOT_FOUND:
            case BAD_REQUEST:
            case UNSUPPORTED_OPERATION: // topic is not writable
                throw new InvalidTopicException(e.getMessage());
                
            case BAD_PAYLOAD:
                throw new InvalidPayloadException(e.getMessage());

            case REQUEST_REJECTED:
                throw new ImplSpecificException(e.getMessage());
                
            case FORBIDDEN:
                throw new AccessControlException("Forbidden");
                
            default:
                throw new IllegalStateException("Internal error", e);
        }
    }
    
    
    protected void stop()
    {
        for (var sub: subscribers.values())
            sub.close();
        subscribers.clear();
    }
}
