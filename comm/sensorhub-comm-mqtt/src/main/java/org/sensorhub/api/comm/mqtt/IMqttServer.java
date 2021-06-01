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
         * Allow handler to check if user can publish on the given topic
         * @param userID 
         * @param topic
         * @throws IllegalArgumentException if topic syntax is invalid
         * @throws AccessControlException if publishing to topic is not authorized
         */
        void checkPublish(String userID, String topic);
        
        
        /**
         * Allow handler to check if user can subscribe on the given topic
         * @param userID 
         * @param topic
         * @throws IllegalArgumentException if topic syntax is invalid
         * @throws AccessControlException if publishing to topic is not authorized
         */
        void checkSubscribe(String userID, String topic);
        
        
        /**
         * Notify the handler of an incoming publish message
         * @param userID 
         * @param topic
         * @param payload
         */
        void publish(String userID, String topic, ByteBuffer payload);
    
        
        /**
         * Notify handler of an incoming subscribe request
         * @param userID 
         * @param server 
         * @param topic
         */
        void subscribe(String userID, String topic, IMqttServer server);
    
        
        /**
         * Notify handler of an incoming unsubscribe request
         * @param userID 
         * @param server 
         * @param topic
         */
        void unsubscribe(String userID, String topic, IMqttServer server);
    }
    
    
    /**
     * Register a handler that will intercept all communications on topics
     * starting with the given prefix
     * @param topicPrefix
     * @param handler
     */
    void registerHandler(String topicPrefix, IMqttHandler handler);
    
    
    /**
     * Publish an outbound message using the MQTT service
     * @param topic
     * @param payload
     * @return future that will be completed when PUBLISH operation is
     * confirmed (true) or rejected (false)
     */
    CompletableFuture<Boolean> publish(String topic, ByteBuffer payload);
    
}
