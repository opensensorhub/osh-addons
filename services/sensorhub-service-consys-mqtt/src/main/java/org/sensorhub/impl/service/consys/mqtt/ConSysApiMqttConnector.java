/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.mqtt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.InvalidPayloadException;
import org.sensorhub.api.comm.mqtt.IMqttServer.IMqttHandler;
import org.sensorhub.api.comm.mqtt.ImplSpecificException;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.api.comm.mqtt.MqttException;
import org.sensorhub.api.comm.mqtt.MqttOutputStream;
import org.sensorhub.impl.service.consys.ConSysApiSecurity;
import org.sensorhub.impl.service.consys.InvalidRequestException;
import org.sensorhub.impl.service.consys.InvalidRequestException.ErrorCode;
import org.sensorhub.impl.service.consys.ConSysApiServlet;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;
import org.sensorhub.impl.service.consys.stream.StreamHandler;
import org.vast.util.Asserts;


/**
 * <p>
 * This class handles communication with the embedded MQTT server and transfers
 * messages to/from the Connected Systems API servlet for processing. 
 * </p>
 *
 * <p>
 * Two distinct MQTT topic categories are handled per OGC CS API Part 3:
 * </p>
 * <ul>
 *   <li><b>Resource Data Topics</b> (path ends with {@code :data}) — routed to the
 *       ConSys HTTP servlet for streaming observations, commands, and resource
 *       registration. Both subscribe and publish are allowed.</li>
 *   <li><b>Resource Event Topics</b> (no {@code :data} suffix) — lifecycle
 *       notifications published proactively by {@link ResourceEventPublisher} as
 *       CloudEvents v1.0 JSON. {@code onSubscribe()} validates permissions only;
 *       no per-subscriber stream is set up here.</li>
 * </ul>
 *
 * @author Alex Robin
 * @since May 9, 2023
 */
public class ConSysApiMqttConnector implements IMqttHandler
{
    static final String DATA_SUFFIX = ":data";

    ConSysApiServlet servlet;
    String endpoint;
    String nodeId; // may be null; when set, topics use "{nodeId}/{resourcePath}" with no endpoint prefix
    String nodeIdPrefix; // pre-computed "{nodeId}/" or null


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

    Map<String, MqttSubscriber> subscribers = new ConcurrentHashMap<>();


    public ConSysApiMqttConnector(ConSysApiServlet servlet, String endpoint, String nodeId)
    {
        this.servlet = servlet;
        this.endpoint = endpoint;
        this.nodeId = nodeId;
        this.nodeIdPrefix = (nodeId != null && !nodeId.isBlank()) ? nodeId + "/" : null;
    }


