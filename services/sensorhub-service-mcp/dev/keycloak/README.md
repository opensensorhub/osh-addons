# OpenSensorHub MCP OAuth Setup

This guide is for the people configuring the OpenSensorHub MCP service and the
Keycloak realm it uses for OAuth.

The MCP service is an HTTP MCP server. When OAuth is enabled, every MCP request
must include:

```text
Authorization: Bearer <access_token>
```

The access token must:

- be issued by the configured Keycloak realm
- contain the required `mcp` scope
- contain an audience (`aud`) equal to the public MCP resource URL

For examples below, replace these values with your deployment:

```text
OSH base URL:       https://osh.example.com/sensorhub
MCP endpoint URL:   https://osh.example.com/sensorhub/mcp
Keycloak realm URL: https://keycloak.example.com/realms/osh-mcp
```

For local development, the equivalent URLs are usually:

```text
OSH base URL:       http://localhost:8080/sensorhub
MCP endpoint URL:   http://localhost:8080/sensorhub/mcp
Keycloak realm URL: http://localhost:8180/realms/osh-mcp
```

## 1. Decide the Public MCP URL

Pick the URL that MCP clients will actually use from their machines. This is
important because this exact URL must also be placed in Keycloak token audiences.

Examples:

```text
https://osh.example.com/sensorhub/mcp
http://localhost:8080/sensorhub/mcp
```

Use one stable URL. Avoid switching between `localhost`, an IP address, and a DNS
name after clients are configured because the token audience must match the MCP
resource URL.

## 2. Configure Keycloak

Create or import a realm for MCP. The local development realm in this folder is:

```text
realm/osh-mcp-realm.json
```

For a production or shared test environment, configure the same concepts in the
Keycloak Admin Console.

### Realm

Recommended realm name:

```text
osh-mcp
```

The realm issuer URL will be:

```text
https://keycloak.example.com/realms/osh-mcp
```

### Client Scope

Create a client scope named:

```text
mcp
```

Set it to include in the token scope claim.

Add an audience mapper to this `mcp` client scope:

```text
Mapper type:             Audience
Name:                    mcp-resource-audience
Included Custom Audience: https://osh.example.com/sensorhub/mcp
Add to access token:      On
Add to ID token:          Off
```

The `Included Custom Audience` must exactly match the public MCP endpoint URL.
For local development, use:

```text
http://localhost:8080/sensorhub/mcp
```

### Static Client for Testing

Create a public client for smoke tests and simple MCP clients:

```text
Client ID:              mcp-client
Client authentication:  Off
Standard flow:          On
Direct access grants:   On
PKCE:                   S256
```

For local development redirect URIs:

```text
http://localhost:*
http://127.0.0.1:*
```

For shared environments, add the redirect URIs used by your MCP clients. If a
client uses OAuth browser login on a callback port, that redirect URI must be
allowed here.

Recommended default client scopes:

```text
profile
email
mcp
```

Recommended optional client scopes:

```text
offline_access
```

Keep `offline_access` optional, not default. If it is default, local password
grant smoke tests can fail with:

```json
{"error":"not_allowed","error_description":"Offline tokens not allowed for the user or client"}
```

### Dynamic Client Registration

Some clients, including Claude Code, may try to dynamically register their own
OAuth client by using the Keycloak registration endpoint.

If you allow dynamic registration, make sure Keycloak registration policies
allow requested scopes such as:

```text
openid
profile
email
mcp
offline_access
```

If the registration policy is too restrictive, Claude Code can fail before login
with:

```text
Policy 'Allowed Client Scopes' rejected request to client-registration service.
Not permitted to use specified clientScope
```

For production, use an initial access token or a carefully restricted
registration policy. For local development, it is acceptable to relax anonymous
registration policy only on the dev realm.

### Users

Create one Keycloak user for each person or service account that should access
the MCP server.

Minimum setup:

```text
Username:        alice
Email verified:  On
Enabled:         On
Password:        set a temporary password, or set a permanent password for dev
```

