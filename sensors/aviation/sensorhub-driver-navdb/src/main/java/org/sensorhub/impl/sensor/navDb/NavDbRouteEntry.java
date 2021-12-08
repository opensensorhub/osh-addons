/***************************** BEGIN COPYRIGHT BLOCK **************************

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

Notice: All information contained herein is, and remains the property of
Delta Air Lines, Inc. The intellectual and technical concepts contained herein
are proprietary to Delta Air Lines, Inc. and may be covered by U.S. and Foreign
Patents, patents in process, and are protected by trade secret or copyright law.
Dissemination, reproduction or modification of this material is strictly
forbidden unless prior written permission is obtained from Delta Air Lines, Inc.

******************************* END COPYRIGHT BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Entry class used to store info for routes (list of fixes):<br/>
 * airways, SIDs, STARs
 * </p>
 *
 * @author Alex Robin
 * @since Nov 11, 2021
 */
public class NavDbRouteEntry extends NavDbEntry
{
    public enum RouteType { RUNWAY, COMMON, ENROUTE }
    public List<NavDbEntryRef> fixes = new ArrayList<>();
    public String transitionId = ""; // ID of transition for SIDs/STARs
    public char routeTypeCode = 0;
    public RouteType routeType;
    
    
    public NavDbRouteEntry(Type type, String id)
    {
        super(type, id);
    }
    
    
    public void addFix(Type type, String fixId)
    {
        fixes.add(new NavDbEntryRef(type, fixId));
    }


    @Override
    public String toString()
    {
        return type + "," + region + "," + airport + "," + id + "," + transitionId + ": " + fixes;
    }


    @Override
    public int compareTo(NavDbEntry o)
    {
        int comp = super.compareTo(o);
        
        if (type != Type.AIRWAY && comp == 0)
            comp = transitionId.compareTo(((NavDbRouteEntry)o).transitionId);
        
        return comp;
    }
}
