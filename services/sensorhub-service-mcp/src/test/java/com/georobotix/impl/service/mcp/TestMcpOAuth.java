package com.georobotix.impl.service.mcp;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class TestMcpOAuth
{
    static final int SERVER_PORT = 8889;
    static final long TIMEOUT = 10000;

    static final String TEST_ISSUER = "https://keycloak.example.com/realms/test";
    static final String TEST_AUTH_ENDPOINT = "https://keycloak.example.com/realms/test/protocol/openid-connect/auth";
    static final String TEST_TOKEN_ENDPOINT = "https://keycloak.example.com/realms/test/protocol/openid-connect/token";
    static final String TEST_JWKS_URI = "https://keycloak.example.com/realms/test/protocol/openid-connect/certs";
    static final String TEST_REGISTRATION_ENDPOINT = "https://keycloak.example.com/realms/test/clients-registrations/openid-connect";
    static final String TEST_SCOPES = "openid,profile,mcp";

    SensorHub hub;
    String metadataUrl;


    @Before
    public void setup() throws SensorHubException
    {
        hub = new SensorHub();
        hub.start();
        var moduleRegistry = hub.getModuleRegistry();

        // Start HTTP server
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.httpPort = SERVER_PORT;
        var httpServer = (HttpServer) moduleRegistry.loadModule(httpConfig, TIMEOUT);

        // Start MCP service with OAuth enabled
        McpServiceConfig mcpCfg = new McpServiceConfig();
        mcpCfg.autoStart = true;
        mcpCfg.oauth.enabled = true;
        mcpCfg.oauth.issuer = TEST_ISSUER;
        mcpCfg.oauth.authorizationEndpoint = TEST_AUTH_ENDPOINT;
        mcpCfg.oauth.tokenEndpoint = TEST_TOKEN_ENDPOINT;
        mcpCfg.oauth.jwksUri = TEST_JWKS_URI;
        mcpCfg.oauth.registrationEndpoint = TEST_REGISTRATION_ENDPOINT;
        mcpCfg.oauth.scopesSupported = TEST_SCOPES;
        // Leave revocationEndpoint and serviceDocumentation null to test omission
        moduleRegistry.loadModule(mcpCfg, TIMEOUT);

        metadataUrl = httpServer.getPublicEndpointUrl(
                "/.well-known/oauth-authorization-server" + mcpCfg.endPoint);
    }


    @After
    public void cleanup()
    {
        if (hub != null)
        {
            try { hub.stop(); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }


    // ==================== Helper Methods ====================

    private HttpResponse<String> sendGet(String url) throws IOException
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(url))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
    }


    private HttpResponse<String> sendOptions(String url) throws IOException
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .uri(URI.create(url))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
    }


    private JsonObject getMetadataJson() throws IOException
    {
        var resp = sendGet(metadataUrl);
        assertEquals(200, resp.statusCode());
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }


    // ==================== Tests ====================

    @Test
    public void testOAuthMetadataEndpointReturnsJson() throws Exception
    {
        var resp = sendGet(metadataUrl);
        assertEquals(200, resp.statusCode());

        var contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertTrue("Content-Type should be application/json, was: " + contentType,
                contentType.contains("application/json"));

        // Should parse as valid JSON
        JsonParser.parseString(resp.body()).getAsJsonObject();
    }


    @Test
    public void testOAuthMetadataRequiredFields() throws Exception
    {
        var json = getMetadataJson();

        assertEquals(TEST_ISSUER, json.get("issuer").getAsString());
        assertEquals(TEST_AUTH_ENDPOINT, json.get("authorization_endpoint").getAsString());
        assertEquals(TEST_TOKEN_ENDPOINT, json.get("token_endpoint").getAsString());

        // RFC 8414 required arrays
        JsonArray responseTypes = json.getAsJsonArray("response_types_supported");
        assertEquals(1, responseTypes.size());
        assertEquals("code", responseTypes.get(0).getAsString());

        JsonArray grantTypes = json.getAsJsonArray("grant_types_supported");
        assertEquals(2, grantTypes.size());
        assertEquals("authorization_code", grantTypes.get(0).getAsString());
        assertEquals("refresh_token", grantTypes.get(1).getAsString());

        JsonArray authMethods = json.getAsJsonArray("token_endpoint_auth_methods_supported");
        assertEquals(2, authMethods.size());
        assertEquals("client_secret_post", authMethods.get(0).getAsString());
        assertEquals("none", authMethods.get(1).getAsString());

        // PKCE (required by MCP spec)
        JsonArray codeChallenges = json.getAsJsonArray("code_challenge_methods_supported");
        assertEquals(1, codeChallenges.size());
        assertEquals("S256", codeChallenges.get(0).getAsString());
    }


    @Test
    public void testOAuthMetadataOptionalFields() throws Exception
    {
        var json = getMetadataJson();

        assertEquals(TEST_JWKS_URI, json.get("jwks_uri").getAsString());
        assertEquals(TEST_REGISTRATION_ENDPOINT, json.get("registration_endpoint").getAsString());
    }


    @Test
    public void testOAuthMetadataOmitsBlankOptionalFields() throws Exception
    {
        var json = getMetadataJson();

        // These were not set in the config, so they should be absent
        assertNull("revocation_endpoint should be absent", json.get("revocation_endpoint"));
        assertNull("service_documentation should be absent", json.get("service_documentation"));
    }


    @Test
    public void testOAuthMetadataScopesParsing() throws Exception
    {
        var json = getMetadataJson();

        JsonArray scopes = json.getAsJsonArray("scopes_supported");
        assertNotNull("scopes_supported should be present", scopes);
        assertEquals(3, scopes.size());
        assertEquals("openid", scopes.get(0).getAsString());
        assertEquals("profile", scopes.get(1).getAsString());
        assertEquals("mcp", scopes.get(2).getAsString());
    }


    @Test
    public void testOAuthMetadataCorsHeaders() throws Exception
    {
        var resp = sendGet(metadataUrl);
        assertEquals(200, resp.statusCode());

        assertEquals("*",
                resp.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals("GET, OPTIONS",
                resp.headers().firstValue("Access-Control-Allow-Methods").orElse(""));
        assertEquals("Content-Type, Authorization",
                resp.headers().firstValue("Access-Control-Allow-Headers").orElse(""));
    }


    @Test
    public void testOAuthMetadataCacheHeaders() throws Exception
    {
        var resp = sendGet(metadataUrl);
        assertEquals(200, resp.statusCode());

        assertEquals("public, max-age=3600",
                resp.headers().firstValue("Cache-Control").orElse(""));
    }


    @Test
    public void testOAuthMetadataOptionsPreflightCors() throws Exception
    {
        var resp = sendOptions(metadataUrl);
        assertEquals(200, resp.statusCode());

        assertEquals("*",
                resp.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals("GET, OPTIONS",
                resp.headers().firstValue("Access-Control-Allow-Methods").orElse(""));
        assertEquals("Content-Type, Authorization",
                resp.headers().firstValue("Access-Control-Allow-Headers").orElse(""));
    }


    @Test
    public void testOAuthMetadataNoAuthRequired() throws Exception
    {
        // Send a plain GET with no Authorization header — should succeed
        var resp = sendGet(metadataUrl);
        assertEquals("OAuth metadata endpoint should not require auth",
                200, resp.statusCode());
    }


    @Test
    public void testOAuthDisabledNoEndpoint() throws Exception
    {
        // Stop current hub
        hub.stop();

        // Start a fresh hub with OAuth disabled
        hub = new SensorHub();
        hub.start();
        var moduleRegistry = hub.getModuleRegistry();

        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.httpPort = SERVER_PORT;
        var httpServer = (HttpServer) moduleRegistry.loadModule(httpConfig, TIMEOUT);

        McpServiceConfig mcpCfg = new McpServiceConfig();
        mcpCfg.autoStart = true;
        mcpCfg.oauth.enabled = false;
        moduleRegistry.loadModule(mcpCfg, TIMEOUT);

        var disabledUrl = httpServer.getPublicEndpointUrl(
                "/.well-known/oauth-authorization-server" + mcpCfg.endPoint);

        var resp = sendGet(disabledUrl);
        assertEquals("OAuth metadata endpoint should not exist when disabled",
                404, resp.statusCode());
    }
}
