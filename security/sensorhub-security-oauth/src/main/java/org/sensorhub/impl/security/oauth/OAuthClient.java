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

import org.eclipse.jetty.security.Authenticator;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.IHttpServer;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.service.HttpServerConfig;


/**
 * <p>
 * OAuth Client module relying on Apache Oltu library
 * </p>
 *
 * @author Alex Robin
 * @since Nov 29, 2016
 */
public class OAuthClient extends AbstractModule<OAuthClientConfig>
{
    Authenticator authenticator;
    
    
    @Override
    public void setConfiguration(OAuthClientConfig config)
    {
        super.setConfiguration(config);
    }
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
        var httpServer = getParentHub().getModuleRegistry().getModuleByType(IHttpServer.class);
        if (httpServer == null)
            throw new SensorHubException("HTTP server module is not loaded");
        
        var callbackBaseUrl = httpServer.getServerBaseUrl();
        var httpConfig = (HttpServerConfig)httpServer.getConfiguration();
        authenticator = new OAuthAuthenticator(config, callbackBaseUrl, httpConfig.enableCORS, getLogger());
        
        getParentHub().getSecurityManager().registerAuthenticator(authenticator);
    }


    @Override
    protected void doStop() throws SensorHubException
    {
        //getParentHub().getSecurityManager().registerAuthenticator(null);
        this.authenticator = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {
    }
}
