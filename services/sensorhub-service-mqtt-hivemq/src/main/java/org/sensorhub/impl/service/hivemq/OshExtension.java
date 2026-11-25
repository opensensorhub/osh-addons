/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.api.comm.mqtt.MqttException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.utils.MapWithWildcards;
import org.slf4j.Logger;
import com.google.common.base.Strings;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.client.parameter.ListenerType;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.AuthenticationSuccessfulInput;
import com.hivemq.extension.sdk.api.events.client.parameters.ClientLifecycleEventListenerProviderInput;
import com.hivemq.extension.sdk.api.events.client.parameters.ConnectionStartInput;
import com.hivemq.extension.sdk.api.events.client.parameters.DisconnectEventInput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extension.sdk.api.services.publish.Publish;


public class OshExtension implements ExtensionMain, EmbeddedExtension, IMqttServer
{
    static final String LOG_SUBSCRIBE_MSG = "Received SUBSCRIBE clientId={}, topic={}: ";
    static final String LOG_UNSUBSCRIBE_MSG = "Received UNSUBSCRIBE clientId={}, topic={}: ";
    static final String CONN_PROP = "OSH_CONN";
    
    MqttServer service;
    MapWithWildcards<IMqttHandler> handlers = new MapWithWildcards<>();
    Map<String, Set<String>> clientTopics = new ConcurrentHashMap<>();
    Map<String, String> clientUsers = new ConcurrentHashMap<>();
    // client ID -> ID of the connection that owns the subscriptions of the client.
    // Only this connection can remove them when it closes.
    Map<String, String> clientConns = new ConcurrentHashMap<>();
    // locks for the changes of owner and the work that depends on the owner
    final Object[] clientLocks = new Object[64];
    volatile WebSocketProxyServlet webSocketProxy;
    volatile TokenExpiryMonitor tokenExpiryMonitor;
    volatile OshAuthenticator authenticator;
    Logger log;
    
    
    public OshExtension(ISensorHub parentHub, MqttServer service)
    {
        this.service = service;
        this.log = service.getLogger();
        for (int i = 0; i < clientLocks.length; i++)
            clientLocks[i] = new Object();
    }
    
    
    // true when the login check and the topic checks are in place. HiveMQ
    // accepts all clients if the extension did not start.
    volatile boolean started;


