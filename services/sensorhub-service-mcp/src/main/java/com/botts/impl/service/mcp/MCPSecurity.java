package com.botts.impl.service.mcp;

import java.io.IOException;
import org.sensorhub.api.security.IAuthorizer;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;
import org.sensorhub.impl.security.ModulePermissions;


public class MCPSecurity extends ModuleSecurity
{
    public MCPSecurity(MCPService service, boolean enable)
    {
        super(service, "mcp", enable);

        // register permission tree
        service.getParentHub().getSecurityManager().registerModulePermissions(rootPerm);
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
