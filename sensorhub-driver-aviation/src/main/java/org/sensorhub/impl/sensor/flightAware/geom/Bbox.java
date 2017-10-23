package org.sensorhub.impl.sensor.flightAware.geom;

import java.awt.geom.Rectangle2D;

/**
 * <p>Title: Bbox.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 * @since Jul 21, 2011
 * 
 * TODO Verify correct min/max order upon construction and setters
 */

public class Bbox
{
	public LatLon minLatLon, maxLatLon;  //  lowerleft, upperRight
	
	public Bbox() {
	}
	
	public Bbox(LatLon minLL, LatLon maxLL) {
		this.minLatLon = minLL;
		this.maxLatLon = maxLL;
	}
	
	
	//  Convert from Java rectangle (used by WCT routines) to Tony Bbox
	//  Java rectangle x,y is UPPER LEFT
	public Bbox(Rectangle2D.Double rectBox) {
		double minLat = rectBox.y;
		double minLon = rectBox.x;
		double maxLat = rectBox.y  + rectBox.height;
		double maxLon = rectBox.x + rectBox.width;
		
		minLatLon = new LatLon(minLat, minLon);
		maxLatLon = new LatLon(maxLat, maxLon);
	}
	
	/**
	 * 
	 * @return LatLon [] of the verts- order is not guaranteed 
	 */
	public LatLon [] getVertices() {
		LatLon [] verts = new LatLon[5];
		
		verts[0] = new LatLon(minLatLon.lat, minLatLon.lon);
		verts[1] = new LatLon(maxLatLon.lat, minLatLon.lon);
		verts[2] = new LatLon(maxLatLon.lat, maxLatLon.lon);
		verts[3] = new LatLon(minLatLon.lat, maxLatLon.lon);
		verts[4] = new LatLon(minLatLon.lat, minLatLon.lon);
		
		return verts;
	}
	
	public static Bbox getBboxQuad(Bbox srcBbox, int quad) {
		Bbox destBbox = new Bbox();
		double minLat, minLon, maxLat, maxLon;
		switch(quad) {
		case 0:  //   UPPER LEFT
			minLat = (srcBbox.maxLatLon.lat + srcBbox.minLatLon.lat)/2.0;
			maxLat = srcBbox.maxLatLon.lat;
			minLon = srcBbox.minLatLon.lon;
			maxLon = (srcBbox.maxLatLon.lon + srcBbox.minLatLon.lon)/2.0;
			break;
		case 1:  //   UPPER RIGHT
			minLat = (srcBbox.maxLatLon.lat + srcBbox.minLatLon.lat)/2.0;
			maxLat = srcBbox.maxLatLon.lat;
			minLon = (srcBbox.maxLatLon.lon + srcBbox.minLatLon.lon)/2.0;
			maxLon = srcBbox.maxLatLon.lon;
			break;
		case 2:  //   LOWER LEFT
			minLat = srcBbox.minLatLon.lat;
			maxLat = (srcBbox.maxLatLon.lat + srcBbox.minLatLon.lat)/2.0;
			minLon = srcBbox.minLatLon.lon;
			maxLon = (srcBbox.maxLatLon.lon + srcBbox.minLatLon.lon)/2.0;
			break;
		case 3:  //   LOWER RIGHT
			minLat = srcBbox.minLatLon.lat;
			maxLat = (srcBbox.maxLatLon.lat + srcBbox.minLatLon.lat)/2.0;
			minLon = (srcBbox.maxLatLon.lon + srcBbox.minLatLon.lon)/2.0;
			maxLon = srcBbox.maxLatLon.lon;
			break;
		default:
	        throw new AssertionError(quad); 
		}
		
		destBbox.minLatLon = new LatLon(minLat, minLon);
		destBbox.maxLatLon = new LatLon(maxLat, maxLon);
		return destBbox;
	}
	
	//  Simple bridge to BboxXY class
	public BboxXY toBboxXY() {
		return new BboxXY(minLatLon.lon, minLatLon.lat, maxLatLon.lon, maxLatLon.lat);
	}
	
	public LatLon getCenterLatLon() {
		return new LatLon((minLatLon.lat + maxLatLon.lat)/2.0, (minLatLon.lon + maxLatLon.lon)/2.0);
	}
	
	public String toString() {
		return minLatLon.lat + "," + minLatLon.lon + " ==> " + maxLatLon.lat  + "," + maxLatLon.lon;
	}
	
}
