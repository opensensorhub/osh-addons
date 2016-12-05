/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security.oauth;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * OAuth Client module relying on Apache Oltu library
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 29, 2016
 */
public class OAuthClient extends AbstractModule<OAuthClientConfig> implements Authenticator
{
    Authenticator authenticator;
    
    
    @Override
    public void setConfiguration(OAuthClientConfig config)
    {
        super.setConfiguration(config);        
        authenticator = new OAuthAuthenticator(config, getLogger());
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        SensorHub.getInstance().getSecurityManager().registerAuthenticator(this);
    }


    @Override
    public void stop() throws SensorHubException
    {
        SensorHub.getInstance().getSecurityManager().registerAuthenticator(authenticator);
        this.authenticator = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {
    }


    @Override
    public void setConfiguration(AuthConfiguration configuration)
    {
        authenticator.setConfiguration(configuration);
    }


    @Override
    public String getAuthMethod()
    {
        return authenticator.getAuthMethod();
    }


    @Override
    public void prepareRequest(ServletRequest request)
    {
        authenticator.prepareRequest(request);        
    }


    @Override
    public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory) throws ServerAuthException
    {
        return authenticator.validateRequest(request, response, mandatory);
    }


    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, User validatedUser) throws ServerAuthException
    {
        return authenticator.secureResponse(request, response, mandatory, validatedUser);
    }

}
