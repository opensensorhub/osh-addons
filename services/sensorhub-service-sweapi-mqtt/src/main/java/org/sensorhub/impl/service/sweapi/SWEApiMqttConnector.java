/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpUtils;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.InvalidPayloadException;
import org.sensorhub.api.comm.mqtt.IMqttServer.IMqttHandler;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.api.comm.mqtt.MqttOutputStream;
import org.sensorhub.impl.service.sweapi.resource.ResourceContext;
import org.sensorhub.impl.service.sweapi.stream.StreamHandler;
import org.vast.util.Asserts;
import com.google.common.base.Strings;


/**
 * <p>
 * This class handles communication with the embedded MQTT server and transfers
 * messages to/from the SensorWeb servlet for processing. 
 * </p>
 *
 * @author Alex Robin
 * @since Jul 29, 2021
 */
public class SWEApiMqttConnector implements IMqttHandler
{
    SWEApiServlet servlet;
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
        
        public void close()
        {
            if (onClose != null)
                onClose.run();
        }
    }
    
    
    public SWEApiMqttConnector(SWEApiServlet servlet, String endpoint)
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
        catch (InvalidRequestException e)
        {
            throw new InvalidTopicException(e.getMessage());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Internal error subscribing to topic " + topic, e);
        }
    }
    
    
    @Override
    public void onUnsubscribe(String userID, String topic, IMqttServer server) throws InvalidTopicException
    {
        subscribers.computeIfPresent(topic, (k, sub) -> {
            var numSub = sub.numSubscribers.decrementAndGet();
            if (numSub <= 0)
            {
                servlet.getLogger().debug("No more clients listening on topic {}. Cancelling subscription.", topic);
                sub.close();
                sub = null;
            }
            else
                servlet.getLogger().debug("{} client(s) still listening on topic {}", numSub, topic);
            return sub;
        });
    }


    @Override
    public void onPublish(String userID, String topic, ByteBuffer payload) throws InvalidTopicException, InvalidPayloadException
    {                
        
    }
    
    
    private ResourceContext getResourceContext(String topic, MqttSubscriber streamHandler) throws InvalidTopicException
    {
        try
        {
            var topicUri = new URI(topic);
            
            // extract path, removing the base URL part
            var path = topicUri.getPath();
            path = "/" + path.replaceFirst(endpoint, "");
            
            // parse query params
            Map<String, String[]> params = Strings.isNullOrEmpty(topicUri.getQuery()) ?
                Collections.emptyMap() :
                HttpUtils.parseQueryString(topicUri.getQuery());
            
            return new ResourceContext(servlet, path, params, streamHandler);
        }
        catch (URISyntaxException e)
        {
            throw new InvalidTopicException("Invalid URI syntax");
        }
    }
    
    
    protected void stop()
    {
        for (var sub: subscribers.values())
            sub.close();
        subscribers.clear();
    }
}
