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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.sensorhub.algo.geoloc.Ellipsoid;
import org.sensorhub.algo.geoloc.GeoTransforms;
import org.sensorhub.algo.vecmath.Vect3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * TODO NavUtils type description
 * </p>
 *
 * @author Alex Robin
 * @since Apr 7, 2025
 */
public class NavUtils
{
    static final Logger log = LoggerFactory.getLogger(NavUtils.class);
    
    public static final double EARTH_RADIUS_KM = Ellipsoid.SPHERICAL.getEquatorRadius() / 1000.;
    public static final double METERS_TO_FEET = 3.2808399;
    public static final double KILOMETERS_TO_NAUTICALMILES = 0.539956803;
    

    /**
     * Compute the distance between two lat/lon points in km
     * @param lat1 Latitude of first point, in degrees
     * @param lon1 Longitude of first point, in degrees
     * @param lat2 Latitude of second point, in degrees
     * @param lon2 Longitude of second point, in degrees
     * @return The distance along the great circle in kilometers
     */
    public static double greatCircleDistance(double lat1, double lon1, double lat2, double lon2)
    {
        double lat1r = Math.toRadians(lat1);
        double lon1r = Math.toRadians(lon1);
        double lat2r = Math.toRadians(lat2);
        double lon2r = Math.toRadians(lon2);
        double thetaR = lon1r - lon2r;

        double dist = Math.sin(lat1r) * Math.sin(lat2r) + Math.cos(lat1r) * Math.cos(lat2r) * Math.cos(thetaR);
        return Math.acos(dist) * EARTH_RADIUS_KM;
    }


    /**
     * Compute the end point of traveling from known location along a great circle
     * @param lat Latitude of initial position, in degrees
     * @param lon Longitude of initial position, in degrees
     * @param bearing Bearing of travel direction, in degrees
     * @param distance Travel distance along the great circle, in kilometers
     * @return The end location as lat/lon coordinates, in degrees
     */
    public static Coordinate getEndpoint(double lat, double lon, double bearing, double distance)
    {
        double bearingR = Math.toRadians(bearing);
        double latR = Math.toRadians(lat);
        double lonR = Math.toRadians(lon);

        // normalized distance
        double distanceNorm = distance / EARTH_RADIUS_KM;

        //lat2 = math.asin( math.sin(lat1)*math.cos(d/R) +  math.cos(lat1)*math.sin(d/R)*math.cos(brng))
        double latEndR = Math.asin(Math.sin(latR) * Math.cos(distanceNorm) + Math.cos(latR) * Math.sin(distanceNorm) * Math.cos(bearingR));

        //lon2 = lon1 + math.atan2(math.sin(brng)*math.sin(d/R)*math.cos(lat1), math.cos(d/R)-math.sin(lat1)*math.sin(lat2))
        double lonEndR = lonR + Math.atan2(Math.sin(bearingR) * Math.sin(distanceNorm) * Math.cos(latR), Math.cos(distanceNorm) - Math.sin(latR) * Math.sin(latEndR));

        return new Coordinate(Math.toDegrees(latEndR), Math.toDegrees(lonEndR));
    }


