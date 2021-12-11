/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.navDb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.sensorhub.impl.sensor.navDb.NavDbEntry.Type;
import org.sensorhub.impl.sensor.navDb.NavDbRouteEntry.RouteType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Parser for navigation DB in ARINC 424 format
 * </p>
 *
 * @author Tony Cook
 * @since Nov, 2017
 */
public class Arinc424Parser
{
    static final Logger log = LoggerFactory.getLogger(Arinc424Parser.class);


    // DDMMSS.SS or DDDMMSS.ss
    // N30114030,W097401160
    private static Double parseCoordString(String s) throws NumberFormatException
    {
        char hemi = s.charAt(0);
        String dd, mm, ss;
        
        if (hemi == 'N' || hemi == 'S')
        {
            dd = s.substring(1, 3);
            mm = s.substring(3, 5);
            ss = s.substring(5, 9);
        }
        else if (hemi == 'E' || hemi == 'W')
        {
            dd = s.substring(1, 4);
            mm = s.substring(4, 6);
            ss = s.substring(6, 10);
        }
        else
        {
            throw new NumberFormatException("Invalid coordinate string: " + s);
        }
        
        double deg = Double.parseDouble(dd);
        double min = Double.parseDouble(mm);
        double sec = Double.parseDouble(ss);
        sec = sec / 100.;
        double coord = deg + (min / 60.) + (sec / 3600.);
        if (hemi == 'S' || hemi == 'W')
            coord = -1 * coord;
        
        return coord;
    }


    private static NavDbPointEntry parseAirport(String l)
    {
        String region = l.substring(1, 4).trim();
        String id = l.substring(6, 10).trim();
        String lats = l.substring(32, 41);
        String lons = l.substring(41, 51);
        String name = l.substring(93, 123).trim();
        double lat;
        double lon;
        
        try
        {
            lat = parseCoordString(lats);
            lon = parseCoordString(lons);
        }
        catch (NumberFormatException e)
        {
            log.error("Error parsing airport record: {}", l, e);
            return null;
        }
        
        NavDbPointEntry airport = new NavDbPointEntry(Type.AIRPORT, id, lat, lon);
        airport.name = name;
        airport.region = region;
        airport.airport = airport.id;
        return airport;
    }


    private static NavDbPointEntry parseWaypoint(String l)
    {
        String region = l.substring(1, 4).trim();
        String icao = l.substring(6, 10).trim();
        String id = l.substring(13, 18).trim();
        String lats = l.substring(32, 41);
        String lons = l.substring(41, 51);
        String name = l.substring(98, 122).trim();
        double lat;
        double lon;
        
        try
        {
            lat = parseCoordString(lats);
            lon = parseCoordString(lons);
        }
        catch (NumberFormatException e)
        {
            log.error("Error parsing waypoint record: {}", l, e);
            return null;
        }
        
        NavDbPointEntry waypoint = new NavDbPointEntry(Type.WAYPOINT, id, lat, lon);
        waypoint.name = name;
        waypoint.region = region;
        waypoint.airport = icao;
        return waypoint;
    }


    private static NavDbPointEntry parseNavaid(String l)
    {
        String region = l.substring(1, 4).trim();
        String id = l.substring(13, 17).trim();
        String name = l.substring(93, 123).trim();
        String lats, lons;
        
        // read VOR lat/lon
        if (l.charAt(32) != ' ')
        {
            lats = l.substring(32, 41);
            lons = l.substring(41, 51);
        }
        
        // else read DME lat/lon
        else
        {
            lats = l.substring(55, 64);
            lons = l.substring(64, 74);
        }
        
        double lat, lon;
        try
        {
            lat = parseCoordString(lats);
            lon = parseCoordString(lons);
        }
        catch (NumberFormatException e)
        {
            log.error("Error parsing navaid record: {}", l, e);
            return null;
        }

        NavDbPointEntry navaid = new NavDbPointEntry(Type.NAVAID, id, lat, lon);
        navaid.name = name;
        navaid.region = region;
        return navaid;
    }


    private static NavDbRouteEntry parseAirway(NavDbRouteEntry airway, String l)
    {
        String region = l.substring(1, 4).trim();
        String id = l.substring(13, 18).trim();
        String fixId = l.substring(29, 34).trim();
        String fixType = l.substring(36, 38).trim();
        boolean boundaryCrossing = l.charAt(43) != ' ';
        
        if (airway == null || !id.equals(airway.id))
        {
            airway = new NavDbRouteEntry(Type.AIRWAY, id);
            airway.region = region;
        }
        
        airway.addFix(parseRecordType(fixType), fixId, boundaryCrossing);
        
        return airway;
    }


    private static NavDbRouteEntry parseSID(NavDbRouteEntry sid, String l)
    {
        return parseSIDSTAR(sid, l, true);
    }


