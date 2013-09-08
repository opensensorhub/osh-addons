/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import org.sensorhub.impl.service.ogc.OgcServiceConfig;


/**
 * <p><b>Title:</b>
 * SPSServiceConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Configuration class for the SPS service module
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 6, 2013
 */
public class SPSServiceConfig extends OgcServiceConfig
{
    private static final long serialVersionUID = 7925901527120268160L;
    
    
    /**
     * List of local IDs of sensors managed by this SPS.
     * The service will attempt to connect to the control interface of
     * each sensor when available.
     */
    public String[] managedSensors;
    
    
    // TODO deploy SPS associated to processes within SensorML process chains

}
