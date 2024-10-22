/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2024 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security.oauth;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.security.SecurityModuleConfig;


public class OAuthClientConfig extends SecurityModuleConfig
{
    
    @DisplayInfo(label="Authorization Endpoint", desc="URL of OAuth provider's authorization endpoint")
    public String authzEndpoint;
    
    
    @DisplayInfo(label="Logout Endpoint", desc="URL of OAuth provider's logout endpoint")
    public String logoutEndpoint;
    
    
    @DisplayInfo(label="Token Endpoint", desc="URL of OAuth provider's token endpoint")
    public String tokenEndpoint;
    
    
    @DisplayInfo(label="User Info Endpoint", desc="URL of OAuth provider's user info endpoint")
    public String userInfoEndpoint;
    
    
    @DisplayInfo(desc="Client ID as provided by your OAuth provider")
    public String clientID;
    
    
    @DisplayInfo(desc="Client Secret as provided by your OAuth provider")
    public String clientSecret;
    
    
    @DisplayInfo(label="OAuth Scope", desc="Defines what information OSH will have access to")
    public String authzScope;
    
    
    @Required
    @DisplayInfo(desc="Path of JSON attribute to use as OSH user ID")
    public String userIdField = "id";
    
    
    @DisplayInfo(desc="Path of JSON attribute containing the user roles")
    public String userRolesField = "";
    
    
    @DisplayInfo(desc="Automatically create users with roles returned by the identity provider")
    public boolean autoAddUser = true;

    @DisplayInfo(label="Client Credentials Token", desc="Configuration options for authenticating client credentials token")
    public OAuthBearerTokenConfig bearerTokenConfig = new OAuthBearerTokenConfig();
}
