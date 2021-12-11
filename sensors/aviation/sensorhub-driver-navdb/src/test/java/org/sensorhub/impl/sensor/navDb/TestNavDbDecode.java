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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sensorhub.impl.sensor.navDb.NavDatabase.RouteDecodeOutput;
import com.google.common.collect.Sets;
import com.google.gson.stream.JsonReader;


public class TestNavDbDecode
{
    static NavDatabase navDB;
    
    
    @BeforeClass
    public static void setup() throws Exception
    {
        navDB = new NavDatabase();
        navDB.reload("/home/alex/GeoRobotix/Projects/Delta/NavDatabase/LidoDLA_2111_20211104/LidoDLA_2111.dat");
    }
    
    
    @Test
    public void testDecodeLatLonCoordType1() throws Exception
    {
        decodeCoordAndCheck("5275N", 52.0, -75.0);
        decodeCoordAndCheck("5040N", 50.0, -40.0);
        decodeCoordAndCheck("0708N", 7.0, -8.0);
        decodeCoordAndCheck("75N70", 75.0, -170.0);
        decodeCoordAndCheck("07N20", 7.0, -120.0);
        decodeCoordAndCheck("5020E", 50.0, 20.0);
        decodeCoordAndCheck("7550E", 75.0, 50.0);
        decodeCoordAndCheck("0608E", 6.0, 8.0);
        decodeCoordAndCheck("75E50", 75.0, 150.0);
        decodeCoordAndCheck("06E10", 6.0, 110.0);
        decodeCoordAndCheck("5275W", -52, -75.0);
        decodeCoordAndCheck("5040W", -50.0, -40.0);
        decodeCoordAndCheck("0708W", -7.0, -8.0);
        decodeCoordAndCheck("75W70", -75.0, -170.0);
        decodeCoordAndCheck("07W20", -7.0, -120.0);
        decodeCoordAndCheck("5020S", -50.0, 20.0);
        decodeCoordAndCheck("7550S", -75.0, 50.0);
        decodeCoordAndCheck("0608S", -6.0, 8.0);
        decodeCoordAndCheck("75S50", -75.0, 150.0);
        decodeCoordAndCheck("06S10", -6.0, 110.0);
    }
    
    
    @Test
    public void testDecodeLatLonCoordType2() throws Exception
    {
        decodeCoordAndCheck("36N/088W", 36.0, -88.0);
        decodeCoordAndCheck("54N115W", 54.0, -115.0);
        decodeCoordAndCheck("2930S/01845E", -29.5, 18.75);
        decodeCoordAndCheck("5930N/0200000W", 59.5, -20.0);
        decodeCoordAndCheck("570000N/0200000W", 57.0, -20.0);
    }
    
    
    @Test
    public void testDecodeBearingDistanceType1() throws Exception
    {
        //decodeCoordAndCheck("KDTW CRL000010", 49.4218321393345, 2.514777777777778);
        decodeCoordAndCheck("KDTW CRL297040", 42.349833333, -84.260333333);
        decodeCoordAndCheck("AIR320014", 54.0, -115.0);
        decodeCoordAndCheck("LDZ170020", 54.0, -115.0);
    }
    
    
    protected void decodeCoordAndCheck(String routeTxt, double expectedLat, double expectedLon)
    {
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        NavDbPointEntry wpt = output.decodedRoute.get(output.decodedRoute.size()-1);
        assertEquals(expectedLat, wpt.lat, 1e-8);
        assertEquals(expectedLon, wpt.lon, 1e-8);
    }
    
    
    @Test
    public void testDecodeRouteWithCoordinates()
    {
        String routeTxt = "LFPG 570000N/0200000W 5830N 590000N/0400000W 570000N/0500000W KATL";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        checkNoError(output);
    }
    
    
    @Test
    public void testDecodeAmbiguousWaypoint() throws Exception
    {
        String routeTxt = "KLAX..CNERY..BLH..SSO..ELP..PEQ..DIESL..KIAH";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        
        String expected = "KLAX CNERY BLH SSO ELP PEQ DIESL KIAH";
        checkWaypoints(output, expected);
        
        // check BLH is located in US (BLH is also a VOR in EU)
        assertTrue(output.decodedRoute.get(2).region.equals("USA"));
    }
    
    
    @Test
    public void testDecodeAmbiguousAirway() throws Exception
    {
        String routeTxt = "KFLL FEMON SSI V3 SAV KRDU";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        
        String expected = "KFLL FEMON SSI BROUN HARPS WOHPY KELER SAV KRDU";
        checkWaypoints(output, expected);
        assertTrue(output.decodedRoute.get(0).region.equals("USA"));
    }
    
    
    @Test
    public void testDecodeAirwayCrossingBoundary() throws Exception
    {
        String routeTxt;
        RouteDecodeOutput output;
        
        /*routeTxt = "KATL URABI T565 GUBIS T565 ANIMO RJTT";
        output = navDB.decodeRoute(routeTxt);
        checkNoError(output);
        
        routeTxt = "KATL URABI T565 ANIMO RJTT";
        output = navDB.decodeRoute(routeTxt);
        checkNoError(output);
        
        routeTxt = "KDTW BANOT B223 LUMIN B223 WKE RJGG";
        output = navDB.decodeRoute(routeTxt);
        checkNoError(output);
        
        routeTxt = "RJGG WKE B223 BANOT KDTW";
        output = navDB.decodeRoute(routeTxt);
        checkNoError(output);*/
        
        //routeTxt = "KDTW ASKIB B933 ODEPI B223 LUMIN B223 WKE V1 CHE Y10 GODIN RJTT";
        routeTxt = "KDTW TRMML3 GNZOE SSM 5000N/08500W 5500N/08700W 5900N/09000W 6000N/09100W 6500N/10000W 7000N/11000W 7200N/12000W JESRU BARIP LUTEM M137 ASKIB B933 ODEPI B223 LUMIN B223 WKE V1 CHE Y10 GODIN RJTT";
        output = navDB.decodeRoute(routeTxt);
        checkNoError(output);
    }
    
    
    @Test
    public void testDecodeSIDAndSTAR() throws Exception
    {
        String routeTxt = "KSLC RUGGD2 PERTY CHE SLN BUM BNA NEWBB IHAVE MTHEW CHPPR1 KATL";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        
        String expected = "KSLC BUBBY RUGGD HERTS PERTY CHE SLN BUM BNA NEWBB IHAVE MTHEW MGRIF BBABE LEMKE CHPPR MRCHH KATL";
        checkWaypoints(output, expected);
        
        
        routeTxt = "KLAX LADYJ4 CSTRO DUCKE LMT HAWKZ7 KSEA";
        output = navDB.decodeRoute(routeTxt);
        
        expected = "KLAX LADYJ RUGBY OROSZ HEYJO CSTRO DUCKE LMT KNGDM WRUSL BTG PTERA KRIEG HAWKZ LIINE KSEA";
        checkWaypoints(output, expected);
    }
    
    
    @Test
    public void testDecodePartialSID() throws Exception
    {
        String routeTxt = "KSLC RUGGD2 HERTS CHE SLN BUM BNA NEWBB IHAVE MTHEW CHPPR1 KATL";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        
        String expected = "KSLC BUBBY RUGGD HERTS CHE SLN BUM BNA NEWBB IHAVE MTHEW MGRIF BBABE LEMKE CHPPR MRCHH KATL";
        checkWaypoints(output, expected);
    }
    
    
    @Test
    public void testDecodePartialSTAR() throws Exception
    {
        String routeTxt = "KLAX LADYJ4 CSTRO DUCKE WRUSL HAWKZ7 KSEA";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        
        String expected = "KLAX LADYJ RUGBY OROSZ HEYJO CSTRO DUCKE WRUSL BTG PTERA KRIEG HAWKZ LIINE KSEA";
        checkWaypoints(output, expected);
    }
    
    
    @Test
    public void testDecodeSIDRunwayOnly() throws Exception
    {
        String routeTxt = "KDFW.FORCK2.FORCK..ELD..SQS.Q30.VUZ..ATL..TWOUP.Q22.SPA.Q60.JAXSN..HPW.J191.PXT.KORRY4.KLGA";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        
        String expected = "KDFW FORCK ELD SQS VUZ ATL TWOUP SPA BYJAC EVING LOOEY JAXSN HPW HUBBS PXT GARED RIDGY ENO SKIPY BESSI EDJER DAVYS HOLEY BRAND KORRY RBV TYKES MINKS RENUE APPLE PROUD KLGA";
        checkWaypoints(output, expected); 
    }
    
    
    @Test
    public void testDecodeLongJump() throws Exception
    {
        String routeTxt = "EGLL PLYMM PARCH KJFK";
        RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
        
        String expected = "EGLL PLYMM PARCH KJFK";
        checkWaypoints(output, expected); 
    }
    
    
    @Test
    public void testDecodeDeltaRoutes() throws Exception
    {
        String[] routes = {
            "KGSO TRI9 CARWN LEAVI OZZZI1 KATL",
            "KATL CUTTN2 HANKO MEMFS PER PUB HBU J28 MLF ILC KITTN Q164 KATTS INYOE DYAMD5 KSFO",
            "KMCO EPCOT1 MUGLS PATOY Q116 JAWJA CABLO PICKS MEI SQS LIT TUL PER LAA EKR LEEHY5 KSLC",
            "KDFW FORCK2 FORCK ELD SQS Q30 VUZ ATL HRTWL Q64 TYI ORF J121 SIE CAMRN4 KJFK",
            "KSLC RUGGD2 PERTY CHE SLN BUM BNA NEWBB IHAVE MTHEW CHPPR1 KATL",
            "KPDX LAVAA6 PDT LWT J70 DIK SMERF GEP J70 AUGER VIO J34 WOOST J146 CXR SLT J584 FQM LVZ LENDY6 KJFK",
            "KLAX LADYJ4 CSTRO DUCKE LMT HAWKZ7 KSEA",
            "KJFK WAVEY EMJAY J174 SWL CEBEE WETRO ILM AR15 BAHAA AR17 VKZ LINEY Y262 LULLS Y196 CANOA UB879 CUN MMUN",
            "KJFK SHIPP Y490 BEHHR Y490 ROLLE ATUGI L454 LUCTI L454 SINGL L454 MNDEZ FIPEK BETIR MDPC",
            "EGLL PLYMM PARCH3 KJFK",
            "LIRF 4000N/06000W SLATN LARGE OWENZ CYN MOL J48 FLASK OZZZI1 KATL",
            "LFPG ATREX UT225 VESAN UL613 SOVAT L613 SANDY L15 BIG UL9 STU N546 BAKUR REVNU PIKIL 570000N/0200000W 580000N/0300000W 590000N/0400000W 570000N/0500000W HOIST N604B MT YOW BUF LANGS JHW EWC HNN SPAYD KTRYN ONDRE1 KATL",
            "KTPA DORMR1 PSTOL REMIS LEV LFK J50 ABI CNX ONM SJN DRK HIPPI GABBL HLYWD1 KLAX",
            "KMSP SCHEP9 ONL DDRTH CKW OCS NORDK6 KSLC",
            "KBOS HYLND5 HYLND CAM Q822 FNT WYNDE2 KORD",
            "KJFK GAYEL Q818 WOZEE LINNG NOSIK DAYYY GRB ABR SWTHN ZERZO MLP GLASR1 KSEA"
        };
        
        for (String route: routes)
        {
            RouteDecodeOutput output = navDB.decodeRoute(route);
            checkNoError(output);
            checkNoDuplicates(output);
        }
    }
    
    
    @Test
    public void testDecodeFaaRoutes() throws Exception
    {
        String routesCsvFile = "/prefroutes_db.csv";
        InputStream resourceStream = getClass().getResourceAsStream(routesCsvFile);
        int lineNumber = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                lineNumber++;
                if (lineNumber == 1)
                    continue;
                
                String[] fields = line.split(",");
                String routeTxt = fields[1];
                
                // add K prefix to orig and dest airports
                //routeTxt = "K" + routeTxt;
                //int destIdx = routeTxt.lastIndexOf(' ') + 1;
                //routeTxt = routeTxt.substring(0, destIdx) + "K" + routeTxt.substring(destIdx);
                
                RouteDecodeOutput output = navDB.decodeRoute(routeTxt);
                //checkNoError(output);
                //checkNoDuplicates(output);
            }
        }
    }
    
    
    protected void checkWaypoints(RouteDecodeOutput output, String expected)
    {
        checkNoError(output);
        
        String[] expectedWaypoints = expected.split(" ");
        int numWaypt = Math.min(expectedWaypoints.length, output.decodedRoute.size());
        
        for (int i = 1; i < numWaypt; i++)
        {
            String expectedWptId = expectedWaypoints[i];
            String ourWptId = output.decodedRoute.get(i).id;
            assertEquals(expectedWptId, ourWptId);
        }
        
        assertEquals("Wrong decoded length, ", expectedWaypoints.length, output.decodedRoute.size());
    }
    
    
    protected void checkNoError(RouteDecodeOutput output)
    {
        assertTrue("Unknown codes: " + output.unknownCodes, output.unknownCodes.isEmpty());
    }
    
    
    protected void checkNoDuplicates(RouteDecodeOutput output)
    {
        // check not duplicates
        TreeSet<NavDbPointEntry> entrySet = new TreeSet<>((a,b) -> a.id.compareTo(b.id));
        entrySet.addAll(output.decodedRoute);
        assertEquals("Duplicated nav points: " + output.decodedRoute, entrySet.size(), output.decodedRoute.size());
    }
    
    
    @Test
    public void testDecodeFirehoseRoutes() throws Exception
    {
        // read FA routes JSON
        String routesJsonFile = "/home/alex/Projects/Workspace_FWV_ML/delta-ifwv-server/include/osh-addons/sensors/aviation/sensorhub-driver-navdb/src/test/resources/routes_fa.txt.gz";
        try (JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(routesJsonFile))))))
        {
            reader.beginArray();
            
            while (reader.hasNext())
            {
                String routeStr = "";
                List<String> expectedWaypoints = new ArrayList<>();
                
                // extract route string and waypoint IDs from Firehose FlightPlan object
                reader.beginObject();
                while (reader.hasNext())
                {
                    String propName = reader.nextName();
                
                    if ("route".equals(propName))
                        routeStr = reader.nextString();
                    else if ("decodedRoute".equals(propName))
                        readFlightXmlWaypoints(reader, expectedWaypoints);
                    else
                        reader.skipValue();
                }
                reader.endObject();
                
                // remove time at end of route string
                if (routeStr.charAt(routeStr.length()-5) == '/')
                    routeStr = routeStr.substring(0, routeStr.lastIndexOf('/'));
                routeStr.replace("*", "");
                
                // skip routes updated mid flight
                if (routeStr.contains("./.") || routeStr.matches(".*N[0-9]{4}F[0-9]{3}.*"))
                    continue;
                
                // decode route string
                List<NavDbPointEntry> waypoints = navDB.decodeRoute(routeStr).decodedRoute;
                
                // cleanup firehose waypoints
                String firstWptId = waypoints.get(1).id;
                String lastWptId = waypoints.get(waypoints.size()-2).id;
                cleanupFirehoseWaypoints(expectedWaypoints, firstWptId, lastWptId);
                
                // compare decode result with Firehose waypoints
                if (waypoints.size() == expectedWaypoints.size())
                {
                    for (int i = 1; i < waypoints.size(); i++)
                    {
                        String expectedWptId = expectedWaypoints.get(i);
                        String ourWptId = waypoints.get(i).id;
                        
                        if (!expectedWptId.equals(ourWptId))
                        {
                            System.out.println(">>> Correct route is " + expectedWaypoints);
                            break;
                        }
                    }
                }
                else
                    System.out.println(">>> Correct route is " + expectedWaypoints);
            }
            
            reader.endArray();
        }
    }
    
    
    void cleanupFirehoseWaypoints(List<String> expectedWaypoints, String firstWptId, String lastWptId)
    {
        // remove SID transition
        ListIterator<String> it = expectedWaypoints.listIterator();
        it.next(); // skip departure airport
        while (it.hasNext())
        {
            String expectedWptId = it.next();
            if (expectedWptId.equals(firstWptId))
                break;
            it.remove();
        }
        
        // remove STAR runway transition
        it = expectedWaypoints.listIterator(expectedWaypoints.size());
        
        it.previous();
        while (it.hasPrevious())
        {
            String expectedWptId = it.previous();
            if (expectedWptId.equals(lastWptId))
                break;
            it.remove();
        }
        
        // remove duplicate waypoints
        String previous = null;
        it = expectedWaypoints.listIterator();
        while (it.hasNext())
        {
            String id = it.next();
            if (previous != null && previous.equals(id))
                it.remove();
            previous = id;
        }
    }
    
    
    void readFlightXmlWaypoints(JsonReader reader, List<String> waypoints) throws IOException
    {
        reader.beginArray();
        while (reader.hasNext())
        {
            reader.beginObject();
            while (reader.hasNext())
            {
                String propName = reader.nextName();
            
                if ("name".equals(propName))
                    waypoints.add(reader.nextString());
                else
                    reader.skipValue();
            }
            reader.endObject();
        }
        reader.endArray();
    }

}
