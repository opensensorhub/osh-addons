## MQTT Support based on HiveMQ

This module exposes common MQTT resources that can be used by other components (such as SensorThings or SensorWeb API). These resources are:

- Common MQTT API (for creating topics and connecting them to internal OSH queues)
- MQTT server
- MQTT client

The default MQTT server and client implementations provided by this module are based on HiveMQ.


### Wildcard subscriptions

Since version 0.2.0, the bundled HiveMQ config (`config.xml`) allows wildcard topic filters (`+`, `#`).

- At start, the MQTT Server copies the bundled config to its config folder (`configFolder`) if there is no config there.
- The MQTT Server does not change a config that is in that folder.
- A node from an earlier version has a config that does not allow wildcards. Earlier bundled configs did not allow them. The MQTT Server then logs a warning at start.
- To allow wildcards on such a node, set `<wildcard-subscriptions><enabled>true</enabled>` in the config, then start the MQTT Server again.

A wildcard filter can match the topics of resources that the user cannot read. So the server checks each message before it sends it to a client:

- A message on a topic that the client subscribed to exactly is sent. The handler checked the subscription.
- Any other message matched a wildcard filter. The handler of its topic decides if it is sent (`IMqttHandler.canDeliver`).
- A handler accepts wildcard filters only if `IMqttHandler.supportsWildcards` returns true. Otherwise the server rejects them. The default is false.

The check of a wildcard match runs outside the HiveMQ threads. Each message that matches a wildcard filter of a client adds one check for that client.


### Persistent sessions

A client can ask the server to keep its session when the connection closes (MQTT 5: `cleanStart=false` and a session expiry interval; MQTT 3.1.1: `cleanSession=false`). The server then keeps the subscriptions of the client. When the client connects again with the same client ID, the CONNACK has `session present = 1` and the client gets data again. It does not have to send SUBSCRIBE again.

- When the client connects again, the server checks each subscription of the session as a new SUBSCRIBE of the user of the new connection. If this user does not have the permission, the server removes the subscription from the session. The client gets no message about this.
- A subscription of the session starts again later if its handler module is stopped when the client connects.
- The server does not keep the messages for a client that is not connected. If a different client has a subscription to the same topic during this time, the client can get the messages that were sent meanwhile (QoS 1 or 2). Do not rely on this.
- With the bundled HiveMQ config, sessions are kept in memory. They are lost when the MQTT server stops.

A connection that does not log in cannot stop the subscriptions of a connected client that has the same client ID.


### Login

This section tells you how to set who can connect to the MQTT server. You set this in the Admin UI:

1. Click **Services** in the list on the left, then click **MQTT Server**.
2. The **General** tab has the **Require Authentication** box.
3. The **Token Login** tab has the **Enabled** box and the **Allow Password Login** box. The settings of the token issuer are in the OAuth module (see Token login settings).

#### Choose a login mode

| You want | Require Authentication | Token Login | Allow Password Login |
|---|---|---|---|
| 1. Clients log in with an OSH user name and password (default) | on | off | - |
| 2. Clients log in with a token from Keycloak only | on | on | off |
| 3. Clients log in with a token or with a password | on | on | on |
| 4. Clients connect without a login | off | any | any |

Two rules apply to all modes:

- **Require Authentication** (`requireAuth`) controls only the clients that send no user name. If it is on, the server refuses them. If it is off, they connect as the `anonymous` user. Since version 0.3.1 the default is on, as for the other OSH services.
- A client that sends a user name must log in correctly. If its password or its token is wrong, the server refuses it. The client does not become `anonymous`. (One exception: a node without a list of users, see Before you start.)

After each change of these settings:

1. Click **Apply Changes** (top right). The MQTT Server starts again with the new settings. This disconnects the clients.
2. Click **Save** (top left), then **Yes**. This keeps the change when the node starts again. Without this step, the change is lost when the node stops.

#### Before you start

- The node must have a list of users (click **Security** in the list on the left, then **Users**). The **Users** tab has the users, and the **Roles** tab has the roles. Without it, the server cannot check a password. Then a client that sends a password is refused if **Require Authentication** is on, and connects as `anonymous` if it is off. Token login works without the list of users.
- What a user can read and write comes from the roles of the user in OSH. The login settings do not change this.

