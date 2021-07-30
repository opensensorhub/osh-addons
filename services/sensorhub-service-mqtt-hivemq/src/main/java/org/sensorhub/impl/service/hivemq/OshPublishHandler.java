/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import java.security.AccessControlException;
import java.time.Duration;
import org.sensorhub.api.comm.mqtt.InvalidPayloadException;
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerOutput;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.services.Services;


/**
 * <p>
 * Publish handler implemented as authorizer so we can handle authorization
 * and return proper error codes while we are processing requests.
 * </p>
 *
 * @author Alex Robin
 * @since Jul 30, 2021
 */
public class OshPublishHandler implements PublishAuthorizer, PublishInboundInterceptor
{
    static final String LOG_PUBLISH_MSG = "Received PUBLISH clientId={}, topic={}: ";
    static final int REQ_TIMEOUT_MS = 5000;
    
    final OshExtension oshExt;
    final Logger log;
    
    
    OshPublishHandler(OshExtension oshExt)
    {
        this.oshExt = Asserts.checkNotNull(oshExt, OshExtension.class);
        this.log = oshExt.log;
    }
    
    
    @Override
    public void authorizePublish(PublishAuthorizerInput publishIn, PublishAuthorizerOutput publishOut)
    {
        var async = publishOut.async(Duration.ofMillis(REQ_TIMEOUT_MS));
        Services.extensionExecutorService().submit(() -> {
            try
            {
                // get topic name and user ID
                var topic = publishIn.getPublishPacket().getTopic();
                var userID = publishIn.getConnectionInformation().getConnectionAttributeStore()
                    .getAsString(OshAuthenticator.MQTT_USER_PROP)
                    .orElse(ISecurityManager.ANONYMOUS_USER);
                
                try
                {
                    // if a handler is found, use it to authorize/publish on this topic
                    var handler = oshExt.handlers.get(topic);
                    if (handler != null)
                    {                    
                        // publish via OSH handler
                        log.debug(LOG_PUBLISH_MSG + "Handled by {}",
                            publishIn.getClientInformation().getClientId(), topic, handler.getClass().getSimpleName());
                        handler.onPublish(userID, topic, publishIn.getPublishPacket().getPayload().get());
                        publishOut.authorizeSuccessfully();                    
                    }
                    
                    // reject in all other cases
                    else
                    {
                        publishOut.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID);
                    }
                }
                catch (AccessControlException e)
                {
                    log.debug("Not authorized: {}", e.getMessage());
                    publishOut.failAuthorization(AckReasonCode.NOT_AUTHORIZED);
                }
                catch (InvalidTopicException e)
                {
                    log.debug("Invalid topic: {}", topic, e);
                    publishOut.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID, e.getMessage());
                }
                catch (InvalidPayloadException e)
                {
                    log.debug("Invalid payload: {}", e);
                    publishOut.failAuthorization(AckReasonCode.PAYLOAD_FORMAT_INVALID, e.getMessage());
                }
                catch (Exception e)
                {
                    log.error("Internal error handling PUBLISH message", e);
                    publishOut.failAuthorization(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "Internal error");
                }
            }
            finally
            {
                async.resume();
            }
        });        
    }
    
    
    @Override
    public void onInboundPublish(final PublishInboundInput publishIn, final PublishInboundOutput publishOut)
    {
        // always prevent direct delivery by MQTT server to force message to go
        // through OSH eventbus, but don't send any error
        //publishOut.preventPublishDelivery();
    }

}
