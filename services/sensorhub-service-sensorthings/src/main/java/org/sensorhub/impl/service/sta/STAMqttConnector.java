/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sta;

import java.nio.ByteBuffer;
import java.security.AccessControlException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.IMqttServer.IMqttHandler;
import org.sensorhub.api.comm.mqtt.InvalidPayloadException;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import com.google.common.base.Charsets;
import de.fraunhofer.iosb.ilt.frostserver.formatter.ResultFormatter;
import de.fraunhofer.iosb.ilt.frostserver.model.core.Entity;
import de.fraunhofer.iosb.ilt.frostserver.parser.path.PathParser;
import de.fraunhofer.iosb.ilt.frostserver.parser.query.QueryParser;
import de.fraunhofer.iosb.ilt.frostserver.path.ResourcePath;
import de.fraunhofer.iosb.ilt.frostserver.persistence.PersistenceManagerFactory;
import de.fraunhofer.iosb.ilt.frostserver.query.Query;
import de.fraunhofer.iosb.ilt.frostserver.service.RequestType;
import de.fraunhofer.iosb.ilt.frostserver.service.Service;
import de.fraunhofer.iosb.ilt.frostserver.service.ServiceRequestBuilder;
import de.fraunhofer.iosb.ilt.frostserver.settings.CoreSettings;


/**
 * <p>
 * This class handles communication with the embedded MQTT server and transfers
 * messages to/from the STA service for processing. 
 * </p>
 *
 * @author Alex Robin
 * @since Apr 14, 2021
 */
public class STAMqttConnector implements IMqttHandler
{
    STAService service;
    String endpoint;
    CoreSettings coreSettings;
    Pattern topicRegex;
    Service frostService;
    OSHPersistenceManager pm;
    Map<String, MqttSubscriber> subscribers = new ConcurrentHashMap<>();
    
    
    class MqttSubscriber implements Subscriber<Entity<?>>
    {
        volatile Subscription subscription;
        IMqttServer server;
        String topic;
        ResourcePath path;
        Query query;
        ResultFormatter formatter;
        AtomicInteger numSubscribers = new AtomicInteger(0);
        AtomicBoolean started = new AtomicBoolean();
        
        MqttSubscriber(String topic, ResourcePath path, Query query, IMqttServer server)
        {
            this.topic = topic;
            this.path = path;
            this.query = query;
            this.server = server;
            this.formatter = coreSettings.getFormatter();
        }
        
        @Override
        public void onSubscribe(Subscription subscription)
        {
            // cancel right away if a subscription already exists
            if (this.subscription != null)
            {
                subscription.cancel();
                return;
            }
            
            this.subscription = subscription;
            maybeStart(); // start if we haven't started before
        }

        @Override
        public void onNext(Entity<?> item)
        {
            var msg = formatter.format(path, query, item, false);
            server.publish(topic, ByteBuffer.wrap(msg.getBytes()));
        }

        @Override
        public void onError(Throwable throwable)
        {
            service.getLogger().error("Error when subscribing to topic {}", topic, throwable);
            if (subscription != null)
                subscription.cancel();
        }

        @Override
        public void onComplete()
        {
        }
        
        public void maybeStart()
        {
            if (subscription != null && started.compareAndSet(false, true))
                subscription.request(Long.MAX_VALUE); // no flow control for now
        }
        
        public void close()
        {
            if (subscription != null)
                subscription.cancel();
        }
    }
    
    
    public STAMqttConnector(STAService service, String endpoint, CoreSettings coreSettings)
    {
        this.service = service;
        this.endpoint = endpoint;
        this.coreSettings = coreSettings;
        this.topicRegex = Pattern.compile(endpoint + 
            ".*(Things|Sensors|Datastreams|MultiDatastreams|Observations|FeaturesOfInterest)");
        this.frostService = new Service(coreSettings);
        
        this.pm = (OSHPersistenceManager)PersistenceManagerFactory.getInstance().create();
    }
    
    
    @Override
    public void onSubscribe(String userID, String topic, IMqttServer server) throws InvalidTopicException
    {
        try
        {
            var path = getResourcePath(topic);
            
            var queryIdx = topic.indexOf('?');
            var queryString = queryIdx > 0 ? topic.substring(queryIdx+1) : "";
            var query = QueryParser.parseQuery(queryString, coreSettings);
            query.validate(path);
            
            // register new subscription if needed
            var sub = subscribers.compute(topic, (k, v) -> {
                // create subscriber if needed
                if (v == null)
                    v = new MqttSubscriber(topic, path, query, server);
                
                return v;
            });
            
            // always evaluate request because we need to check for permissions
            // even if subscriber was already created
            service.getSecurityHandler().setCurrentUser(userID);
            var handler = pm.getHandler(path);
            handler.subscribeToCollection(path, query, sub);
            
            // start stream if all went well
            sub.numSubscribers.incrementAndGet();
            sub.maybeStart();
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidTopicException(e.getMessage());
        }
    }
    
    
    @Override
    public void onUnsubscribe(String userID, String topic, IMqttServer server) throws InvalidTopicException
    {
        subscribers.computeIfPresent(topic, (k, sub) -> {
            var numSub = sub.numSubscribers.decrementAndGet();
            if (numSub <= 0)
            {
                service.getLogger().debug("No more clients listening on topic {}. Cancelling subscription.", topic);
                sub.close();
                sub = null;
            }
            else
                service.getLogger().debug("{} client(s) still listening on topic {}", numSub, topic);
            return sub;
        });
    }


    @Override
    public void onPublish(String userID, String topic, ByteBuffer payload) throws InvalidTopicException, InvalidPayloadException
    {
        var collectionUrl = "/" + topic.replaceFirst(endpoint, "");
        
        var req = new ServiceRequestBuilder(coreSettings.getFormatter())
            .withRequestType(RequestType.CREATE)
            .withUrlPath(collectionUrl)
            .withContent(Charsets.UTF_8.decode(payload).toString())
            .build();                
        
        service.getSecurityHandler().setCurrentUser(userID);
        var resp = frostService.execute(req);
        if (!resp.isSuccessful())
        {
            if (resp.getCode() == 403 || service.getSecurityHandler().getPermissionError() != null)
                throw new AccessControlException("");
            else if (resp.getCode() == 404)
                throw new InvalidTopicException(resp.getMessage());
            else if (resp.getCode() == 400)
                throw new InvalidPayloadException(resp.getMessage());
            else
                throw new IllegalStateException("Internal STA error: " + resp.getMessage(), null);
        }
    }
    
    
    private ResourcePath getResourcePath(String topic)
    {
        var queryIdx = topic.indexOf('?');
        var resourcePath = queryIdx > 0 ? topic.substring(0, queryIdx) : topic;
        
        // remove base URL part
        resourcePath = "/" + resourcePath.replaceFirst(endpoint, "");
        return PathParser.parsePath(pm.getIdManager(), "/", resourcePath);
    }
    
    
    protected void stop()
    {
        for (var sub: subscribers.values())
            sub.close();
        subscribers.clear();
    }
}
