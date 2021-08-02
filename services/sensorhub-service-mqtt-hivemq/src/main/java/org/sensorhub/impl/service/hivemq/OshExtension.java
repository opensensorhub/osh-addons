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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.MqttException;
import org.sensorhub.utils.MapWithWildcards;
import org.slf4j.Logger;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
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
    ISensorHub parentHub;
    MapWithWildcards<IMqttHandler> handlers = new MapWithWildcards<>();
    Logger log;
    
    
    public OshExtension(ISensorHub parentHub, MqttServer service)
    {
        this.parentHub = parentHub;
        this.log = service.getLogger();
    }
    
    
    @Override
    public void extensionStart(ExtensionStartInput extensionStartInput, ExtensionStartOutput extensionStartOutput)
    {
        // set authenticator to validate credentials on CONNECT
        var oshAuth = new OshAuthenticator(parentHub.getSecurityManager(), this);
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
                        
                        Services.subscriptionStore().getSubscriptions(clientId)
                            .thenAccept(set -> set.forEach(ts -> {
                                var topic = ts.getTopicFilter();
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
                            })
                        );                        
                    }
                };
            }        
        });
        
        /*// start watchdog to clean resources for unused topics
        var subscriptionStore = Services.subscriptionStore();
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            subscriptionStore.iterateAllSubscriptions(new IterationCallback<SubscriptionsForClientResult>() {
                @Override
                public void iterate(IterationContext context, SubscriptionsForClientResult subscriptionsForClient) {
                    // this callback is called for every client with its subscriptions
                    final String clientId = subscriptionsForClient.getClientId();
                    final Set<TopicSubscription> subscriptions = subscriptionsForClient.getSubscriptions();
                    log.debug("{}: {}", clientId, subscriptions);
                }
            });
        }, 0, 1, TimeUnit.SECONDS);*/
    }


    @Override
    public void extensionStop(ExtensionStopInput extensionStopInput, ExtensionStopOutput extensionStopOutput)
    {
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
        Publish message = Builders.publish()
            .topic(topic)
            .qos(Qos.AT_LEAST_ONCE)
            .payload(payload)
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