    @Override
    public void onSubscribe(String userID, String topic, IMqttServer server) throws MqttException
    {
        // Resource Event Topics (no :data suffix) are published proactively by
        // ResourceEventPublisher — no per-subscriber stream to set up here.
        // Just validate the topic and check permissions.
        if (!isDataTopic(topic))
        {
            servlet.getLogger().info("User '{}' subscribing to resource event topic: {}", userID, topic);
            try
            {
                checkResourceEventTopicPermission(userID, topic);
                servlet.getLogger().debug("Subscription accepted for resource event topic: {}", topic);
            }
            catch (MqttException e)
            {
                servlet.getLogger().warn("Subscription rejected for user '{}' on topic '{}': {}", userID, topic, e.getMessage());
                throw e;
            }
            return;
        }

        // Resource Data Topic: route through the ConSys HTTP servlet as before
        servlet.getLogger().info("User '{}' subscribing to resource data topic: {}", userID, topic);
        try
        {
            // register new subscription if needed
            var sub = subscribers.compute(topic, (k, v) -> {
                if (v == null)
                    v = new MqttSubscriber(server, topic);
                return v;
            });
            
            // always evaluate request because we need to check for permissions
            // even if subscriber was already created
            var ctx = getResourceContext(topic, sub);
            servlet.getSecurityHandler().setCurrentUser(userID);
            servlet.getRootHandler().doGet(ctx);
            
            // start stream if all went well
            sub.numSubscribers.incrementAndGet();
            sub.maybeStart();
            servlet.getLogger().debug("Subscription accepted for resource data topic: {}", topic);
        }
        catch (SecurityException | InvalidTopicException e)
        {
            servlet.getLogger().warn("Subscription rejected for user '{}' on topic '{}': {}", userID, topic, e.getMessage());
            throw e;
        }
        catch (InvalidRequestException e)
        {
            servlet.getLogger().warn("Subscription rejected for user '{}' on topic '{}': {}", userID, topic, e.getMessage());
            throw new InvalidTopicException(e.getMessage());
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Internal error subscribing to topic " + topic, e);
        }
        finally
        {
            servlet.getSecurityHandler().clearCurrentUser();
        }
    }
    
    
    @Override
    public void onUnsubscribe(String userID, String topic, IMqttServer server) throws InvalidTopicException
    {
        servlet.getLogger().info("User '{}' unsubscribing from topic: {}", userID, topic);

        // Resource Event Topics have no stream to clean up
        if (!isDataTopic(topic))
            return;

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
        // Per OGC CS API Part 3: only the server may publish to Resource Event Topics.
        // Clients SHALL be prevented from publishing on these channels.
        if (!isDataTopic(topic))
        {
            servlet.getLogger().warn("User '{}' attempted to publish to resource event topic '{}' — rejected", userID, topic);
            throw new InvalidTopicException("Publishing to resource event topics is not permitted");
        }

        try
        {
            var ctx = getResourceContext(topic, payload);
            ctx.setRequestContentType(ResourceFormat.AUTO.getMimeType());
            
            if (correlData != null)
            {
                long cmdID = 0;
                if (correlData.remaining() == 8)
                    cmdID = correlData.getLong();
                if (correlData.remaining() == 4)
                    cmdID = correlData.getInt();
                if (cmdID == 0)
                    throw new InvalidRequestException(ErrorCode.BAD_PAYLOAD, "Invalid correlation data");
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
        catch (Exception e)
        {
            throw new IllegalStateException("Internal error publishing to topic " + topic, e);
        }
        finally
        {
            servlet.getSecurityHandler().clearCurrentUser();
        }
    }


    /**
     * @return true if this topic is a Resource Data Topic (ends with {@code :data})
     */
    private boolean isDataTopic(String topic)
    {
        return topic.endsWith(DATA_SUFFIX);
    }


    /**
     * Validate permissions for a Resource Event Topic subscription without
     * creating a stream. Checks are ordered most-specific first to avoid
     * a broad "systems" check shadowing nested datastream/controlstream paths.
     */
    private void checkResourceEventTopicPermission(String userID, String topic)
            throws MqttException
    {
        try
        {
            servlet.getSecurityHandler().setCurrentUser(userID);
            var path = stripPrefixes(topic);
            var security = (ConSysApiSecurity) servlet.getSecurityHandler();

            // Check most-specific resource types first
            if (path.contains("/observations"))
                security.checkPermission(security.obs_permissions.stream);
            else if (path.contains("/datastreams"))
                security.checkPermission(security.datastream_permissions.stream);
            else if (path.contains("/controlstreams"))
                security.checkPermission(security.commandstream_permissions.stream);
            else if (path.startsWith("systems"))
                security.checkPermission(security.system_permissions.stream);
            // Unknown resource type: permit — no events will be published to invalid topics
        }
        catch (SecurityException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new InvalidTopicException("Invalid resource event topic: " + topic);
        }
        finally
        {
            servlet.getSecurityHandler().clearCurrentUser();
        }
    }


    /**
     * Strip the nodeId prefix (if set) or the endpoint prefix from a topic,
     * returning the resource path without a leading slash.
     * Used for permission checking on event topics.
     */
    private String stripPrefixes(String topic)
    {
        if (nodeIdPrefix != null && topic.startsWith(nodeIdPrefix))
        {
            // nodeId present: resource path follows directly after "{nodeId}/"
            return topic.substring(nodeIdPrefix.length());
        }

        // No nodeId: strip leading slash then endpoint prefix (e.g. "api/")
        if (topic.startsWith("/"))
            topic = topic.substring(1);
        var ep = endpoint.startsWith("/") ? endpoint.substring(1) : endpoint;
        if (topic.startsWith(ep))
            topic = topic.substring(ep.length());
        if (topic.startsWith("/"))
            topic = topic.substring(1);
        return topic;
    }


    private RequestContext getResourceContext(String topic, MqttSubscriber streamHandler) throws InvalidTopicException
    {
        return new RequestContext(
            servlet,
            getResourceUri(topic),
            streamHandler);
    }
    
    
    private RequestContext getResourceContext(String topic, ByteBuffer payload) throws InvalidTopicException
    {
        return new RequestContext(
            servlet,
            getResourceUri(topic),
            new ByteBufferInputStream(payload));
    }
    
    
    private URI getResourceUri(String topic) throws InvalidTopicException
    {
        try
        {
            // Strip nodeId prefix if set (e.g. "mynode/").
            // When a nodeId is present, topics are "{nodeId}/{resourcePath}" — no endpoint prefix.
            boolean nodeIdStripped = false;
            if (nodeIdPrefix != null && topic.startsWith(nodeIdPrefix))
            {
                topic = topic.substring(nodeIdPrefix.length());
                nodeIdStripped = true;
            }

            if (!nodeIdStripped)
            {
                // No nodeId: validate and strip the endpoint prefix (e.g. "/api")
                if (!topic.startsWith(endpoint))
                    throw new InvalidTopicException(
                        "Topic '" + topic + "' does not match endpoint prefix '" + endpoint + "'");
                topic = topic.substring(endpoint.length());
            }
            else if (!topic.startsWith("/"))
            {
                // After stripping nodeId the path has no leading slash — add it for URI parsing
                topic = "/" + topic;
            }

            // Strip :data suffix — only from the end (it is guaranteed present for data topics)
            if (topic.endsWith(DATA_SUFFIX))
                topic = topic.substring(0, topic.length() - DATA_SUFFIX.length());

            // parse URI (this also URL decodes the query string)
            return new URI(topic);
        }
        catch (InvalidTopicException e)
        {
            throw e;
        }
        catch (URISyntaxException e)
        {
            throw new InvalidTopicException("Invalid SWE API resource URI");
        }        
    }
    
    
    private void handleInvalidRequestException(InvalidRequestException e) throws MqttException, SecurityException
    {
        switch (e.getErrorCode())
        {
            case NOT_FOUND:
            case BAD_REQUEST:
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