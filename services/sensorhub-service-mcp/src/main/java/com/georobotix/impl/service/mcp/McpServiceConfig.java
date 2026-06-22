/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp;

import com.georobotix.impl.service.mcp.oauth.McpOAuthConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.service.HttpServiceConfig;


public class McpServiceConfig extends HttpServiceConfig
{
    @DisplayInfo(label="Write Database ID", desc="Module ID of the database to use for write operations (observations, commands). If null, writes are disabled.")
    public String writeDatabaseId = null;


    @DisplayInfo(desc="Security related options")
    public SecurityConfig security = new SecurityConfig();


    @DisplayInfo(label="OAuth Configuration", desc="OAuth 2.0 configuration for MCP client authentication (proxies to external provider)")
    public McpOAuthConfig oauth = new McpOAuthConfig();

    public McpServiceConfig()
    {
        this.moduleClass = McpService.class.getCanonicalName();
        this.endPoint = "/mcp";
    }
}
