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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;


/**
 * Validates RS256 JWT bearer tokens for the MCP resource server role.
 */
public class McpBearerTokenValidator
{
    private static final Logger log = LoggerFactory.getLogger(McpBearerTokenValidator.class);
    private static final long JWKS_CACHE_MILLIS = 5 * 60 * 1000L;

    private final McpOAuthConfig config;
    private final String resourceUri;
    private final Set<String> acceptedAudienceValues;
    private final HttpClient httpClient;
    private final Clock clock;

    private volatile JsonArray cachedJwks;
    private volatile long cachedAtMillis;

    public McpBearerTokenValidator(McpOAuthConfig config, String resourceUri)
    {
        this(config, resourceUri, HttpClient.newHttpClient(), Clock.systemUTC());
    }

    McpBearerTokenValidator(McpOAuthConfig config, String resourceUri, HttpClient httpClient, Clock clock)
    {
        this.config = config;
        this.resourceUri = McpOAuthHelper.canonicalizeResourceUri(resourceUri);
        this.acceptedAudienceValues = buildAcceptedAudienceValues(resourceUri);
        this.httpClient = httpClient;
        this.clock = clock;
    }

    public boolean isValid(String token)
    {
        return validate(token).isValid();
    }

    public ValidationResult validate(String token)
    {
        try
        {
            if (token == null || token.isBlank())
                return invalid("missing bearer token");

            var parts = token.split("\\.");
            if (parts.length != 3)
                return invalid("bearer token is not a JWT");

            var header = JsonParser.parseString(decodeBase64Url(parts[0])).getAsJsonObject();
            var claims = JsonParser.parseString(decodeBase64Url(parts[1])).getAsJsonObject();

            if (!"RS256".equals(getString(header, "alg")))
                return invalid("unsupported JWT algorithm: " + getString(header, "alg"));

            var kid = getString(header, "kid");
            if (kid == null || kid.isBlank())
                return invalid("JWT header does not contain kid");

            var key = findKey(kid);
            if (key == null)
                return invalid("JWKS does not contain key id: " + kid);

            if (!verifySignature(parts, key))
                return invalid("JWT signature verification failed");

            return validateClaims(claims);
        }
        catch (Exception e)
        {
            log.debug("Bearer token validation failed", e);
            return invalid("bearer token validation failed");
        }
    }

    private ValidationResult validateClaims(JsonObject claims)
    {
        if (config.issuer != null && !config.issuer.isBlank() &&
                !config.issuer.equals(getString(claims, "iss")))
            return invalid("issuer mismatch. expected=" + config.issuer + ", actual=" + getString(claims, "iss"));

        long now = clock.instant().getEpochSecond();
        if (!claims.has("exp") || claims.get("exp").getAsLong() <= now)
            return invalid("token is expired or missing exp");

        if (claims.has("nbf") && claims.get("nbf").getAsLong() > now)
            return invalid("token is not valid yet");

        if (!hasAudience(claims.get("aud")))
            return invalid("audience mismatch. expected=" + resourceUri + ", actual=" + claims.get("aud"));

        if (!hasRequiredScopes(claims))
            return invalid("required scopes are missing. required=" + parseRequiredScopes());

        var subject = getString(claims, "preferred_username");
        if (subject == null || subject.isBlank())
            subject = getString(claims, "email");
        if (subject == null || subject.isBlank())
            subject = getString(claims, "sub");

        return ValidationResult.valid(subject);
    }

    private boolean hasAudience(JsonElement audience)
    {
        if (audience == null || audience.isJsonNull())
            return false;

        if (audience.isJsonPrimitive())
            return acceptedAudienceValues.contains(McpOAuthHelper.canonicalizeResourceUri(audience.getAsString()));

        if (audience.isJsonArray())
        {
            for (var item: audience.getAsJsonArray())
            {
                if (acceptedAudienceValues.contains(McpOAuthHelper.canonicalizeResourceUri(item.getAsString())))
                    return true;
            }
        }

        return false;
    }

