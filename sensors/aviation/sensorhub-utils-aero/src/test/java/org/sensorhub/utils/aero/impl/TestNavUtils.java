/***************************** BEGIN COPYRIGHT BLOCK **************************

Copyright (C) 2025 Delta Air Lines, Inc. All Rights Reserved.

Notice: All information contained herein is, and remains the property of
Delta Air Lines, Inc. The intellectual and technical concepts contained herein
are proprietary to Delta Air Lines, Inc. and may be covered by U.S. and Foreign
Patents, patents in process, and are protected by trade secret or copyright law.
Dissemination, reproduction or modification of this material is strictly
forbidden unless prior written permission is obtained from Delta Air Lines, Inc.

******************************* END COPYRIGHT BLOCK ***************************/

package org.sensorhub.utils.aero.impl;

import static org.junit.Assert.*;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;


public class TestNavUtils
{

    @Test
    public void testBearingCalc()
    {
        assertEquals(0.0, NavUtils.getBearing(0, 0, 90, 0), 1e-10);
        assertEquals(90.0, NavUtils.getBearing(0, 0, 0, 90), 1e-10);
        assertEquals(180.0, NavUtils.getBearing(0, 0, -90, 0), 1e-10);
        assertEquals(-90.0, NavUtils.getBearing(0, 0, 0, -90), 1e-10);
        
        var lat = 0.0;
        for (var lon = -180; lon < 180; lon += 10) {
            assertEquals(0.0, NavUtils.getBearing(lat, lon, lat+10, lon), 1e-10);
            assertEquals(90.0, NavUtils.getBearing(lat, lon, lat, lon+10), 1e-10);
            assertEquals(180.0, NavUtils.getBearing(lat, lon, lat-10, lon), 1e-10);
            assertEquals(-90.0, NavUtils.getBearing(lat, lon, 0, lon-10), 1e-10);
        }
    }
    
    
    @Test
    public void testDistanceToSegment()
    {
        var dist = NavUtils.distanceToSegment(new Coordinate(-10,0), new Coordinate(10,0), 0, 10, 10000);
        assertEquals(NavUtils.EARTH_RADIUS_KM*NavUtils.KILOMETERS_TO_NAUTICALMILES*Math.toRadians(10), dist, 1e-6);
        
        dist = NavUtils.distanceToSegment(new Coordinate(40,10), new Coordinate(40,-10), 50, 0, 10000);
        assertEquals(NavUtils.EARTH_RADIUS_KM*NavUtils.KILOMETERS_TO_NAUTICALMILES*Math.toRadians(10), dist, 1e-6);
        
        dist = NavUtils.distanceToSegment(new Coordinate(0,0), new Coordinate(0,90), 10, 0, 10000);
        assertEquals(NavUtils.EARTH_RADIUS_KM*NavUtils.KILOMETERS_TO_NAUTICALMILES*Math.toRadians(10), dist, 1e-6);
        
        dist = NavUtils.distanceToSegment(new Coordinate(0,0), new Coordinate(0,90), 0, 0, 10000);
        assertEquals(0.0, dist, 1e-6);
        
        dist = NavUtils.distanceToSegment(new Coordinate(0,0), new Coordinate(0,90), 0, -20, 10000);
        assertTrue(Double.isNaN(dist));
        
        // beyond max distance
        dist = NavUtils.distanceToSegment(new Coordinate(0,0), new Coordinate(0,90), 20, 0, 1000);
        assertTrue(Double.isNaN(dist));
    }
    
    
    @Test
    public void testDistanceToPolyline()
    {
        var jts = new GeometryFactory();
        var coords = new Coordinate[] {
            new Coordinate(-20,0),
            new Coordinate(-10,0),
            new Coordinate(10,0),
            new Coordinate(20,0)
        };
        var polyline = jts.createLineString(coords);
        
        var dist = NavUtils.distanceToPolyline(polyline, 0, 10, 10000);
        assertEquals(NavUtils.EARTH_RADIUS_KM*NavUtils.KILOMETERS_TO_NAUTICALMILES*Math.toRadians(10), dist, 1e-6);
        
        dist = NavUtils.distanceToPolyline(polyline, 0, -10, 10000);
        assertEquals(NavUtils.EARTH_RADIUS_KM*NavUtils.KILOMETERS_TO_NAUTICALMILES*Math.toRadians(10), dist, 1e-6);
        
        dist = NavUtils.distanceToPolyline(polyline, 10, -10, 10000);
        assertEquals(NavUtils.EARTH_RADIUS_KM*NavUtils.KILOMETERS_TO_NAUTICALMILES*Math.toRadians(10), dist, 1e-6);
        
        dist = NavUtils.distanceToPolyline(polyline, 18, 0, 10000);
        assertEquals(0.0, dist, 1e-6);
        
        dist = NavUtils.distanceToPolyline(polyline, 0, 0, 10000);
        assertEquals(0.0, dist, 1e-6);
        
        dist = NavUtils.distanceToPolyline(polyline, 30, 0, 10000);
        assertTrue(Double.isNaN(dist));
        
        // beyond max distance
        dist = NavUtils.distanceToPolyline(polyline, 0, 40, 1000);
        assertTrue(Double.isNaN(dist));
    }
    
    
    @Test
    public void testDistanceToPolyline2()
    {
        var jts = new GeometryFactory();
        var coords = new Coordinate[] {
            new Coordinate(-94.71388888888889, 39.29761111111111),
            new Coordinate(-94.73706111111112, 39.28528333333333),
            new Coordinate(-95.37412499999999, 39.21389166666667),
            new Coordinate(-95.94635000000001, 39.14666388888889),
            new Coordinate(-97.62133333333333, 38.92513888888889),
            new Coordinate(-103.60005555555556, 37.25865833333334),
            new Coordinate(-110.79501944444445, 35.0616),
            new Coordinate(-112.48034166666667, 34.70255277777778),
            new Coordinate(-113.63936944444445, 34.56228888888889),
            new Coordinate(-114.05572222222222, 34.515769444444445),
            new Coordinate(-114.92164166666667, 34.33013888888889),
            new Coordinate(-115.21845, 34.264830555555555),
            new Coordinate(-115.68945555555555, 34.16383333333333),
            new Coordinate(-115.98965555555556, 34.134769444444444),
            new Coordinate(-116.29878611111111, 34.10423611111111),
            new Coordinate(-116.58839166666667, 34.07315277777778),
            new Coordinate(-116.88798611111112, 34.04518888888889),
            new Coordinate(-117.03101666666666, 34.02666388888889),
            new Coordinate(-117.22911944444445, 34.00313888888889),
            new Coordinate(-117.30848611111111, 33.99361944444445),
            new Coordinate(-117.48848611111111, 33.97241388888889),
            new Coordinate(-118.40805, 33.94249722222222)
        };
        var polyline = jts.createLineString(coords);
        
        var dist = NavUtils.distanceToPolyline(polyline, -102.26556, 38.77969, 100);
        assertEquals(62.75, dist, 1e-2);
        
        dist = NavUtils.distanceToPolyline(polyline, -95.35947777777777, 39.29526944444444, 100);
        assertEquals(4.73, dist, 1e-2);
    }

}
