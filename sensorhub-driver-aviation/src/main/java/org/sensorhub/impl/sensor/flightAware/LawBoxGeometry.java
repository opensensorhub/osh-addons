/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2017 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flightAware;

import java.util.List;

import org.sensorhub.impl.sensor.flightAware.geom.GeoConstants.Units;
import org.sensorhub.impl.sensor.flightAware.geom.GeoUtil;
import org.sensorhub.impl.sensor.flightAware.geom.LatLon;
import org.sensorhub.impl.sensor.flightAware.geom.LatLonAlt;
import org.sensorhub.impl.sensor.navDb.LufthansaParser;
import org.sensorhub.impl.sensor.navDb.NavDbEntry;

/**
 * 
 * <p>
 * Compute the corners of a rectangular volume (the LawBox in pilot-speak)
 * based on inputs from a plane in flight
 * 
 * 	Length = Speed/2.  Minimum 100nm, max 250nm
	Width = 10nm (+/- 5nm either side of plane)
	Height = Vertical rate x2 (in appropriate direction).  Minimum dimension +/- 2,000ft, maximum in one direction is +10,000ft

 *Nice-to-haves*
	Height:
	If within 100nm of origin vertical dimensions = present altitude -2,000ft, +10,000ft
	If within 200nm of destination vertical dimensions = present altitude +2,000ft, -10,000ft
	If both distances are true then vertical dimensions = present altitude +2,000ft, -10,000ft
 * </p>
 *
 * @author tcook
 * @since Oct 18, 2017
 *
 */

public class LawBoxGeometry
{
	Double groundSpeed;
	Double alt;
	Double verticalRate;  //  negative is descent
	Double lat; 
	Double lon;
//	String origIcao;  // if present, fix length 
//	Double origLat, origLon;
	NavDbEntry origin;
	NavDbEntry destination;
//	String destIcao;  // if present, fix length to max 100 mi w/i 100 mi
	Double destLat, destLon;
	Double heading;

	public static final double MIN_LENGTH = 100.0;  // all are nautical miles
	public static final double MAX_LENGTH = 250.0;
	public static final double WIDTH = 10.0;  // fixed
	public static final double MIN_VERTICAL = 2000.0;  // feet
	public static final double MAX_VERTICAL = 10000.0;  // feet
	public static final double MIN_LOWER_BOUND = 1000.0;  // feet
	public static final double MAX_UPPER_BOUND = 50000.0;  // feet
	  
	public LawBoxGeometry(FlightObject pos) {
		// Note- Sensor or Output will need to provide vert rate
		this(pos.getLatitude(), pos.getLongtiude(), pos.getAltitude(), pos.getGroundSpeed(), pos.verticalChange, pos.getHeading());
		if(pos.orig != null) {
			
		}
		if(pos.dest != null) {
			
		}
	}

	public LawBoxGeometry(Double lat, Double lon, Double alt, Double groundSpeed, Double verticalRate, Double heading) {
		this.groundSpeed = groundSpeed;
		this.lat = lat;
		this.lon = lon;
		this.alt = alt;
		this.verticalRate = verticalRate;
		this.heading = heading;
	}
	
//	public void setIcaoCode(String icaoCode) {
//		this.origIcao = icaoCode;
//	}
//
//	public void setDestLat(Double destLat) {
//		this.destLat = destLat;
//	}
//
//	public void setDestLon(Double destLon) {
//		this.destLon = destLon;
//	}
//
	private boolean hasOrigin() {
		return origin != null;
	}

	private boolean hasDestination() {
		return destination != null;
	}

	private double getLength() {
		if(hasDestination()) {
			Double distance = GeoUtil.distance(lat, lon, destination.lat, destination.lon,  Units.NAUTICAL_MILES);
			if(distance < 200.) {
				return (distance < 100.) ? 100. : distance;
			}
		}

		if(hasOrigin()) {
			Double distance = GeoUtil.distance(lat, lon, origin.lat, origin.lon,  Units.NAUTICAL_MILES);
			if(distance < 200.) {
				return (distance < 100.) ? 100. : distance;
			}
		}

		if(groundSpeed == null) {
			return MAX_LENGTH;
		}
		double length = groundSpeed / 2.;  // nautical miles
		if(length < MIN_LENGTH)  return MIN_LENGTH;
		if(length > MAX_LENGTH)  return MAX_LENGTH;
		return length;
	}