    @Override
    public void extensionStart(ExtensionStartInput extensionStartInput, ExtensionStartOutput extensionStartOutput)
    {
        // set authenticator to validate credentials on CONNECT
        // token logins are disconnected when the token expires
        tokenExpiryMonitor = new TokenExpiryMonitor(
            (task, delayMs) -> Services.extensionExecutorService().schedule(task, delayMs, TimeUnit.MILLISECONDS),
            this::disconnectExpiredClient, Clock.systemUTC(), log);
        var oshAuth = new OshAuthenticator(this);
        oshAuth.expiryMonitor = tokenExpiryMonitor;
        authenticator = oshAuth;
        Services.securityRegistry().setAuthenticatorProvider(authProviderInput -> {
            return oshAuth;
        });
            
        // set authorizers to authorize and handle incoming SUBSCRIBE and PUBLISH
        var oshAuthz = new OshAuthorizers(this);
        Services.securityRegistry().setAuthorizerProvider(authorizerProviderInput -> {
            return oshAuthz;
        });
        
        // set client initializer to handle UNSUBSCRIBE and prevent direct PUBLISH
        var unsubHandler = new OshUnsubscribeHandler(this);
        var publishHandler = new OshPublishHandler(this);
        var deliveryFilter = new OshDeliveryFilter(this);
        Services.initializerRegistry().setClientInitializer(new ClientInitializer() {
            @Override
            public void initialize(InitializerInput initializerInput, ClientContext clientContext)
            {
                // add interceptors to the context of the connecting client
                clientContext.addUnsubscribeInboundInterceptor(unsubHandler);
                clientContext.addPublishInboundInterceptor(publishHandler);
                clientContext.addPublishOutboundInterceptor(deliveryFilter);
            }
        });

        // set client listener to clean subscriptions on DISCONNECT
        // according to HiveMQ docs, this should handle both clean and dirty disconnect cases
        Services.eventRegistry().setClientLifecycleEventListener(new ClientLifecycleEventListenerProvider() {
            @Override
            public ClientLifecycleEventListener getClientLifecycleEventListener(ClientLifecycleEventListenerProviderInput clientLifecycleEventListenerProviderInput)
            {
                return new ClientLifecycleEventListener() {
                    @Override
                    public void onMqttConnectionStart(ConnectionStartInput connectionStartInput)
                    {
                        var clientId = connectionStartInput.getClientInformation().getClientId();
                        log.debug("Client {} connected", clientId);
                    }

                    @Override
                    public void onAuthenticationSuccessful(AuthenticationSuccessfulInput authenticationSuccessfulInput)
                    {
                        // this connection now owns the subscriptions of the client ID
                        var connId = connectionStarted(authenticationSuccessfulInput.getClientInformation().getClientId());
                        authenticationSuccessfulInput.getConnectionInformation()
                            .getConnectionAttributeStore().putAsString(CONN_PROP, connId);
                    }

                    @Override
                    public void onDisconnect(DisconnectEventInput disconnectEventInput)
                    {
                        var clientId = disconnectEventInput.getClientInformation().getClientId();
                        log.debug("Client {} disconnected", clientId);
                        
                        // cancel the token expiry timer of this connection only, and
                        // remove its token user if this was the last connection of the user
                        var connId = disconnectEventInput.getConnectionInformation()
                            .getConnectionAttributeStore().getAsString(OshAuthenticator.TOKEN_CONN_PROP).orElse(null);
                        tokenExpiryMonitor.onDisconnect(connId);
                        oshAuth.releaseTokenUser(connId);
                        
                        connectionClosed(clientId, disconnectEventInput.getConnectionInformation()
                            .getConnectionAttributeStore().getAsString(CONN_PROP).orElse(null));
                    }
                };
            }
        });

        // start the subscriptions of a persistent session again when its client connects again.
        // The broker keeps them, but the handlers stopped when the earlier connection closed
        Services.interceptorRegistry().setConnackOutboundInterceptorProvider(providerInput -> (connackIn, connackOut) -> {
            var connack = connackIn.getConnackPacket();
            if (connack.getReasonCode() != ConnackReasonCode.SUCCESS)
                return;

            var clientId = connackIn.getClientInformation().getClientId();
            var attrs = connackIn.getConnectionInformation().getConnectionAttributeStore();
            var userId = attrs.getAsString(OshAuthenticator.MQTT_USER_PROP).orElse(ISecurityManager.ANONYMOUS_USER);
            var connId = attrs.getAsString(CONN_PROP).orElse(null);

            // subscriptions of an earlier connection that did not close yet (takeover)
            sessionStarted(clientId);

            if (connack.getSessionPresent() && connId != null)
            {
                Services.subscriptionStore().getSubscriptions(clientId).whenCompleteAsync((subs, e) -> {
                    if (e != null)
                    {
                        log.error("Cannot read the session subscriptions of client {}", clientId, e);
                        return;
                    }

                    var topics = new ArrayList<String>();
                    for (var sub: subs)
                        topics.add(sub.getTopicFilter());
                    restoreSession(userId, clientId, connId, topics);
                }, Services.extensionExecutorService());
            }
        });
        
        // deploy websocket proxy if configured
        var config = service.getConfiguration();
        if (config.enableWebSocketProxy && !Strings.isNullOrEmpty(config.webSocketProxyEndpoint))
        {
            for (var l: extensionStartInput.getServerInformation().getListener())
            {
                if (l.getListenerType() == ListenerType.TCP_LISTENER)
                {
                    deployWebSocketProxy(config.webSocketProxyEndpoint, l.getPort());
                    break;
                }
            }
        }

        started = true;
    }
    
    
    /**
     * A client logged in: its connection now owns the subscriptions of the
     * client ID
     * @return the ID of the connection
     */
    String connectionStarted(String clientId)
    {
        var connId = UUID.randomUUID().toString();
        synchronized (clientLock(clientId))
        {
            clientConns.put(clientId, connId);
        }
        return connId;
    }


