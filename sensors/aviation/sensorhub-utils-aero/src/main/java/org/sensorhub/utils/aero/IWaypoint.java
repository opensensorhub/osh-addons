/***************************** BEGIN COPYRIGHT BLOCK **************************

Copyright (C) 2025 Delta Air Lines, Inc. All Rights Reserved.

Notice: All information contained herein is, and remains the property of
Delta Air Lines, Inc. The intellectual and technical concepts contained herein
are proprietary to Delta Air Lines, Inc. and may be covered by U.S. and Foreign
Patents, patents in process, and are protected by trade secret or copyright law.
Dissemination, reproduction or modification of this material is strictly
forbidden unless prior written permission is obtained from Delta Air Lines, Inc.

******************************* END COPYRIGHT BLOCK ***************************/

package org.sensorhub.utils.aero;


public interface IWaypoint
{
    public enum WaypointType
    {
        AIRPORT,
        NAVAID,
        WAYPOINT
    }
    
    public static final String REDISPATCH_TAG = "REDISPATCH";
    public static final String ETOPS_ENTRY_TAG = "ETOPS_ENTRY";
    public static final String ETOPS_EXIT_TAG = "ETOPS_EXIT";
    public static final String ALTERNATE_TAG = "ALTERNATE";
    
    /**
     * First waypoint after a route change (usually PPOS)
     */
    public static final String ROUTE_CHANGE_TAG = "ROUTE_CHANGE";

    
    /**
     * @return Waypoint code (3 to 5 letters ICAO code) 
     */
    String getCode();
    
    /**
     * @return Type of waypoint (null if unknown)
     */
    String getType();
    
    /**
     * @return Geodetic latitude of waypoint (deg, WGS84)
     */
    double getLatitude();
    
    /**
     * @return Longitude of waypoint (deg, WGS84)
     */
    double getLongitude();
    
    /**
     * @return Barometric altitude at waypoint (ft)
     */
    double getBaroAltitude();
    
    /**
     * @return Waypoint info (null if none provided)
     */
    String getInfo();
}