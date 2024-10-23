/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2024 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.security.oauth;

import org.sensorhub.api.config.DisplayInfo;

public class OAuthBearerTokenConfig {

    @DisplayInfo(label="Token Issuer", desc="Token claim issuer")
    public String issuer = null;

    @DisplayInfo(label="Token Audience", desc="Token audience claim")
    public String audience = null;

    @DisplayInfo(label="JSON Web Key Set Endpoint", desc="Endpoint to retrieve Web Key Tokens for verification of bearer tokens")
    public String jwksUri = null;

    @DisplayInfo(label="JWK Cache Duration", desc="Length of time in minutes in which to hold a public key in cache")
    public int cacheDuration = 60;
}
