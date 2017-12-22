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

import org.sensorhub.impl.sensor.flightAware.geom.LatLonAlt;
import org.sensorhub.impl.sensor.navDb.NavDbEntry;

import com.vividsolutions.jts.geom.Coordinate;

public class LawBox
{
	LatLonAlt brTopLla;
	LatLonAlt brBottomLla; 
	LatLonAlt blTopLla;
	LatLonAlt blBottomLla; 
	LatLonAlt frTopLla;
	LatLonAlt frBottomLla;
	LatLonAlt flTopLla;
	LatLonAlt flBottomLla; 
	LawBoxGeometry geom;
	Float maxTurb;
	Coordinate maxCoordXYZ;
	LatLonAlt maxCoordLla;  // harmonize with my container LatLon classes
	FlightObject position;
	int changeFlag;
//	Double origLat, origLon;
//	Double destLat, destLon;
	
	public LawBox(FlightObject position) {
		this.position = position;
		this.geom = new LawBoxGeometry(position);
	}
	
	public LawBox(LawBoxGeometry geometry) {
		this.geom = geometry;
	}
	
	public void computeBox() {
		geom.computeBox(this);
	}
	
	public void computeBox(NavDbEntry origin, NavDbEntry destination) {
		geom.computeBox(this, origin, destination);
	}
	
	public double []  getBoundary() {
		// assert br,bl,fr,fl non null
		double [] boundary = new double[] {
				blTopLla.lat, blTopLla.lon,
				brTopLla.lat, brTopLla.lon,
				frTopLla.lat, frTopLla.lon,
				flTopLla.lat, flTopLla.lon
		};
		
		
		return boundary;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		if(position != null)
			b.append(position.getTimeStr() + "\n");
		b.append("brTop: " + brTopLla + "\n");
		b.append("blTop: " + blTopLla + "\n");
		b.append("flTop: " + flTopLla + "\n");
		b.append("frTop: " + frTopLla + "\n");
		b.append("brBottom: " + brBottomLla + "\n");
		b.append("blBottom: " + blBottomLla + "\n");
		b.append("flBottom: " + flBottomLla + "\n");
		b.append("frBottom: " + frBottomLla + "\n");
		b.append("lowerBound: " + brBottomLla.alt + "\n");
		b.append("upperBound: " + brTopLla.alt + "\n");
		b.append("MaxTurb = " + maxTurb + "\n");
		b.append("MaxCoordLL = " + maxCoordLla + "\n");
		
		return b.toString();
	}
	
}
