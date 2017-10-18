package org.sensorhub.impl.sensor.lawBox;

/**
 * <p>Title: GeoConstants.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Jan 11, 2013
 */
public class GeoConstants
{
	public static enum Units { STATUTE_MILES, KILOMETERS, NAUTICAL_MILES }
	
	public static final double EARTH_RADIUS_KM = 6371.0;
	public static final double EARTH_RADIUS_MILES = 3959.0;  // tighten this up, add major/minor axis
	public static final double WGS84_POLAR_RADIUS_LM = 6378.137;
	public static final double WGS84_EQUATORIAL_RADIUS_LM = 6356.7523142;
	
	// multipliers for common conversions
	public static final double METERS_TO_FEET = 3.2808399;
	public static final double FEET_TO_METERS = 0.3048;
	public static final double METERS_TO_STATUTEMILES = 0.000621371;
	public static final double FEET_TO_STATUTEMILES = 0.000189394;
	public static final double STATUTEMILES_TO_FEET = 5280.0;
	public static final double KILOMETERS_TO_NAUTICALMILES = 0.539956803;
	public static final double STATUTEMILES_TO_NAUTICALMILES = 0.8684;
	public static final double KILOMETERS_TO_STATUTEMILES = 0.6213712;
	public static final double STATUTEMILES_TO_KILOMETERS = 1.609344;
	public static final double CM_TO_INCHES = 0.393701;
	public static final double MM_TO_INCHES = 0.0393701;
	
	//  Source: http://en.wikipedia.org/wiki/Extreme_points_of_the_United_States#Westernmost
	public static double CONUS_NORTH_LAT = 49.3457868;
	public static double CONUS_WEST_LON =-124.7844079;
	public static double CONUS_EAST_LON = -66.9513812;
	public static double CONUS_SOUTH_LAT = 24.7433195;

	public static double toKm(double distance, Units units) {
		switch (units) {
		case KILOMETERS:
			return distance;
		case NAUTICAL_MILES:
			return distance / KILOMETERS_TO_NAUTICALMILES; 
		case STATUTE_MILES:
			return distance * STATUTEMILES_TO_KILOMETERS; 
		default:
			System.err.println("GeoConstants.toKm(): Unknown units");
			return distance;
		}
	}
}
