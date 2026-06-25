/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;
import org.sensorhub.impl.security.ModulePermissions;

/**
 * Defines the security permissions available through the Service
 *
 * @author Nick Garay
 * @since Jan. 7, 2022
 */
public class DiscoveryServiceSecurity extends ModuleSecurity {

    private static final String NAME_DISCOVER = "Discover";
    private static final String NAME_RULES = "Rules";
    private static final String NAME_VIZ = "Visualizations";

    private static final String LABEL_DISCOVER = "Discover DataStreams";
    private static final String LABEL_RULES = "Current Rules";
    private static final String LABEL_VIZ = "Discover Visualizations";

    public final IPermission apiRead;
//    public final IPermission apiCreate;
    public final IPermission apiUpdate;
//    public final IPermission apiDelete;

    public final ResourcePermissions discoveryPermissions = new ResourcePermissions();
    public final ResourcePermissions rulesPermissions = new ResourcePermissions();
    public final ResourcePermissions visualizationPermissions = new ResourcePermissions();

    /**
     * Constructor
     *
     * @param service The parent service instance owning this security profile
     * @param enable  Flag indicating if access control is enabled in the configuration
     */
    public DiscoveryServiceSecurity(DiscoveryService service, boolean enable) {

        super(service, "discoveryService", enable);

        apiRead = new ItemPermission(rootPerm, "get");
        discoveryPermissions.read = new ItemPermission(apiRead, NAME_DISCOVER, LABEL_DISCOVER);
        rulesPermissions.read = new ItemPermission(apiRead, NAME_RULES, LABEL_RULES);
        visualizationPermissions.read = new ItemPermission(apiRead, NAME_VIZ, LABEL_VIZ);

//        apiCreate = new ItemPermission(rootPerm, "create");
//        rulesPermissions.create = new ItemPermission(apiCreate, NAME_RULES, LABEL_RULES);

        apiUpdate = new ItemPermission(rootPerm, "update");
        rulesPermissions.update = new ItemPermission(apiUpdate, NAME_RULES, LABEL_RULES);

//        apiDelete = new ItemPermission(rootPerm, "delete");
//        rulesPermissions.delete = new ItemPermission(apiDelete, NAME_RULES, LABEL_RULES);

        // register wildcard permission tree usable for all SPS services
        // do it at this point so we don't include specific offering permissions
        ModulePermissions wildcardPerm = rootPerm.cloneAsTemplatePermission("Discovery Service Permissions");
        service.getParentHub().getSecurityManager().registerModulePermissions(wildcardPerm);

        // register this instance permission tree
        service.getParentHub().getSecurityManager().registerModulePermissions(rootPerm);
    }
}