    private boolean hasRequiredScopes(JsonObject claims)
    {
        var requiredScopes = parseRequiredScopes();
        if (requiredScopes.isEmpty())
            return true;

        var tokenScopes = new HashSet<String>();
        addSpaceSeparatedClaim(tokenScopes, claims, "scope");
        addArrayClaim(tokenScopes, claims, "scp");
        addArrayClaim(tokenScopes, claims, "scope");

        return tokenScopes.containsAll(requiredScopes);
    }

    private Set<String> parseRequiredScopes()
    {
        var scopes = new HashSet<String>();
        if (config.requiredScopes == null || config.requiredScopes.isBlank())
            return scopes;

        for (var scope: config.requiredScopes.split("\\s+"))
        {
            if (!scope.isBlank())
                scopes.add(scope.trim());
        }

        return scopes;
    }

    private void addSpaceSeparatedClaim(Set<String> scopes, JsonObject claims, String name)
    {
        var value = claims.get(name);
        if (value == null || !value.isJsonPrimitive())
            return;

        for (var scope: value.getAsString().split("\\s+"))
        {
            if (!scope.isBlank())
                scopes.add(scope.trim());
        }
    }

    private void addArrayClaim(Set<String> scopes, JsonObject claims, String name)
    {
        var value = claims.get(name);
        if (value == null || !value.isJsonArray())
            return;

        for (var scope: value.getAsJsonArray())
        {
            if (scope.isJsonPrimitive())
                scopes.add(scope.getAsString());
        }
    }

    private boolean verifySignature(String[] parts, RSAPublicKey key) throws Exception
    {
        var verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(key);
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(Base64.getUrlDecoder().decode(parts[2]));
    }

    private RSAPublicKey findKey(String kid) throws Exception
    {
        for (var keyElement: getJwks())
        {
            var key = keyElement.getAsJsonObject();
            if (!kid.equals(getString(key, "kid")) || !"RSA".equals(getString(key, "kty")))
                continue;

            var modulus = unsignedBigInteger(key.get("n").getAsString());
            var exponent = unsignedBigInteger(key.get("e").getAsString());
            var keySpec = new RSAPublicKeySpec(modulus, exponent);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        }

        return null;
    }

    private JsonArray getJwks() throws Exception
    {
        if (config.jwksUri == null || config.jwksUri.isBlank())
            throw new IllegalStateException("jwksUri must be configured for bearer token validation");

        var now = clock.millis();
        if (cachedJwks != null && now - cachedAtMillis < JWKS_CACHE_MILLIS)
            return cachedJwks;

        var request = HttpRequest.newBuilder()
                .uri(URI.create(config.jwksUri))
                .header("Accept", "application/json")
                .GET()
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IOException("JWKS request failed with HTTP " + response.statusCode());

        var keys = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonArray("keys");
        if (keys == null)
            throw new IOException("JWKS response does not contain keys");

        cachedJwks = keys;
        cachedAtMillis = now;
        return keys;
    }

    private BigInteger unsignedBigInteger(String value)
    {
        return new BigInteger(1, Base64.getUrlDecoder().decode(value));
    }

    private String decodeBase64Url(String value)
    {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String getString(JsonObject obj, String name)
    {
        var value = obj.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private Set<String> buildAcceptedAudienceValues(String resourceUri)
    {
        var values = new HashSet<String>();
        var canonical = McpOAuthHelper.canonicalizeResourceUri(resourceUri);
        values.add(canonical);

        if (canonical != null && canonical.endsWith("/"))
            values.add(canonical.substring(0, canonical.length() - 1));

        return Set.copyOf(values);
    }

    private ValidationResult invalid(String reason)
    {
        log.debug("Bearer token validation failed: {}", reason);
        return ValidationResult.invalid(reason);
    }

    public static final class ValidationResult
    {
        private final boolean valid;
        private final String subject;
        private final String errorDescription;

        private ValidationResult(boolean valid, String subject, String errorDescription)
        {
            this.valid = valid;
            this.subject = subject;
            this.errorDescription = errorDescription;
        }

        static ValidationResult valid(String subject)
        {
            return new ValidationResult(true, subject, null);
        }

        static ValidationResult invalid(String errorDescription)
        {
            return new ValidationResult(false, null, errorDescription);
        }

        public boolean isValid()
        {
            return valid;
        }

        public String subject()
        {
            return subject;
        }

        public String errorDescription()
        {
            return errorDescription;
        }
    }
}
