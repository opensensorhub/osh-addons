/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.drivers;

import org.sensorhub.api.sensor.SensorDriverConfig;


/**
 * <p><b>Title:</b>
 * USBDriverConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Driver configuration options for the USB hardware interface
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class USBDriverConfig extends SensorDriverConfig
{
	private static final long serialVersionUID = -5168999795369453594L;
	

    public int deviceID;
	
	public int deviceClass;
}
