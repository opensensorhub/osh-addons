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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.eclipse.jetty.util.security.Credential;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserInfo;
import org.slf4j.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    final static String NO_OAUTH_JAR_MSG = "Token login is on, but the node does not have the OAuth module "
        + "(the jar sensorhub-security-oauth, version 2.0.0 or later). The server refuses all token logins. "
        + "Add this jar to the node and start the node again, or set Token Login > Enabled to off. Cause: {}";
    
    final ISecurityManager securityMgr;
    final MqttServerConfig config;
    final Supplier<IModule<?>> oauthModule;
    final Logger log;
    volatile TokenExpiryMonitor expiryMonitor;
    
    // The token connections that hold a user that this server added to the
    // OSH user registry. The user is removed when its last connection closes.
    // Lock on tokenConnUsers to read or change the three maps and the user.
    final Map<String, String> tokenConnUsers = new HashMap<>(); // connection ID -> user ID
    final Map<String, Integer> tokenUserConns = new HashMap<>(); // user ID -> number of connections
    final Map<String, IUserInfo> tokenUsers = new HashMap<>(); // user ID -> the user that this server added
    
    
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
        this(oshExt.service.getParentHub().getSecurityManager(), oshExt.service.getConfiguration(),
            () -> OAuthTokenCheck.module(oshExt.service.getParentHub()), oshExt.log);
    }


    /**
     * @param oauthModule gives the OAuth module of the node, or null if the
     * node has none. It is read at each token login, because the OAuth module
     * can start after the MQTT server. It gives a LinkageError if the node
     * does not have the jar of the OAuth module.
     */
    OshAuthenticator(ISecurityManager securityMgr, MqttServerConfig config, Supplier<IModule<?>> oauthModule, Logger log)
    {
        this.securityMgr = securityMgr;
        this.config = config;
        this.oauthModule = oauthModule;
        this.log = log;

        var anon = config.requireAuth ? "" : ". Connections without a user name are accepted (requireAuth is off)";
        if (isTokenOnly())
            log.info("MQTT login: token only, passwords are refused. The OAuth module checks the tokens{}", anon);
        else if (isTokenLoginOn())
            log.warn("MQTT login: token and password (allowPasswordLogin is on). The OAuth module checks the tokens{}", anon);
        else
            log.info("MQTT login: password{}", anon);

        // tell the operator at start, not only at the first token login
        if (isTokenLoginOn())
        {
            try
            {
                OAuthTokenCheck.checkAvailable();
            }
            catch (LinkageError e)
            {
                log.error(NO_OAUTH_JAR_MSG, e.toString());
            }
        }
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
        if (userID != null && isTokenLoginOn() && isJwt(pwd))
        {
            // the OAuth module checks the token, as it does for HTTP
            var tokenUser = checkToken(pwd);
            if (tokenUser == null)
            {
                log.debug("Token login refused for user {}: the OAuth module did not accept the token (see the log of the OAuth module, or the error above)", userID);
                return null;
            }

            if (!userID.equals(tokenUser.getId()))
            {
                log.debug("Token login refused for user {}: the MQTT username is not equal to the user ID of the token", userID);
                return null;
            }

            // the server closes the connection at this time
            var expiry = tokenExpiry(pwd);
            if (expiry == null)
            {
                log.debug("Token login refused for user {}: the token has no expiry time", userID);
                return null;
            }

            var connId = UUID.randomUUID().toString();
            if (isAccessControlEnabled() && !registerTokenUser(connId, tokenUser))
            {
                log.debug("Token login refused for user {}: not an OSH user", userID);
                return null;
            }
            return new Login(userID, expiry, connId);
        }

        // token login is on: a user logs in with a token only, unless the operator
        // allows passwords. A CONNECT without a user name is not a login: see requireAuth
        if (userID != null && isTokenOnly())
        {
            log.debug("Password login refused for user {}: only token login is allowed", userID);
            return null;
        }

        // check user exists and password matches
        if (userID != null && isAccessControlEnabled())
            return checkPassword(userID, pwd.toCharArray()) ? new Login(userID, null) : null;

        if (!config.requireAuth)
            return new Login(ISecurityManager.ANONYMOUS_USER, null);

        return null;
    }


    /**
     * @return true if the server accepts access tokens as the MQTT password
     */
    boolean isTokenLoginOn()
    {
        return config.tokenAuth != null && config.tokenAuth.enabled;
    }


    /**
     * @return true if a user can log in with a token only: token login is on
     * and the config does not allow password login. This is also true if the
     * OAuth module cannot check tokens (not on the node, not started or
     * settings not valid): the server then refuses all users, it does not go
     * back to passwords.
     */
    boolean isTokenOnly()
    {
        return isTokenLoginOn() && !config.tokenAuth.allowPasswordLogin;
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
     * Ask the OAuth module of the node to check an access token
     * @return the user ID and the roles of the token, or null if the token
     * is refused or the OAuth module cannot check it (it is not on the node
     * or it is not started)
     */
    IUserInfo checkToken(String token)
    {
        try
        {
            var oauth = oauthModule.get();
            if (oauth == null)
            {
                // all token logins fail, so tell the operator
                log.warn("Token login refused: the node has no OAuth module");
                return null;
            }
            return OAuthTokenCheck.checkToken(oauth, token);
        }
        catch (LinkageError e)
        {
            // the jar of the OAuth module is not on the node, or it is too old
            log.error(NO_OAUTH_JAR_MSG, e.toString());
            return null;
        }
    }


    /**
     * @return the autoAddUser setting of the OAuth module: add the user of a
     * token to the OSH users
     */
    boolean autoAddUser()
    {
        try
        {
            var oauth = oauthModule.get();
            return oauth != null && OAuthTokenCheck.autoAddUser(oauth);
        }
        catch (LinkageError e)
        {
            return false;
        }
    }


    /**
     * @return true if the password has the form of a JSON Web Token
     */
    static boolean isJwt(String pwd)
    {
        var header = jwtPart(pwd, 0);
        return header != null && header.has("alg") && jwtPart(pwd, 1) != null;
    }


    /**
     * @return the expiry time of a token that the OAuth module accepted, or
     * null if the token has none
     */
    static Instant tokenExpiry(String token)
    {
        var claims = jwtPart(token, 1);
        if (claims == null || !claims.has("exp"))
            return null;

        try
        {
            return Instant.ofEpochSecond(claims.get("exp").getAsLong());
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }


    /* the header (0) or the claims (1) of a token, or null if the text is not a token */
    static JsonObject jwtPart(String token, int index)
    {
        if (token == null)
            return null;

        var parts = token.split("\\.", -1);
        if (parts.length != 3)
            return null;

        try
        {
            var json = new String(Base64.getUrlDecoder().decode(parts[index]), StandardCharsets.UTF_8);
            var elt = JsonParser.parseString(json);
            return elt.isJsonObject() ? elt.getAsJsonObject() : null;
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }


    /**
     * Put the user of a valid token in the OSH user registry with the roles
     * of the token, so that subscribe and publish find the user. The user
     * stays until the last connection that logged in with a token for it
     * closes (see releaseTokenUser).
     * A user that this server did not add stays as it is: a user of the OSH
     * config, or a user that the OAuth module added for an HTTP login. The
     * login then gets the roles that this user has.
     * @param connId unique ID of the connection
     * @param tokenUser the user that the OAuth module made from the token
     * @return false to refuse the login
     */
    boolean registerTokenUser(String connId, IUserInfo tokenUser)
    {
        // a connection without a user name gets this user: a token must not set its roles
        var userID = tokenUser.getId();
        if (ISecurityManager.ANONYMOUS_USER.equals(userID))
            return false;

        var users = userRegistry();
        synchronized (tokenConnUsers)
        {
            var user = users.get(userID);
            boolean ownUser = user == null || user == tokenUsers.get(userID);

            // a user that a token login added earlier is not a user of OSH
            if (!autoAddUser())
                return user != null && !OAuthTokenCheck.isTokenLoginUser(user);

            if (ownUser)
            {
                // the user in the registry must not have the empty password of an OAuth user
                var newUser = OAuthTokenCheck.newTokenUser(tokenUser);
                users.put(userID, newUser);
                tokenUsers.put(userID, newUser);
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
            // (for example an HTTP login of the same user): then it stays
            if (userRegistry().remove(userID, tokenUsers.remove(userID)))
                log.debug("Token user {} removed: no connection left", userID);
        }
    }


    /**
     * Remove all the users that this server added from the OSH user registry
     */
    void removeTokenUsers()
    {
        synchronized (tokenConnUsers)
        {
            var users = userRegistry();
            tokenUsers.forEach(users::remove);
            tokenUsers.clear();
            tokenConnUsers.clear();
            tokenUserConns.clear();
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
