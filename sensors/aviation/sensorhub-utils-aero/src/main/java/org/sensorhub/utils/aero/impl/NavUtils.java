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
    public static final double EARTH_RADIUS_KM = 6371.0;
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
     * Compute bearing of great circle linking two lat/lon points
     * @param lat1 Latitude of first point, in degrees
     * @param lon1 Longitude of first point, in degrees
     * @param lat2 Latitude of second point, in degrees
     * @param lon2 Longitude of second point, in degrees
     * @return The bearing from point 1 to point 2, in degrees
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

}
