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

import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.time.Duration;
import org.eclipse.jetty.util.security.Credential;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserRegistry;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerOutput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extension.sdk.api.services.Services;


public class OshAuth implements SimpleAuthenticator, PublishAuthorizer, SubscriptionAuthorizer
{
    final static String MQTT_USER_PROP = "MQTT_USER";
    final static int AUTH_TIMEOUT_MS = 5000;
    
    IUserRegistry users;
    OshExtension oshExt;
    
    
    OshAuth(IUserRegistry users, OshExtension oshExt)
    {
        this.users = users;
        this.oshExt = oshExt;
    }


    @Override
    public void onConnect(SimpleAuthInput authInput, SimpleAuthOutput authOutput)
    {
        var async = authOutput.async(Duration.ofMillis(AUTH_TIMEOUT_MS));
        Services.extensionExecutorService().submit(() -> {
            try
            {
                var userID = authInput.getConnectPacket().getUserName().orElse(null);
                var pwd = authInput.getConnectPacket().getPassword()
                    .map(buf -> StandardCharsets.UTF_8.decode(buf).array())
                    .orElse(new char[0]);
                
                if (userID != null)
                {
                    // check user exists and password matches
                    var user = users.getUserInfo(userID);
                    if (user != null)
                    {
                        Credential storedCredential = Credential.getCredential(user.getPassword());
                        if (storedCredential.check(pwd))
                        {
                            authInput.getConnectionInformation().getConnectionAttributeStore().putAsString(MQTT_USER_PROP, userID);
                            authOutput.authenticateSuccessfully();
                            return;
                        }
                    }
                }
                else
                {
                    authInput.getConnectionInformation().getConnectionAttributeStore().putAsString(MQTT_USER_PROP, ISecurityManager.ANONYMOUS_USER);
                    authOutput.authenticateSuccessfully();
                    return;
                }
                
                authOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);
            }
            catch (Exception e)
            {
                oshExt.log.error("Error checking MQTT credentials", e);
                authOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);
            }
            finally
            {
                async.resume();
            }
        });
    }
    
    
    @Override
    public void authorizeSubscribe(SubscriptionAuthorizerInput authInput, SubscriptionAuthorizerOutput authOutput)
    {
        var async = authOutput.async(Duration.ofMillis(AUTH_TIMEOUT_MS));
        Services.extensionExecutorService().submit(() -> {
            try
            {
                var topic = authInput.getSubscription().getTopicFilter();
                var userID = authInput.getConnectionInformation().getConnectionAttributeStore()
                    .getAsString(MQTT_USER_PROP)
                    .orElse(ISecurityManager.ANONYMOUS_USER);
                
                // if a handler is found, use it to authorize on this topic
                var handler = oshExt.handlers.get(topic);
                if (handler != null)
                {
                    try
                    {
                        handler.checkSubscribe(userID, topic);
                        authOutput.authorizeSuccessfully();
                    }
                    catch (IllegalArgumentException e)
                    {
                        authOutput.failAuthorization(SubackReasonCode.TOPIC_FILTER_INVALID);
                    }
                    catch (AccessControlException e)
                    {
                        authOutput.failAuthorization(SubackReasonCode.NOT_AUTHORIZED);
                    }
                    catch (Exception e)
                    {
                        oshExt.log.error("Error authorizing SUBSCRIBE message", e);
                        authOutput.failAuthorization(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
                    }
                    
                    return;
                }
                
                // reject in all other cases
                authOutput.failAuthorization(SubackReasonCode.TOPIC_FILTER_INVALID);
            }
            finally
            {
                async.resume();
            }
        });
    }
    

    @Override
    public void authorizePublish(PublishAuthorizerInput authInput, PublishAuthorizerOutput authOutput)
    {
        var async = authOutput.async(Duration.ofMillis(AUTH_TIMEOUT_MS));
        Services.extensionExecutorService().submit(() -> {
            try
            {
                var topic = authInput.getPublishPacket().getTopic();
                var userID = authInput.getConnectionInformation().getConnectionAttributeStore()
                    .getAsString(MQTT_USER_PROP)
                    .orElse(ISecurityManager.ANONYMOUS_USER);
                
                // if a handler is found, use it to authorize on this topic
                var handler = oshExt.handlers.get(topic);
                if (handler != null)
                {
                    try
                    {
                        handler.checkPublish(userID, topic);
                        authOutput.authorizeSuccessfully();
                    }
                    catch (IllegalArgumentException e)
                    {
                        authOutput.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID);
                    }
                    catch (AccessControlException e)
                    {
                        authOutput.failAuthorization(AckReasonCode.NOT_AUTHORIZED);
                    }
                    catch (Exception e)
                    {
                        oshExt.log.error("Error authorizing PUBLISH message", e);
                        authOutput.failAuthorization(AckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
                    }
                    
                    return;
                }
                
                // reject in all other cases
                authOutput.failAuthorization(AckReasonCode.TOPIC_NAME_INVALID);
            }
            finally
            {
                async.resume();
            }
        });        
    }

}
