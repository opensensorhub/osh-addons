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
 * RS232DriverConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Driver configuration options for the RS232 hardware interface
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class RS232DriverConfig extends SensorDriverConfig
{
	private static final long serialVersionUID = 7284981188037101005L;
	

    /**
	 * Try to detect newly connected device by polling
	 */
    public boolean autodetect;
	
	public int autodetectInterval;
	
	public int portNumber;
	
	public int baudRate;
	
	public int numberOfBits;
	
	public boolean parity;
}