    /**
     * The server accepted a connection: remove the subscriptions that an
     * earlier connection with the same client ID left (takeover). The client
     * has not sent a SUBSCRIBE on the new connection yet.
     */
    void sessionStarted(String clientId)
    {
        synchronized (clientLock(clientId))
        {
            releaseClient(clientId);
        }
    }


    Object clientLock(String clientId)
    {
        return clientLocks[Math.floorMod(clientId.hashCode(), clientLocks.length)];
    }


    /**
     * A connection closed: stop the subscriptions of its client.
     * Nothing is done if the connection does not own them. This is a
     * connection that did not log in (for example a wrong password with the
     * client ID of a connected client), or a connection that a new connection
     * with the same client ID replaced.
     * @param connId ID of the connection, or null if it did not log in
     */
    void connectionClosed(String clientId, String connId)
    {
        if (connId == null)
            return;

        synchronized (clientLock(clientId))
        {
            if (clientConns.remove(clientId, connId))
                releaseClient(clientId);
        }
    }


    /** Stop the handler subscriptions of a client and forget its topics */
    void releaseClient(String clientId)
    {
        clientUsers.remove(clientId);
        var topicList = clientTopics.remove(clientId);
        if (topicList != null)
        {
            for (var topic: topicList)
            {
                var handler = handlers.get(topic);
                if (handler != null)
                {
                    log.debug("Unsubscribing {} from {}", clientId, topic);
                    try {
                        handler.onUnsubscribe("anon", topic, OshExtension.this);
                    }
                    catch (MqttException e)
                    {
                    }
                }
            }
        }
    }


    /**
     * Start the subscriptions of a persistent session again. The broker
     * keeps them while the client is not connected, but the handlers stop
     * when the connection closes.
     * Each topic is checked as a new SUBSCRIBE of the user of the new
     * connection. A subscription that is refused now is removed from the
     * session in the broker.
     * Nothing is done if the connection does not own the client ID now: it
     * closed, or a new connection replaced it. That connection does its own
     * restore, with the check of its own user.
     * @param connId ID of the connection that asks for the restore
     * @param topics topic filters of the session in the broker
     */
    void restoreSession(String userId, String clientId, String connId, Collection<String> topics)
    {
        synchronized (clientLock(clientId))
        {
            if (!connId.equals(clientConns.get(clientId)))
            {
                log.debug("Session of client {} not restored: its connection closed", clientId);
                return;
            }

            doRestoreSession(userId, clientId, topics);
        }
    }


    private void doRestoreSession(String userId, String clientId, Collection<String> topics)
    {
        int restored = 0, removed = 0;

        for (var topic: topics)
        {
            try
            {
                if (subscribeToTopic(userId, clientId, topic))
                {
                    restored++;
                    log.debug("Restored session subscription of {} to {}", clientId, topic);
                }
                else
                {
                    // no handler now: keep the topic, registerHandler() starts it
                    // when the handler comes back (see restoreSubscriptions)
                    clientTopics.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(topic);
                    clientUsers.put(clientId, userId);
                    log.debug("Session subscription of {} to {} waits for its handler", clientId, topic);
                }
            }
            catch (Exception e)
            {
                // for example, the resource is gone or this user does not have the permission
                log.debug("Cannot restore session subscription of {} to {}: {}", clientId, topic, e.getMessage());
                removeBrokerSubscription(clientId, topic);
                removed++;
            }
        }

        if (restored + removed > 0)
            log.info("Restored {} session subscription(s) of client {}, removed {}", restored, clientId, removed);
    }


