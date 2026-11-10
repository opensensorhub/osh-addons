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
import java.time.Instant;
import java.util.UUID;
import org.eclipse.jetty.util.security.Credential;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.Logger;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.services.Services;


public class OshAuthenticator implements SimpleAuthenticator
{
    final static String MQTT_USER_PROP = "MQTT_USER";
    final static String TOKEN_CONN_PROP = "TOKEN_CONN";
    final static int AUTH_TIMEOUT_MS = 5000;
    
    final ISecurityManager securityMgr;
    final MqttServerConfig config;
    final TokenVerifier tokenVerifier;
    final Logger log;
    volatile TokenExpiryMonitor expiryMonitor;
    
    
    /**
     * The result of a successful login
     */
    static class Login
    {
        final String user;
        final Instant tokenExpiry; // null if the login did not use a token
        
        Login(String user, Instant tokenExpiry)
        {
            this.user = user;
            this.tokenExpiry = tokenExpiry;
        }
    }
    
    
    OshAuthenticator(OshExtension oshExt)
    {
        this(oshExt.service.getParentHub().getSecurityManager(), oshExt.service.getConfiguration(), oshExt.log);
    }


    OshAuthenticator(ISecurityManager securityMgr, MqttServerConfig config, Logger log)
    {
        this(securityMgr, config, TokenVerifier.create(config.tokenAuth, log), log);
    }


    OshAuthenticator(ISecurityManager securityMgr, MqttServerConfig config, TokenVerifier tokenVerifier, Logger log)
    {
        this.securityMgr = securityMgr;
        this.config = config;
        this.tokenVerifier = tokenVerifier;
        this.log = log;
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
                    .map(buf -> StandardCharsets.UTF_8.decode(buf).toString())
                    .orElse("");
                
                var login = authenticate(userID, pwd);
                if (login != null)
                {
                    var attrs = authInput.getConnectionInformation().getConnectionAttributeStore();
                    attrs.putAsString(MQTT_USER_PROP, login.user);
                    
                    // close the connection when its token expires
                    var monitor = expiryMonitor;
                    if (login.tokenExpiry != null && monitor != null)
                    {
                        var connId = UUID.randomUUID().toString();
                        attrs.putAsString(TOKEN_CONN_PROP, connId);
                        monitor.onTokenLogin(connId, authInput.getClientInformation().getClientId(), login.tokenExpiry);
                    }
                    
                    authOutput.authenticateSuccessfully();
                }
                else
                    authOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);
            }
            catch (Exception e)
            {
                log.error("Error checking MQTT credentials", e);
                authOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);
            }
            finally
            {
                async.resume();
            }
        });
    }


    /**
     * Check the credentials of a CONNECT
     * @return the OSH user of the connection and the expiry time of its
     * token if it has one, or null to refuse the connection
     */
    Login authenticate(String userID, String pwd)
    {
        // an access token as the password: it must be valid, there is no fallback
        if (userID != null && tokenVerifier != null && TokenVerifier.isJwt(pwd))
        {
            try
            {
                var token = tokenVerifier.verify(pwd, userID);
                return new Login(userID, token.getExpiresAtAsInstant());
            }
            catch (JWTVerificationException | JwkException e)
            {
                // the key server is down or refuses requests: all token logins fail, so tell the operator
                if (TokenVerifier.isKeyServerError(e))
                    log.warn("Token login refused for user {}: cannot get the public keys from {}: {}",
                        userID, tokenVerifier.config.jwksUri, TokenVerifier.rootMessage(e));
                else
                    log.debug("Token login refused for user {}: {}", userID, e.getMessage());
                return null;
            }
        }

        // check user exists and password matches
        if (userID != null && isAccessControlEnabled())
            return checkPassword(userID, pwd.toCharArray()) ? new Login(userID, null) : null;

        if (!config.requireAuth)
            return new Login(ISecurityManager.ANONYMOUS_USER, null);

        return null;
    }


    boolean isAccessControlEnabled()
    {
        return securityMgr.isAccessControlEnabled();
    }


    boolean checkPassword(String userID, char[] pwd)
    {
        var user = securityMgr.getUserRegistry().get(userID);
        return user != null && Credential.getCredential(user.getPassword()).check(pwd);
    }
}
