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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.impl.security.oauth.OAuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    static final Logger LOG = LoggerFactory.getLogger(TestOshAuthenticator.class);

    final List<String> passwordChecks = new ArrayList<>();
    final Map<String, IUserInfo> users = new ConcurrentHashMap<>();

    // the autoAddUser setting of the OAuth module
    boolean autoAddUser = true;


    @BeforeClass
    public static void createKeys() throws Exception
    {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        realmKeys = gen.generateKeyPair();
    }


    static TokenAuthConfig tokenConfig()
    {
        var config = new TokenAuthConfig();
        config.enabled = true;
        return config;
    }


    /* token login on, and the operator also allows password login */
    static TokenAuthConfig mixedConfig()
    {
        var config = tokenConfig();
        config.allowPasswordLogin = true;
        return config;
    }


    /* authenticator without a security manager: access control on, one user with a password.
       The token check of the OAuth module is replaced by a check with the test keys. */
    class TestAuthenticator extends OshAuthenticator
    {
        boolean accessControl = true;
        final boolean oauthModule;

        TestAuthenticator(MqttServerConfig config, boolean oauthModule)
        {
            super(null, config, () -> null, LOG);
            this.oauthModule = oauthModule;
        }

        @Override
        OAuthUser checkToken(String token)
        {
            return oauthModule ? checkTestToken(token) : null;
        }

        @Override
        boolean autoAddUser()
        {
            return autoAddUser;
        }

        @Override
        boolean isAccessControlEnabled()
        {
            return accessControl;
        }

        @Override
        Map<String, IUserInfo> userRegistry()
        {
            return users;
        }

        @Override
        boolean checkPassword(String userID, char[] pwd)
        {
            passwordChecks.add(userID);
            return PASSWORD.equals(new String(pwd));
        }

        /* the user of the login, or null if refused */
        String user(String userID, String pwd)
        {
            var login = authenticate(userID, pwd);
            return login != null ? login.user : null;
        }
    }


    TestAuthenticator authenticator(TokenAuthConfig tokenConfig, boolean requireAuth)
    {
        return authenticator(tokenConfig, requireAuth, true);
    }


    TestAuthenticator authenticator(TokenAuthConfig tokenConfig, boolean requireAuth, boolean oauthModule)
    {
        var config = new MqttServerConfig();
        config.requireAuth = requireAuth;
        config.tokenAuth = tokenConfig;
        return new TestAuthenticator(config, oauthModule);
    }


    /* what the OAuth module does: check the signature, the issuer, the audience and the
       expiry if the token has one, then read the user ID and the Keycloak realm roles */
    static OAuthUser checkTestToken(String token)
    {
        try
        {
            var verified = JWT.require(Algorithm.RSA256((RSAPublicKey)realmKeys.getPublic(), null))
                .withIssuer(ISSUER)
                .withAudience(AUDIENCE)
                .build().verify(token);

            var roles = new ArrayList<String>(List.of("anon"));
            var realmAccess = verified.getClaim("realm_access").as(Object.class);
            if (realmAccess instanceof Map && ((Map<?,?>)realmAccess).get("roles") instanceof List)
            {
                for (var role: (List<?>)((Map<?,?>)realmAccess).get("roles"))
                    roles.add((String)role);
            }
            return new OAuthUser(verified.getClaim("preferred_username").asString(), roles);
        }
        catch (JWTVerificationException e)
        {
            return null;
        }
    }


    TestAuthenticator authenticator()
    {
        return authenticator(tokenConfig(), true);
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


    /* a token with the given value at the given path, as Keycloak does for realm_access/roles */
    static String tokenWithClaim(String username, String claim, Object value)
    {
        return JWT.create()
            .withKeyId(KID)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withExpiresAt(Instant.now().plusSeconds(300))
            .withClaim("preferred_username", username)
            .withPayload(Map.of(claim, value))
            .sign(Algorithm.RSA256((RSAPublicKey)realmKeys.getPublic(), (RSAPrivateKey)realmKeys.getPrivate()));
    }


    static String tokenWithRoles(String username, String... roles)
    {
        return tokenWithClaim(username, "realm_access", Map.of("roles", List.of(roles)));
    }


    /* a user that the operator added to OSH */
    static IUserInfo localUser(String userID, String password, String... roles)
    {
        return new IUserInfo() {
            public String getId() { return userID; }
            public String getName() { return userID; }
            public String getDescription() { return null; }
            public String getPassword() { return password; }
            public Collection<String> getRoles() { return List.of(roles); }
            public Map<String, Object> getAttributes() { return Map.of(); }
            public Collection<IPermissionPath> getAllowList() { return List.of(); }
            public Collection<IPermissionPath> getDenyList() { return List.of(); }
        };
    }


    @Test
    public void testTokenRolesBecomeOshRoles()
    {
        assertEquals("alice", authenticator().user("alice", tokenWithRoles("alice", "osh_operator", "mqtt_sub")));
        var user = users.get("alice");
        assertEquals("alice", user.getId());
        assertEquals(List.of("anon", "osh_operator", "mqtt_sub"), List.copyOf(user.getRoles()));
        assertTrue(user.getAllowList().isEmpty());
    }


    @Test
    public void testTokenWithoutRolesGetsAnonRole()
    {
        assertEquals("alice", authenticator().user("alice", token("alice")));
        assertEquals(List.of("anon"), List.copyOf(users.get("alice").getRoles()));

        // a roles claim that is not a list of names
        assertEquals("bob", authenticator().user("bob", tokenWithClaim("bob", "realm_access", "osh_admin")));
        assertEquals(List.of("anon"), List.copyOf(users.get("bob").getRoles()));
        assertEquals("carol", authenticator().user("carol", tokenWithClaim("carol", "realm_access", Map.of("roles", "osh_admin"))));
        assertEquals(List.of("anon"), List.copyOf(users.get("carol").getRoles()));
    }


    @Test
    public void testNewTokenUpdatesRoles()
    {
        var auth = authenticator();
        auth.user("alice", tokenWithRoles("alice", "osh_admin"));
        auth.user("alice", tokenWithRoles("alice", "osh_viewer"));
        assertEquals(List.of("anon", "osh_viewer"), List.copyOf(users.get("alice").getRoles()));
    }


    @Test
    public void testLocalUserKeepsItsRoles()
    {
        // a token must not change a user that the operator added to OSH
        var local = localUser("alice", PASSWORD, "osh_viewer");
        users.put("alice", local);
        assertEquals("alice", authenticator().user("alice", tokenWithRoles("alice", "osh_admin")));
        assertSame(local, users.get("alice"));

        // also if the user has no password
        var noPwd = localUser("bob", "", "osh_viewer");
        users.put("bob", noPwd);
        assertEquals("bob", authenticator().user("bob", tokenWithRoles("bob", "osh_admin")));
        assertSame(noPwd, users.get("bob"));
    }


    @Test
    public void testTokenForAnonymousIsRefused()
    {
        // connections without a user name get the anonymous user
        var token = tokenWithRoles(ISecurityManager.ANONYMOUS_USER, "osh_admin");
        assertNull(authenticator().user(ISecurityManager.ANONYMOUS_USER, token));
        assertTrue(users.isEmpty());

        var anon = localUser(ISecurityManager.ANONYMOUS_USER, "", "anon");
        users.put(ISecurityManager.ANONYMOUS_USER, anon);
        assertNull(authenticator().user(ISecurityManager.ANONYMOUS_USER, token));
        assertSame(anon, users.get(ISecurityManager.ANONYMOUS_USER));
    }


    @Test
    public void testUnknownUserWithAutoAddOff()
    {
        autoAddUser = false;
        var auth = authenticator();
        assertNull(auth.user("alice", tokenWithRoles("alice", "osh_admin")));
        assertTrue(users.isEmpty());

        // a user that the OAuth module added before the setting was off is not a user of OSH
        users.put("alice", new OAuthUser("alice", List.of("osh_admin")));
        assertNull(auth.user("alice", tokenWithRoles("alice", "osh_admin")));

        // a user of OSH logs in and keeps its OSH roles
        var local = localUser("alice", PASSWORD, "osh_viewer");
        users.put("alice", local);
        assertEquals("alice", auth.user("alice", tokenWithRoles("alice", "osh_admin")));
        assertSame(local, users.get("alice"));
    }


    @Test
    public void testRefusedTokenAddsNoUser()
    {
        var expired = token("alice", ISSUER, AUDIENCE, Instant.now().minusSeconds(60), realmKeys);
        assertNull(authenticator().user("alice", expired));
        assertNull(authenticator().user("bob", tokenWithRoles("alice", "osh_admin")));
        assertTrue(users.isEmpty());
    }


    @Test
    public void testTokenUserStaysWhileConnected()
    {
        // The handlers look up the user after the CONNECT: at subscribe and publish, at the
        // subscription restore after a handler restart, and for each message of a wildcard subscription.
        var auth = authenticator();
        auth.user("alice", tokenWithRoles("alice", "osh_operator"));
        auth.user("bob", tokenWithRoles("bob", "osh_viewer"));
        assertEquals(List.of("anon", "osh_operator"), List.copyOf(users.get("alice").getRoles()));
        assertEquals(List.of("anon", "osh_viewer"), List.copyOf(users.get("bob").getRoles()));
    }


    @Test
    public void testTokenUserRemovedAtLastDisconnect()
    {
        var auth = authenticator();
        var conn1 = auth.authenticate("alice", tokenWithRoles("alice", "osh_operator")).connId;
        var conn2 = auth.authenticate("alice", tokenWithRoles("alice", "osh_viewer")).connId;
        var bobConn = auth.authenticate("bob", tokenWithRoles("bob", "osh_viewer")).connId;
        assertNotNull(conn1);
        assertNotEquals(conn1, conn2);

        // the first connection closes, also if its client connected again before (same client ID)
        auth.releaseTokenUser(conn1);
        assertEquals(List.of("anon", "osh_viewer"), List.copyOf(users.get("alice").getRoles()));

        // a second disconnect event of the same connection changes nothing
        auth.releaseTokenUser(conn1);
        auth.releaseTokenUser(null);
        auth.releaseTokenUser("unknown");
        assertTrue(users.containsKey("alice"));

        // the last connection closes
        auth.releaseTokenUser(conn2);
        assertFalse(users.containsKey("alice"));
        assertTrue(users.containsKey("bob"));

        auth.releaseTokenUser(bobConn);
        assertTrue(users.isEmpty());

        // a new login adds the user again
        var conn3 = auth.authenticate("alice", tokenWithRoles("alice", "osh_admin")).connId;
        assertEquals(List.of("anon", "osh_admin"), List.copyOf(users.get("alice").getRoles()));
        auth.releaseTokenUser(conn3);
        assertTrue(users.isEmpty());
    }


    @Test
    public void testLocalUserIsNotRemoved()
    {
        // a user of OSH that logs in with a token
        var auth = authenticator();
        var local = localUser("alice", PASSWORD, "osh_viewer");
        users.put("alice", local);
        auth.releaseTokenUser(auth.authenticate("alice", tokenWithRoles("alice", "osh_admin")).connId);
        assertSame(local, users.get("alice"));

        // another module replaced the token user while the client was connected
        var conn = auth.authenticate("bob", tokenWithRoles("bob", "osh_admin")).connId;
        var replaced = localUser("bob", "", "osh_viewer");
        users.put("bob", replaced);
        auth.releaseTokenUser(conn);
        assertSame(replaced, users.get("bob"));

        // the same with autoAddUser off
        autoAddUser = false;
        auth = authenticator();
        auth.releaseTokenUser(auth.authenticate("alice", tokenWithRoles("alice", "osh_admin")).connId);
        assertSame(local, users.get("alice"));
    }


    @Test
    public void testTokenUsersRemovedAtStop()
    {
        var auth = authenticator();
        var local = localUser("carol", PASSWORD, "osh_viewer");
        users.put("carol", local);
        var conn = auth.authenticate("alice", tokenWithRoles("alice", "osh_operator")).connId;
        auth.authenticate("alice", tokenWithRoles("alice", "osh_operator"));
        auth.authenticate("bob", tokenWithRoles("bob", "osh_viewer"));
        auth.authenticate("carol", tokenWithRoles("carol", "osh_admin"));

        auth.removeTokenUsers();
        assertEquals(Map.of("carol", local), users);

        // a disconnect event that comes after the stop
        auth.releaseTokenUser(conn);
        assertEquals(Map.of("carol", local), users);
    }


    @Test
    public void testNoTokenUserWithoutAccessControl()
    {
        // no user lookup at subscribe and publish, so no user is added
        var auth = authenticator();
        auth.accessControl = false;
        var login = auth.authenticate("alice", tokenWithRoles("alice", "osh_admin"));
        assertEquals("alice", login.user);
        assertNotNull(login.connId);
        assertTrue(users.isEmpty());
        auth.releaseTokenUser(login.connId);
        assertTrue(users.isEmpty());

        // a login without a token has no connection ID
        assertNull(authenticator(mixedConfig(), true).authenticate("alice", PASSWORD).connId);
    }


    @Test
    public void testTokenUserHasNoUsablePassword()
    {
        authenticator().user("alice", tokenWithRoles("alice", "osh_admin"));
        var pwd = users.get("alice").getPassword();
        assertFalse(OshAuthenticator.passwordMatches(pwd, new char[0]));

        // a random value, not the empty password of an OAuth user
        assertTrue(pwd.length() >= 32);
        authenticator().user("bob", tokenWithRoles("bob", "osh_admin"));
        assertNotEquals(pwd, users.get("bob").getPassword());
    }


    @Test
    public void testValidToken()
    {
        assertEquals("alice", authenticator().user("alice", token("alice")));
        assertTrue(passwordChecks.isEmpty());
    }


    @Test
    public void testLoginHasTokenExpiry()
    {
        var expiry = Instant.now().plusSeconds(300).truncatedTo(ChronoUnit.SECONDS);
        var login = authenticator().authenticate("alice", token("alice", ISSUER, AUDIENCE, expiry, realmKeys));
        assertEquals("alice", login.user);
        assertEquals(expiry, login.tokenExpiry);

        // no expiry for a password or an anonymous login
        assertNull(authenticator(mixedConfig(), true).authenticate("alice", PASSWORD).tokenExpiry);
        assertNull(authenticator(mixedConfig(), false).authenticate(null, "").tokenExpiry);
    }


    @Test
    public void testServiceAccount()
    {
        var user = "service-account-osh-client";
        assertEquals(user, authenticator().user(user, token(user)));
    }


    @Test
    public void testUsernameNotEqualToClaim()
    {
        assertNull(authenticator().user("bob", token("alice")));
    }


    @Test
    public void testRefusedTokenIsNotTriedAsPassword()
    {
        assertNull(authenticator().user("bob", token("alice")));
        assertTrue(passwordChecks.isEmpty());
    }


    @Test
    public void testTokenOnlyRefusesPassword()
    {
        // token login on: a correct password is refused, and it is not checked
        assertNull(authenticator().user("alice", PASSWORD));
        assertNull(authenticator(tokenConfig(), false).user("alice", PASSWORD));
        assertTrue(passwordChecks.isEmpty());
    }


    @Test
    public void testTokenOnlyRefusesLocalUserPassword()
    {
        // a user of the OSH config must also use a token
        users.put("alice", localUser("alice", PASSWORD, "osh_viewer"));
        var auth = authenticator();
        assertNull(auth.user("alice", PASSWORD));
        assertEquals("alice", auth.user("alice", token("alice")));
    }


    @Test
    public void testTokenOnlyKeepsRequireAuth()
    {
        // requireAuth alone controls a CONNECT without a user name
        assertEquals(ISecurityManager.ANONYMOUS_USER, authenticator(tokenConfig(), false).user(null, ""));
        assertNull(authenticator(tokenConfig(), true).user(null, ""));

        // a user name without a valid token does not become anonymous
        assertNull(authenticator(tokenConfig(), false).user("alice", ""));
        assertNull(authenticator(tokenConfig(), false).user("alice", PASSWORD));
    }


    @Test
    public void testTokenOnlyWithoutAccessControl()
    {
        var auth = authenticator(tokenConfig(), false);
        auth.accessControl = false;
        assertNull(auth.user("alice", PASSWORD));
        assertEquals("alice", auth.user("alice", token("alice")));
    }


    @Test
    public void testInvalidTokenSettingsDoNotAllowPasswords()
    {
        // token login is on but the OAuth module cannot check tokens (it is not on the node,
        // it is not started or its settings are not valid): the server refuses all users,
        // it does not go back to passwords
        var auth = authenticator(tokenConfig(), true, false);
        assertNull(auth.user("alice", PASSWORD));
        assertNull(auth.user("alice", token("alice")));
        assertNull(auth.user(null, ""));
        assertTrue(passwordChecks.isEmpty());

        // with password login allowed, passwords work
        assertEquals("alice", authenticator(mixedConfig(), true, false).user("alice", PASSWORD));
    }


    @Test
    public void testPasswordLoginWhenAllowed()
    {
        var auth = authenticator(mixedConfig(), true);
        assertEquals("alice", auth.user("alice", token("alice")));
        assertEquals("alice", auth.user("alice", PASSWORD));
        assertNull(auth.user("alice", "wrong"));
        assertEquals(List.of("alice", "alice"), passwordChecks);
    }


    @Test
    public void testTokenLoginOff()
    {
        // without token settings, a token is checked as a password
        var auth = authenticator(new TokenAuthConfig(), true);
        assertNull(auth.user("alice", token("alice")));
        assertEquals(List.of("alice"), passwordChecks);
    }


    @Test
    public void testAnonymousWhenAuthNotRequired()
    {
        assertEquals(ISecurityManager.ANONYMOUS_USER, authenticator(mixedConfig(), false).user(null, ""));
        assertNull(authenticator(mixedConfig(), true).user(null, ""));
        assertEquals(ISecurityManager.ANONYMOUS_USER, authenticator(new TokenAuthConfig(), false).user(null, ""));
    }


    @Test
    public void testRefusedTokens()
    {
        // the OAuth module refuses the token
        assertNull(authenticator().user("alice", token("alice", ISSUER, AUDIENCE, Instant.now().minusSeconds(60), realmKeys)));
        assertNull(authenticator().user("alice", token("alice", "https://keycloak.example/realms/other", AUDIENCE, Instant.now().plusSeconds(300), realmKeys)));

        // the OAuth module accepts a token without an expiry time: the MQTT server refuses it,
        // because it closes the connection at this time
        assertNull(authenticator().user("alice", token("alice", ISSUER, AUDIENCE, null, realmKeys)));
        assertTrue(passwordChecks.isEmpty());
    }


    @Test
    public void testIsJwt()
    {
        assertTrue(OshAuthenticator.isJwt(token("alice")));
        assertFalse(OshAuthenticator.isJwt(PASSWORD));
        assertFalse(OshAuthenticator.isJwt("a.b.c"));
        assertFalse(OshAuthenticator.isJwt(""));
        assertFalse(OshAuthenticator.isJwt(null));
    }
}
