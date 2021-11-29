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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.navDb.NavDbEntry.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;


/**
 * <p>
 * Navigation database parsed from ARINC 424 file
 * </p>
 *
 * @author Alex Robin
 * @since Nov 9, 2021
 */
public class NavDatabase
{
    static final Logger log = LoggerFactory.getLogger(NavDatabase.class);
    static final long START_TIMEOUT = 30000;
    static final Pattern COORD_WPT_REGEX1 = Pattern.compile("[0-9]{2}[0-9NESW][0-9][0-9NESW]");
    static final Pattern COORD_WPT_REGEX2 = Pattern.compile("[0-9]{4}[NS]/[0-9]{5}[EW]");
    
    Map<String, NavDbPointEntry> airports = new TreeMap<>();
    Multimap<String, NavDbPointEntry> navaids = TreeMultimap.create();
    Multimap<String, NavDbPointEntry> waypoints = TreeMultimap.create();
    Multimap<String, NavDbRouteEntry> airways = TreeMultimap.create();
    Multimap<String, NavDbRouteEntry> sids = TreeMultimap.create();
    Multimap<String, NavDbRouteEntry> stars = TreeMultimap.create();
    
    
    public static class RouteDecodeOutput
    {
        public List<NavDbPointEntry> decodedRoute = new ArrayList<>();
        public List<String> unknownCodes = new ArrayList<>();
    }
        
    
    public NavDatabase()
    {
    }
    
    
    public static NavDatabase getInstance(String moduleID)
    {
        ModuleRegistry reg;
        try
        {
            reg = SensorHub.getInstance().getModuleRegistry();
            NavDriver navDriver = (NavDriver)reg.getModuleById(moduleID);
            if (!navDriver.waitForState(ModuleState.STARTED, START_TIMEOUT))
                throw new IllegalStateException("Navigation database did not start in the last " + START_TIMEOUT/1000 + "s");
            return navDriver.getNavDatabase();
        }
        catch (SensorHubException e)
        {
            throw new IllegalStateException("Turbulence database module not available", e);
        }
    }
    
    
    void reload(String dbPath) throws IOException
    {
        //LufthansaParser.getNavDbEntries(dbPath);
        Arinc424Parser.loadEntries(dbPath, this);
        
        log.info("{} airports loaded", airports.size());
        log.info("{} navaids loaded", navaids.size());
        log.info("{} waypoints loaded", waypoints.size());
        log.info("{} airways loaded", airways.size());
        log.info("{} SIDs loaded", sids.size());
        log.info("{} STARs loaded", stars.size());
    }
    
    
    public Map<String, NavDbPointEntry> getAirports()
    {
        return Collections.unmodifiableMap(airports);
    }
    
    
    public Multimap<String, NavDbPointEntry> getNavaids()
    {
        return Multimaps.unmodifiableMultimap(navaids);
    }
    
    
    public Multimap<String, NavDbPointEntry> getWaypoints()
    {
        return Multimaps.unmodifiableMultimap(waypoints);
    }
    
    
    public Multimap<String, NavDbRouteEntry> getAirways()
    {
        return Multimaps.unmodifiableMultimap(airways);
    }
    
    
    public RouteDecodeOutput decodeRoute(String route)
    {
        log.debug("Route Decode: input = {}", route);
        
        String[] items = route.split("\\s|\\.\\.|\\./\\.|\\*\\.|\\.");
        log.debug("Items are {}", Arrays.asList(items));
        
        ArrayList<NavDbEntry> inputRoute = new ArrayList<>();
        for (int i = 0; i < items.length; i++)
            inputRoute.add(new NavDbEntry(NavDbEntry.Type.UNKNOWN, items[i]));
        
        return decodeRoute(inputRoute);
    }
        
        
    public RouteDecodeOutput decodeRoute(List<NavDbEntry> inputRoute)
    {
        NavDbPointEntry previousWaypt = null;
        RouteDecodeOutput decodeOutput = new RouteDecodeOutput();
        int inputRouteSize = inputRoute.size();
        
        for (int i = 0; i < inputRouteSize; i++)
        {
            NavDbEntry entry = inputRoute.get(i); 
            String code = entry.id;
            String nextCode = (i < inputRouteSize-1) ? inputRoute.get(i+1).id : null;
            
            // skip if special code 'DCT' meaning 'direct'
            if ("DCT".equals(code))
                continue;
            
            // add as-is if input entry is already a waypoint with lat/lon
            if (entry instanceof NavDbPointEntry)
            {
                NavDbPointEntry waypoint = (NavDbPointEntry)entry;
                if (waypoint.lat != null && waypoint.lon != null)
                {
                    decodeOutput.decodedRoute.add(waypoint);
                    previousWaypt = waypoint;
                    continue;
                }
            }
            
            // if undesignated coordinate waypoint type 1, decode it
            if (COORD_WPT_REGEX1.matcher(code).matches())
            {
                NavDbPointEntry wpt = decodeCoordinateWaypoint1(code);
                if (wpt != null)
                {
                    addWaypointToDecodedRoute(wpt, decodeOutput);
                    previousWaypt = wpt;
                }
                continue;
            }
            
            // if undesignated coordinate waypoint type 2, decode it
            if (COORD_WPT_REGEX2.matcher(code).matches())
            {
                NavDbPointEntry wpt = decodeCoordinateWaypoint2(code);
                if (wpt != null)
                {
                    addWaypointToDecodedRoute(wpt, decodeOutput);
                    previousWaypt = wpt;
                }
                continue;
            }
            
            // if first or last, try to get airport
            if (i == 0 || i == inputRouteSize-1)
            {
                NavDbPointEntry airport = airports.get(code);
                if (airport != null)
                {
                    addWaypointToDecodedRoute(airport, decodeOutput);
                    previousWaypt = airport;
                    continue;
                }
            }
            
            // if second code in list, try to decode SID
            if (i == 1)
            {
                Collection<NavDbRouteEntry> possibleSids = sids.get(code);
                if (possibleSids != null && !possibleSids.isEmpty())
                {
                    previousWaypt = decodeSID(possibleSids, previousWaypt, nextCode, decodeOutput);
                    continue;
                }
            }
            
            // if one before last, try to decode STAR
            if (i == inputRouteSize-2)
            {
                Collection<NavDbRouteEntry> possibleStars = stars.get(code);
                if (possibleStars != null && !possibleStars.isEmpty())
                {
                    previousWaypt = decodeSTAR(possibleStars, previousWaypt, nextCode, decodeOutput);
                    continue;
                }
            }
            
            // try navaids
            NavDbPointEntry navaid = findClosest(code, Type.NAVAID, previousWaypt);
            if (navaid != null)
            {
                addWaypointToDecodedRoute(navaid, decodeOutput);
                previousWaypt = navaid;
                continue;
            }
            
            // try waypoints
            NavDbPointEntry wpt = findClosest(code, Type.WAYPOINT, previousWaypt);
            if (wpt != null)
            {
                addWaypointToDecodedRoute(wpt, decodeOutput);
                previousWaypt = wpt;
                continue;
            }
            
            // try airways
            Collection<NavDbRouteEntry> possibleAirways = airways.get(code);
            if (possibleAirways != null && !possibleAirways.isEmpty())
            {
                previousWaypt = decodeAirway(possibleAirways, previousWaypt, nextCode, decodeOutput);
                continue;
            }
            
            // otherwise add unknown code to error list
            log.error("Unknown code: {}", code);
            decodeOutput.unknownCodes.add(code);
        }
        
        if (log.isDebugEnabled())
        {
            Collection<String> fixIds = decodeOutput.decodedRoute.stream()
                .map(entry -> entry.id)
                .collect(Collectors.toList());
            log.debug("Route Decode: output = {}", fixIds);
            log.debug("************************");
        }
        
        return decodeOutput;
    }
    
    
    /*
     * Decode undesignated waypoint defined by coordinates
     * in ARINC 424 DB format (see section 7.2.5)
     */
    private NavDbPointEntry decodeCoordinateWaypoint1(String code)
    {
        double lat, lon;
        
        // detect letter and its position
        char letter = code.charAt(2);
        boolean letterLast = false;
        if (!Character.isLetter(letter))
        {
            letter = code.charAt(4);
            letterLast = true;
        }
        
        double abslat = Integer.parseInt(code.substring(0, 2));
        double abslon = letterLast ? Integer.parseInt(code.substring(2, 4)) : 
            Integer.parseInt(code.substring(3, 5));
        
        switch (letter)
        {
            case 'N':
                lat = abslat;
                lon = -(letterLast ? abslon : abslon+100);
                break;
                
            case 'E':
                lat = abslat;
                lon = letterLast ? abslon : abslon+100;
                break;
                
            case 'W':
                lat = -abslat;
                lon = -(letterLast ? abslon : abslon+100);
                break;
                
            case 'S':
                lat = -abslat;
                lon = letterLast ? abslon : abslon+100;
                break;
                
            default:
                log.error("Invalid coordinate waypoint: {}", code);
                return null;
        }
        
        if (lat > 90.0 || lat < -90 ||
            lon > 180.0 || lon < -180)
        {
            log.error("Invalid coordinate waypoint: {}", code);
        }
        
        return new NavDbPointEntry(Type.WAYPOINT, code, lat, lon);
    }
    
    
    /*
     * Decode undesignated waypoint defined by coordinates
     * in FAA route format
     */
    private NavDbPointEntry decodeCoordinateWaypoint2(String code)
    {
        // latitude value
        char latLetter = code.charAt(4);
        double abslat = Integer.parseInt(code.substring(0, 4)) / 100.;
        double lat = latLetter == 'N' ? abslat : -abslat;
        
        // longitude value
        char lonLetter = code.charAt(code.length()-1);
        double abslon = Integer.parseInt(code.substring(6, 11)) / 100.;
        double lon = lonLetter == 'E' ? abslon : -abslon;
        
        return new NavDbPointEntry(Type.WAYPOINT, code, lat, lon);
    }
    
    
    /*
     * Decode a SID and return the last waypoint added to the decoded route
     */
    NavDbPointEntry decodeSID(Collection<NavDbRouteEntry> possibleSids, NavDbPointEntry previousWaypt, String nextCode, RouteDecodeOutput decodedRoute)
    {
        String airport = previousWaypt.id;
        boolean found = false;
        
        // add common route
        for (NavDbRouteEntry sid: possibleSids)
        {
            // select the procedure for correct airport and waypoint
            if (sid.airport.equals(airport) && sid.transitionId.equals("ALL"))
            {
                previousWaypt = decodeFixes(sid, -1, sid.fixes.size(), previousWaypt, decodedRoute);
                found = true;
                break;
            }
        }
        
        // add enroute transition
        for (NavDbRouteEntry sid: possibleSids)
        {
            // keep only SIDs for correct airport
            if (sid.airport.equals(airport) && !sid.transitionId.equals("ALL"))
            {
                // find end fix
                int endIdx = -1;
                for (int i = 0; i < sid.fixes.size(); i++)
                {
                    NavDbEntryRef fix = sid.fixes.get(i);
                    if (fix.id.equals(nextCode))
                    {
                        endIdx = i;
                        break;
                    }
                }
                
                if (endIdx >= 0)
                {
                    previousWaypt = decodeFixes(sid, -1, endIdx, previousWaypt, decodedRoute);
                    found = true;
                    break;
                }
            }
        }
        
        if (!found)
        {
            String code = possibleSids.iterator().next().id;
            log.error("Could not find SID {} for {} airport", code, airport);
            decodedRoute.unknownCodes.add(code);
        }
        
        return previousWaypt;
    }
    
    
    /*
     * Decode a STAR and return the last waypoint added to the decoded route
     */
    NavDbPointEntry decodeSTAR(Collection<NavDbRouteEntry> possibleStars, NavDbPointEntry previousWaypt, String nextCode, RouteDecodeOutput decodedRoute)
    {
        String airport = nextCode;
        boolean found = false;
        
        // add enroute transition
        for (NavDbRouteEntry star: possibleStars)
        {
            // keep only STARs for correct airport
            if (star.airport.equals(airport) && !star.transitionId.equals("ALL"))
            {
                // find start fix
                int startIdx = -1;
                for (int i = 0; i < star.fixes.size(); i++)
                {
                    NavDbEntryRef fix = star.fixes.get(i);
                    if (fix.id.equals(previousWaypt.id))
                    {
                        startIdx = i;
                        break;
                    }
                }
                
                if (startIdx >= 0)
                {
                    previousWaypt = decodeFixes(star, startIdx, star.fixes.size(), previousWaypt, decodedRoute);
                    found = true;
                    break;
                }
            }
        }
        
        // add common route
        for (NavDbRouteEntry star: possibleStars)
        {
            // select the procedure for correct airport and waypoint
            if (star.airport.equals(airport) && star.transitionId.equals("ALL"))
            {
                previousWaypt = decodeFixes(star, -1, star.fixes.size(), previousWaypt, decodedRoute);
                found = true;
                break;
            }
        }
        
        if (!found)
        {
            String code = possibleStars.iterator().next().id;
            log.error("Could not find STAR {} for {} airport", code, nextCode);
            decodedRoute.unknownCodes.add(code);
        }
        
        return previousWaypt;
    }
    
    
    /*
     * Decode an airway and return the last waypoint added to the decoded route
     */
    NavDbPointEntry decodeAirway(Collection<NavDbRouteEntry> possibleAirways, NavDbPointEntry previousWaypt, String nextCode, RouteDecodeOutput decodedRoute)
    {
        boolean found = false;
        
        for (NavDbRouteEntry airway: possibleAirways)
        {
            int startIdx = -1;
            int endIdx = -1;
            
            // find start/end fixes
            for (int i = 0; i < airway.fixes.size(); i++)
            {
                NavDbEntryRef fix = airway.fixes.get(i);
                
                if (fix.id.equals(previousWaypt.id))
                    startIdx = i;
                else if (fix.id.equals(nextCode))
                    endIdx = i;
            }
            
            // add all fixes in between to decoded route
            if (startIdx >= 0 && endIdx >= 0)
            {
                previousWaypt = decodeFixes(airway, startIdx, endIdx, previousWaypt, decodedRoute);
                found = true;
                break;
            }
        }
        
        if (!found)
        {
            String code = possibleAirways.iterator().next().id;
            log.error("Could not find airway {} containing {} and {}", code, previousWaypt.id, nextCode);
            decodedRoute.unknownCodes.add(code);
        }
        
        return previousWaypt;
    }
    
    
    /*
     * Extract, resolve and decode a list of fixes and return the last waypoint added to the decoded route.
     * Only fixes between start and end index are extracted
     */
    NavDbPointEntry decodeFixes(NavDbRouteEntry route, int startIdx, int endIdx, NavDbPointEntry previousWaypt, RouteDecodeOutput decodedRoute)
    {
        if (startIdx < endIdx)
        {
            for (int i = startIdx+1; i < endIdx; i++)
            {
                NavDbEntryRef fixRef = route.fixes.get(i);
                NavDbPointEntry wpt = findClosest(fixRef.id, fixRef.type, previousWaypt);
                addWaypointToDecodedRoute(wpt, decodedRoute);
                previousWaypt = wpt;
            }
        }
        else
        {
            for (int i = startIdx-1; i > endIdx; i--)
            {
                NavDbEntryRef fixRef = route.fixes.get(i);
                NavDbPointEntry wpt = findClosest(fixRef.id, fixRef.type, previousWaypt);
                addWaypointToDecodedRoute(wpt, decodedRoute);
                previousWaypt = wpt;
            }
        }
        
        return previousWaypt;
    }
    
    
    void addWaypointToDecodedRoute(NavDbPointEntry wpt, RouteDecodeOutput decodeOutput)
    {
        List<NavDbPointEntry> decodedRoute = decodeOutput.decodedRoute;
        
        // avoid duplicate waypoints
        String lastWptId = decodedRoute.isEmpty() ? "" : decodedRoute.get(decodedRoute.size()-1).id;
        if (!lastWptId.equals(wpt.id))
            decodedRoute.add(wpt);
    }
    
    
    NavDbPointEntry findClosest(String id, Type type, NavDbPointEntry previousWaypt)
    {
        // case of airport
        if (type == Type.AIRPORT)
            return airports.get(id);
        
        // case of waypoint or navaid
        Multimap<String, NavDbPointEntry> entryMap = type == Type.NAVAID ? navaids : waypoints;
        
        // since several waypoints/navaids can have the same identifier
        // get the closest one from previous waypoint
        Collection<NavDbPointEntry> possibleWaypts = entryMap.get(id);
        if (possibleWaypts != null && !possibleWaypts.isEmpty())
        {
            NavDbPointEntry wpt = findClosest(possibleWaypts, previousWaypt);
            return wpt;
        }
        
        return null;
    }
    
    
    NavDbPointEntry findClosest(Collection<NavDbPointEntry> possibleEntries, NavDbPointEntry previousWaypt)
    {
        if (previousWaypt == null)
            return possibleEntries.iterator().next();
        
        double minDist2 = Double.MAX_VALUE;
        NavDbPointEntry closestEntry = null;
        for (NavDbPointEntry entry: possibleEntries)
        {
            double dLat = entry.lat - previousWaypt.lat;
            double dLon = entry.lon - previousWaypt.lon;
            double dist2 = dLat*dLat + dLon*dLon;
            if (dist2 < minDist2)
            {
                closestEntry = entry;
                minDist2 = dist2;
            }
        }
        
        return closestEntry;
    }
    
    
    public static void main(String[] args) throws Exception
    {
        NavDatabase db = new NavDatabase();
        db.reload("/home/alex/GeoRobotix/Projects/Delta/NavDatabase/LidoDLA_2111_20211104/LidoDLA_2111.dat");
        
        //for (NavDbRouteEntry route: db.airways.values())
        //    System.out.println(route);
        
        //for (NavDbRouteEntry route: db.sids.get("ILEXY3"))
        //    System.out.println(route);
        
        //for (NavDbRouteEntry route: db.stars.get("HLYWD1"))
        //    System.out.println(route);
        
        //db.decodeRoute("KJFK..DRK.J231.ACH..GABBL.HLYWD1.KLAX");
        //db.decodeRoute("KATL.VRSTY2.MCN..YANTI.Q89.MANLE.Y185.BEERD.Y185.RENAH.Y355.FIPEK.Y294.GESSO.L467.ANADA.UG449.LEPOD.UA312.TONOM.UL452.TOPAM.UL452.GELVA.UZ6.NIMKI.UZ38.NIPKI..VUNOX..SBGR");
        //db.decodeRoute("KLAX.SUMMR2.DINTY..DUETS..3000N/13000W..2400N/14000W..2100N/14500W..1700N/15000W..1300N/15500W..0800N/16000W..0400N/16500W..0100S/17000W..0700S/17500W..1200S/18000W..1800S/17500E..2400S/17000E..TEKEP..WARTY..RIKNI.N774.TESAT..YSSY");
        //db.decodeRoute("KSJC.TECKY3.TECKY..SJC..BMRNG..HRNER..RBL..LMT.HAWKZ7.KSEA");
        //db.decodeRoute("KDTW./.GGUCE225069..KMDT");
        //db.decodeRoute("KDTW./.EKR062015..RACER.LEEHY5.KSLC");
        db.decodeRoute("SKBO..SIE.CAMRN4.KJFK");
        
        // wait for keypress
        try { System.in.read(); }
        catch (IOException e1) { }
        
        //String testFile = "/home/alex/Projects/Workspace_FWV_ML/delta-ifwv-server/include/osh-addons/sensors/aviation/sensorhub-driver-navdb/src/test/resources/routes.txt";
        String testFile = "/home/alex/Projects/Workspace_FWV_ML/delta-ifwv-server/include/osh-addons/sensors/aviation/sensorhub-driver-navdb/src/test/resources/routes_fa.txt";
        String route = null;
        try (BufferedReader br = new BufferedReader(new FileReader(testFile)))
        {
            while ((route = br.readLine()) != null)
            {
                db.decodeRoute(route);
            }
        }
        catch (Exception e)
        {
            log.error("Error decoding route {}", route, e);
        }
    }
}
