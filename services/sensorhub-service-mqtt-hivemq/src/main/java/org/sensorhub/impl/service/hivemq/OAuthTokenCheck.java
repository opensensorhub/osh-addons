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

import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.impl.security.oauth.OAuthClient;
import org.sensorhub.impl.security.oauth.OAuthUser;


/**
 * <p>
 * The calls to the OAuth module (sensorhub-security-oauth). Only this class
 * and {@link TokenUser} use the classes of the OAuth module, so that the MQTT
 * server also starts on a node that does not have this module. The Java VM
 * loads this class at the first token login. If the OAuth module is not on
 * the node, each method then gives a {@link LinkageError}.
 * </p>
 */
final class OAuthTokenCheck
{
    private OAuthTokenCheck()
    {
    }


    /**
     * Makes sure that the node has the classes of the OAuth module, in a
     * version that has the token check
     * @throws LinkageError if the jar is not on the node or is too old
     */
    static void checkAvailable()
    {
        try
        {
            OAuthClient.class.getMethod("checkBearerToken", String.class);
        }
        catch (NoSuchMethodException e)
        {
            throw new NoSuchMethodError(e.getMessage());
        }
    }


    /**
     * @return the OAuth module of the node, or null if the node has none
     */
    static IModule<?> module(ISensorHub hub)
    {
        return hub.getModuleRegistry().getModuleByType(OAuthClient.class);
    }


    /**
     * @return the user ID and the roles of the token, or null if the token
     * is refused or the OAuth module is not started
     */
    static IUserInfo checkToken(IModule<?> oauthModule, String token)
    {
        return ((OAuthClient)oauthModule).checkBearerToken(token);
    }


    /**
     * @return the autoAddUser setting of the OAuth module
     */
    static boolean autoAddUser(IModule<?> oauthModule)
    {
        return ((OAuthClient)oauthModule).getConfiguration().autoAddUser;
    }


    /**
     * @return true if a token login added this user (HTTP or MQTT)
     */
    static boolean isTokenLoginUser(IUserInfo user)
    {
        return user instanceof OAuthUser;
    }


    /**
     * @return the user that the MQTT server puts in the OSH user registry
     */
    static IUserInfo newTokenUser(IUserInfo tokenUser)
    {
        return new TokenUser(tokenUser);
    }
}