#### Mode 1: user name and password (default)

1. Click **Services**, then **MQTT Server**.
2. On the **General** tab, make sure that **Require Authentication** is on.
3. On the **Token Login** tab, make sure that **Enabled** is off.
4. Click **Apply Changes**, then **Save**.

The client sends the user name and the password of an OSH user. Example:

```
mosquitto_sub -h <node address> -p 1883 -u <OSH user> -P <password> -t '<topic>'
```

Rules for passwords:

- A user must have a password to log in with a password. The server refuses a CONNECT that has a user name and an empty password or no password.
- The server also refuses a user that has no password in the user registry, for example a user that an OAuth login added. Such a user can log in with a token (mode 2 or 3).

#### Mode 2: tokens only

Since version 0.3.0, a client can send an OAuth 2.0 access token (for example from Keycloak) as the MQTT password. Since version 0.3.1, when token login is on, a user can log in with a token only. The server refuses each password, also the correct password of a user in the OSH config (for example an administrator).

You need this data from the administrator of Keycloak:

- The realm URL, for example `https://keycloak.example.org/realms/osh`.
- The names of the realm roles that the users have.

The OAuth module (`sensorhub-security-oauth`, **OAuth2 Authentication** in the Admin UI) checks the tokens. For token login, the node must have this module, and the module must be started. A node that uses only password login (mode 1) does not need it. Its token settings apply to HTTP and to MQTT, so a token gives the same user and the same roles on the two protocols.

Steps:

1. Click **Security**, then the OAuth module, then **Client Credentials Token**.
2. In **JSON Web Key Set Endpoint**, type the realm URL and then `/protocol/openid-connect/certs`. Example: `https://keycloak.example.org/realms/osh/protocol/openid-connect/certs`.
3. In **Token Issuer**, type the realm URL. It must be exactly the same as in the tokens (the `iss` value).
4. In **Token Audience**, type the audience that the tokens have (the `aud` value). It is required. If the tokens of your Keycloak client have no `aud` value, add an audience mapper to the client in Keycloak.
5. In the config of the OAuth module, set the user ID field to `preferred_username` and the user roles field to `realm_access/roles` (Keycloak realm roles). See Token login settings.
6. Click **Apply Changes**, then **Save**.
7. Click **Services**, then **MQTT Server**, then the **Token Login** tab. Set **Enabled** to on. Leave **Allow Password Login** off.
8. Click **Apply Changes**, then **Save**.
9. Click **Security**, then **Users**. On the **Roles** tab, make one OSH role for each Keycloak realm role, with the same name, and give it permissions. See Token users and roles.

The client sends:

- User name: the Keycloak user name. For a Keycloak service account, this is `service-account-<client ID>`.
- Password: the access token.

Example (get a token, then connect):

```
TOKEN=$(curl -s <realm URL>/protocol/openid-connect/token \
  -d grant_type=password -d client_id=<client ID> \
  -d username=<Keycloak user> -d password=<Keycloak password> \
  | sed 's/.*"access_token":"\([^"]*\)".*/\1/')

mosquitto_sub -h <node address> -p 1883 -u <Keycloak user> -P "$TOKEN" -t '<topic>'
```

A user in the OSH config can also log in with a token. The Keycloak user name must be the same as the OSH user name. This user gets its roles from OSH, not from the token.

If the OAuth module cannot check tokens (the module is not on the node, it is not started, or its endpoint is not a URL), no user can log in. The server does not go back to password login.

#### Mode 3: tokens and passwords

Do the steps of mode 2, but in step 7 set **Allow Password Login** (`allowPasswordLogin`) to on. Then a client can log in with a token (as in mode 2) or with the user name and password of an OSH user (as in mode 1). The log shows a warning when the server starts, because the server accepts two types of login.

#### Mode 4: no login

On the **General** tab, set **Require Authentication** to off. Click **Apply Changes**, then **Save**. A client that sends no user name then connects as the `anonymous` user. It can do only what the role `anon` permits.

This setting does not change the login of clients that send a user name. Mode 1, 2 or 3 applies to them.

Example:

```
mosquitto_sub -h <node address> -p 1883 -t '<topic>'
```

#### Make sure that the mode is correct

When the MQTT Server starts, it writes one line to the node log. The line tells you which logins the server accepts:

