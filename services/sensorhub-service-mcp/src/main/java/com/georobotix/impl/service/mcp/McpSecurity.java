/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.georobotix.impl.service.mcp;

import org.sensorhub.impl.module.ModuleSecurity;


public class McpSecurity extends ModuleSecurity
{
    public McpSecurity(McpService service, boolean enable)
    {
        super(service, "mcp", enable);

        // register permission tree
        service.getParentHub().getSecurityManager().registerModulePermissions(rootPerm);
        // TODO Add permissions for resources / tools
    }
    
    
//    @Override
//    public void checkResourcePermission(IPermission perm, String id) throws IOException
//    {
//        checkPermission(perm);
//    }
//
//
//    @Override
//    public void checkParentPermission(IPermission perm, String parentId) throws IOException
//    {
//        checkPermission(perm);
//    }
}
