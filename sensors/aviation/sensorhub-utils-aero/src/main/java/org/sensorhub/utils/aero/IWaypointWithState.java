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

import java.time.Instant;

public interface IWaypointWithState extends IWaypoint
{
    /**
     * @return Estimated time at waypoint (UTC)
     */
    Instant getTime();
    
    
    /**
     * @return Estimated ground speed at waypoint (knot)
     */
    double getGroundSpeed();
    
    
    /**
     * @return Estimated true airspeed (TAS) at waypoint (knot)
     */
    double getTrueAirSpeed();
    
    
    /**
     * @return Estimated mach at waypoint
     */
    double getMach();
    
    
    /**
     * @return Estimated fuel on board at waypoint (lbs)
     */
    double getFuelOnBoard();
}