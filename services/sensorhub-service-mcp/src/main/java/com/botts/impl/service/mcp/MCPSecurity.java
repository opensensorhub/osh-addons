package com.botts.impl.service.mcp;

import java.io.IOException;
import org.sensorhub.api.security.IAuthorizer;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;
import org.sensorhub.impl.security.ModulePermissions;


public class MCPSecurity extends ModuleSecurity
{
    private static final String NAME_FOI = "fois";
    private static final String LABEL_FOI = "Features of Interest";
    
    public final IPermission api_get;
    public final IPermission api_list;
    public final IPermission api_create;
    public final IPermission api_update;
    public final IPermission api_delete;
    public final IPermission api_stream;
    
    public MCPSecurity(MCPService service, boolean enable)
    {
        
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
