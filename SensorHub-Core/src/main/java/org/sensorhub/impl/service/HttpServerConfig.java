/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service;

import org.sensorhub.api.module.ModuleConfig;


/**
 * <p><b>Title:</b>
 * HttpServerConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Configuration class for the HTTP server module
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 6, 2013
 */
public class HttpServerConfig extends ModuleConfig
{
    private static final long serialVersionUID = -3737530047358507543L;

    
    /**
     * TCP port where HTTP server will listen
     */
    public int httpPort = 8080;
    
    
    /**
     * Root URL where the server will accept requests
     * This will be the prefix to all servlet URLs
     */
    public String rootURL = "/sensorhub";
    
}
