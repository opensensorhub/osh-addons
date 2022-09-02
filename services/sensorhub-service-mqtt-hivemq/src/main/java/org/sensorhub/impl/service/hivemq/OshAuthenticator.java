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
import java.time.Duration;
import org.eclipse.jetty.util.security.Credential;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserRegistry;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.services.Services;


public class OshAuthenticator implements SimpleAuthenticator
{
    final static String MQTT_USER_PROP = "MQTT_USER";
    final static int AUTH_TIMEOUT_MS = 5000;
    
    IUserRegistry users;
    OshExtension oshExt;
    
    
    OshAuthenticator(IUserRegistry users, OshExtension oshExt)
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
                    var user = users.get(userID);
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
                else if (!oshExt.service.getConfiguration().requireAuth)
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

}