    void disconnectExpiredClient(String clientId)
    {
        Services.clientService()
            .disconnectClient(clientId, false, DisconnectReasonCode.NOT_AUTHORIZED, "The access token expired")
            .whenComplete((found, e) -> {
                if (e != null)
                    log.error("Cannot disconnect client {} with an expired token", clientId, e);
                else if (!Boolean.TRUE.equals(found))
                    log.debug("Client {} with an expired token is not connected", clientId);
            });
    }
    
    
    /**
     * Remove the users that token logins added to the OSH user registry.
     * They must not stay when the server stops.
     */
    void removeTokenUsers()
    {
        var oshAuth = authenticator;
        if (oshAuth == null)
            return;
        
        try
        {
            oshAuth.removeTokenUsers();
        }
        catch (RuntimeException e)
        {
            log.error("Cannot remove the token users from the OSH users", e);
        }
    }
    
    
    void deployWebSocketProxy(String endPoint, int mqttPort)
    {
        service.getParentHub().getModuleRegistry().waitForModuleType(IHttpServer.class, ModuleState.STARTED)
            .thenAccept(http -> {
                if (http != null)
                {
                    try
                    {
                        var mqttAddress = new InetSocketAddress(InetAddress.getLocalHost(), mqttPort);
                        webSocketProxy = new WebSocketProxyServlet(mqttAddress, log);
                        http.deployServlet(webSocketProxy, endPoint);
                    }
                    catch (Exception e)
                    {
                        service.reportError("Error creating websocket proxy", e);
                    }
                }
            })
            .orTimeout(10, TimeUnit.SECONDS)
            .exceptionally(e -> {
                service.reportError("No HTTP server available to deploy websocket proxy", e);
                return null;
            });
    }
    
    
    boolean subscribeToTopic(String userId, String clientId, String topic) throws MqttException
    {
        // if a handler is found, use it to authorize/subscribe to this topic
        var handler = handlers.get(topic);
        if (handler != null)
        {
            // only handlers that check each message can take wildcard filters
            if (isFilter(topic) && !handler.supportsWildcards())
                throw new InvalidTopicException("Wildcard topic filters are not supported on this topic");

            var topics = clientTopics.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet());
            if (!topics.contains(topic))
            {
                log.debug(LOG_SUBSCRIBE_MSG + "Handled by {}",
                    clientId, topic, handler.getClass().getSimpleName());

                // record the topic only after the handler accepts it,
                // so a retry of a rejected subscribe is checked again
                handler.onSubscribe(userId, topic, this);

                // a parallel subscribe to the same topic was recorded first
                if (!topics.add(topic))
                    handler.onUnsubscribe(userId, topic, this);

                // keep the user to subscribe again if the handler restarts
                clientUsers.put(clientId, userId);
            }
            
            return true;
        }
        
