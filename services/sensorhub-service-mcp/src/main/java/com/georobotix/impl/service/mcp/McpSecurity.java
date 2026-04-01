package com.georobotix.impl.service.mcp;

import org.sensorhub.impl.module.ModuleSecurity;


public class McpSecurity extends ModuleSecurity
{
    public McpSecurity(McpService service, boolean enable)
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
