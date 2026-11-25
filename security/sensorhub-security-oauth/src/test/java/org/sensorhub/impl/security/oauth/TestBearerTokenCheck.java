/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security.oauth;

import static org.junit.Assert.*;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.sun.net.httpserver.HttpServer;


/**
 * Tests the token check that other modules call (for example the MQTT
 * server), with a local server for the public keys of the issuer
 */
public class TestBearerTokenCheck
{
    static final String ISSUER = "https://keycloak.example/realms/osh";
    static final String AUDIENCE = "osh";
    static final String KID = "key1";
    static final String EC_KID = "key2";

    static KeyPair realmKeys;
    static KeyPair otherKeys;
    static KeyPair ecKeys;
    static HttpServer slowKeyServer;
    static HttpServer keyServer;
    static String jwksUri;


    @BeforeClass
    public static void startKeyServer() throws Exception
    {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        realmKeys = gen.generateKeyPair();
        otherKeys = gen.generateKeyPair();

        var key = (RSAPublicKey)realmKeys.getPublic();
        var ecGen = KeyPairGenerator.getInstance("EC");
        ecGen.initialize(256);
        ecKeys = ecGen.generateKeyPair();
        var ecPoint = ((ECPublicKey)ecKeys.getPublic()).getW();

        var jwks = "{\"keys\":[{\"kid\":\"" + KID + "\",\"kty\":\"RSA\",\"alg\":\"RS256\",\"use\":\"sig\","
            + "\"n\":\"" + base64Url(key.getModulus()) + "\",\"e\":\"" + base64Url(key.getPublicExponent()) + "\"},"
            + "{\"kid\":\"" + EC_KID + "\",\"kty\":\"EC\",\"crv\":\"P-256\",\"alg\":\"ES256\",\"use\":\"sig\","
            + "\"x\":\"" + base64Url(ecPoint.getAffineX(), 32) + "\",\"y\":\"" + base64Url(ecPoint.getAffineY(), 32) + "\"}]}";

        keyServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        keyServer.createContext("/certs", exchange -> {
            var body = jwks.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        keyServer.start();
        jwksUri = "http://localhost:" + keyServer.getAddress().getPort() + "/certs";

        // a key server that answers after the time limit
        slowKeyServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        slowKeyServer.createContext("/certs", exchange -> {
            try
            {
                Thread.sleep(OAuthAuthenticator.JWKS_TIMEOUT_MS + 3000L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        slowKeyServer.start();
    }


    @AfterClass
    public static void stopKeyServer()
    {
        keyServer.stop(0);
        slowKeyServer.stop(0);
    }


    static String base64Url(BigInteger value)
    {
        var bytes = value.toByteArray();
        if (bytes[0] == 0)
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


    static String base64Url(BigInteger value, int length)
    {
        var bytes = value.toByteArray();
        var fixed = new byte[length];
        int n = Math.min(bytes.length, length);
        System.arraycopy(bytes, bytes.length - n, fixed, length - n, n);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(fixed);
    }


    static OAuthClientConfig config()
    {
        var config = new OAuthClientConfig();
        config.bearerTokenConfig.jwksUri = jwksUri;
        config.bearerTokenConfig.issuer = ISSUER;
        config.bearerTokenConfig.audience = AUDIENCE;
        config.userIdField = "preferred_username";
        config.userRolesField = "realm_access/roles";
        return config;
    }


    static OAuthAuthenticator authenticator(OAuthClientConfig config)
    {
        return new OAuthAuthenticator(config, "http://localhost", false, null,
            LoggerFactory.getLogger(TestBearerTokenCheck.class));
    }


    static JWTCreator.Builder claims(String username)
    {
        return JWT.create()
            .withKeyId(KID)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withExpiresAt(Instant.now().plusSeconds(300))
            .withClaim("preferred_username", username);
    }


    static String sign(JWTCreator.Builder claims, KeyPair keys)
    {
        return claims.sign(Algorithm.RSA256((RSAPublicKey)keys.getPublic(), (RSAPrivateKey)keys.getPrivate()));
    }


    static String signEc(JWTCreator.Builder claims)
    {
        return claims.sign(Algorithm.ECDSA256((ECPublicKey)ecKeys.getPublic(), (ECPrivateKey)ecKeys.getPrivate()));
    }


    @Test
    public void testValidTokenGivesUserAndRoles()
    {
        var token = sign(claims("alice").withClaim("realm_access", Map.of("roles", List.of("osh_operator", "mqtt_sub"))), realmKeys);
        var user = authenticator(config()).checkBearerToken(token);
        assertEquals("alice", user.getId());
        assertEquals(List.of("anon", "osh_operator", "mqtt_sub"), List.copyOf(user.getRoles()));

        // a token without the roles claim gives only the anon role
        user = authenticator(config()).checkBearerToken(sign(claims("bob"), realmKeys));
        assertEquals("bob", user.getId());
        assertEquals(List.of("anon"), List.copyOf(user.getRoles()));
    }


    @Test
    public void testRefusedTokens()
    {
        var auth = authenticator(config());
        assertNull(auth.checkBearerToken(sign(claims("alice").withExpiresAt(Instant.now().minusSeconds(60)), realmKeys)));
        assertNull(auth.checkBearerToken(sign(claims("alice").withIssuer("https://keycloak.example/realms/other"), realmKeys)));
        assertNull(auth.checkBearerToken(sign(claims("alice").withAudience("other"), realmKeys)));
        assertNull(auth.checkBearerToken(sign(claims("alice"), otherKeys)));
        assertNull(auth.checkBearerToken(sign(claims("alice").withKeyId("other-key"), realmKeys)));
        assertNull(auth.checkBearerToken("not a token"));
        assertNull(auth.checkBearerToken("a.b.c"));
    }


    @Test
    public void testTokenWithoutUserIdClaim()
    {
        var config = config();
        config.userIdField = "id";
        assertNull(authenticator(config).checkBearerToken(sign(claims("alice"), realmKeys)));
    }


    @Test
    public void testNoKeyServer()
    {
        // no JWKS URL in the config
        var config = config();
        config.bearerTokenConfig.jwksUri = null;
        assertNull(authenticator(config).checkBearerToken(sign(claims("alice"), realmKeys)));

        // nothing listens on port 1
        config.bearerTokenConfig.jwksUri = "http://localhost:1/certs";
        assertNull(authenticator(config).checkBearerToken(sign(claims("alice"), realmKeys)));
    }


    @Test
    public void testEcKey()
    {
        var auth = authenticator(config());
        var user = auth.checkBearerToken(signEc(claims("alice").withKeyId(EC_KID)));
        assertEquals("alice", user.getId());

        // the algorithm of the token does not match the type of the key
        assertNull(auth.checkBearerToken(signEc(claims("alice"))));
        assertNull(auth.checkBearerToken(sign(claims("alice").withKeyId(EC_KID), realmKeys)));
    }


    @Test
    public void testAlgorithmsWithoutPublicKey()
    {
        var auth = authenticator(config());
        assertNull(auth.checkBearerToken(claims("alice").sign(Algorithm.none())));

        // a token signed with the public key of the issuer as a shared secret
        var secret = realmKeys.getPublic().getEncoded();
        assertNull(auth.checkBearerToken(claims("alice").sign(Algorithm.HMAC256(secret))));
    }


    @Test
    public void testTokenWithoutExpiry()
    {
        var token = JWT.create()
            .withKeyId(KID)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("preferred_username", "alice");
        assertNull(authenticator(config()).checkBearerToken(sign(token, realmKeys)));
    }


    @Test
    public void testAudienceNotSet()
    {
        // no audience in the config: all tokens are refused
        var config = config();
        config.bearerTokenConfig.audience = null;
        var auth = authenticator(config);
        assertNull(auth.checkBearerToken(sign(claims("alice"), realmKeys)));
        assertNull(auth.checkBearerToken(sign(claims("alice").withAudience("other"), realmKeys)));

        var noAudience = JWT.create()
            .withKeyId(KID)
            .withIssuer(ISSUER)
            .withExpiresAt(Instant.now().plusSeconds(300))
            .withClaim("preferred_username", "alice");
        assertNull(auth.checkBearerToken(sign(noAudience, realmKeys)));

        // an audience in the config: the token must have it
        assertNull(authenticator(config()).checkBearerToken(sign(noAudience, realmKeys)));
    }


    @Test
    public void testIssuerNotSet()
    {
        var config = config();
        config.bearerTokenConfig.issuer = null;
        assertNull(authenticator(config).checkBearerToken(sign(claims("alice"), realmKeys)));
    }


    @Test
    public void testSlowKeyServer()
    {
        var config = config();
        config.bearerTokenConfig.jwksUri = "http://localhost:" + slowKeyServer.getAddress().getPort() + "/certs";
        var auth = authenticator(config);

        long t0 = System.currentTimeMillis();
        assertNull(auth.checkBearerToken(sign(claims("alice"), realmKeys)));
        long time = System.currentTimeMillis() - t0;
        assertTrue("The check must stop at the time limit, it took " + time + " ms", time < OAuthAuthenticator.JWKS_TIMEOUT_MS + 2000L);
    }


    @Test
    public void testKeyServerError()
    {
        var config = config();
        config.bearerTokenConfig.jwksUri = "http://localhost:1/certs";
        var auth = authenticator(config);
        try
        {
            auth.verifyBearerToken(sign(claims("alice"), realmKeys));
            fail("The key server does not answer");
        }
        catch (Exception e)
        {
            assertTrue(OAuthAuthenticator.isKeyServerError(e));
        }

        // a key ID that the issuer does not have is an error of the token
        try
        {
            authenticator(config()).verifyBearerToken(sign(claims("alice").withKeyId("other-key"), realmKeys));
            fail("The issuer does not have this key");
        }
        catch (Exception e)
        {
            assertFalse(OAuthAuthenticator.isKeyServerError(e));
        }
    }
}
