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
 * the MQTT password. The OAuth module (sensorhub-security-oauth) checks the
 * tokens and adds the users: the settings of the token issuer, of the claims
 * and of the users are in the config of that module, and they apply to HTTP
 * and MQTT. When token login is on, a user can log in with a token only,
 * unless {@link #allowPasswordLogin} is set.
 * </p>
 */
public class TokenAuthConfig
{
    @DisplayInfo(label="Enabled", desc="Accept access tokens as the MQTT password. The OAuth module must be on the node, with its bearer token settings")
    public boolean enabled = false;


    @DisplayInfo(label="Allow Password Login", desc="Also accept the password of an OSH user when token login is on. If off, a user can log in with a token only: the server refuses passwords, also of local users")
    public boolean allowPasswordLogin = false;
}
