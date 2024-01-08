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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.IUserInfo;


public class OAuthUser implements IUserInfo
{
    private final String userID;
    private final Collection<String> roles;
    
    
    public OAuthUser(String userID, Collection<String> roles)
    {
        this.userID = userID;
        this.roles = roles;
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
    public Collection<IPermissionPath> getAllowList()
    {
        return Collections.emptyList();
    }


    @Override
    public Collection<IPermissionPath> getDenyList()
    {
        return Collections.emptyList();
    }


    @Override
    public String getId()
    {
        return userID;
    }


    @Override
    public String getPassword()
    {
        return "";
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

}
