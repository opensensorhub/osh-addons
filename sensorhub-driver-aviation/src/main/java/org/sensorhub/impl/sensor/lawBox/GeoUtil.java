package org.sensorhub.impl.sensor.lawBox;

import org.sensorhub.impl.sensor.lawBox.GeoConstants.Units;

/**
 * <p>Title: GeoUtil.java</p>
 * <p>Description:  Compute distance between two lat-lon points
 *
 *  reference: http://www.inventeksys.com/GPS_Facts_Great_Circle_Distances.pdf  </p>
 * @author Tony Cook
 * @since Jul 1, 2011
 * 
 *  TODO:  add support for radians- all methods currently assume degrees
 */

public class GeoUtil
{
	public static double distance(double lat1, double lon1, double lat2, double lon2, GeoConstants.Units units) {
		double lat1r = Math.toRadians(lat1);
		double lon1r = Math.toRadians(lon1);
		double lat2r = Math.toRadians(lat2);
		double lon2r = Math.toRadians(lon2);
		double thetaR = lon1r - lon2r;

		double dist = Math.sin(lat1r) * Math.sin(lat2r) + Math.cos(lat1r) * Math.cos(lat2r) * Math.cos(thetaR);
		dist = Math.acos(dist) * GeoConstants.EARTH_RADIUS_KM;

		if (units == GeoConstants.Units.STATUTE_MILES) {
			dist = dist * GeoConstants.KILOMETERS_TO_STATUTEMILES;
		} else if (units == GeoConstants.Units.NAUTICAL_MILES) {
			dist = dist * GeoConstants.KILOMETERS_TO_NAUTICALMILES ;
		}  // 
		return (dist);
	}

	//  default to statute miles 
	public static double distance(double lat1, double lon1, double lat2, double lon2) {
		return distance(lat1, lon1, lat2, lon2, GeoConstants.Units.STATUTE_MILES);
	}

	// should I be using NAUTICAL_MILES here?
	public static BboxXY getBoundingBox(double latCenter, double lonCenter, double radiusMiles) {
		return getBoundingBox(latCenter, lonCenter, radiusMiles, GeoConstants.Units.STATUTE_MILES);
	}
	
	public static BboxXY getBoundingBox(double latCenter, double lonCenter, double radius, GeoConstants.Units units) {
		//  compute four cardinal directions and construct Bbox from this info
		LatLon northLL = getEndpoint(latCenter, lonCenter, 0.0, radius, units);
		LatLon eastLL = getEndpoint(latCenter, lonCenter, 90.0, radius, units);
		LatLon southLL = getEndpoint(latCenter, lonCenter, 180.0, radius, units);
		LatLon westLL = getEndpoint(latCenter, lonCenter, 270.0, radius,units);
		
		BboxXY bbox = new BboxXY(westLL.lon, southLL.lat, eastLL.lon, northLL.lat);
		return bbox;
	}
	