	private double getUp() {
		if(hasDestination()) {
			Double distance = GeoUtil.distance(lat, lon, destination.lat, destination.lon,  Units.NAUTICAL_MILES);
			if(distance <= 200.) {
				return 2_000;
			}
		}

		if(hasOrigin()) {
			Double distance = GeoUtil.distance(lat, lon, origin.lat, origin.lon,  Units.NAUTICAL_MILES);
			if(distance <= 100.) {
				return 10_000;
			}
		}
		
		if (verticalRate == null)
			return MIN_VERTICAL; 
		double val = (verticalRate > 0) ? verticalRate * 2. : verticalRate;
		if (val < MIN_VERTICAL)  return MIN_VERTICAL;
		if (val > MAX_VERTICAL)  return MAX_VERTICAL;
		return val;
	}

	private double getDown() {
		if(hasDestination()) {
			Double distance = GeoUtil.distance(lat, lon, destination.lat, destination.lon,  Units.NAUTICAL_MILES);
			if(distance <= 200.) {
				return 10_000;
			}
		}

		if(hasOrigin()) {
			Double distance = GeoUtil.distance(lat, lon, origin.lat, origin.lon,  Units.NAUTICAL_MILES);
			if(distance <= 100.) {
				return 2_000;
			}
		}
		
		if (verticalRate == null)
			return MIN_VERTICAL; 
		double val = (verticalRate < 0) ? verticalRate * 2. : verticalRate;
		if (val < MIN_VERTICAL)  return MIN_VERTICAL;
		if (val > MAX_VERTICAL)  return MAX_VERTICAL;
		return val;
	}

	public void computeBox(LawBox lawBox) {
		computeBox(lawBox, null, null);
	}
	
	//	Length = Speed/2.  Minimum 100nm, max 250nm
	//			Width = 10nm (+/- 5nm either side of plane)
	//			Height = Vertical rate x2 in direction of vert change, x1 in other direction.  Minimum dimension +/- 2,000ft, maximum in one direction is +10,000ft
	//	public LawBox computeBox() {
	public void computeBox(LawBox lawBox, NavDbEntry origin, NavDbEntry destination) {
		this.origin = origin;
		this.destination = destination;
		
		// Need at least location to compute anything
//		assert lat != null &&  lon != null && alt != null;

		double length = getLength(); // nautical miles!
		double up = getUp();
		double down = getDown();

		//  Plane lat Lon Alt 
		// heading > 360 should be taken care of with conversion to radians in util method, but check!
		LatLon br = GeoUtil.getEndpoint(lat, lon, heading + 90., WIDTH, Units.NAUTICAL_MILES); 
		LatLon bl = GeoUtil.getEndpoint(lat, lon, heading - 90., WIDTH, Units.NAUTICAL_MILES); 
		LatLon forwardLl =  GeoUtil.getEndpoint(lat, lon, heading, length, Units.NAUTICAL_MILES);
		LatLon fr = GeoUtil.getEndpoint(forwardLl.lat, forwardLl.lon, heading + 90., WIDTH, Units.NAUTICAL_MILES);
		LatLon fl = GeoUtil.getEndpoint(forwardLl.lat, forwardLl.lon, heading - 90., WIDTH, Units.NAUTICAL_MILES);

		double topAlt = alt + up;
		if(topAlt > MAX_UPPER_BOUND)  topAlt = MAX_UPPER_BOUND;
		double bottomAlt = alt - down;
		if(bottomAlt < MIN_LOWER_BOUND)  bottomAlt = MIN_LOWER_BOUND;
		lawBox.brTopLla = new LatLonAlt(br.lat, br.lon, topAlt); 
		lawBox.brBottomLla = new LatLonAlt(br.lat, br.lon, bottomAlt); 
		lawBox.blTopLla = new LatLonAlt(bl.lat, bl.lon, topAlt); 
		lawBox.blBottomLla = new LatLonAlt(bl.lat, bl.lon, bottomAlt); 
		lawBox.frTopLla = new LatLonAlt(fr.lat, fr.lon, topAlt);
		lawBox.frBottomLla = new LatLonAlt(fr.lat, fr.lon, bottomAlt);
		lawBox.flTopLla = new LatLonAlt(fl.lat, fl.lon, topAlt); 
		lawBox.flBottomLla = new LatLonAlt(fl.lat, fl.lon, bottomAlt); 

	}

	public static void main(String[] args) throws Exception {
		double lat = 30.;
		double lon = -100.;
		double alt = 2_000.;
		double groundSpeed = 300.;
		double verticalRate = -8000;
		double heading = 90.;

		LawBoxGeometry lbGeom = new LawBoxGeometry(lat, lon, alt, groundSpeed, verticalRate, heading);
//		lbGeom.origLat = 2.1;
//		lbGeom.origLon = -110.1;
//		lbGeom.destLat = 31.9;
//		lbGeom.destLon = -101.1;
		LawBox box = new LawBox(lbGeom);
		box.computeBox();
		System.err.println(box);
	}
}
