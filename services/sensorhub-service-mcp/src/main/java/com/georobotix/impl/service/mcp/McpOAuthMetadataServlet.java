/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2024 Botts Innovative Research, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Servlet that serves OAuth 2.0 Authorization Server Metadata (RFC 8414)
 * at the well-known endpoint. This enables MCP clients (like Claude Code)
 * to discover the OAuth provider's endpoints for authentication.
 * <p>
 * Deployed at: /.well-known/oauth-authorization-server
 * Must be accessible WITHOUT authentication.
 * </p>
 */
public class McpOAuthMetadataServlet extends HttpServlet
{
    private final String metadataJson;

    public McpOAuthMetadataServlet(McpOAuthConfig oauthConfig)
    {
        this.metadataJson = buildMetadataJson(oauthConfig);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // CORS headers for OAuth discovery
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Cache-Control", "public, max-age=3600");

        resp.getWriter().write(metadataJson);
        resp.getWriter().flush();
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static String buildMetadataJson(McpOAuthConfig config)
    {
        var sb = new StringBuilder();
        sb.append("{\n");
        appendField(sb, "issuer", config.issuer, true);
        appendField(sb, "authorization_endpoint", config.authorizationEndpoint, true);
        appendField(sb, "token_endpoint", config.tokenEndpoint, true);

        if (config.jwksUri != null && !config.jwksUri.isBlank())
            appendField(sb, "jwks_uri", config.jwksUri, true);

        if (config.registrationEndpoint != null && !config.registrationEndpoint.isBlank())
            appendField(sb, "registration_endpoint", config.registrationEndpoint, true);

        if (config.revocationEndpoint != null && !config.revocationEndpoint.isBlank())
            appendField(sb, "revocation_endpoint", config.revocationEndpoint, true);

        if (config.serviceDocumentation != null && !config.serviceDocumentation.isBlank())
            appendField(sb, "service_documentation", config.serviceDocumentation, true);

        // Response types supported (required by RFC 8414)
        sb.append("  \"response_types_supported\": [\"code\"],\n");

        // Grant types supported
        sb.append("  \"grant_types_supported\": [\"authorization_code\", \"refresh_token\"],\n");

        // Token endpoint auth methods
        sb.append("  \"token_endpoint_auth_methods_supported\": [\"client_secret_post\", \"none\"],\n");

        // PKCE support (required by MCP spec)
        sb.append("  \"code_challenge_methods_supported\": [\"S256\"],\n");

        // Scopes
        if (config.scopesSupported != null && !config.scopesSupported.isBlank()) {
            sb.append("  \"scopes_supported\": [");
            var scopes = config.scopesSupported.split(",");
            for (int i = 0; i < scopes.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("\"").append(scopes[i].trim()).append("\"");
            }
            sb.append("]\n");
        }

        sb.append("}");
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String name, String value, boolean comma)
    {
        if (value == null) return;
        sb.append("  \"").append(name).append("\": \"").append(value).append("\"");
        if (comma) sb.append(",");
        sb.append("\n");
    }
}
