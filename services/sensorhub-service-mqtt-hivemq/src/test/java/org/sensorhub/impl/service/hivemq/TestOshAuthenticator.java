/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import static org.junit.Assert.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sensorhub.api.security.ISecurityManager;
import org.slf4j.LoggerFactory;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.NetworkException;
import com.auth0.jwk.RateLimitReachedException;
import com.auth0.jwk.SigningKeyNotFoundException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;


public class TestOshAuthenticator
{
    static final String ISSUER = "https://keycloak.example/realms/osh";
    static final String AUDIENCE = "osh";
    static final String KID = "key1";
    static final String PASSWORD = "secret";

    static KeyPair realmKeys;
    static KeyPair otherKeys;

    final List<String> passwordChecks = new ArrayList<>();


    @BeforeClass
    public static void createKeys() throws Exception
    {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        realmKeys = gen.generateKeyPair();
        otherKeys = gen.generateKeyPair();
    }


    static TokenAuthConfig tokenConfig()
    {
        var config = new TokenAuthConfig();
        config.jwksUri = "https://keycloak.example/realms/osh/protocol/openid-connect/certs";
        config.issuer = ISSUER;
        config.audience = AUDIENCE;
        return config;
    }


    OshAuthenticator authenticator(TokenAuthConfig tokenConfig, boolean requireAuth)
    {
        JwkProvider realmJwks = kid -> {
            if (KID.equals(kid))
                return jwk((RSAPublicKey)realmKeys.getPublic());
            throw new SigningKeyNotFoundException("No key " + kid, null);
        };
        var verifier = tokenConfig.jwksUri != null ? new TokenVerifier(tokenConfig, realmJwks) : null;
        return authenticator(tokenConfig, requireAuth, verifier);
    }


    OshAuthenticator authenticator(TokenAuthConfig tokenConfig, boolean requireAuth, TokenVerifier verifier)
    {
        var config = new MqttServerConfig();
        config.requireAuth = requireAuth;
        config.tokenAuth = tokenConfig;

        // no security manager: access control on, one user with a password
        return new OshAuthenticator(null, config, verifier, LoggerFactory.getLogger(TestOshAuthenticator.class)) {
            @Override
            boolean isAccessControlEnabled()
            {
                return true;
            }

            @Override
            boolean checkPassword(String userID, char[] pwd)
            {
                passwordChecks.add(userID);
                return PASSWORD.equals(new String(pwd));
            }
        };
    }


    OshAuthenticator authenticator()
    {
        return authenticator(tokenConfig(), true);
    }


    static Jwk jwk(RSAPublicKey key)
    {
        var values = new HashMap<String, Object>();
        values.put("kid", KID);
        values.put("kty", "RSA");
        values.put("alg", "RS256");
        values.put("use", "sig");
        values.put("n", base64Url(key.getModulus()));
        values.put("e", base64Url(key.getPublicExponent()));
        return Jwk.fromValues(values);
    }


