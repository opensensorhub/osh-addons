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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import org.slf4j.Logger;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwk.NetworkException;
import com.auth0.jwk.RateLimitReachedException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;


/**
 * <p>
 * Checks an access token sent as the MQTT password: signature with the
 * issuer's public keys (JWKS), issuer, expiry, audience if set, and that the
 * username claim is equal to the MQTT username.
 * </p>
 */
class TokenVerifier
{
    // below the time HiveMQ gives the authenticator (OshAuthenticator.AUTH_TIMEOUT_MS)
    static final int JWKS_TIMEOUT_MS = 2000;

    final TokenAuthConfig config;
    final JwkProvider jwkProvider;


    TokenVerifier(TokenAuthConfig config, JwkProvider jwkProvider)
    {
        this.config = config;
        this.jwkProvider = jwkProvider;
    }


    /**
     * @return a verifier, or null if token login is off or not configured correctly
     */
    static TokenVerifier create(TokenAuthConfig config, Logger log)
    {
        if (config == null || isBlank(config.jwksUri))
            return null;

        if (isBlank(config.issuer))
        {
            log.error("Token login is off: set the token issuer");
            return null;
        }

        try
        {
            var jwkProvider = new JwkProviderBuilder(new URL(config.jwksUri))
                .cached(config.cacheSize, Duration.ofMinutes(config.cacheDuration))
                .timeouts(JWKS_TIMEOUT_MS, JWKS_TIMEOUT_MS)
                .build();
            log.info("Token login is on, issuer {}", config.issuer);
            return new TokenVerifier(config, jwkProvider);
        }
        catch (MalformedURLException e)
        {
            log.error("Token login is off: invalid JWKS URL {}", config.jwksUri);
            return null;
        }
    }


    /**
     * @return true if the password has the form of a JSON Web Token
     */
    static boolean isJwt(String pwd)
    {
        if (pwd == null || pwd.chars().filter(c -> c == '.').count() != 2)
            return false;

        try
        {
            return JWT.decode(pwd).getAlgorithm() != null;
        }
        catch (JWTDecodeException e)
        {
            return false;
        }
    }


    /**
     * Verify the token for the given MQTT username
     * @return the verified token
     * @throws JWTVerificationException if the token is not valid for this user
     * @throws JwkException if the public key cannot be found
     */
    DecodedJWT verify(String token, String userID) throws JwkException
    {
        var jwt = JWT.decode(token);
        var jwk = jwkProvider.get(jwt.getKeyId());

        var verification = JWT.require(algorithm(jwt.getAlgorithm(), jwk.getPublicKey()))
            .withIssuer(config.issuer)
            .withClaimPresence("exp");
        if (!isBlank(config.audience))
            verification.withAudience(config.audience);
        var verified = verification.build().verify(jwt);

        var name = verified.getClaim(config.userIdField).asString();
        if (name == null || !name.equals(userID))
            throw new JWTVerificationException("MQTT username is not equal to the token claim " + config.userIdField);

        return verified;
    }


    static Algorithm algorithm(String alg, PublicKey key)
    {
        try
        {
            switch (alg)
            {
                case "RS256": return Algorithm.RSA256((RSAPublicKey)key, null);
                case "RS384": return Algorithm.RSA384((RSAPublicKey)key, null);
                case "RS512": return Algorithm.RSA512((RSAPublicKey)key, null);
                case "ES256": return Algorithm.ECDSA256((ECPublicKey)key, null);
                case "ES384": return Algorithm.ECDSA384((ECPublicKey)key, null);
                case "ES512": return Algorithm.ECDSA512((ECPublicKey)key, null);
                default: throw new JWTVerificationException("Token algorithm not supported: " + alg);
            }
        }
        catch (ClassCastException e)
        {
            throw new JWTVerificationException("Token algorithm " + alg + " does not match the key type");
        }
    }


    /**
     * @return true if the error comes from the key server (network error or
     * rate limit), not from the token. The key cache wraps network errors in
     * a {@link com.auth0.jwk.SigningKeyNotFoundException}, so check the causes too.
     */
    static boolean isKeyServerError(Throwable e)
    {
        for (var t = e; t != null; t = t.getCause())
        {
            if (t instanceof NetworkException || t instanceof RateLimitReachedException)
                return true;
        }
        return false;
    }


    /**
     * @return the message of the deepest cause that has one
     */
    static String rootMessage(Throwable e)
    {
        var msg = e.getMessage();
        for (var t = e.getCause(); t != null; t = t.getCause())
        {
            if (t.getMessage() != null)
                msg = t.getMessage();
        }
        return msg;
    }


    static boolean isBlank(String s)
    {
        return s == null || s.isBlank();
    }
}
