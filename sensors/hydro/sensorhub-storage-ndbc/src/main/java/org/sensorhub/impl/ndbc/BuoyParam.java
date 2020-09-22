/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.ndbc;

public enum BuoyParam
{
	// Environmental Scalars
    AIR_PRESSURE_AT_SEA_LEVEL,
    AIR_TEMPERATURE,
    SEA_FLOOR_DEPTH_BELOW_SEA_SURFACE,
    SEA_WATER_ELECTRICAL_CONDUCTIVITY,
    SEA_WATER_SALINITY,
    SEA_WATER_TEMPERATURE,
    //  All Environmental scalars will be grouped in one data record
    ENVIRONMENTAL,
    
    //  Groups
    CURRENTS,
    WAVES,
    WINDS,
    
    //  GPS is not a requestable parameter from NDBC SOS, but is included with 
    //  every response. Client code must handle GPS as a special case 
    GPS;
    
	public static void main(String[] args) throws Exception {
		BuoyParam  p = BuoyParam.valueOf("SEA_WATER_TEMPERATURE");
		System.err.println(p);
	}
}
