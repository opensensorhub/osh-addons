/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.flightAware;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.flightAware.DecodeFlightRouteResponse.Waypoint;
import org.sensorhub.impl.sensor.navDb.NavDatabase;
import org.sensorhub.impl.sensor.navDb.NavDatabase.RouteDecodeOutput;
import org.sensorhub.impl.sensor.navDb.NavDbPointEntry;
import org.slf4j.Logger;
import com.google.common.base.Strings;


/**
 * Decoder implementation using the ARINC 424 navigation database provided
 * in a separate OSH module
 * @author Alex Robin
 * @since Nov 26, 2021
 */
public class FlightRouteDecoderNavDb implements IFlightRouteDecoder
{
    Logger log;
    NavDatabase navDB;
        
    
    public FlightRouteDecoderNavDb(FlightAwareDriver driver, String navDbModuleID)
    {
        this.log = driver.getLogger();
        this.navDB = NavDatabase.getInstance(navDbModuleID);
    }
    
    
    @Override
    public List<Waypoint> decode(FlightObject fltPlan, String route) throws SensorHubException
    {
        try
        {
            // call decoder
            RouteDecodeOutput decodeOut = navDB.decodeRoute(route);
            if (decodeOut == null || decodeOut.decodedRoute == null || decodeOut.decodedRoute.isEmpty())
                throw new SensorHubException("Empty response from route decoder");
            int numWaypoints = decodeOut.decodedRoute.size();
            
            // build waypoint list and set altitude according to filed altitude
            int i = 0;
            ArrayList<Waypoint> waypoints = new ArrayList<>(numWaypoints);
            for (NavDbPointEntry entry: decodeOut.decodedRoute)
            {
                Waypoint wp = new Waypoint(entry.id, entry.type.toString(), entry.lat, entry.lon);
                if (i == 0 || i == numWaypoints-1)
                    wp.altitude = 0.0;
                else if (!Strings.nullToEmpty(fltPlan.alt).trim().isEmpty())
                    wp.altitude = Double.parseDouble(fltPlan.alt);
                i++;
            }
            
            return waypoints;
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error decoding Firehose route using ARINC database", e);
        }
    }

}
