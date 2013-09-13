/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.v4l;

import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * Configuration class for the generic Video4Linux camera driver
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
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
