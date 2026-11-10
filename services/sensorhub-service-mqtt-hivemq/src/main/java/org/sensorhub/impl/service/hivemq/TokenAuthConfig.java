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

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Settings to accept OAuth 2.0 access tokens (for example from Keycloak) as
 * the MQTT password. Token login is on when {@link #jwksUri} is set.
 * The names are the same as in the bearer token settings of the OAuth module.
 * </p>
 */
public class TokenAuthConfig
{
    @DisplayInfo(label="JSON Web Key Set Endpoint", desc="URL of the public keys of the token issuer (JWKS). Set it to accept access tokens as the MQTT password. Keycloak: {realm URL}/protocol/openid-connect/certs")
    public String jwksUri;


    @DisplayInfo(label="Token Issuer", desc="Issuer that the token must have (iss claim). Keycloak: the realm URL. Required when token login is on")
    public String issuer;


    @DisplayInfo(label="Token Audience", desc="Audience that the token must have (aud claim). Leave empty to skip the audience check")
    public String audience;


    @DisplayInfo(label="Username Claim", desc="Token claim that must be equal to the MQTT username")
    public String userIdField = "preferred_username";


    @DisplayInfo(label="JWK Cache Size", desc="Number of public keys to keep in cache")
    public int cacheSize = 5;


    @DisplayInfo(label="JWK Cache Duration", desc="Time in minutes to keep a public key in cache")
    public int cacheDuration = 60;
}
