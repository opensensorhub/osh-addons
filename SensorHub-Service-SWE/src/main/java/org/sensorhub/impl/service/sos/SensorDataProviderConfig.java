/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;


/**
 * <p><b>Title:</b>
 * SensorDataProviderConfig
 * </p>
 *
 * <p><b>Description:</b><br/>
 * Configuration class for SOS data providers using the sensor API
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @date Sep 7, 2013
 */
public class SensorDataProviderConfig extends SOSProviderConfig
{
    
    /**
     * Local ID of sensor to use as data source
     */
    public String sensorID;
    
    
    /**
     * Names of sensor output to make available through the SOS
     */
    public String[] selectedOutputs;
    
    
    /**
     * If true, forward "new sensor data" events via the WS-Notification
     * interface of the service
     */
    public boolean activateNotifications;

}
