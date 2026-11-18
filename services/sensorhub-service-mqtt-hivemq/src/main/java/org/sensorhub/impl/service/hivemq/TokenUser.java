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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.IUserInfo;


/**
 * <p>
 * A user that the MQTT server added to the OSH user registry after a token
 * login. Its roles are the roles of the token. It has no permissions of its
 * own.
 * </p>
 */
final class TokenUser implements IUserInfo
{
    static final SecureRandom RANDOM = new SecureRandom();

    final String userID;
    final Collection<String> roles;

    // The user logs in with a token only. A random value that nobody knows
    // makes sure that no password login (MQTT or HTTP) can match.
    final String password = new BigInteger(256, RANDOM).toString(36);


    TokenUser(String userID, Collection<String> roles)
    {
        this.userID = userID;
        this.roles = List.copyOf(roles);
    }


    @Override
    public String getId()
    {
        return userID;
    }


    @Override
    public String getName()
    {
        return userID;
    }


    @Override
    public String getDescription()
    {
        return null;
    }


    @Override
    public String getPassword()
    {
        return password;
    }


    @Override
    public Collection<String> getRoles()
    {
        return roles;
    }


    @Override
    public Map<String, Object> getAttributes()
    {
        return Collections.emptyMap();
    }


    @Override
    public Collection<IPermissionPath> getAllowList()
    {
        return Collections.emptyList();
    }


    @Override
    public Collection<IPermissionPath> getDenyList()
    {
        return Collections.emptyList();
    }
}
