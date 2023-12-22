/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security.oauth;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.OAuth.HttpMethod;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.UserIdentity;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HttpHeaders;
import com.google.gson.stream.JsonReader;


public class OAuthAuthenticator extends LoginAuthenticator
{
    private static final String AUTH_METHOD_OAUTH2 = "OAUTH2";
    private static final String OAUTH_CODE_CALLBACK_PATH = "/oauthcode";
    private static final String LOGOUT_PATH = "/logout";
    
    private Logger log;
    private OAuthClientConfig config;
    private String serverBaseUrl = null;
    private Map<String, OAuthState> generatedState;
    
    
    static class OAuthState
    {
        String redirectUrl;
        
        OAuthState(String redirectUrl)
        {
            this.redirectUrl = redirectUrl;
        }
    }
    

    public OAuthAuthenticator(OAuthClientConfig config, String serverBaseUrl, Logger log)
    {
        this.log = Asserts.checkNotNull(log, Logger.class);
        
        this.config = Asserts.checkNotNull(config, OAuthClientConfig.class);
        Asserts.checkNotNullOrEmpty("The userIdField config property must be configured", config.userIdField);
        
        if (!serverBaseUrl.contains("localhost"))
        {
            this.serverBaseUrl = serverBaseUrl.endsWith("/") ?
                serverBaseUrl.substring(0, serverBaseUrl.length()-1) : serverBaseUrl;
        }
        
        this.generatedState = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .<String, OAuthState>build()
            .asMap();
    }
    
    
    @Override
    public String getAuthMethod()
    {
        return AUTH_METHOD_OAUTH2;
    }


    @Override
    public void prepareRequest(ServletRequest arg0)
    {
        // nothing to prepare
    }


