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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.api.comm.mqtt.MqttException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
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
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extension.sdk.api.services.publish.Publish;


public class OshExtension implements ExtensionMain, EmbeddedExtension, IMqttServer
{
    static final String LOG_SUBSCRIBE_MSG = "Received SUBSCRIBE clientId={}, topic={}: ";
    static final String LOG_UNSUBSCRIBE_MSG = "Received UNSUBSCRIBE clientId={}, topic={}: ";
    
    MqttServer service;
    MapWithWildcards<IMqttHandler> handlers = new MapWithWildcards<>();
    Map<String, Set<String>> clientTopics = new ConcurrentHashMap<>();
    volatile WebSocketProxyServlet webSocketProxy;
    Logger log;
    
    
    public OshExtension(ISensorHub parentHub, MqttServer service)
    {
        this.service = service;
        this.log = service.getLogger();
    }
    
    
    @Override
    public void extensionStart(ExtensionStartInput extensionStartInput, ExtensionStartOutput extensionStartOutput)
    {
        // set authenticator to validate credentials on CONNECT
        var oshAuth = new OshAuthenticator(service.getParentHub().getSecurityManager(), this);
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
        Services.initializerRegistry().setClientInitializer(new ClientInitializer() {
            @Override
            public void initialize(InitializerInput initializerInput, ClientContext clientContext)
            {
                // add interceptors to the context of the connecting client
                clientContext.addUnsubscribeInboundInterceptor(unsubHandler);
                clientContext.addPublishInboundInterceptor(publishHandler);
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
                    }

                    @Override
                    public void onDisconnect(DisconnectEventInput disconnectEventInput)
                    {
                        var clientId = disconnectEventInput.getClientInformation().getClientId();
                        log.debug("Client {} disconnected", clientId);
                        
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
                };
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
    
    
    boolean subscribeToTopic(String userId, String clientId, String topic) throws InvalidTopicException
    {
        // if a handler is found, use it to authorize/subscribe to this topic
        var handler = handlers.get(topic);
        if (handler != null)
        {
            AtomicBoolean notSubscribed = new AtomicBoolean();
            clientTopics.compute(clientId, (k,v) -> {
                if (v == null)
                    v = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
                notSubscribed.set(v.add(topic));
                return v;
            });
            
            if (notSubscribed.get())
            {
                log.debug(LOG_SUBSCRIBE_MSG + "Handled by {}",
                    clientId, topic, handler.getClass().getSimpleName());
                handler.onSubscribe(userId, topic, this);
            }
            
            return true;
        }
        
        return false;
    }
    
    
    boolean unsubscribeFromTopic(String userId, String clientId, String topic) throws InvalidTopicException
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


    @Override
    public void extensionStop(ExtensionStopInput extensionStopInput, ExtensionStopOutput extensionStopOutput)
    {
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
