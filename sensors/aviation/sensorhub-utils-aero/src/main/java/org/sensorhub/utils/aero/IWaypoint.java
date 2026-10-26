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
    
    // list of info tags
    // A TAG STRING MUST NOT BE A SUBSTRING OF ANOTHER TAG
    
    /**
     * Part of a SID (SID procedure code provided after ':')
     */
    public static final String SID_TAG = "SID:";
    
    /**
     * Part of a STAR (STAR procedure code provided after ':')
     */
    public static final String STAR_TAG = "STAR:";
    
    /**
     * Part of a Airway (Airway code provided after ':')
     */
    public static final String AIRWAY_TAG = "AWY:";
    
    /**
     * Redispatch waypoint = required to pass through it for fuel check
     */
    public static final String REDISPATCH_TAG = "REDISPATCH";
    
    /**
     * Enter ETOPS part of flight plan
     */
    public static final String ETOPS_ENTRY_TAG = "ETOPS_ENTRY";
    
    /**
     * Exit ETOPS part of flight plan
     */
    public static final String ETOPS_EXIT_TAG = "ETOPS_EXIT";
    
    /**
     * New waypoint that is not part of the current active route
     */
    public static final String ALTERNATE_TAG = "ALTERNATE";
    
    /**
     * First waypoint after a route change (usually PPOS)
     */
    public static final String ROUTE_CHANGE_TAG = "ROUTE_CHANGE";

    /**
     * Required waypoint
     */
    public static final String REQUIRED_TAG = "REQUIRED";

    
    /**
     * @return Waypoint code (3 to 5 letters ICAO code) 
     */
    String getCode();
    
    /**
     * @return Type of waypoint (null if unknown)
     */
    default String getType()
    {
        return "UKN";
    }
    
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
    default double getBaroAltitude()
    {
        return Double.NaN;
    }
    
    /**
     * @return Waypoint info (comma separated list of tags, null if none provided)
     */
    default String getInfo()
    {
        return null;
    }
    
    /**
     * @return true if tag is part of the tag list included in the info field, false otherwise
     */
    public default boolean hasTag(String tag)
    {
        var info = getInfo();
        return info != null && info.contains(tag);
    }
}
