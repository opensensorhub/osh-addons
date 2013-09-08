/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.sensorhub.impl.service.ogc.OgcServiceConfig;


/**
 * <p><b>Title:</b>
 * SOSServiceConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Configuration class for the SOS service module
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class SOSServiceConfig extends OgcServiceConfig
{
    private static final long serialVersionUID = -957079629610700869L;

    
    /**
     * Providers configuration
     */
    public SOSProviderConfig[] dataProviders;
}
