/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2022 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package com.botts.impl.service.discovery;

import org.sensorhub.api.security.IPermission;

/**
 * Defines the individual permissions for a resource through available through the Service
 *
 * @author Nick Garay
 * @since May 19, 2022
 */
public class ResourcePermissions {

    public IPermission read;
//    public IPermission create;
    public IPermission update;
//    public IPermission delete;
//    public IPermission stream;

    protected ResourcePermissions() {
    }
}
