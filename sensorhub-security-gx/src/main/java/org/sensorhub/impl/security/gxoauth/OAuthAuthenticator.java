/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security.gxoauth;

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
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
import com.google.gson.stream.JsonReader;


public class OAuthAuthenticator extends LoginAuthenticator
{
    private static final String AUTH_METHOD_OAUTH2 = "OAUTH2";
    
    private Logger log;
    private OAuthClientConfig config;
    private String generatedState;
    

    public OAuthAuthenticator(OAuthClientConfig config, Logger log)
    {
        this.log = log;
        this.config = config;
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
            String redirectUrl = config.redirectURL != null ? config.redirectURL : request.getRequestURL().toString();
            HttpSession session = request.getSession(true);
            
            // check for cached auth
            Authentication cachedAuth = (Authentication) session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
            if (cachedAuth != null && cachedAuth instanceof Authentication.User)
            {
                if (!_loginService.validate(((Authentication.User)cachedAuth).getUserIdentity()))
                    session.removeAttribute(SessionAuthentication.__J_AUTHENTICATED);
                else
                    return cachedAuth;
            }
            
            // if calling back from provider with auth code
            if (generatedState != null && request.getParameter(OAuth.OAUTH_CODE) != null)
            {
                try
                {
                    // first request temporary access token
                    OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
                    String code = oar.getCode();
                    log.debug("OAuth Code = " + code);
                    
                    // check state parameter
                    if (!generatedState.equals(oar.getState()))
                        throw OAuthProblemException.error("Invalid state parameter");

                    OAuthClientRequest authRequest = OAuthClientRequest.tokenLocation(config.tokenEndpoint).setCode(code)
                            .setRedirectURI(redirectUrl).setGrantType(GrantType.AUTHORIZATION_CODE).buildBodyMessage();

                    String authString = config.clientID +":"+config.clientSecret;
                    byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
                    String authStringEnc = new String(authEncBytes);

                    authRequest.addHeader("Authorization", "Basic "+authStringEnc);
                    authRequest.addHeader("Accept", "application/json");

                    OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
                    OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(authRequest, HttpMethod.POST);

                    // read access token and store in session
                    String accessToken = oAuthResponse.getAccessToken();
                    log.debug("OAuth Token = " + accessToken);

                    // request user info
                    OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(config.userInfoEndpoint).setAccessToken(accessToken).buildQueryMessage();
                    bearerClientRequest.addHeader("Authorization", accessToken);
                    OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, HttpMethod.GET, OAuthResourceResponse.class);

                    // parse user info
                    log.debug("UserInfo = " + resourceResponse.getBody());
                    JsonReader jsonReader = new JsonReader(new StringReader(resourceResponse.getBody()));
                    String userId = parseUserInfoJson(jsonReader);
                    
                    // login and return UserAuth object
                    UserIdentity user = login(userId, "NONE", req);
                    if (user != null)
                    {
                        UserAuthentication userAuth = new UserAuthentication(getAuthMethod(), user);
                        session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, userAuth);
                        return userAuth;
                    }
                    
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return Authentication.SEND_FAILURE;
                }
                catch (OAuthProblemException | OAuthSystemException | IOException e)
                {
                    log.error("Cannot complete authentication at endpoint " + config.tokenEndpoint, e);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                    return Authentication.SEND_FAILURE;
                }
            }

            // first login at auth provider
            else
            {
                try
                {
                    // generate request to auth provider
                    this.generatedState = UUID.randomUUID().toString();
                    OAuthClientRequest authRequest = OAuthClientRequest.authorizationLocation(config.authzEndpoint).setClientId(config.clientID).setRedirectURI(redirectUrl)
                            .setResponseType(OAuth.OAUTH_CODE).setScope(config.authzScope).setState(generatedState).buildQueryMessage();

                    // send as redirect
                    String loginUrl = authRequest.getLocationUri();
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
        }
        catch (IOException e)
        {
            log.error("Cannot send HTTP error", e);
            return Authentication.SEND_FAILURE;
        }
    }


    private String parseUserInfoJson(JsonReader reader) throws IOException
    {
        String userId = null;

        reader.beginObject();        
        while (reader.hasNext())
        {
            String name = reader.nextName();
            if ("id".equals(name) || "user_id".equals(name) || "uid".equals(name))
                userId = reader.nextString();
            else
                reader.skipValue();
        }        
        reader.endObject();
        
        return userId;
    }
}
