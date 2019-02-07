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

import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;
import ucar.nc2.dt.GridCoordSystem;


public class GribUtil
{
	//  Make consistent with doubles/floats
	public static Coordinate latLonToXY (GridCoordSystem gcs, LatLon ll) throws IOException {
		return latLonToXY(gcs, (float)ll.lat, (float)ll.lon);
	}

	public static Coordinate latLonToXY (GridCoordSystem gcs, float lat, float lon) throws IOException {
		int [] idx = gcs.findXYindexFromLatLon(lat, lon, null);
		if(idx == null || idx[0] == - 1 ||  idx[1] == -1) {
			throw new IOException("Projection fromLatLon failed. Point in path is off the available grid: " + lat + "," + lon);
		}
		return  new Coordinate(idx[0], idx[1]);
	}

	public static Coordinate [] getGridXYCoordinates(GridCoordSystem gcs, float [] lat, float [] lon) throws IOException {
		Coordinate [] pts = new Coordinate [lat.length];

		//  Translate the input data into XY indexes
		for(int i=0; i<lat.length; i++) {
			pts[i] = latLonToXY(gcs, lat[i], lon[i]);
		}
		return pts;
	}

	public static double min (Coordinate[] carr, int axis) {
		double min = Double.MAX_VALUE;
		for(Coordinate c: carr) {
			switch (axis) {
			case Coordinate.X:
				if(c.x < min)  min = c.x;
				break;
			case Coordinate.Y:
				if(c.y < min)  min = c.y;
				break;
			case Coordinate.Z:
				if(c.z < min)  min = c.z;
				break;
			default:
				throw new IllegalArgumentException("Value of axis must be between 0 and 2");
			}
				
		}
		
		return min;
	}
	
	public static double max (Coordinate[] carr, int axis) {
		double max = Double.MIN_VALUE;
		for(Coordinate c: carr) {
			switch (axis) {
			case Coordinate.X:
				if(c.x > max)  max = c.x;
				break;
			case Coordinate.Y:
				if(c.y > max)  max = c.y;
				break;
			case Coordinate.Z:
				if(c.z > max)  max = c.z;
				break;
			default:
				throw new IllegalArgumentException("Value of axis must be between 0 and 2");
			}
				
		}
		
		return max;
	}
	
	public static void main(String[] args) throws Exception {
		Coordinate [] c = new Coordinate[] {
				new Coordinate(1,1,1),
				new Coordinate(2,-2,2),
				new Coordinate(3,13,3),
				new Coordinate(4,4,14),
		};
		System.err.println(min(c, Coordinate.Z));
		System.err.println(max(c, 3));
	}
}
