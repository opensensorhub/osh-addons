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
    WINDS
}