    /**
     * Compute initial bearing of great circle linking two lat/lon points
     * @param lat1 Latitude of first point, in degrees
     * @param lon1 Longitude of first point, in degrees
     * @param lat2 Latitude of second point, in degrees
     * @param lon2 Longitude of second point, in degrees
     * @return The bearing from point 1 to point 2, in degrees (-180 to 180)
     */
    public static double getBearing(double lat1, double lon1, double lat2, double lon2)
    {
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        
        double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
        
        return Math.toDegrees(Math.atan2(y, x));
    }
    
    
    public static double distanceToPolyline(LineString polyline, double posLon, double posLat, double maxDist)
    {
        var minDist = Double.POSITIVE_INFINITY;
        
        for (int i = 0; i < polyline.getNumPoints()-1; i++)
        {
            var p1 = polyline.getCoordinateN(i);
            var p2 = polyline.getCoordinateN(i+1);
            var dist = distanceToSegment(p1, p2, posLon, posLat, maxDist);
            if (!Double.isNaN(dist) && dist < minDist)
                minDist = dist;
        }
        
        return Double.isFinite(minDist) ? minDist : Double.NaN;
    }
    
    
    /**
     * Compute nearest point 
     * @param p1
     * @param p2
     * @param lat
     * @param lon
     * @return
     */
    public static double distanceToSegment(Coordinate p1, Coordinate p2, double posLon, double posLat, double maxDist)
    {
        GeoTransforms transforms = new GeoTransforms(Ellipsoid.SPHERICAL);
        
        var p1Vec = new Vect3d(Math.toRadians(p1.x), Math.toRadians(p1.y), 0.0);
        transforms.LLAtoECEF(p1Vec, p1Vec);
        
        var p2Vec = new Vect3d(Math.toRadians(p2.x), Math.toRadians(p2.y), 0.0);
        transforms.LLAtoECEF(p2Vec, p2Vec);
        
        var posVec = new Vect3d(Math.toRadians(posLon), Math.toRadians(posLat), 0.0);
        transforms.LLAtoECEF(posVec, posVec);
        
        return nearestPointOnSegment(p1Vec, p2Vec, posVec, posVec, maxDist);
    }

    
    
    /**
     * Finds the point on a great circle segment defined by p1 and p2 that is closest to pos.
     * This method uses great circle distance metric with a spherical approximation.
     * @param p1 first point of great circle segment, in ECEF coordinates
     * @param p2 second point of great circle segment, in ECEF coordinates
     * @param pos ECEF position to compute distance to
     * @param nearest vector that will receive computed nearest point, in ECEF coordinates
     * @param maxDist if given max distance (in nautical miles) is reached, the nearest point is not computed
     * @return distance to nearest point in nautical miles, or NaN if too far or outside segment
     **/
    public static double nearestPointOnSegment(Vect3d p1, Vect3d p2, Vect3d pos, Vect3d nearest, double maxDist)
    {
        var n = new Vect3d();
        
        // normal to great circle plane
        n.cross(p1, p2).normalize();
        
        // dot product with pos gives us measure of how far we are from plane
        double dot = n.dot(pos);
        
        // convert to angle and then distance on earth surface
        double angle = Math.PI/2 - Math.acos(dot/pos.norm());
        double dist = KILOMETERS_TO_NAUTICALMILES * EARTH_RADIUS_KM * Math.abs(angle);
        log.trace("Orthogonal distance to segment = {} NM", dist);
        if (dist > maxDist)
        {
            log.trace("Greater than maximum acceptable distance");
            return Double.NaN;
        }
        
        // compute projection of pos on great circle plane X = P - dot(N,P)*N
        n.scale(dot);
        nearest.set(pos).sub(n);
        log.trace("Nearest point is {}", nearest);
        
        // check if point is on segment
        if (isPointOnSegment(p1, p2, nearest))
            return dist;
        else
            return Double.NaN;
    }
    
    
    /**
     * Check that point is within a great circle segment
     * @param p1 first point of great circle segment, in ECEF coordinates
     * @param p2 second point of great circle segment, in ECEF coordinates
     * @param m 
     * @return
     */
    public static boolean isPointOnSegment(Vect3d p1, Vect3d p2, Vect3d m)
    {
        var tmp1 = new Vect3d();
        var tmp2 = new Vect3d();
        
        // compute dot products to check that projection is on segment
        tmp1.sub(m, p1).normalize();
        tmp2.sub(p2, p1).normalize();
        double dot1 = tmp1.dot(tmp2);
        
        tmp1.sub(m, p2).normalize();
        double dot2 = -tmp1.dot(tmp2);
        
        if (dot1 >= 0 && dot2 >= 0) {
            return true;
        }
        else {
            log.trace("Point is not on segment");
            return false;
        }       
    }

}
