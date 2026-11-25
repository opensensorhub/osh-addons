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

import java.math.BigInteger;
import java.security.SecureRandom;
import org.sensorhub.impl.security.oauth.OAuthUser;


/**
 * <p>
 * The user that the MQTT server adds to the OSH user registry for a token
 * login. It has the user ID and the roles that the OAuth module read from the
 * token. The user logs in with a token only: its password is a random value
 * that nobody knows, so that no password login (MQTT or HTTP) can match.
 * </p>
 */
final class TokenUser extends OAuthUser
{
    static final SecureRandom RANDOM = new SecureRandom();

    final String password = new BigInteger(256, RANDOM).toString(36);


    TokenUser(OAuthUser tokenUser)
    {
        super(tokenUser.getId(), tokenUser.getRoles());
    }


    @Override
    public String getPassword()
    {
        return password;
    }
}