If you use normal authorization-code login, users do not need a special role for
the MCP service unless your organization adds its own authorization rules. They
do need to authenticate through a client that receives the `mcp` scope.

For local development, the imported test user is:

```text
username: mcp-user
password: mcp-password
```

## 3. Configure the OSH MCP Service

Add or edit an MCP service module in the OSH configuration.

Important fields:

```json
{
  "moduleClass": "com.georobotix.impl.service.mcp.McpService",
  "endPoint": "/mcp",
  "writeDatabaseId": null,
  "security": {
    "enableAccessControl": false,
    "requireAuth": true
  },
  "oauth": {
    "enabled": true,
    "issuer": "https://keycloak.example.com/realms/osh-mcp",
    "authorizationEndpoint": "https://keycloak.example.com/realms/osh-mcp/protocol/openid-connect/auth",
    "tokenEndpoint": "https://keycloak.example.com/realms/osh-mcp/protocol/openid-connect/token",
    "jwksUri": "https://keycloak.example.com/realms/osh-mcp/protocol/openid-connect/certs",
    "registrationEndpoint": "https://keycloak.example.com/realms/osh-mcp/clients-registrations/openid-connect",
    "scopesSupported": "openid,profile,email,mcp,offline_access",
    "requiredScopes": "mcp",
    "allowedOrigins": "https://osh.example.com",
    "serviceDocumentation": null
  }
}
```

Use these local development values when OSH is reachable at
`http://localhost:8080/sensorhub/mcp`:

```json
{
  "oauth": {
    "enabled": true,
    "issuer": "http://localhost:8180/realms/osh-mcp",
    "authorizationEndpoint": "http://localhost:8180/realms/osh-mcp/protocol/openid-connect/auth",
    "tokenEndpoint": "http://localhost:8180/realms/osh-mcp/protocol/openid-connect/token",
    "jwksUri": "http://localhost:8180/realms/osh-mcp/protocol/openid-connect/certs",
    "registrationEndpoint": "http://localhost:8180/realms/osh-mcp/clients-registrations/openid-connect",
    "scopesSupported": "openid,profile,email,mcp,offline_access",
    "requiredScopes": "mcp",
    "allowedOrigins": "http://localhost:8080,http://127.0.0.1:8080"
  }
}
```

### Field Notes

`endPoint`

The MCP endpoint path under the OSH servlet root. The default is:

```text
/mcp
```

If the OSH servlet root is `/sensorhub`, the full public MCP URL is:

```text
https://osh.example.com/sensorhub/mcp
```

`writeDatabaseId`

Set this to the module ID of the database that should receive write operations
such as observations and commands. Leave it `null` if MCP clients should be
read-only.

`security.requireAuth`

When OAuth is enabled, the MCP service validates bearer tokens itself. Leave
this as `true` unless you have a specific reason to use another OSH auth layer.

`oauth.issuer`

Must match the token `iss` claim exactly. For Keycloak, use the realm URL.

`oauth.jwksUri`

The MCP service uses this endpoint to verify JWT signatures.

`oauth.registrationEndpoint`

Include this if clients should discover dynamic client registration. Leave it
blank only if all clients use pre-created client IDs or manually supplied bearer
tokens.

`oauth.requiredScopes`

Use:

```text
mcp
```

The access token must include this scope.

`oauth.allowedOrigins`

Comma-separated browser origins allowed to access the MCP endpoint. This is only
checked when a request sends an `Origin` header. For non-browser CLI clients,
there is usually no `Origin` header.

Examples:

```text
https://osh.example.com
http://localhost:8080,http://127.0.0.1:8080
```

## 4. Verify Discovery Endpoints

After OSH starts, these endpoints should be public and should not require auth.

Protected resource metadata:

```bash
curl -i https://osh.example.com/.well-known/oauth-protected-resource/sensorhub/mcp
```

Expected fields:

```json
{
  "resource": "https://osh.example.com/sensorhub/mcp",
  "authorization_servers": [
    "https://keycloak.example.com/realms/osh-mcp"
  ],
  "scopes_supported": [
    "openid",
    "profile",
    "email",
    "mcp",
    "offline_access"
  ],
  "bearer_methods_supported": [
    "header"
  ]
}
```

