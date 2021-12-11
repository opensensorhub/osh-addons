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

import org.sensorhub.impl.sensor.navDb.NavDbEntry.Type;


public class NavDbEntryRef
{
    public Type type;
    public String id;
    public boolean boundaryCrossing = false;
    
    
    public NavDbEntryRef(Type type, String id)
    {
        this.type = type;
        this.id = id;
    }
    
    
    public NavDbEntryRef(Type type, String id, boolean boundaryCrossing)
    {
        this(type, id);
        this.boundaryCrossing = boundaryCrossing;
    }
    
    
    public String toString()
    {
        return id;
    }
}