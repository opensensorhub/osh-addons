/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;

import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p><b>Title:</b>
 * V4LCameraConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Configuration class for the generic Video4Linux camera driver
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 6, 2013
 */
public class V4LCameraConfig extends SensorConfig
{
    private static final long serialVersionUID = 8576796293927887843L;
    
    
    /**
     * Name of video device to use
     * example: /dev/video0
     */
    public String deviceName;
    
    
    /**
     * Maximum number of frames that can be kept in storage
     * (These last N frames will be stored in memory)
     */
    public int frameStorageCapacity;
    
    
    /**
     * Default camera params to use on startup
     * These can then be changed with the control interface
     */
    public V4LCameraParams defaultParams = new V4LCameraParams();

}
