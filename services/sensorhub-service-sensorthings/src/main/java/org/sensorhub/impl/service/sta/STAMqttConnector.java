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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.sensorhub.api.comm.mqtt.IMqttServer;
import org.sensorhub.api.comm.mqtt.IMqttServer.IMqttHandler;
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
        Subscription subscription;
        IMqttServer server;
        String topic;
        ResourcePath path;
        Query query;
        ResultFormatter formatter;
        AtomicInteger numSubscribers = new AtomicInteger(0);        
        
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
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE); // no flow control for now
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
    public void checkPublish(String userID, String topic)
    {
        // no need to check access rights here since it will be done in publish() method        
        checkTopicAccessible(userID, topic);
    }


    @Override
    public void checkSubscribe(String userID, String topic)
    {
        checkSubscribeAccessRights(userID, topic);
        checkTopicAccessible(userID, topic);
    }
    
    
    protected void checkTopicAccessible(String userID, String topic)
    {
        if (!topicRegex.matcher(topic).matches())
            throw new IllegalArgumentException();
        
        var path = getResourcePath(topic);
        if (!pm.validatePath(path))
            throw new AccessControlException("Unknown resource " + path);
    }
    
    
    protected void checkSubscribeAccessRights(String userID, String topic)
    {
        var path = getResourcePath(topic);
        var entityType = path.getMainElementType();
        
        var securityHandler = service.getSecurityHandler();
        securityHandler.setCurrentUser(userID);
        
        switch (entityType)
        {
            case THING:
                securityHandler.checkPermission(securityHandler.sta_read_thing);
                break;
                
            case SENSOR:
                securityHandler.checkPermission(securityHandler.sta_read_sensor);
                break;
                
            case DATASTREAM:
            case MULTIDATASTREAM:
                securityHandler.checkPermission(securityHandler.sta_read_datastream);
                break;
                
            case FEATUREOFINTEREST:
                securityHandler.checkPermission(securityHandler.sta_read_foi);
                break;
                
            case LOCATION:
            case HISTORICALLOCATION:
                securityHandler.checkPermission(securityHandler.sta_read_location);
                break;
                
            case OBSERVATION:
                securityHandler.checkPermission(securityHandler.sta_read_obs);
                break;
                
            default:
                throw new AccessControlException("Subscribe to resource " + entityType + " not allowed");
        }
    }
    
    
    @Override
    public void subscribe(String userID, String topic, IMqttServer server)
    {
        // create subscription if needed
        maybeAddSubscription(userID, topic, server);
    }


    protected void maybeAddSubscription(String userID, String topic, IMqttServer server)
    {
        var path = getResourcePath(topic);
        
        var queryIdx = topic.indexOf('?');
        var queryString = queryIdx > 0 ? topic.substring(queryIdx+1) : "";
        var query = QueryParser.parseQuery(queryString, coreSettings);
        query.validate(path);
        
        synchronized (subscribers)
        {
            // register new subscription if needed
            subscribers.compute(topic, (k,v) -> {
                
                if (v == null)
                {
                    // create subscriber
                    v = new MqttSubscriber(topic, path, query, server);
                    
                    // subscribe to collection events
                    service.getSecurityHandler().setCurrentUser(userID);
                    var handler = pm.getHandler(path);
                    handler.subscribeToCollection(path, query, v);
                }
                
                v.numSubscribers.incrementAndGet();
                return v;
            });
        }
    }
    
    
    @Override
    public void unsubscribe(String userID, String topic, IMqttServer server)
    {
        synchronized (subscribers)
        {
            subscribers.computeIfPresent(topic, (k, sub) -> {
                var numSub = sub.numSubscribers.decrementAndGet();
                if (numSub <= 0)
                {
                    if (sub.subscription != null)
                        sub.subscription.cancel();
                    sub = null;
                }
                return sub;
            });
        }
    }


    @Override
    public void publish(String userID, String topic, ByteBuffer payload)
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
            if (resp.getCode() == 403)
                throw new AccessControlException("");
            else
                throw new IllegalStateException("Invalid topic or payload: " + resp.getMessage(), null);
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
}