| Log line | Meaning |
|---|---|
| `MQTT login: password` | Mode 1 |
| `MQTT login: token only, passwords are refused ...` | Mode 2 |
| `MQTT login: token and password (allowPasswordLogin is on) ...` | Mode 3 (warning) |
| `... Connections without a user name are accepted (requireAuth is off)` | Added to the line in mode 4 |

#### If a client cannot connect

The client gets the answer `Bad user name or password` for each refused login. Look for the cause in this list:

- Mode 2, and the client sent a password that is not a token. Use a token, or use mode 3.
- The token expired. Get a new token. Keycloak tokens are valid for a short time (frequently 5 to 15 minutes).
- The user name is not the user name in the token. The server reads it from the claim that the user ID field of the OAuth module gives.
- The OAuth module is not on the node, is not started, or has no JSON Web Key Set Endpoint. If the jar of the module is not on the node (or is older than 2.0.0), the log shows an error when the MQTT server starts and at each token login.
- **Token Issuer** is not exactly the `iss` value of the token. A different host name, port or `http`/`https` is sufficient to fail.
- The token does not have the audience that **Token Audience** gives, or **Token Audience** is empty. The OAuth module refuses all tokens when this field is empty.
- The token has no expiry time (`exp`), or **Token Issuer** is empty.
- The node cannot connect to the JSON Web Key Set Endpoint. The log then shows an error of the OAuth module for each token login.
- The client sent no user name, and **Require Authentication** is on.
- The client sent a user name without a password.

Refusals are in the node log at debug level (`org.sensorhub`).

The client connects but cannot subscribe or publish: the user has no OSH role that gives the permission. See Token users and roles.

#### What the login settings do not change

- The settings apply to MQTT only, also through the websocket proxy. They do not change the login of the HTTP server (Admin UI, CS API).
- The websocket proxy does not ask for HTTP credentials. The client logs in with the MQTT CONNECT, as on the TCP port.


### Token login

#### Token login settings

The MQTT server does not check tokens itself. It uses the token check of the OAuth module (`sensorhub-security-oauth`, version 2.0.0 or later), which is the check of HTTP bearer tokens. The settings are in two places.

MQTT Server config, **Token Login** (`tokenAuth` in the config file):

| Admin UI | Config file | Meaning |
|---|---|---|
| Enabled | `enabled` | Accept access tokens as the MQTT password. Default: off. |
| Allow Password Login | `allowPasswordLogin` | Also accept the user name and password of an OSH user. Default: off. |

OAuth module config. These settings apply to HTTP and to MQTT:

| Config file | Meaning |
|---|---|
| `bearerTokenConfig.jwksUri` | URL of the public keys of the issuer. Keycloak: `{realm URL}/protocol/openid-connect/certs`. |
| `bearerTokenConfig.issuer` | Issuer that the token must have (`iss`). Keycloak: the realm URL. It is required: with no issuer, the module refuses all tokens. |
| `bearerTokenConfig.audience` | Audience that the token must have (`aud`). It is required: with no audience, the module refuses all tokens. |
| `bearerTokenConfig.cacheSize`, `bearerTokenConfig.cacheDuration` | Number of public keys to keep, and time in minutes to keep them. |
| `userIdField` | Path of the token claim that must be equal to the MQTT user name. Keycloak: `preferred_username`. Default: `id`. |
| `userRolesField` | Path of the token claim that contains the roles of the user, with `/` between the names. Keycloak realm roles: `realm_access/roles`. Default: empty (only the role `anon`). |
| `autoAddUser` | Add the user of a token to the OSH users. Default: on. If off, only users that are in OSH can log in. |

Example of the MQTT Server config for mode 2:

```json
{
  "objClass": "org.sensorhub.impl.service.hivemq.MqttServerConfig",
  "moduleClass": "org.sensorhub.impl.service.hivemq.MqttServer",
  "requireAuth": true,
  "tokenAuth": {
    "enabled": true
  }
}
```

The OAuth module needs an HTTP server module on the node. It reads the token settings when it starts: start it again after a change.

#### How the server checks a token

The OAuth module checks the signature (RS256, RS384, RS512, ES256, ES384 or ES512), the issuer, the audience and the expiry time (`exp`), as for an HTTP bearer token. A token without an expiry time is refused. The MQTT server then checks that the MQTT user name is equal to the user ID of the token, and that the token has an expiry time (`exp`). If the password is a token and one check fails, the server refuses the connection. It does not try the token as a password.

