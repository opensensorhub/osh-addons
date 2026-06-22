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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Adds MCP OAuth resource metadata discovery to unauthorized MCP responses.
 */
public class McpOAuthChallengeHandler extends AbstractHandler
{
    private final String endpointPath;
    private final String resourceMetadataUrl;
    private final String requiredScopes;

    public McpOAuthChallengeHandler(String endpointPath, String resourceMetadataUrl, String requiredScopes)
    {
        this.endpointPath = endpointPath;
        this.resourceMetadataUrl = resourceMetadataUrl;
        this.requiredScopes = requiredScopes;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        if (target != null && target.startsWith(endpointPath) &&
                response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED &&
                response.getHeader(McpOAuthHelper.WWW_AUTHENTICATE) == null)
        {
            McpOAuthHelper.appendBearerChallenge(response, resourceMetadataUrl, requiredScopes);
        }
    }
}