	public static LatLon getEndpoint(double lat, double lon, double bearing, double distance, GeoConstants.Units units) {
		double bearingR = Math.toRadians(bearing);
		double latR = Math.toRadians(lat);
		double lonR = Math.toRadians(lon);
		
		// convert to Km
		double distanceKm = GeoConstants.toKm(distance, units);
		// normalize
		double distanceNorm = distanceKm/GeoConstants.EARTH_RADIUS_KM;
		
		//lat2 = math.asin( math.sin(lat1)*math.cos(d/R) +  math.cos(lat1)*math.sin(d/R)*math.cos(brng))
		double latEndR = Math.asin( Math.sin(latR) * Math.cos(distanceNorm) + Math.cos(latR) * Math.sin(distanceNorm) * Math.cos(bearingR ) );

		//lon2 = lon1 + math.atan2(math.sin(brng)*math.sin(d/R)*math.cos(lat1), math.cos(d/R)-math.sin(lat1)*math.sin(lat2))
		double lonEndR = Math.atan2(Math.sin(bearingR) * Math.sin(distanceNorm) * Math.cos(latR), Math.cos(distanceNorm) - Math.sin(latR) * Math.sin(latEndR) );
		lonEndR += lonR;
		
		return new LatLon(Math.toDegrees(latEndR), Math.toDegrees(lonEndR));
	}
	
	
	public static double getBearing(double lat1, double lon1, double lat2, double lon2) {
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);
		lon1 = Math.toRadians(lon1);
		lon2 = Math.toRadians(lon2);
		double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
		double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2-lon1);
		double bearing = Math.toDegrees(Math.atan2(y, x));
		return bearing;
	}
	
	/**
	 * Given a lat-lon center point and a distance, return the smallest radius that encloses all cardinal points 
	 * that are the given distance away from the center point. 
	 * @param latCenter
	 * @param lonCenter
	 * @param distance
	 * @param units
	 * @return-
	 */
	public static double getRadiusRadians(double latCenter, double lonCenter, double distance, GeoConstants.Units units) {
		//  compute four cardinal directions and construct Bbox from this info
		LatLon northLL = getEndpoint(latCenter, lonCenter, 0.0, distance, units);
		LatLon eastLL = getEndpoint(latCenter, lonCenter, 90.0, distance, units);
		LatLon southLL = getEndpoint(latCenter, lonCenter, 180.0, distance, units);
		LatLon westLL = getEndpoint(latCenter, lonCenter, 270.0, distance,units);
		
		double delNorth = Math.abs(northLL.lat - latCenter);
		double delSouth = Math.abs(southLL.lat - latCenter);
		double delEast = Math.abs(eastLL.lon - lonCenter);
		double delWest = Math.abs(westLL.lon - lonCenter);
		
		double disEw = distance(eastLL.lat, eastLL.lon, latCenter, lonCenter, Units.STATUTE_MILES);
		double disNs = distance(latCenter + delEast, lonCenter , latCenter, lonCenter, Units.STATUTE_MILES);
		System.err.println("Distance: " + disNs + " : " + disEw);
		
		//  in order to ensure our value to Mongo geoWithin function doesn't miss any points, 
		//  need to just the largest of the 4 values above.
		//  Of course, this means Mongo will return points that are outside of the distance radius, so they will ahve to be filtered out.
		double delNs = Math.max(delSouth, delNorth);
		double delEw = Math.max(delEast, delWest);
		return Math.max(delNs, delEw);
	}

	public static boolean checkValidLatLon(LatLon ll) {
		if(ll.lat < -90.0 || ll.lat > 90.0)  return false;
		if(ll.lon < -180.0 || ll.lon > 180.0)  return false;
		return true;
	}
	
	public static long test(int numPoints) {
		LatLon [] ll = new LatLon[numPoints];
		LatLon test = new LatLon(35.0, -85.0);
		
		for(int i=0;i< numPoints; i++ ) {
			double random = Math.random();
			ll[i] = new LatLon(90.0 * random, -180.0*random);
		}
		
		long t1 = System.currentTimeMillis();
		int match = 0;
		double thresh = 1000.0;
		for(int i=0; i<numPoints; i++) {
			double distance = distance(test.lat, test.lon, ll[i].lat, ll[i].lon, GeoConstants.Units.STATUTE_MILES);
			if(distance <= thresh)
				match++;
		}
		long t2 = System.currentTimeMillis();
		System.err.println(numPoints + " tested took " + (t2-t1) + " seconds");
		return t2 - t1;
	}

	public static void main(String[] args) {
//		GeoUtil.test(1000000);

//		LatLon ll = new LatLon(16.42, -92);
//		LatLon ll2 = getEndpoint(ll.lat, ll.lon, 202, 70, Units.STATUTE_MILES);
//		System.err.println(ll2);
//		
		LatLon ll1 = new LatLon( 30.0,-90.000005);
		LatLon ll2 = new LatLon(30.0, -90.0);
		double distance = distance(ll1.lat, ll1.lon, ll2.lat, ll2.lon, Units.STATUTE_MILES);
		distance *= GeoConstants.STATUTEMILES_TO_FEET;
		System.err.println("Distance = " + distance);
		System.err.println("Bearing = " + getBearing(ll1.lat, ll1.lon, ll2.lat, ll2.lon));
		
		LatLon testLL = new LatLon( 42.2801,-85.6454);
		BboxXY bbox = getBoundingBox(testLL.lat, testLL.lon, 40);
		System.err.println(bbox);
		System.err.println(bbox.minX + "," + bbox.minY + "," + bbox.maxX + "," +  bbox.maxY);
		
//		LatLon testLL = new LatLon( 45.0, -100.0);
//		double radius = getRadiusRadians(testLL.lat, testLL.lon, 25.0, Units.STATUTE_MILES);
//		System.err.println(radius);
		
	}

}