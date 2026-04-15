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

import org.sensorhub.api.config.DisplayInfo;


/**
 * OAuth 2.0 configuration for the MCP service.
 * <p>
 * Configures the MCP service to proxy OAuth 2.0 authorization server metadata
 * to an external provider (e.g. Keycloak, Auth0). The MCP service serves the
 * RFC 8414 metadata at /.well-known/oauth-authorization-server, pointing clients
 * to the external provider's authorization and token endpoints.
 * </p>
 */
public class McpOAuthConfig
{
    @DisplayInfo(label="Enable OAuth", desc="Enable OAuth 2.0 authentication for MCP clients")
    public boolean enabled = false;

    @DisplayInfo(label="Issuer URL", desc="OAuth 2.0 issuer identifier (e.g. https://keycloak.example.com/realms/myrealm)")
    public String issuer;

    @DisplayInfo(label="Authorization Endpoint", desc="OAuth 2.0 authorization endpoint URL (e.g. https://keycloak.example.com/realms/myrealm/protocol/openid-connect/auth)")
    public String authorizationEndpoint;

    @DisplayInfo(label="Token Endpoint", desc="OAuth 2.0 token endpoint URL (e.g. https://keycloak.example.com/realms/myrealm/protocol/openid-connect/token)")
    public String tokenEndpoint;

    @DisplayInfo(label="JWKS URI", desc="JSON Web Key Set endpoint for token signature verification (e.g. https://keycloak.example.com/realms/myrealm/protocol/openid-connect/certs)")
    public String jwksUri;

    @DisplayInfo(label="Registration Endpoint", desc="OAuth 2.0 dynamic client registration endpoint (optional)")
    public String registrationEndpoint;

    @DisplayInfo(label="Revocation Endpoint", desc="OAuth 2.0 token revocation endpoint (optional)")
    public String revocationEndpoint;

    @DisplayInfo(label="Scopes Supported", desc="Comma-separated list of supported OAuth scopes (e.g. openid,profile,email)")
    public String scopesSupported = "openid,profile";

    @DisplayInfo(label="Service Documentation URL", desc="URL of the service documentation page (optional)")
    public String serviceDocumentation;
}