    static String base64Url(BigInteger value)
    {
        var bytes = value.toByteArray();
        if (bytes[0] == 0)
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    static String token(String username, String issuer, String audience, Instant expiry, KeyPair keys)
    {
        var builder = JWT.create()
            .withKeyId(KID)
            .withIssuer(issuer)
            .withClaim("preferred_username", username);
        if (audience != null)
            builder.withAudience(audience);
        if (expiry != null)
            builder.withExpiresAt(expiry);
        return builder.sign(Algorithm.RSA256((RSAPublicKey)keys.getPublic(), (RSAPrivateKey)keys.getPrivate()));
    }


    static String token(String username)
    {
        return token(username, ISSUER, AUDIENCE, Instant.now().plusSeconds(300), realmKeys);
    }


    @Test
    public void testValidToken()
    {
        assertEquals("alice", authenticator().authenticate("alice", token("alice")));
        assertTrue(passwordChecks.isEmpty());
    }


    @Test
    public void testServiceAccount()
    {
        var user = "service-account-osh-client";
        assertEquals(user, authenticator().authenticate(user, token(user)));
    }


    @Test
    public void testExpiredToken()
    {
        var expired = token("alice", ISSUER, AUDIENCE, Instant.now().minusSeconds(60), realmKeys);
        assertNull(authenticator().authenticate("alice", expired));
    }


    @Test
    public void testTokenWithoutExpiry()
    {
        var noExpiry = token("alice", ISSUER, AUDIENCE, null, realmKeys);
        assertNull(authenticator().authenticate("alice", noExpiry));
    }


    @Test
    public void testWrongIssuer()
    {
        var other = token("alice", "https://keycloak.example/realms/other", AUDIENCE, Instant.now().plusSeconds(300), realmKeys);
        assertNull(authenticator().authenticate("alice", other));
    }


    @Test
    public void testBadSignature()
    {
        var forged = token("alice", ISSUER, AUDIENCE, Instant.now().plusSeconds(300), otherKeys);
        assertNull(authenticator().authenticate("alice", forged));
    }


    @Test
    public void testUsernameNotEqualToClaim()
    {
        assertNull(authenticator().authenticate("bob", token("alice")));
    }


    @Test
    public void testAudience()
    {
        var otherAudience = token("alice", ISSUER, "other", Instant.now().plusSeconds(300), realmKeys);
        assertNull(authenticator().authenticate("alice", otherAudience));

        // the audience check is optional
        var config = tokenConfig();
        config.audience = null;
        assertEquals("alice", authenticator(config, true).authenticate("alice", otherAudience));
    }


    @Test
    public void testUnknownKey()
    {
        var token = JWT.create()
            .withKeyId("other-key")
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("preferred_username", "alice")
            .withExpiresAt(Instant.now().plusSeconds(300))
            .sign(Algorithm.RSA256((RSAPublicKey)realmKeys.getPublic(), (RSAPrivateKey)realmKeys.getPrivate()));
        assertNull(authenticator().authenticate("alice", token));
    }


    @Test
    public void testRefusedTokenIsNotTriedAsPassword()
    {
        assertNull(authenticator().authenticate("bob", token("alice")));
        assertTrue(passwordChecks.isEmpty());
    }


    @Test
    public void testPasswordLoginStillWorks()
    {
        var auth = authenticator();
        assertEquals("alice", auth.authenticate("alice", PASSWORD));
        assertNull(auth.authenticate("alice", "wrong"));
        assertEquals(List.of("alice", "alice"), passwordChecks);
    }


    @Test
    public void testTokenLoginOff()
    {
        // without token settings, a token is checked as a password
        var auth = authenticator(new TokenAuthConfig(), true);
        assertNull(auth.authenticate("alice", token("alice")));
        assertEquals(List.of("alice"), passwordChecks);
    }


    @Test
    public void testAnonymousWhenAuthNotRequired()
    {
        assertEquals(ISecurityManager.ANONYMOUS_USER, authenticator(tokenConfig(), false).authenticate(null, ""));
        assertNull(authenticator(tokenConfig(), true).authenticate(null, ""));
    }


    @Test
    public void testCreateVerifier()
    {
        var log = LoggerFactory.getLogger(TestOshAuthenticator.class);
        assertNull(TokenVerifier.create(null, log));
        assertNull(TokenVerifier.create(new TokenAuthConfig(), log));

        // the issuer is required
        var config = tokenConfig();
        config.issuer = null;
        assertNull(TokenVerifier.create(config, log));

        config = tokenConfig();
        config.jwksUri = "not a url";
        assertNull(TokenVerifier.create(config, log));

        assertNotNull(TokenVerifier.create(tokenConfig(), log));
    }


    @Test
    public void testKeyServerError()
    {
        assertTrue(TokenVerifier.isKeyServerError(new NetworkException("Cannot obtain jwks", null)));
        assertTrue(TokenVerifier.isKeyServerError(new RateLimitReachedException(1000)));

        // the key cache wraps network errors
        var wrapped = new SigningKeyNotFoundException("Failed to get key", new NetworkException("Cannot obtain jwks", null));
        assertTrue(TokenVerifier.isKeyServerError(wrapped));
        assertEquals("Cannot obtain jwks", TokenVerifier.rootMessage(wrapped));

        // errors in the token itself
        assertFalse(TokenVerifier.isKeyServerError(new SigningKeyNotFoundException("No key other-key", null)));
        assertFalse(TokenVerifier.isKeyServerError(new JWTVerificationException("The Token has expired")));
    }


    @Test
    public void testKeyServerDown() throws Exception
    {
        // nothing listens on port 1: the real key provider gets a network error
        var config = tokenConfig();
        config.jwksUri = "http://localhost:1/realms/osh/protocol/openid-connect/certs";
        var verifier = TokenVerifier.create(config, LoggerFactory.getLogger(TestOshAuthenticator.class));

        var e = assertThrows(JwkException.class, () -> verifier.verify(token("alice"), "alice"));
        assertTrue(TokenVerifier.isKeyServerError(e));

        // the login is refused, and not tried as a password
        var auth = authenticator(config, true, verifier);
        assertNull(auth.authenticate("alice", token("alice")));
        assertTrue(passwordChecks.isEmpty());
    }


    @Test
    public void testIsJwt()
    {
        assertTrue(TokenVerifier.isJwt(token("alice")));
        assertFalse(TokenVerifier.isJwt(PASSWORD));
        assertFalse(TokenVerifier.isJwt("a.b.c"));
        assertFalse(TokenVerifier.isJwt(""));
        assertFalse(TokenVerifier.isJwt(null));
    }
}
