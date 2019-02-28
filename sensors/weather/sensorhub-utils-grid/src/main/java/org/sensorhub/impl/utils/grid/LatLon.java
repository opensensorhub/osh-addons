/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.utils.grid;

import java.text.DecimalFormat;

/**
 * <p>Title: LatLonAlt.java</p>
 * <p>Description: Note we don't define degrees or radians here, except for some convenience methods.
 *    that is up to the calling class to deal with </p>
 * @author Tony Cook
 * @since Feb 13, 2011
 */

public class LatLon 
{
	public double lat, lon;
	static final double DTR = Math.PI / 180.0;
	static final double RTD = 1 / DTR;
	
	public LatLon() {
	}
	
	public LatLon(double lat, double lon) {
		this.lat = lat; 
		this.lon = lon;
	}	
	
	//  convention here is positive North lat, positive east Lon
	public LatLon(int latDegrees, int latMinutes, double latSeconds, int lonDegrees, int lonMinutes, double lonSeconds) {
		double latSign = (latDegrees >= 0) ? 1.0: -1.0;
		double lonSign = (lonDegrees >= 0) ? 1.0: -1.0;
		this.lat = latDegrees + (latSign * (double)latMinutes/60.0) + latSign*latSeconds/3600.0;
		this.lon = lonDegrees + (lonSign * (double)lonMinutes/60.0) + (lonSeconds/3600.0);
	}
	
	//  convention here is positive North lat, positive east Lon
	public LatLon(int latDegrees, double latMinutes, int lonDegrees, double lonMinutes) {
		double latSign = (latDegrees >= 0) ? 1.0: -1.0;
		double lonSign = (lonDegrees >= 0) ? 1.0: -1.0;
		this.lat = latDegrees + (latSign * (double)latMinutes/60.0);
		this.lon = lonDegrees + (lonSign * (double)lonMinutes/60.0);
	}
	
	// lat,lon
	public LatLon(String llStr) throws NumberFormatException {
		String [] llSplit = llStr.split(",");
		this.lat = Double.parseDouble(llSplit[0]);
		this.lon = Double.parseDouble(llSplit[1]);
	}
	
	public void toRadians() {
		lat *= DTR;
		lon *= DTR;
	}
	
	public void toDegrees() {
		lat *= RTD;
		lon *= RTD;
	}

	public String toString() {
		DecimalFormat df = new DecimalFormat("###.####");
		return df.format(lat) + "," + df.format(lon);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(lat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(lon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LatLon other = (LatLon) obj;
		if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
			return false;
		if (Double.doubleToLongBits(lon) != Double.doubleToLongBits(other.lon))
			return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		LatLon ll1 = new LatLon(32.456, -100.0999999);
		LatLon ll2 = new LatLon(32.456, -100.0999999);
		System.err.println(ll1.equals(ll2));
	}
}
