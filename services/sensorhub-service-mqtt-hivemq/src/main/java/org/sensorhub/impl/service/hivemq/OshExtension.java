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
import java.security.AccessControlException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.utils.MapWithWildcards;
import org.slf4j.Logger;
import com.hivemq.embedded.EmbeddedExtension;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
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
    static final String LOG_SUBSCRIBE_MSG = "Received SUBSCRIBE request for topic {}: ";
    static final String LOG_UNSUBSCRIBE_MSG = "Received UNSUBSCRIBE request for topic {}: ";
    static final String LOG_PUBLISH_MSG = "Received PUBLISH message on topic {}: ";
    
    ISensorHub parentHub;
    MapWithWildcards<IMqttHandler> handlers = new MapWithWildcards<>();
    Logger log;
    
    
    public OshExtension(ISensorHub parentHub, MqttServer service)
    {
        this.parentHub = parentHub;
        this.log = service.getLogger();
    }
    
    
    @Override
    public void extensionStart(@NotNull
    ExtensionStartInput extensionStartInput, @NotNull
    ExtensionStartOutput extensionStartOutput)
    {
        /*Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            try
            {
                Publish message = Builders.publish()
                    .topic("/swa/v1.0/datastreams/ef448p/observations")
                    .qos(Qos.AT_MOST_ONCE)
                    .payload(Charset.forName("UTF-8").encode("payload-" + Instant.now()))
                    .retain(false)
                    .build();
                Services.publishService().publish(message);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);*/
        
        
        Services.securityRegistry().setAuthenticatorProvider(authProviderInput -> {
            return new OshAuth(parentHub.getSecurityManager(), this);
        });
        
        Services.securityRegistry().setAuthorizerProvider(authorizerProviderInput -> {
            return new OshAuth(parentHub.getSecurityManager(), this);
        });
        
        
        // create a new subscribe inbound interceptor
        final var subscribeInboundInterceptor = new SubscribeInboundInterceptor() {
            @Override
            public void onInboundSubscribe(@NotNull
            SubscribeInboundInput subscribeIn, @NotNull
            SubscribeInboundOutput subscribeOut)
            {
                for (var sub: subscribeIn.getSubscribePacket().getSubscriptions())
                {
                    var topic = sub.getTopicFilter();
                
                    // try to find a handler
                    // if none was found, proceed normally with HiveMQ pipeline
                    var handler = handlers.get(topic);
                    if (handler == null)
                    {
                        log.debug(LOG_SUBSCRIBE_MSG + "Handled by HiveMQ", topic);
                        return;
                    }
                    
                    log.debug(LOG_SUBSCRIBE_MSG + "Handled by {}", topic, handler.getClass().getSimpleName());
                    
                    // notify handler asynchronously
                    var async = subscribeOut.async(Duration.ofMillis(1000));
                    Services.extensionExecutorService().submit(() -> {
                        try
                        {
                            var userID = subscribeIn.getConnectionInformation().getConnectionAttributeStore()
                                .getAsString(OshAuth.MQTT_USER_PROP)
                                .orElse(ISecurityManager.ANONYMOUS_USER);
                            
                            // subscribe using OSH handler
                            handler.subscribe(userID, topic, OshExtension.this);
                        }
                        catch (AccessControlException e)
                        {
                            log.error("Unauthorized SUBSCRIBE", e);
                        }
                        catch (Exception e)
                        {
                            log.error("Error handling SUBSCRIBE message", e);
                            throw e;
                        }
                        finally
                        {
                            async.resume();
                        }   
                    });
                }
            }
        };
        
        
        // create a new unsubscribe inbound interceptor
        final var unsubscribeInboundInterceptor = new UnsubscribeInboundInterceptor() {
            @Override
            public void onInboundUnsubscribe(@NotNull
            UnsubscribeInboundInput unsubscribeIn, @NotNull
            UnsubscribeInboundOutput unsubscribeOut)
            {
                for (var topic: unsubscribeIn.getUnsubscribePacket().getTopicFilters())
                {
                    // try to find a handler
                    // if none was found, proceed normally with HiveMQ pipeline
                    var handler = handlers.get(topic);
                    if (handler == null)
                    {
                        log.debug(LOG_UNSUBSCRIBE_MSG + "Handled by HiveMQ", topic);
                        return;
                    }
                    
                    log.debug(LOG_UNSUBSCRIBE_MSG + "Handled by {}", topic, handler.getClass().getSimpleName());
                    
                    // notify handler asynchronously
                    var async = unsubscribeOut.async(Duration.ofMillis(1000));
                    Services.extensionExecutorService().submit(() -> {
                        try
                        {
                            var userID = unsubscribeIn.getConnectionInformation().getConnectionAttributeStore()
                                .getAsString(OshAuth.MQTT_USER_PROP)
                                .orElse(ISecurityManager.ANONYMOUS_USER);
                            
                            // subscribe using OSH handler
                            handler.unsubscribe(userID, topic, OshExtension.this);
                        }
                        catch (AccessControlException e)
                        {
                            log.error("Unauthorized UNSUBSCRIBE", e);
                        }
                        catch (Exception e)
                        {
                            log.error("Error handling UNSUBSCRIBE message", e);
                            throw e;
                        }
                        finally
                        {
                            async.resume();
                        }   
                    });
                }
            }
        };
        
                
        // create a new publish inbound interceptor
        final var publishInboundInterceptor = new PublishInboundInterceptor()
        {
            @Override
            public void onInboundPublish(
                final @NotNull PublishInboundInput publishIn,
                final @NotNull PublishInboundOutput publishOut)
            {
                var topic = publishIn.getPublishPacket().getTopic();
                                
                // try to find a handler
                // if none was found, proceed normally with HiveMQ pipeline
                var handler = handlers.get(topic);
                if (handler == null)
                {
                    log.debug(LOG_PUBLISH_MSG + "Handled by HiveMQ", topic);
                    return;
                }
                
                // publish to handler asynchronously
                log.debug(LOG_PUBLISH_MSG + "Handled by {}", topic, handler.getClass().getSimpleName());
                var async = publishOut.async(Duration.ofMillis(1000));
                Services.extensionExecutorService().submit(() -> {
                    try
                    {
                        var userID = publishIn.getConnectionInformation().getConnectionAttributeStore()
                            .getAsString(OshAuth.MQTT_USER_PROP)
                            .orElse(ISecurityManager.ANONYMOUS_USER);
                        
                        // publish via OSH handler
                        handler.publish(userID, topic, publishIn.getPublishPacket().getPayload().get());
                        
                        // prevent direct delivery by MQTT server to force message to go
                        // through OSH eventbus, but don't send any error
                        publishOut.preventPublishDelivery();
                    }
                    catch (AccessControlException e)
                    {
                        publishOut.preventPublishDelivery(AckReasonCode.NOT_AUTHORIZED);
                    }
                    catch (Exception e)
                    {
                        log.error("Error dispatching PUBLISH message", e);
                        publishOut.preventPublishDelivery(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, e.getMessage());
                    }
                    finally
                    {
                        async.resume();
                    }
                });     
            }
        };

        // create a new client initializer
        final var clientInitializer = new ClientInitializer()
        {
            @Override
            public void initialize(
                final @NotNull InitializerInput initializerInput,
                final @NotNull ClientContext clientContext)
            {
                // add interceptors to the context of the connecting client
                clientContext.addSubscribeInboundInterceptor(subscribeInboundInterceptor);
                clientContext.addUnsubscribeInboundInterceptor(unsubscribeInboundInterceptor);
                clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
            }
        };

        //register the client initializer
        Services.initializerRegistry().setClientInitializer(clientInitializer);
    }


    @Override
    public void extensionStop(@NotNull
    ExtensionStopInput extensionStopInput, @NotNull
    ExtensionStopOutput extensionStopOutput)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void registerHandler(String topicPrefix, IMqttHandler handler)
    {
        handlers.put(topicPrefix + "*", handler);
    }


    @Override
    public CompletableFuture<Boolean> publish(String topic, ByteBuffer payload)
    {
        Publish message = Builders.publish()
            .topic(topic)
            .qos(Qos.AT_MOST_ONCE)
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
