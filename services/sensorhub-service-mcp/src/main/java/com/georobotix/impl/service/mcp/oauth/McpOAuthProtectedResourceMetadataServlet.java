/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp.oauth;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Serves OAuth 2.0 Protected Resource Metadata for MCP authorization discovery.
 */
public class McpOAuthProtectedResourceMetadataServlet extends HttpServlet
{
    private final String metadataJson;

    public McpOAuthProtectedResourceMetadataServlet(McpOAuthConfig oauthConfig, String resourceUri)
    {
        this.metadataJson = McpOAuthHelper.buildProtectedResourceMetadataJson(oauthConfig, resourceUri);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        setCorsAndCacheHeaders(resp);

        resp.getWriter().write(metadataJson);
        resp.getWriter().flush();
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
    {
        resp.setStatus(HttpServletResponse.SC_OK);
        setCorsAndCacheHeaders(resp);
    }

    private void setCorsAndCacheHeaders(HttpServletResponse resp)
    {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Cache-Control", "public, max-age=3600");
    }
}
