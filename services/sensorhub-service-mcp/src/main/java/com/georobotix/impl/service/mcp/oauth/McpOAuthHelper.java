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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public final class McpOAuthHelper
{
    static final String PROTECTED_RESOURCE_WELL_KNOWN = "/.well-known/oauth-protected-resource";
    static final String AUTHORIZATION_SERVER_WELL_KNOWN = "/.well-known/oauth-authorization-server";
    static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private McpOAuthHelper()
    {
    }

    public static String getProtectedResourcePath(String servletPath, String endpoint)
    {
        return PROTECTED_RESOURCE_WELL_KNOWN + servletPath + endpoint;
    }

    public static String getAuthorizationServerPath(String servletPath, String endpoint)
    {
        return AUTHORIZATION_SERVER_WELL_KNOWN + servletPath + endpoint;
    }

    static String buildAuthorizationServerMetadataJson(McpOAuthConfig config)
    {
        var metadata = new LinkedHashMap<String, Object>();
        putIfPresent(metadata, "issuer", config.issuer);
        putIfPresent(metadata, "authorization_endpoint", config.authorizationEndpoint);
        putIfPresent(metadata, "token_endpoint", config.tokenEndpoint);
        putIfPresent(metadata, "jwks_uri", config.jwksUri);
        putIfPresent(metadata, "registration_endpoint", config.registrationEndpoint);
        putIfPresent(metadata, "revocation_endpoint", config.revocationEndpoint);
        putIfPresent(metadata, "service_documentation", config.serviceDocumentation);

        metadata.put("response_types_supported", List.of("code"));
        metadata.put("grant_types_supported", List.of("authorization_code", "refresh_token"));
        metadata.put("token_endpoint_auth_methods_supported", List.of("none", "client_secret_post"));
        metadata.put("code_challenge_methods_supported", List.of("S256"));

        var scopes = parseCommaList(config.scopesSupported);
        if (!scopes.isEmpty())
            metadata.put("scopes_supported", scopes);

        return GSON.toJson(metadata);
    }

    public static String buildProtectedResourceMetadataJson(McpOAuthConfig config, String resourceUri)
    {
        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("resource", canonicalizeResourceUri(resourceUri));

        var authorizationServers = new ArrayList<String>();
        if (config.issuer != null && !config.issuer.isBlank())
            authorizationServers.add(config.issuer.trim());
        metadata.put("authorization_servers", authorizationServers);

        var scopes = parseCommaList(config.scopesSupported);
        if (!scopes.isEmpty())
            metadata.put("scopes_supported", scopes);

        metadata.put("bearer_methods_supported", List.of("header"));
        putIfPresent(metadata, "resource_documentation", config.serviceDocumentation);

        return GSON.toJson(metadata);
    }

    static String buildBearerChallenge(String resourceMetadataUrl, String scope)
    {
        return buildBearerChallenge(resourceMetadataUrl, scope, null, null);
    }

    static String buildBearerChallenge(String resourceMetadataUrl, String scope, String error, String errorDescription)
    {
        var challenge = new StringBuilder("Bearer resource_metadata=\"")
                .append(escapeHeaderValue(resourceMetadataUrl))
                .append("\"");

        if (error != null && !error.isBlank())
            challenge.append(", error=\"").append(escapeHeaderValue(error.trim())).append("\"");

        if (errorDescription != null && !errorDescription.isBlank())
            challenge.append(", error_description=\"").append(escapeHeaderValue(errorDescription.trim())).append("\"");

        if (scope != null && !scope.isBlank())
            challenge.append(", scope=\"").append(escapeHeaderValue(scope.trim())).append("\"");

        return challenge.toString();
    }

    public static void addBearerChallenge(HttpServletResponse resp, String resourceMetadataUrl, String scope)
    {
        addBearerChallenge(resp, resourceMetadataUrl, scope, null, null);
    }

    public static void addBearerChallenge(HttpServletResponse resp, String resourceMetadataUrl, String scope,
                                          String error, String errorDescription)
    {
        if (resourceMetadataUrl != null && !resourceMetadataUrl.isBlank())
            resp.setHeader(WWW_AUTHENTICATE,
                    buildBearerChallenge(resourceMetadataUrl, scope, error, errorDescription));
    }

    static void appendBearerChallenge(HttpServletResponse resp, String resourceMetadataUrl, String scope)
    {
        if (resourceMetadataUrl != null && !resourceMetadataUrl.isBlank())
            resp.addHeader(WWW_AUTHENTICATE, buildBearerChallenge(resourceMetadataUrl, scope));
    }

    static List<String> parseCommaList(String value)
    {
        var values = new ArrayList<String>();
        if (value == null || value.isBlank())
            return values;

        for (var item: value.split(","))
        {
            var trimmed = item.trim();
            if (!trimmed.isEmpty())
                values.add(trimmed);
        }

        return values;
    }

    static String canonicalizeResourceUri(String resourceUri)
    {
        if (resourceUri == null || resourceUri.isBlank())
            return resourceUri;

        try
        {
            var uri = new URI(resourceUri.trim());
            var scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : null;
            var host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : null;
            var path = uri.getRawPath();
            if ("/".equals(path))
                path = "";

            return new URI(
                    scheme,
                    uri.getRawUserInfo(),
                    host,
                    uri.getPort(),
                    path,
                    uri.getRawQuery(),
                    null
            ).toString();
        }
        catch (URISyntaxException e)
        {
            return resourceUri.trim();
        }
    }

    private static void putIfPresent(Map<String, Object> metadata, String name, String value)
    {
        if (value != null && !value.isBlank())
            metadata.put(name, value.trim());
    }

    private static String escapeHeaderValue(String value)
    {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