Authorization server metadata:

```bash
curl -i https://osh.example.com/.well-known/oauth-authorization-server/sensorhub/mcp
```

Expected fields include:

```text
issuer
authorization_endpoint
token_endpoint
jwks_uri
registration_endpoint
code_challenge_methods_supported: S256
```

Unauthenticated MCP requests should return `401` and a `WWW-Authenticate`
header:

```bash
curl -i https://osh.example.com/sensorhub/mcp
```

Expected header:

```text
WWW-Authenticate: Bearer resource_metadata="https://osh.example.com/.well-known/oauth-protected-resource/sensorhub/mcp", scope="mcp"
```

## 5. Verify a Token

For local development only, you can request a token with the password grant:

```bash
curl -s http://localhost:8180/realms/osh-mcp/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=mcp-client' \
  -d 'username=mcp-user' \
  -d 'password=mcp-password' \
  -d 'scope=openid profile email mcp'
```

Copy the returned `access_token`.

Then test MCP initialization:

```bash
curl -i http://localhost:8080/sensorhub/mcp \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H "Authorization: Bearer <access_token>" \
  --data-binary '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl-test","version":"1.0.0"}}}'
```

Expected result:

```text
HTTP/1.1 200 OK
Mcp-Session-Id: <session-id>
```

The response body should include:

```json
{
  "protocolVersion": "2025-06-18",
  "serverInfo": {
    "name": "OpenSensorHub"
  }
}
```

## 6. Configure MCP Clients

### Claude Code

From the OSH project root:

```bash
claude mcp add --transport http osh https://osh.example.com/sensorhub/mcp
```

Then check:

```bash
claude mcp get osh
```

If Claude Code dynamic registration is enabled and Keycloak policy allows the
requested scopes, Claude should start OAuth login automatically.

For a quick bearer-token smoke test:

```bash
claude mcp add --transport http osh-auth https://osh.example.com/sensorhub/mcp \
  -H "Authorization: Bearer <access_token>"

claude mcp get osh-auth
```

Remove the token-backed test entry afterwards:

```bash
claude mcp remove osh-auth -s local
```

### Codex

Persistent config:

```bash
codex mcp add osh \
  --url https://osh.example.com/sensorhub/mcp \
  --oauth-client-id mcp-client \
  --oauth-resource https://osh.example.com/sensorhub/mcp

codex mcp login osh --scopes openid,profile,email,mcp
```

Bearer-token smoke test:

```bash
export OSH_MCP_TOKEN=<access_token>

codex mcp list \
  -c 'mcp_servers.osh.url="https://osh.example.com/sensorhub/mcp"' \
  -c 'mcp_servers.osh.bearer_token_env_var="OSH_MCP_TOKEN"'
```

## 7. Troubleshooting

`401 Bearer token required`

The client did not send an `Authorization: Bearer` header.

`401 Invalid bearer token`

Check that:

- `oauth.issuer` matches the token `iss`
- `oauth.jwksUri` is reachable from OSH
- token `aud` equals the public MCP endpoint URL
- token `scope` includes `mcp`
- token is not expired

`audience mismatch`

Update the Keycloak `mcp-resource-audience` mapper so the included custom
audience exactly matches the MCP resource URL:

```text
https://osh.example.com/sensorhub/mcp
```

`Policy 'Allowed Client Scopes' rejected request`

Claude Code or another client tried dynamic registration, but Keycloak rejected
one of the requested scopes. Update the Keycloak registration policy to allow:

```text
openid profile email mcp offline_access
```

or use a pre-created public client such as `mcp-client`.

`Offline tokens not allowed`

Move `offline_access` from default client scopes to optional client scopes.

`Origin not allowed`

Add the browser origin to `oauth.allowedOrigins`, for example:

```text
https://osh.example.com
```

CLI clients usually do not send an `Origin` header, so this mostly affects
browser-based MCP tools.
