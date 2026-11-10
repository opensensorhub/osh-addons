## MQTT Support based on HiveMQ

This module exposes common MQTT resources that can be used by other components (such as SensorThings or SensorWeb API). These resources are:

- Common MQTT API (for creating topics and connecting them to internal OSH queues)
- MQTT server
- MQTT client

The default MQTT server and client implementations provided by this module are based on HiveMQ.


### Wildcard subscriptions

Since version 0.2.0, the bundled HiveMQ config (`config.xml`) allows wildcard topic filters (`+`, `#`).

- At start, the MQTT Server copies the bundled config to its config folder (`configFolder`) if there is no config there.
- If the config in that folder is an unchanged earlier default, the MQTT Server replaces it with the new default. Earlier defaults did not allow wildcards.
- If you changed the config, the MQTT Server keeps it. If it does not allow wildcards, the MQTT Server logs a warning. To allow them, set `<wildcard-subscriptions><enabled>true</enabled>` in it.

A wildcard filter can match the topics of resources that the user cannot read. So the server checks each message before it sends it to a client:

- A message on a topic that the client subscribed to exactly is sent. The handler checked the subscription.
- Any other message matched a wildcard filter. The handler of its topic decides if it is sent (`IMqttHandler.canDeliver`).
- A handler accepts wildcard filters only if `IMqttHandler.supportsWildcards` returns true. Otherwise the server rejects them. The default is false.

The check of a wildcard match runs outside the HiveMQ threads. Each message that matches a wildcard filter of a client adds one check for that client.


### Token login

Since version 0.3.0, a client can send an OAuth 2.0 access token (for example from Keycloak) as the MQTT password. Password login stays as it is.

To turn it on, set these settings in the MQTT Server config (`tokenAuth`). The names are the same as in the bearer token settings of the OAuth module (`sensorhub-security-oauth`).

- `jwksUri`: URL of the public keys of the issuer. Keycloak: `{realm URL}/protocol/openid-connect/certs`.
- `issuer`: issuer that the token must have (`iss`). Keycloak: the realm URL. Required.
- `audience`: audience that the token must have (`aud`). Leave it empty to skip this check.
- `userIdField`: token claim that must be equal to the MQTT username. Default: `preferred_username`.
- `cacheSize`, `cacheDuration`: number of public keys to keep, and time in minutes to keep them.

The client sends:

- Username: the value of the username claim. For a Keycloak service account, this is `service-account-<client ID>`.
- Password: the access token.

The server checks the signature, the issuer, the expiry (`exp` is required) and the audience if set. If the password is a token and one check fails, the server refuses the connection. It does not try the token as a password.

If the server cannot get the public keys (the issuer is down, or it refuses too many requests), it refuses all token logins. Each refusal adds a warning to the log. Other refusals are logged at debug level only.

The connection gets the username as its OSH user, also when OSH access control is off. With access control on, the user must also exist in OSH, or subscribe and publish are refused.

#### Token expiry

When the token of a connection expires, the server closes the connection. An MQTT 5 client gets a DISCONNECT with the reason code `Not authorized` (0x87). An MQTT 3.1.1 client sees only that the connection closes.

To keep its data, the client must:

1. Get a new token before the old token expires, for example at 80% of the token lifetime.
2. Connect again with the new token.
3. Subscribe again to all its topics.

Known limits:

- The client must subscribe again after each new connection, also when it uses a persistent session. The server reports that the session is present, but it sends no data until the client subscribes again.
- The client cannot send a new token on an open connection (MQTT 5 re-authentication).
- The server reads the expiry time when the client connects. If the user or the token is revoked at the issuer before that time, the connection stays open until the token expires.

Connections that use a password or no credentials have no expiry.
