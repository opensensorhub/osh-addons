/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.mfapi;

import java.io.IOException;
import org.sensorhub.api.security.IAuthorizer;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;
import org.sensorhub.impl.security.ModulePermissions;
import org.sensorhub.impl.service.sweapi.RestApiSecurity;
import org.sensorhub.impl.service.sweapi.RestApiServlet.ResourcePermissions;


public class MFApiSecurity extends ModuleSecurity implements RestApiSecurity
{
    private static final String NAME_FOI = "fois";
    private static final String LABEL_FOI = "Features of Interest";
    
    public final IPermission api_get;
    public final IPermission api_list;
    public final IPermission api_create;
    public final IPermission api_update;
    public final IPermission api_delete;
    public final IPermission api_stream;
    
    public final ResourcePermissions foi_permissions = new ResourcePermissions();
    
    IAuthorizer authorizer;
    
    
    public MFApiSecurity(MFApiService service, boolean enable)
    {
        super(service, "mfapi", enable);
        
        // register permission structure
        api_get = new ItemPermission(rootPerm, "get");
        foi_permissions.get = new ItemPermission(api_get, NAME_FOI, LABEL_FOI);
        
        api_list = new ItemPermission(rootPerm, "list");
        foi_permissions.list = new ItemPermission(api_list, NAME_FOI, LABEL_FOI);
        
        api_create = new ItemPermission(rootPerm, "create");
        foi_permissions.create = new ItemPermission(api_create, NAME_FOI, LABEL_FOI);
        
        api_update = new ItemPermission(rootPerm, "update");
        foi_permissions.update = new ItemPermission(api_update, NAME_FOI, LABEL_FOI);
        
        api_delete = new ItemPermission(rootPerm, "delete");
        foi_permissions.delete = new ItemPermission(api_delete, NAME_FOI, LABEL_FOI);
        
        api_stream = new ItemPermission(rootPerm, "stream");
        foi_permissions.stream = new ItemPermission(api_stream, NAME_FOI, LABEL_FOI);
        
        
        // register wildcard permission tree usable for all MovingFeature services
        // do it at this point so we don't include specific offering permissions
        ModulePermissions wildcardPerm = rootPerm.cloneAsTemplatePermission("MovingFeature API Services");
        service.getParentHub().getSecurityManager().registerModulePermissions(wildcardPerm);
        
        // register permission tree
        service.getParentHub().getSecurityManager().registerModulePermissions(rootPerm);
    }
    
    
    @Override
    public void checkResourcePermission(IPermission perm, String id) throws IOException
    {
        checkPermission(perm);
    }


    @Override
    public void checkParentPermission(IPermission perm, String parentId) throws IOException
    {
        checkPermission(perm);
    }
}
