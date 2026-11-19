/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import java.time.Duration;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.services.Services;


/**
 * <p>
 * Checks each message before HiveMQ sends it to a client. A wildcard topic
 * filter ({@code +}, {@code #}) can match topics of resources that the user
 * cannot read, so the subscribe check is not sufficient for it.
 * </p><p>
 * A message on a topic that the client subscribed to exactly is sent at once.
 * Any other message matched a wildcard filter of the client: the handler of
 * the topic decides if it is sent (see {@code IMqttHandler.canDeliver}).
 * If the check fails or takes too long, the message is not sent.
 * </p>
 */
public class OshDeliveryFilter implements PublishOutboundInterceptor
{
    static final int REQ_TIMEOUT_MS = 5000;

    final OshExtension oshExt;
    final Logger log;


    OshDeliveryFilter(OshExtension oshExt)
    {
        this.oshExt = Asserts.checkNotNull(oshExt, OshExtension.class);
        this.log = oshExt.log;
    }


    @Override
    public void onOutboundPublish(PublishOutboundInput publishIn, PublishOutboundOutput publishOut)
    {
        var topic = publishIn.getPublishPacket().getTopic();
        var clientId = publishIn.getClientInformation().getClientId();

        // fast path: the subscribe check covers exact topics
        if (oshExt.isExactSubscription(clientId, topic))
            return;

        var userId = publishIn.getConnectionInformation().getConnectionAttributeStore()
            .getAsString(OshAuthenticator.MQTT_USER_PROP)
            .orElse(ISecurityManager.ANONYMOUS_USER);

        // the handler can block, so do not check on the HiveMQ thread
        var async = publishOut.async(Duration.ofMillis(REQ_TIMEOUT_MS), TimeoutFallback.FAILURE);
        Services.extensionExecutorService().submit(() -> {
            try
            {
                if (!oshExt.canDeliverToWildcard(userId, clientId, topic))
                {
                    log.trace("Not sent to client {} (wildcard match): {}", clientId, topic);
                    publishOut.preventPublishDelivery();
                }
            }
            catch (Throwable e)
            {
                log.error("Internal error checking PUBLISH to client {}", clientId, e);
                publishOut.preventPublishDelivery();
            }
            finally
            {
                async.resume();
            }
        });
    }
}
