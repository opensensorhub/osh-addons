/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.AbstractLoginService.UserPrincipal;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;


/**
 * Checks that the authenticator creates an HTTP session only for the browser
 * login (authorization code callback), and not for requests that carry their
 * own credentials or no credentials.
 */
public class TestOAuthSessions
{
    static final String ISSUER = "test-issuer";
    static final String AUDIENCE = "test-audience";
    static final String KEY_ID = "test-key";
    static final String USER_ID = "testuser";

    Server idp;
    Server node;
    ServletContextHandler nodeContext;
    String idpUrl;
    String nodeUrl;
    RSAPrivateKey privateKey;
    RSAPublicKey publicKey;
    HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();


    @Before
    public void setup() throws Exception
    {
        var keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keys = keyGen.generateKeyPair();
        privateKey = (RSAPrivateKey)keys.getPrivate();
        publicKey = (RSAPublicKey)keys.getPublic();

        // fake identity provider: JWKS and token endpoints
        idp = new Server();
        var idpConnector = new ServerConnector(idp);
        idpConnector.setHost("127.0.0.1");
        idp.addConnector(idpConnector);
        var idpContext = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        idpContext.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                var enc = Base64.getUrlEncoder().withoutPadding();
                resp.setContentType("application/json");
                resp.getWriter().write("{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + KEY_ID + "\","
                    + "\"n\":\"" + enc.encodeToString(toUnsigned(publicKey.getModulus().toByteArray())) + "\","
                    + "\"e\":\"" + enc.encodeToString(toUnsigned(publicKey.getPublicExponent().toByteArray())) + "\"}]}");
            }
        }), "/jwks");
        idpContext.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                var token = newToken();
                resp.setContentType("application/json");
                resp.getWriter().write("{\"access_token\":\"" + token + "\",\"id_token\":\"" + token + "\","
                    + "\"token_type\":\"Bearer\",\"expires_in\":300}");
            }
        }), "/token");
        idp.setHandler(idpContext);
        idp.start();
        idpUrl = "http://127.0.0.1:" + idpConnector.getLocalPort();

        // node with sessions enabled, as in the OSH HTTP server
        var config = new OAuthClientConfig();
        config.authzEndpoint = idpUrl + "/auth";
        config.tokenEndpoint = idpUrl + "/token";
        config.clientID = "test-client";
        config.clientSecret = "test-secret";
        config.authzScope = "openid";
        config.userIdField = "sub";
        config.autoAddUser = false;
        config.bearerTokenConfig.issuer = ISSUER;
        config.bearerTokenConfig.audience = AUDIENCE;
        config.bearerTokenConfig.jwksUri = idpUrl + "/jwks";

        node = new Server();
        var nodeConnector = new ServerConnector(node);
        nodeConnector.setHost("127.0.0.1");
        node.addConnector(nodeConnector);
        nodeContext = new ServletContextHandler(ServletContextHandler.SESSIONS);

        var security = new ConstraintSecurityHandler();
        security.setLoginService(new TestLoginService());
        security.setAuthenticator(new OAuthAuthenticator(config, "http://localhost", false, null,
            LoggerFactory.getLogger(TestOAuthSessions.class)));
        var constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] {Constraint.ANY_AUTH});
        var mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);
        security.addConstraintMapping(mapping);
        nodeContext.setSecurityHandler(security);

        var okServlet = new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.getWriter().write(String.valueOf(req.getRemoteUser()));
            }
        });
        nodeContext.addServlet(okServlet, "/api/*");
        nodeContext.addServlet(okServlet, "/oauthcode");
        node.setHandler(nodeContext);
        node.start();
        nodeUrl = "http://127.0.0.1:" + nodeConnector.getLocalPort();
    }


    @After
    public void cleanup() throws Exception
    {
        if (node != null)
            node.stop();
        if (idp != null)
            idp.stop();
    }


    @Test
    public void testValidBearerTokenCreatesNoSession() throws Exception
    {
        var token = newToken();
        for (int i = 0; i < 200; i++)
        {
            var resp = get("/api/systems", "Authorization", "Bearer " + token);
            assertEquals(200, resp.statusCode());
            assertEquals(USER_ID, resp.body());
            assertTrue(resp.headers().firstValue("Set-Cookie").isEmpty());
        }
        assertEquals(0, sessionCount());
    }


    @Test
    public void testInvalidBearerTokenCreatesNoSession() throws Exception
    {
        // token signed with a key that the provider does not know
        var keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        var otherKey = (RSAPrivateKey)keyGen.generateKeyPair().getPrivate();
        var token = newToken(otherKey);

        for (int i = 0; i < 50; i++)
            assertEquals(401, get("/api/systems", "Authorization", "Bearer " + token).statusCode());
        assertEquals(0, sessionCount());
    }


    @Test
    public void testNoCredentialsCreatesNoSession() throws Exception
    {
        for (int i = 0; i < 50; i++)
        {
            var resp = get("/api/systems");
            assertEquals(302, resp.statusCode());
            assertTrue(resp.headers().firstValue("Location").orElse("").startsWith(idpUrl + "/auth"));
        }
        assertEquals(0, sessionCount());
    }


    @Test
    public void testBrowserLoginCreatesOneSession() throws Exception
    {
        // first request is redirected to the provider
        var resp = get("/api/systems");
        assertEquals(302, resp.statusCode());
        var location = resp.headers().firstValue("Location").orElseThrow();
        var state = location.replaceAll(".*[?&]state=([^&]+).*", "$1");
        assertEquals(0, sessionCount());

        // provider calls back with the authorization code
        resp = get("/oauthcode?code=abc&state=" + state);
        assertEquals(302, resp.statusCode());
        assertEquals(nodeUrl + "/api/systems", resp.headers().firstValue("Location").orElseThrow());
        var cookie = resp.headers().firstValue("Set-Cookie").orElse(null);
        assertNotNull("No session cookie after login", cookie);
        cookie = cookie.substring(0, cookie.indexOf(';'));
        assertEquals(1, sessionCount());

        // the session cookie is enough for the next requests
        for (int i = 0; i < 20; i++)
        {
            resp = get("/api/systems", "Cookie", cookie);
            assertEquals(200, resp.statusCode());
            assertEquals(USER_ID, resp.body());
        }

        // token requests still make no session
        var token = newToken();
        for (int i = 0; i < 20; i++)
            assertEquals(200, get("/api/systems", "Authorization", "Bearer " + token).statusCode());
        assertEquals(1, sessionCount());
    }


    @Test
    public void testBearerTokenStatusCodes() throws Exception
    {
        assertEquals(200, bearerStatus(claims()));

        // tokens that the check refuses
        assertEquals(401, bearerStatus(claims().withExpiresAt(Instant.now().minusSeconds(60))));
        assertEquals(401, bearerStatus(claims().withIssuer("other-issuer")));
        assertEquals(401, bearerStatus(claims().withAudience("other-audience")));
        assertEquals(401, bearerStatus(claims().withKeyId("other-key")));
        assertEquals(401, bearerStatus(claims().sign(Algorithm.none())));
        assertEquals(401, bearerStatus("not-a-token"));

        // no expiry time
        var noExpiry = JWT.create().withKeyId(KEY_ID).withIssuer(ISSUER).withAudience(AUDIENCE).withSubject(USER_ID);
        assertEquals(401, bearerStatus(noExpiry));

        // no audience
        var noAudience = JWT.create().withKeyId(KEY_ID).withIssuer(ISSUER).withSubject(USER_ID)
            .withExpiresAt(Instant.now().plusSeconds(300));
        assertEquals(401, bearerStatus(noAudience));

        // the key server does not answer, and the key of the token is not in the cache
        idp.stop();
        assertEquals(403, bearerStatus(claims().withKeyId("key-not-in-cache")));
        assertEquals(0, sessionCount());
    }


    HttpResponse<String> get(String path, String... headers) throws Exception
    {
        var req = HttpRequest.newBuilder(URI.create(nodeUrl + path)).GET();
        if (headers.length > 0)
            req.headers(headers);
        return http.send(req.build(), BodyHandlers.ofString());
    }


    int sessionCount()
    {
        return nodeContext.getSessionHandler().getSessionsCreated();
    }


    String newToken()
    {
        return newToken(privateKey);
    }


    String newToken(RSAPrivateKey key)
    {
        return claims().sign(Algorithm.RSA256(null, key));
    }


    JWTCreator.Builder claims()
    {
        return JWT.create()
            .withKeyId(KEY_ID)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(USER_ID)
            .withExpiresAt(Instant.now().plusSeconds(300));
    }


    int bearerStatus(JWTCreator.Builder claims) throws Exception
    {
        return bearerStatus(claims.sign(Algorithm.RSA256(null, privateKey)));
    }


    int bearerStatus(String token) throws Exception
    {
        return get("/api/systems", "Authorization", "Bearer " + token).statusCode();
    }


    static byte[] toUnsigned(byte[] bytes)
    {
        if (bytes.length > 1 && bytes[0] == 0)
        {
            var out = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, out, 0, out.length);
            return out;
        }
        return bytes;
    }


    /* accepts any user that the authenticator has already verified */
    static class TestLoginService implements LoginService
    {
        IdentityService identityService = new DefaultIdentityService();

        @Override
        public String getName()
        {
            return "test";
        }

        @Override
        public UserIdentity login(String username, Object credentials, ServletRequest request)
        {
            var principal = new UserPrincipal(username, null);
            var subject = new Subject();
            subject.getPrincipals().add(principal);
            return identityService.newUserIdentity(subject, principal, new String[] {"user"});
        }

        @Override
        public boolean validate(UserIdentity user)
        {
            return true;
        }

        @Override
        public IdentityService getIdentityService()
        {
            return identityService;
        }

        @Override
        public void setIdentityService(IdentityService service)
        {
            this.identityService = service;
        }

        @Override
        public void logout(UserIdentity user)
        {
        }
    }
}
