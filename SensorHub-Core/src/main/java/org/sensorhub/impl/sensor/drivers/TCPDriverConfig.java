/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2010, Sensia Software LLC
 All Rights Reserved.

 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.drivers;

import java.net.InetAddress;
import org.sensorhub.api.sensor.SensorDriverConfig;


/**
 * <p><b>Title:</b>
 * TCPDriverConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Driver configuration options for the TCP/IP network protocol
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @date Nov 5, 2010
 */
public class TCPDriverConfig extends SensorDriverConfig
{
	private static final long serialVersionUID = 9140070588893081102L;
	

    public InetAddress networkAddress;
	
	public int port;
	
	public boolean discoverAddress;
}