    @Override
    public boolean secureResponse(ServletRequest arg0, ServletResponse arg1, boolean arg2, User arg3) throws ServerAuthException
    {
        return false;
    }


    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse resp, boolean mandatory) throws ServerAuthException
    {
        try
        {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;
            
            // catch logout case
            if (request.getServletPath() != null && LOGOUT_PATH.equals(request.getServletPath()))
            {
                try
                {
                    request.logout();
                    var session = request.getSession(false);
                    if (session != null)
                        session.invalidate();
                    
                    log.debug("Log out from auth provider @ " + config.logoutEndpoint);
                    var adminUrl = request.getRequestURL().toString().replace(request.getServletPath(), "/admin");
                    response.sendRedirect(config.logoutEndpoint + "?redirect_uri=" + adminUrl);
                    return Authentication.SEND_CONTINUE;
                }
                catch (ServletException e)
                {
                    log.error("Error while logging out", e);
                    return Authentication.SEND_FAILURE;
                }
            }
        
            // check for cached session
            HttpSession session = request.getSession(true);
            var cachedSession = session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
            if (cachedSession != null && cachedSession instanceof Authentication.User)
            {
                if (!_loginService.validate(((Authentication.User)cachedSession).getUserIdentity()))
                    session.removeAttribute(SessionAuthentication.__J_AUTHENTICATED);
                else
                    return (Authentication.User)cachedSession;
            }
            
            
            String accessToken = null;
            String idToken = null;
            String postLoginRedirectUrl = null;
            String oauthCallbackUrl = getCallbackBaseUrl(request) + OAUTH_CODE_CALLBACK_PATH;
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            
            // if calling back from provider with auth code, get access token
            if (OAUTH_CODE_CALLBACK_PATH.equals(request.getServletPath()) &&
                request.getParameter(OAuth.OAUTH_STATE) != null)
            {
                String code = null;
                OAuthState state = null;
                try
                {
                    // decode request received from auth provider
                    OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
                    
                    // check state parameter
                    // only continue if state is valid
                    if ((state = generatedState.remove(oar.getState())) != null)
                    {
                        code = oar.getCode();
                        postLoginRedirectUrl = state.redirectUrl;
                        log.debug("OAuth Code={}, Redirect={}", code, postLoginRedirectUrl);
                    }
                    else
                    {
                        log.error("Invalid or expired state in oauth callback: {}", oar.getState());
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return Authentication.SEND_FAILURE;
                    }
                }
                catch (OAuthProblemException e)
                {
                    log.error("Bad auth code callback: {} - {}", e.getError(), e.getDescription());
                }
                
                try
                {
                    if (code != null)
                    {
                        OAuthClientRequest authRequest = OAuthClientRequest
                            .tokenLocation(config.tokenEndpoint)
                            .setGrantType(GrantType.AUTHORIZATION_CODE)
                            .setCode(code)
                            .setClientId(config.clientID)
                            .setClientSecret(config.clientSecret)
                            .setRedirectURI(oauthCallbackUrl)
                            .buildBodyMessage();
                        authRequest.addHeader("Accept", "application/json");
    
                        OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(authRequest, HttpMethod.POST);
                        log.debug("Token Endpoint Response: {}", oAuthResponse.getBody());
                        
                        idToken = oAuthResponse.getParam("id_token");
                        accessToken = oAuthResponse.getAccessToken();
                    }
                }
                catch (OAuthProblemException e)
                {
                    log.error("Error received from token endpoint: {} - {}", e.getError(), e.getDescription());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return Authentication.SEND_FAILURE;
                }
                catch (OAuthSystemException e)
                {
                    log.error("Error while requesting access token from " + config.tokenEndpoint, e);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                    return Authentication.SEND_FAILURE;
                }
            }
            
            // if no OAuth token received check if credentials are provided in authorization header
            // and use back channel direct auth flow
            if (accessToken == null && authHeader != null && !request.getServletPath().equals("/admin"))
            {
                try
                {
                    var credentials = parseBasicAuth(authHeader);
                    if (credentials == null)
                    {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return Authentication.SEND_FAILURE;
                    }
                    
                    // try to execute direct grant flow
                    OAuthClientRequest authRequest = OAuthClientRequest
                        .tokenLocation(config.tokenEndpoint)
                        .setGrantType(GrantType.PASSWORD)
                        .setClientId(config.clientID)
                        .setClientSecret(config.clientSecret)
                        .setUsername(credentials[0])
                        .setPassword(credentials[1])
                        .buildBodyMessage();
                    authRequest.addHeader("Accept", "application/json");

                    OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(authRequest, HttpMethod.POST);
                    accessToken = oAuthResponse.getAccessToken();
                }
                catch (OAuthProblemException e)
                {
                    if (!mandatory)
                        return Authentication.UNAUTHENTICATED;
                    
                    log.error("Error received from token endpoint: {} - {}", e.getError(), e.getDescription());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return Authentication.SEND_FAILURE;
                }
                catch (OAuthSystemException e)
                {
                    log.error("Error while requesting access token from " + config.tokenEndpoint, e);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                    return Authentication.SEND_FAILURE;
                }
            }
            
            // if token was obtained, get user info
            if (accessToken != null)
            {
                try
                {
                    String userId = null;
                    
                    if (idToken != null)
                    {
                        log.debug("ID Token = " + idToken);
                        
                        String[] chunks = idToken.split("\\.");
                        Base64.Decoder decoder = Base64.getUrlDecoder();
                        String header = new String(decoder.decode(chunks[0]));
                        String payload = new String(decoder.decode(chunks[1]));
                        log.debug("ID Token Header = " + header);
                        log.debug("ID Token Payload = " + payload);
                        
                        JsonReader jsonReader = new JsonReader(new StringReader(payload));
                        userId = parseUserInfoJson(jsonReader);
                    }
                    else
                    {
                        log.debug("OAuth Token = " + accessToken);
                        
                        // request user info
                        OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(config.userInfoEndpoint)
                            .setAccessToken(accessToken)
                            //.buildQueryMessage();
                            .buildHeaderMessage();
                        OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, HttpMethod.GET, OAuthResourceResponse.class);
    
                        // parse user info
                        log.debug("UserInfo = " + resourceResponse.getBody());
                        JsonReader jsonReader = new JsonReader(new StringReader(resourceResponse.getBody()));
                        userId = parseUserInfoJson(jsonReader);
                    }
                    
                    // login and return UserAuth object
                    UserIdentity user = login(userId, "", req);
                    if (user != null)
                    {
                        UserAuthentication userAuth = new UserAuthentication(getAuthMethod(), user);
                        session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, userAuth);
                        if (postLoginRedirectUrl != null)
                        {
                            response.sendRedirect(response.encodeRedirectURL(postLoginRedirectUrl));
                            return Authentication.SEND_CONTINUE;
                        }
                        else
                            //return Authentication.SEND_SUCCESS;
                            return userAuth;
                    }
                    else
                    {
                        log.error("Unknown user: {}", userId);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return Authentication.SEND_FAILURE;
                    }
                }
                catch (OAuthProblemException e)
                {
                    log.error("Error received from authorization endpoint: {} - {}", e.getError(), e.getDescription());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return Authentication.SEND_FAILURE;
                }
                catch (OAuthSystemException | IOException e)
                {
                    log.error("Cannot complete authentication at endpoint " + config.tokenEndpoint, e);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                    return Authentication.SEND_FAILURE;
                }
            }
            else if (!mandatory)
            {
                // case of auth not needed
                return Authentication.NOT_CHECKED;
            }

            // else redirect to auth provider endpoint for first login 
            try
            {
                // generate request to auth provider
                var state = UUID.randomUUID().toString();
                
                postLoginRedirectUrl = getCallbackBaseUrl(request)
                    + (request.getServletPath() != null ? request.getServletPath() : "")
                    + (request.getPathInfo() != null ? request.getPathInfo() : "")
                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
                    
                generatedState.put(state, new OAuthState(postLoginRedirectUrl));
                OAuthClientRequest authRequest = OAuthClientRequest.authorizationLocation(config.authzEndpoint)
                    .setClientId(config.clientID)
                    .setRedirectURI(oauthCallbackUrl)
                    .setResponseType(OAuth.OAUTH_CODE)
                    .setScope(config.authzScope)
                    .setState(state)
                    .buildQueryMessage();

                // send as redirect
                String loginUrl = authRequest.getLocationUri();
                log.debug("Redirecting to auth provider login @ " + loginUrl);
                response.sendRedirect(response.encodeRedirectURL(loginUrl));
                return Authentication.SEND_CONTINUE;
            }
            catch (OAuthSystemException e)
            {
                log.error("Cannot redirect to authentication endpoint " + config.authzEndpoint, e);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                return Authentication.SEND_FAILURE;
            }
        }
        catch (IOException e)
        {
            log.error("Cannot send HTTP error", e);
            return Authentication.SEND_FAILURE;
        }
    }
    
    
    private String getCallbackBaseUrl(HttpServletRequest request)
    {
        if (serverBaseUrl == null)
        {
            var requestUrl = request.getRequestURL();
            return requestUrl.substring(0, requestUrl.indexOf(request.getServletPath()));
        }
        else
            return serverBaseUrl + request.getContextPath();
    }
    
    
    private String[] parseBasicAuth(String credentials)
    {
        int space = credentials.indexOf(' ');
        if (space > 0)
        {
            String method = credentials.substring(0, space);
            if ("basic".equalsIgnoreCase(method))
            {
                credentials = credentials.substring(space + 1);
                credentials = new String(Base64.getDecoder().decode(credentials), StandardCharsets.ISO_8859_1);
                int i = credentials.indexOf(':');
                if (i > 0)
                {
                    String username = credentials.substring(0, i);
                    String password = credentials.substring(i + 1);
                    return new String[] {username, password};
                }
            }
        }
        
        return null;
    }


    private String parseUserInfoJson(JsonReader reader) throws IOException
    {
        String userId = null;

        reader.beginObject();
        while (reader.hasNext())
        {
            String name = reader.nextName();
            if (config.userIdField.equals(name))
                userId = reader.nextString();
            else
                reader.skipValue();
        }        
        reader.endObject();
        
        return userId;
    }
}
