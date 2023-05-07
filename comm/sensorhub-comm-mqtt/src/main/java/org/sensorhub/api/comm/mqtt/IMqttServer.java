/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm.mqtt;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;


/**
 * <p>
 * Simple base interface for all MQTT service implementations
 * </p>
 *
 * @author Alex Robin
 * @since Apr 14, 2021
 */
public interface IMqttServer
{
    
    /**
     * <p>
     * Handler to receive inbound subscribe notifications and publish messages
     * from the embedded MQTT service
     * </p>
     *
     * @author Alex Robin
     * @since Apr 14, 2021
     */
    public interface IMqttHandler
    {
        
        /**
         * Notify the handler of an incoming publish message
         * @param userID Authenticated user who published the message
         * @param topic MQTT topic where the message was published
         * @param payload Payload data embedded in the message
         * @param correlData Correlation data or null if not present
         * @throws InvalidTopicException if the topic is invalid
         * @throws InvalidPayloadException if the payload is invalid
         * @throws AccessControlException if the user is not allowed to publish to the topic
         * @throws MqttException for all other errors
         */
        void onPublish(String userID, String topic, ByteBuffer payload, ByteBuffer correlData) throws MqttException;
    
        
        /**
         * Notify handler of an incoming subscribe request
         * @param userID Authenticated user who wants to subscribe
         * @param topic Topic the user wants to subscribe to
         * @param server MQTT server where subscribe request was received
         * @throws InvalidTopicException if the topic is invalid
         * @throws AccessControlException if the user is not allowed to subscribe to the topic
         * @throws MqttException for all other errors
         */
        void onSubscribe(String userID, String topic, IMqttServer server) throws MqttException;
    
        
        /**
         * Notify handler of an incoming unsubscribe request
         * @param userID Authenticated user who wants to unsubscribe
         * @param topic Topic the user wants to unsubscribe from
         * @param server MQTT server where unsubscribe request was received
         * @throws InvalidTopicException if the topic is invalid
         * @throws AccessControlException if the user is not allowed to unsubscribe from the topic
         * @throws MqttException for all other errors
         */
        void onUnsubscribe(String userID, String topic, IMqttServer server) throws MqttException;
    }
    
    
    /**
     * Register a handler that will intercept all communications on topics
     * starting with the given prefix
     * @param topicPrefix Prefix of topics to be managed by this handler
     * @param handler The handler to forward requests to
     */
    void registerHandler(String topicPrefix, IMqttHandler handler);
    
    
    /**
     * Unregister a handler for the given topic prefix
     * @param topicPrefix Prefix of topics that were managed by this handler
     * @param handler The handler that will stop receiving requests
     */
    void unregisterHandler(String topicPrefix, IMqttHandler handler);
    
    
    /**
     * Publish an outbound message using the MQTT service
     * @param topic MQTT topic to publish to
     * @param payload Payload data
     * @return future that will be completed when PUBLISH operation is
     * confirmed (true) or rejected (false)
     */
    CompletableFuture<Boolean> publish(String topic, ByteBuffer payload);
    
    
    /**
     * Publish an outbound message using the MQTT service
     * @param topic MQTT topic to publish to
     * @param payload Payload data
     * @param correlData Correlation data or null if not present
     * @return future that will be completed when PUBLISH operation is
     * confirmed (true) or rejected (false)
     */
    CompletableFuture<Boolean> publish(String topic, ByteBuffer payload, ByteBuffer correlData);
    
}
