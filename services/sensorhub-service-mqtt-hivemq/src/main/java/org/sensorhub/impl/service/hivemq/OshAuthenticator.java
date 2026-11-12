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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.jetty.util.security.Credential;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserInfo;
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
    
    // The token connections that hold a user that this server added to the
    // OSH user registry. The user is removed when its last connection closes.
    // Lock on tokenConnUsers to read or change the two maps and the user.
    final Map<String, String> tokenConnUsers = new HashMap<>(); // connection ID -> user ID
    final Map<String, Integer> tokenUserConns = new HashMap<>(); // user ID -> number of connections
    
    
    /**
     * The result of a successful login
     */
    static class Login
    {
        final String user;
        final Instant tokenExpiry; // null if the login did not use a token
        final String connId; // unique ID of a token connection, or null
        
        Login(String user, Instant tokenExpiry)
        {
            this(user, tokenExpiry, null);
        }
        
        Login(String user, Instant tokenExpiry, String connId)
        {
            this.user = user;
            this.tokenExpiry = tokenExpiry;
            this.connId = connId;
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
            Login login = null;
            try
            {
                var userID = authInput.getConnectPacket().getUserName().orElse(null);
                var pwd = authInput.getConnectPacket().getPassword()
                    .map(buf -> StandardCharsets.UTF_8.decode(buf).toString())
                    .orElse("");
                
                login = authenticate(userID, pwd);
                if (login != null)
                {
                    var attrs = authInput.getConnectionInformation().getConnectionAttributeStore();
                    attrs.putAsString(MQTT_USER_PROP, login.user);
                    
                    if (login.connId != null)
                    {
                        // the disconnect event uses this ID to release the token user
                        // and to cancel the timer
                        attrs.putAsString(TOKEN_CONN_PROP, login.connId);
                        
                        // close the connection when its token expires
                        var monitor = expiryMonitor;
                        if (monitor != null)
                            monitor.onTokenLogin(login.connId, authInput.getClientInformation().getClientId(), login.tokenExpiry);
                    }
                    
                    authOutput.authenticateSuccessfully();
                }
                else
                    authOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD);
            }
            catch (Exception e)
            {
                log.error("Error checking MQTT credentials", e);
                if (login != null)
                    releaseTokenUser(login.connId);
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
                var connId = UUID.randomUUID().toString();
                if (isAccessControlEnabled() && !registerTokenUser(connId, userID, tokenVerifier.roles(token)))
                {
                    log.debug("Token login refused for user {}: not an OSH user", userID);
                    return null;
                }
                return new Login(userID, token.getExpiresAtAsInstant(), connId);
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


    Map<String, IUserInfo> userRegistry()
    {
        return securityMgr.getUserRegistry();
    }


    /**
     * Put the user of a valid token in the OSH user registry with the roles
     * of the token, so that subscribe and publish find the user. The user
     * stays until the last connection that logged in with a token for it
     * closes (see releaseTokenUser).
     * A user that this server did not add stays as it is: the login then
     * gets the roles that the user has in OSH.
     * @param connId unique ID of the connection
     * @return false to refuse the login
     */
    boolean registerTokenUser(String connId, String userID, Collection<String> roles)
    {
        // a connection without a user name gets this user: a token must not set its roles
        if (ISecurityManager.ANONYMOUS_USER.equals(userID))
            return false;

        var users = userRegistry();
        synchronized (tokenConnUsers)
        {
            var user = users.get(userID);
            boolean ownUser = user == null || user instanceof TokenUser;

            // a user that this server added earlier is not a user of OSH
            if (!config.tokenAuth.autoAddUser)
                return !ownUser;

            if (ownUser)
            {
                users.put(userID, new TokenUser(userID, roles));
                tokenConnUsers.put(connId, userID);
                tokenUserConns.merge(userID, 1, Integer::sum);
            }
            return true;
        }
    }


    /**
     * Remove the user of a token connection that closed from the OSH user
     * registry, if this server added the user and no other connection uses it
     * @param connId unique ID of the connection, or null if it has no token
     */
    void releaseTokenUser(String connId)
    {
        if (connId == null)
            return;

        synchronized (tokenConnUsers)
        {
            var userID = tokenConnUsers.remove(connId);
            if (userID == null)
                return;

            if (tokenUserConns.merge(userID, -1, Integer::sum) > 0)
                return;
            tokenUserConns.remove(userID);

            // another module can replace the user while the client is connected
            var users = userRegistry();
            if (users.get(userID) instanceof TokenUser)
            {
                users.remove(userID);
                log.debug("Token user {} removed: no connection left", userID);
            }
        }
    }


    /**
     * Remove all the users that this server added from the OSH user registry
     */
    void removeTokenUsers()
    {
        synchronized (tokenConnUsers)
        {
            tokenConnUsers.clear();
            tokenUserConns.clear();
            userRegistry().values().removeIf(user -> user instanceof TokenUser);
        }
    }


    boolean checkPassword(String userID, char[] pwd)
    {
        var user = userRegistry().get(userID);
        return user != null && passwordMatches(user.getPassword(), pwd);
    }


    /**
     * Check the password of a CONNECT against the stored password of the user
     * @return false if one of the two passwords is null or empty: a user
     * without a password cannot log in with a password
     */
    static boolean passwordMatches(String storedPwd, char[] pwd)
    {
        if (storedPwd == null || storedPwd.isEmpty() || pwd == null || pwd.length == 0)
            return false;
        
        return Credential.getCredential(storedPwd).check(pwd);
    }
}
