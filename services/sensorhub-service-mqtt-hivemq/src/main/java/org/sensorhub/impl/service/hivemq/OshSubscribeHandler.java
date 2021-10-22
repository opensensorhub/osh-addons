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
import org.sensorhub.api.comm.mqtt.InvalidTopicException;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extension.sdk.api.services.Services;


public class OshSubscribeHandler implements SubscribeInboundInterceptor, SubscriptionAuthorizer
{
    static final int REQ_TIMEOUT_MS = 5000;
    
    final OshExtension oshExt;
    final Logger log;
    
    
    OshSubscribeHandler(OshExtension oshExt)
    {
        this.oshExt = Asserts.checkNotNull(oshExt, OshExtension.class);
        this.log = oshExt.log;
    }


    @Override
    public void onInboundSubscribe(SubscribeInboundInput subscribeIn, SubscribeInboundOutput subscribeOut)
    {
        // TODO canonicalize all topic names (especially the query part) ?
        // need to call the handler to canonicalize        
    }
    
    
    @Override
    public void authorizeSubscribe(final SubscriptionAuthorizerInput subscribeIn, final SubscriptionAuthorizerOutput subscribeOut)
    {
        var async = subscribeOut.async(Duration.ofMillis(REQ_TIMEOUT_MS));
        Services.extensionExecutorService().submit(() -> {
            
            // get topic name, user ID and client ID
            var topic = subscribeIn.getSubscription().getTopicFilter();
            var userId = subscribeIn.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(OshAuthenticator.MQTT_USER_PROP)
                .orElse(ISecurityManager.ANONYMOUS_USER);
            var clientId = subscribeIn.getClientInformation().getClientId();
            
            try
            {
                // authorize request if subscribe completes successfully, reject otherwise
                if (oshExt.subscribeToTopic(userId, clientId, topic))
                    subscribeOut.authorizeSuccessfully();
                else
                    subscribeOut.failAuthorization(SubackReasonCode.TOPIC_FILTER_INVALID);
            }
            catch (AccessControlException e)
            {
                log.debug("Not authorized: {}", e.getMessage());
                subscribeOut.failAuthorization(SubackReasonCode.NOT_AUTHORIZED);
            }
            catch (InvalidTopicException e)
            {
                log.debug("Invalid topic {}: {}", topic, e.getMessage());
                subscribeOut.failAuthorization(SubackReasonCode.TOPIC_FILTER_INVALID, e.getMessage());
            }
            catch (Throwable e)
            {
                log.error("Internal error handling SUBSCRIBE message", e);
                subscribeOut.failAuthorization(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "Internal error");
            }
            finally
            {
                async.resume();
            }
        });
    }

}
