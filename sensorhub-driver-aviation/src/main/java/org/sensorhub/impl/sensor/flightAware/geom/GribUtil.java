/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2017 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flightAware.geom;

import java.io.IOException;

import org.sensorhub.impl.sensor.flightAware.TurbulenceReader;

import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;

import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonPoint;

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

	public static Coordinate [] getProjectedWaypoints(GridCoordSystem gcs, float [] lat, float [] lon) throws IOException {
		Coordinate [] pts = new Coordinate [lat.length];

		//  Translate the input data into XY indexes
		for(int i=0; i<lat.length; i++) {
			pts[i] = latLonToXY(gcs, lat[i], lon[i]);
		}
		return pts;
	}

	
	/**
	 * 
	 * @param lat - array of latitudes
	 * @param lon - array of longitudes
	 * @return  an array of coordinates of x/y indices of all points between and including first lat/lon to last lat/lon
	 * @throws IOException
	 */
	public static Coordinate[] getPathIndices(GridCoordSystem gcs, float [] lat, float [] lon) throws IOException {
		Coordinate[] coords  = new Coordinate[lat.length] ;
		for(int i = 0; i<lat.length; i++)
			coords[i] = latLonToXY(gcs, lat[i], lon[i]);
		return getPathIndices(gcs, coords);
	}
	
	/**
	 * 
	 * @param lat - array of latitudes
	 * @param lon - array of longitudes
	 * @return  an array of coordinates of x/y indices of all points between and including first lat/lon to last lat/lon
	 * @throws IOException
	 */
	public static Coordinate[] getPathIndices(GridCoordSystem gcs, Coordinate [] coords) throws IOException {
		PrecisionModel pm = new PrecisionModel(1.0);
		GeometryFactory geometryFactory = new GeometryFactory(pm);
		
		LineString line = geometryFactory.createLineString(coords);
//		System.err.println(line);
		LineString l2 = (LineString)Densifier.densify(line, 1.);
//		System.err.println(l2);
		
		return l2.getCoordinates();
	}

	/**
	 * 
	 * @param lat - array of latitudes
	 * @param lon - array of longitudes
	 * @return  an array of coordinates of x/y indices of all points between and including first lat/lon to last lat/lon
	 * @throws IOException
	 */
	public static Coordinate[] getPathIndices(GridCoordSystem gcs, Coordinate start, Coordinate stop) throws IOException {
		PrecisionModel pm = new PrecisionModel(1.0);
		GeometryFactory geometryFactory = new GeometryFactory(pm);
		
		LineString line = geometryFactory.createLineString(new Coordinate[] {start, stop});
		//System.err.println(line);
		LineString l2 = (LineString)Densifier.densify(line, 1.);
		//System.err.println(l2);
		
		return l2.getCoordinates();
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
	
	public static  void main_(String [] args)  throws Exception {
		// TODO Auto-generated method stub
		TurbulenceReader reader = new TurbulenceReader("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710231815.grb2");
		//		reader.removeNaNs();
		//		double [] lat = {30.0, 32.0, 34.0, 36.0, 38.0, 40.0};
		//		double [] lon = {-95., -95., -95., -95., -95., -95.};

		//				double [] lat = {30.0, 30.0, 30.0, 30.0, 30.0, 30.0};
		//				double [] lon = {-90., -92., -94., -96., -98., -100.};
		//
		float [] lat = {30.0f, 32.0f, 34.0f, 36.0f, 38.0f, 40.0f};
		float [] lon = {-90.f, -92.f, -94.f, -96.f, -98.f, -100.f};

		//		double [] lat = {30.0, 30.001, 30.002, 30.003, 30.004, 30.005};
		//		double [] lon = {-90.0, -90.001, -90.002, -90.003, -90.004, -90.005};

		Coordinate [] path = getPathIndices(reader.getGridCoordSystem() ,lat, lon);
	}


}