    private static NavDbRouteEntry parseSTAR(NavDbRouteEntry star, String l)
    {
        return parseSIDSTAR(star, l, false);
    }
    
    
    private static NavDbRouteEntry parseSIDSTAR(NavDbRouteEntry prevEntry, String l, boolean isSID)
    {
        String region = l.substring(1, 4).trim();
        String icao = l.substring(6, 10).trim();
        String id = l.substring(13, 19).trim();
        char transType = l.charAt(19);
        String transId = l.substring(20, 25).trim();
        String fixId = l.substring(29, 34).trim();
        String fixType = l.substring(36, 38).trim();
        RouteType routeType = null;
        
        if (isSID) // SID case
        {
            // set route type
            if (transType == '1' || transType == '4' || transType == 'F' || transType == 'T')
                routeType = RouteType.RUNWAY;
            else if (transType == '2' || transType == '5' || transType == 'M')
                routeType = RouteType.COMMON;
            else if (transType == '3' || transType == '6' || transType == 'S' || transType == 'V')
                routeType = RouteType.ENROUTE;
        }
        else // STAR case
        {
            // set route type
            if (transType == '1' || transType == '4' || transType == '7' || transType == 'F')
                routeType = RouteType.ENROUTE;
            else if (transType == '2' || transType == '5' || transType == '8' || transType == 'M')
                routeType = RouteType.COMMON;
            else if (transType == '3' || transType == '6' || transType == '9' || transType == 'S')
                routeType = RouteType.RUNWAY;
        }
        
        // create a new route entry for each ID/airport/transition ID combination
        if (prevEntry == null || !id.equals(prevEntry.id) || !icao.equals(prevEntry.airport)|| !transId.equals(prevEntry.transitionId))
        {
            prevEntry = new NavDbRouteEntry(Type.SID, id);
            prevEntry.region = region;
            prevEntry.airport = icao;
            prevEntry.transitionId = transId;
            prevEntry.routeTypeCode = transType;
            prevEntry.routeType = routeType;
        }
        
        // add fix to route entry
        if (!fixType.isEmpty() && !fixType.equals("PG")) // skip empty and runway fixes
            prevEntry.addFix(parseRecordType(fixType), fixId);
        
        return prevEntry;
    }
    
    
    static Type parseRecordType(String type)
    {
        if ("PA".equals(type))
            return Type.AIRPORT;
        if ("EA".equals(type) || "PC".equals(type))
            return Type.WAYPOINT;
        else if ("D".equals(type) || "DB".equals(type) || "PN".equals(type))
            return Type.NAVAID;
        
        log.error("Unknown fix type: {}", type);
        return null;
    }


    public static void loadEntries(String dbPath, NavDatabase db) throws IOException
    {
        try (BufferedReader br = new BufferedReader(new FileReader(dbPath)))
        {
            char subCode;
            char continuationNum = 0;
            NavDbPointEntry entry;
            NavDbRouteEntry route = null;
            String line;
            
            while ((line = br.readLine()) != null)
            {
                switch (line.charAt(4)) // main section code
                {
                    case 'P':
                        //  if character at 5 is not blank, this is not a valid P record
                        if (line.charAt(5) != ' ')
                            continue;
                        
                        subCode = line.charAt(12);
                        if (subCode == 'A') // airport
                        {
                            // skip continuation records
                            continuationNum = line.charAt(21);
                            if (continuationNum > '1')
                                continue;
                            
                            entry = parseAirport(line);
                            if (entry != null)
                            {
                                NavDbPointEntry old = db.airports.put(entry.id, entry);
                                if (old != null)
                                    log.debug("Duplicate airport: " + entry);
                            }
                        }
                        else if (subCode == 'C') // terminal waypoint
                        {
                            // skip continuation records
                            continuationNum = line.charAt(21);
                            if (continuationNum > '1')
                                continue;
                            
                            entry = parseWaypoint(line);
                            if (entry != null)
                            {
                                boolean added = db.waypoints.put(entry.id, entry);
                                if (!added)
                                    log.debug("Duplicate waypoint: " + entry);
                            }
                        }
                        else if (subCode == 'N') // terminal navaid
                        {
                            // skip continuation records
                            continuationNum = line.charAt(21);
                            if (continuationNum > '1')
                                continue;
                            
                            entry = parseNavaid(line);
                            if (entry != null)
                            {
                                boolean added = db.navaids.put(entry.id, entry);
                                if (!added)
                                    log.debug("Duplicate navaid: " + entry);
                            }
                        }
                        else if (subCode == 'D') // SID
                        {
                            // skip continuation records
                            continuationNum = line.charAt(38);
                            //if (continuationNum > '1')
                            //    continue;
                            
                            NavDbRouteEntry previousRoute = route;
                            route = parseSID(route, line);
                            if (route != null && previousRoute != route)
                                db.sids.put(route.id, route);
                        }
                        else if (subCode == 'E') // STAR
                        {
                            // skip continuation records
                            continuationNum = line.charAt(38);
                            //if (continuationNum > '1')
                            //    continue;
                            
                            NavDbRouteEntry previousRoute = route;
                            route = parseSTAR(route, line);
                            if (route != null && previousRoute != route)
                                db.stars.put(route.id, route);
                        }
                        break;
                        
                    case 'D':
                        // skip continuation records
                        continuationNum = line.charAt(21);
                        if (continuationNum > '1')
                            continue;
                        
                        subCode = line.charAt(5);
                        if (subCode == 'B' || subCode == ' ') // navaid
                        {
                            entry = parseNavaid(line);
                            if (entry != null)
                            {
                                boolean added = db.navaids.put(entry.id, entry);
                                if (!added)
                                    log.debug("Duplicate navaid: " + entry);
                            }
                        }
                        
                        break;
                        
                    case 'E':
                        subCode = line.charAt(5);
                        if (subCode == 'A') // enroute waypoint
                        {
                            // skip continuation records
                            continuationNum = line.charAt(21);
                            if (continuationNum > '1')
                                continue;
                            
                            entry = parseWaypoint(line);
                            if (entry != null)
                            {
                                boolean added = db.waypoints.put(entry.id, entry);
                                if (!added)
                                    log.debug("Duplicate waypoint: " + entry);
                            }
                        }
                        else if (subCode == 'R') // enroute airway
                        {
                            // skip continuation records
                            continuationNum = line.charAt(38);
                            if (continuationNum > '1')
                                continue;
                            
                            NavDbRouteEntry previousRoute = route;
                            route = parseAirway(route, line);
                            if (route != null && previousRoute != route)
                                db.airways.put(route.id, route);
                        }
                        break;
                        
                    default:
                        continue;
                }
            }
        }
    }
}