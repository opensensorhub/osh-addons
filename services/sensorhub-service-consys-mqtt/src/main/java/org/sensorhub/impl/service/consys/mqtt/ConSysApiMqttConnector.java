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
 * @author Alex Robin
 * @since May 9, 2023
 */
public class ConSysApiMqttConnector implements IMqttHandler
{
    ConSysApiServlet servlet;
    String endpoint;
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
    
    
    public ConSysApiMqttConnector(ConSysApiServlet servlet, String endpoint)
    {
        this.servlet = servlet;
        this.endpoint = endpoint;
    }


    @Override
    public void onSubscribe(String userID, String topic, IMqttServer server) throws InvalidTopicException
    {
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
            var ctx = getResourceContext(topic, sub);
            servlet.getSecurityHandler().setCurrentUser(userID);
            servlet.getRootHandler().doGet(ctx);
            
            // start stream if all went well
            sub.numSubscribers.incrementAndGet();
            sub.maybeStart();
        }
        catch (SecurityException | InvalidTopicException e)
        {
            throw e;
        }
        catch (InvalidRequestException e)
        {
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
            var ctx = getResourceContext(topic, payload);
            ctx.setRequestContentType(ResourceFormat.AUTO.getMimeType());
            
            if (correlData != null)
            {
                int cmdID = 0;
                if (correlData.remaining() == 4)
                    cmdID = correlData.getInt();
                if (cmdID <= 0)
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
        catch (Exception e)
        {
            throw new IllegalStateException("Internal error publishing to topic " + topic, e);
        }
        finally
        {
            servlet.getSecurityHandler().clearCurrentUser();
        }
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
            // remove the base URL part
            topic = topic.replaceFirst(endpoint, "");
            
            // parse URI (this also URL decodes the query string)
            return new URI(topic);
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
