/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security.gxoauth;

import java.security.Principal;
import javax.security.auth.Subject;
import org.eclipse.jetty.server.UserIdentity;


public class OAuthUserIdentity implements UserIdentity
{
    OAuthPrincipal principal;
    String userName;
    
    
    public class OAuthPrincipal implements Principal
    {
        @Override
        public String getName()
        {
            return userName;
        }
        
        @Override
        public String toString()
        {
            return userName;
        }
    }
    
    
    public OAuthUserIdentity(String userName)
    {
        this.userName = userName;
        this.principal = new OAuthPrincipal();
    }
    
    
    @Override
    public Subject getSubject()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Principal getUserPrincipal()
    {
        return principal;
    }


    @Override
    public boolean isUserInRole(String arg0, Scope arg1)
    {
        return false;
    }

}
