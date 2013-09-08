/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.service;

import org.sensorhub.api.module.ModuleConfig;


/**
 * <p><b>Title:</b>
 * ServiceConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Common configuration options for all services
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 16, 2010
 */
public class ServiceConfig extends ModuleConfig
{
    private static final long serialVersionUID = 4242373168411772594L;

    
    /**
     * Name of endpoint.
     * This is the part appended to the common sensorhub URL such as http://server.net/sensorhub/services/
     */
    public String endPoint;
}
