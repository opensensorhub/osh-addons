/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import org.sensorhub.api.sensor.SensorConfig;


public class FakeGpsConfig extends SensorConfig
{
    private static final long serialVersionUID = 1L;
    
    
    public String googleApiUrl = "http://maps.googleapis.com/maps/api/directions/json";
    
    public double centerLatitude; // in deg
    
    public double centerLongitude; // in deg
    
    public double areaSize = 0.1; // in deg
    
    public double vehicleSpeed = 40; // km/h
}