If the server cannot get the public keys (the issuer is down, or it refuses too many requests), it refuses all token logins. Each refusal adds an error of the OAuth module to the log. Other refusals are logged at debug level only.

The connection gets the user name as its OSH user, also when OSH access control is off.

#### Token users and roles

With OSH access control on, subscribe and publish look up the user in the OSH users. So the server adds the user of a valid token to the OSH users when the client connects (`autoAddUser` of the OAuth module). The user has the same type as the user that the OAuth module adds for an HTTP login.

- The user gets the roles of the token, read from the roles claim (`userRolesField`), and the role `anon`. HTTP and MQTT use the same settings, so the roles are the same on the two protocols.
- A role of the token gives permissions only if OSH has a role with the same name. Make an OSH role for each Keycloak realm role that you use, with the permissions that it must give. For example, make the OSH role `osh_operator` for the Keycloak realm role `osh_operator`. The server ignores the other roles of the token (for example `offline_access`).
- Each new login of the user replaces the roles with the roles of the new token.
- The server removes the user from the OSH users when the last connection that logged in with a token for this user closes. When the MQTT server stops, it removes all the users that it added. The users are not saved in the OSH config.
- If `autoAddUser` is off, the server adds no users. It refuses a token login if the user is not in OSH.

A user that the server did not add stays as it is, and the server does not remove it. This is a user in the OSH config or a user that the OAuth module added for an HTTP login. A token login for this user gets the roles that the user has in OSH. For a user of an HTTP login, these are the roles of its HTTP token, read with the same settings.

The server refuses a token for the user name `anonymous`. Connections without a user name use this user.

A user that the MQTT server added has a random password that nobody knows. It cannot log in with a password, on MQTT or on HTTP. A user that the OAuth module added for an HTTP login has no password. It cannot log in with a password on MQTT, because the server refuses an empty password.

Known limits:

- The request for the public keys has a time limit of 2 seconds in the OAuth module. If the issuer does not answer in this time, the login is refused.
- If the roles of a user change at the issuer, open connections keep the old roles until the client connects again with a new token.
- The Admin UI cannot change the permissions of a user that the server added.
- A user that the server added cannot get permissions on a resource that it makes. If the roles of the user do not give all permissions (`*`), a publication that makes a resource (for example a system) makes the resource, but the log shows `Internal error handling PUBLISH message` with a `ClassCastException`. The user then has only the permissions of its roles on the new resource. A publication of observations is not affected. The cause is in the OSH core.
- The basic security realm of OSH cannot write its permissions file (`user_permissions.json`) while the OSH users contain a user that the server added. This is the time when a token client is connected. The cause is in the OSH core. Users that the OAuth module adds give the same error. The realm writes the file in two cases:
  - A permission of a user changes. The CS API gives a user permissions on a resource that the user makes, if the user does not have them. A user with all permissions (`*`) gets none. The request of the user then fails with the status 500. The file is complete again after the next change, when no token client is connected.
  - The node stops. The node saves the state of the realm before it stops the MQTT server, so the clients are still connected. To prevent the error, stop the MQTT server module before you stop the node.
- After this error, the log shows a `ClassCastException` for `OAuthUser` (at node stop, with `State could not be saved for module`), and the file is not complete. If the node starts with this file, the log shows `Cannot load state` for the realm. The users and roles of the OSH config are not changed. Permissions that were set in the Admin UI and not saved in the config are lost.

#### Token expiry

When the token of a connection expires, the server closes the connection. An MQTT 5 client gets a DISCONNECT with the reason code `Not authorized` (0x87). An MQTT 3.1.1 client sees only that the connection closes.

To keep its data, the client must:

1. Get a new token before the old token expires, for example at 80% of the token lifetime.
2. Connect again with the new token.
3. Subscribe again to all its topics. A client with a persistent session does not have to do this (see Persistent sessions).

Known limits:

- The client cannot send a new token on an open connection (MQTT 5 re-authentication).
- The server reads the expiry time when the client connects. If the user or the token is revoked at the issuer before that time, the connection stays open until the token expires.

Connections that use a password or no credentials have no expiry.
