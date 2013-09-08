/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.module.ModuleConfig;
import org.vast.sensorML.system.SMLSystem;


/**
 * <p><b>Title:</b>
 * SensorConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Configuration options for sensors/actuators
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class SensorConfig extends ModuleConfig
{
    private static final long serialVersionUID = 2834895717702955136L;


    /**
     * SensorML description of the sensor
     */
    public SMLSystem sensorml;
    
    
    /**
     * Automatically activate sensor when plugged in
     */
    public boolean autoActivate = true;
    
    
    /**
     * Enables/disables maintenance of SensorML history
     */
    public boolean enableHistory = true;
    
    
    /**
     * Allows hiding some of the sensor interfaces
     */
    public Map<String, Boolean> ioMask;
    
        
    /**
     * Driver configuration groups (potentially one for each layer of a protocol stack)
     * Can be included in SensorML in v2.0
     */
    public Map<String, SensorDriverConfig> driverConfigs = new HashMap<String, SensorDriverConfig>();
}