        return false;
    }
    
    
    boolean unsubscribeFromTopic(String userId, String clientId, String topic) throws MqttException
    {
        var handler = handlers.get(topic);
        if (handler != null)
        {
            var topicList = clientTopics.get(clientId);
            if (topicList != null && topicList.remove(topic))
            {
                log.debug(LOG_UNSUBSCRIBE_MSG + "Handled by {}",
                    clientId, topic, handler.getClass().getSimpleName());
                
                // unsubscribe using OSH handler
                handler.onUnsubscribe(userId, topic, this);
                
                return true;
            }
        }
        
        return false;
    }


    static boolean isFilter(String topic)
    {
        return topic.indexOf('+') >= 0 || topic.indexOf('#') >= 0;
    }
    
    
    /**
     * @return true if the client subscribed to this exact topic. Its handler
     * checked the subscription, so its messages can be sent without a new check.
     */
    boolean isExactSubscription(String clientId, String topic)
    {
        var topics = clientTopics.get(clientId);
        return topics != null && topics.contains(topic);
    }


    /**
     * Check a message that a client gets only through a wildcard topic filter
     * @return true if the handler of the topic allows it, false otherwise
     */
    boolean canDeliverToWildcard(String userId, String clientId, String topic)
    {
        var handler = handlers.get(topic);
        if (handler == null)
            return false;

        try
        {
            return handler.canDeliver(userId, topic);
        }
        catch (RuntimeException e)
        {
            log.error("Cannot check delivery of {} to client {}", topic, clientId, e);
            return false;
        }
    }


    @Override
    public void extensionStop(ExtensionStopInput extensionStopInput, ExtensionStopOutput extensionStopOutput)
    {
        if (tokenExpiryMonitor != null)
            tokenExpiryMonitor.stop();
        removeTokenUsers();
        
        // stop websocket proxy if enabled
        if (webSocketProxy != null)
        {
            service.getParentHub().getModuleRegistry().waitForModuleType(IHttpServer.class, ModuleState.STARTED)
                .thenAccept(http -> {
                    if (http != null)
                    {
                        log.debug("Websocket proxy servlet undeployed");
                        http.undeployServlet(webSocketProxy);
                        webSocketProxy = null;
                    }
                });
        }
    }


    @Override
    public void registerHandler(String topicPrefix, IMqttHandler handler)
    {
        handlers.put(topicPrefix + "*", handler);
        restoreSubscriptions(handler);
    }


    /**
     * Give a new handler the subscriptions that clients made before it was
     * registered. HiveMQ keeps them when a handler module restarts, but the
     * handler only learns about a topic through onSubscribe().
     */
    void restoreSubscriptions(IMqttHandler handler)
    {
        int restored = 0, dropped = 0;

        for (var entry: clientTopics.entrySet())
        {
            var clientId = entry.getKey();
            var topics = entry.getValue();
            var userId = clientUsers.getOrDefault(clientId, ISecurityManager.ANONYMOUS_USER);

            for (var topic: topics)
            {
                if (handlers.get(topic) != handler)
                    continue;

                try
                {
                    // same check as a new SUBSCRIBE
                    if (isFilter(topic) && !handler.supportsWildcards())
                        throw new InvalidTopicException("Wildcard topic filters are not supported on this topic");

                    handler.onSubscribe(userId, topic, this);
                    restored++;
                    log.debug("Restored subscription of {} to {}", clientId, topic);
                }
                catch (Exception e)
                {
                    // for example, the resource is gone or the user lost the permission
                    log.debug("Cannot restore subscription of {} to {}: {}", clientId, topic, e.getMessage());
                    topics.remove(topic);
                    removeBrokerSubscription(clientId, topic);
                    dropped++;
                }
            }
        }

        if (restored + dropped > 0)
            log.info("Restored {} client subscription(s) for {}, removed {}", restored, handler.getClass().getSimpleName(), dropped);
    }


    /** Remove a subscription from the client in HiveMQ */
    void removeBrokerSubscription(String clientId, String topic)
    {
        Services.subscriptionStore().removeSubscription(clientId, topic)
            .exceptionally(e -> {
                log.warn("Cannot remove subscription of {} to {}", clientId, topic, e);
                return null;
            });
    }


    @Override
    public void unregisterHandler(String topicPrefix, IMqttHandler handler)
    {
        handlers.remove(topicPrefix + "*");
    }


    @Override
    public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload)
    {
        return publish(topic, payload, null);
    }


    @Override
    public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload, ByteBuffer correlData)
    {
        Publish message = Builders.publish()
            .topic(topic)
            .qos(Qos.AT_LEAST_ONCE)
            .payload(payload)
            .correlationData(correlData)
            .retain(false)
            .build();
        Services.publishService().publish(message);
        return CompletableFuture.completedFuture(true);
    }


    @Override
    public @NotNull String getId()
    {
        return OshExtension.class.getCanonicalName();
    }


    @Override
    public @NotNull String getName()
    {
        return "OSH Extension";
    }


    @Override
    public @NotNull String getVersion()
    {
        return "1.0";
    }


    @Override
    public @Nullable String getAuthor()
    {
        return null;
    }


    @Override
    public int getPriority()
    {
        return 1000;
    }


    @Override
    public int getStartPriority()
    {
        return 1000;
    }


    @Override
    public @NotNull ExtensionMain getExtensionMain()
    {
        return this;
    }
}
