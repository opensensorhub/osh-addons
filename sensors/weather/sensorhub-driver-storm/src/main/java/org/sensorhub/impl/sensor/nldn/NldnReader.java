/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nldn;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sensorhub.impl.sensor.nldn.NldnRecord.NldnPoint;
import org.sensorhub.impl.utils.grid.EarthcastUtil;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;

/**
 * 
 * <p>
 * 
 * </p>
 *
 * @author tcook
 * @since Aug 29, 2017
 * 
 *
 * SLF4J: Class path contains multiple SLF4J bindings.
SLF4J: Found binding in [jar:file:/C:/Users/tcook/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-jdk14/1.7.21/168ee1e516a458bd80fb23caf2e512ed41e1e865/slf4j-jdk14-1.7.21.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: Found binding in [jar:file:/C:/Users/tcook/.gradle/caches/modules-2/files-2.1/ch.qos.logback/logback-classic/1.1.2/b316e9737eea25e9ddd6d88eaeee76878045c6b2/logback-classic-1.1.2.jar!/org/slf4j/impl/StaticLoggerBinder.class]
SLF4J: See http://www.slf4j.org/codes.html#multiple_bindings for an explanation.
SLF4J: Actual binding is of type [org.slf4j.impl.JDK14LoggerFactory]
int LambertConformal_Projection;
  :grid_mapping_name = "lambert_conformal_conic";
  :latitude_of_projection_origin = 25.0; // double
  :longitude_of_central_meridian = 265.0; // double
  :standard_parallel = 25.0; // double
  :earth_radius = 6371229.0; // double
  :_CoordinateTransformType = "Projection";
  :_CoordinateAxisTypes = "GeoX GeoY";

float LightningDensityNLDN5min_altitude_above_msl(time=1, altitude_above_msl=1, y=674, x=902);
  :long_name = "LightningDensityNLDN5min @ Specific altitude above mean sea level";
  :units = "flashes/km2/min";
  :description = "CG Lightning Density 5-min - NLDN";
  :missing_value = -1.0f; // float
  :_FillValue = -3.0f; // float
  :grid_mapping = "LambertConformal_Projection";
  :coordinates = "reftime time altitude_above_msl y x ";
  :Grib_Variable_Id = "VAR_209-2-1_L102";
  :Grib2_Parameter = 209, 2, 1; // int
  :Grib2_Parameter_Name = "LightningDensityNLDN5min";
  :Grib2_Level_Type = "Specific altitude above mean sea level";
  :Grib2_Generating_Process_Type = "Observation";

float x(x=902);
  :standard_name = "projection_x_coordinate";
  :units = "km";
  :_CoordinateAxisType = "GeoX";

float y(y=674);
  :standard_name = "projection_y_coordinate";
  :units = "km";
  :_CoordinateAxisType = "GeoY";

double reftime;
  :units = "Minute since 2017-09-14T20:28:00Z";
  :standard_name = "forecast_reference_time";
  :long_name = "GRIB reference time";
  :calendar = "proleptic_gregorian";
  :_CoordinateAxisType = "RunTime";

double time(time=1);
  :units = "Minute since 2017-09-14T20:28:00Z";
  :standard_name = "time";
  :long_name = "GRIB forecast or observation time";
  :calendar = "proleptic_gregorian";
  :_CoordinateAxisType = "Time";

float altitude_above_msl(altitude_above_msl=1);
  :units = "m";
  :long_name = "Specific altitude above mean sea level";
  :positive = "up";
  :Grib_level_type = 102; // int
  :datum = "mean sea level";
  :_CoordinateAxisType = "Height";
  :_CoordinateZisPositive = "up";


 *
 *
 *
 */

public class NldnReader
{
	//  Var names we need from the Grib file.  Note, obviously we are going to have to rely on these to not change
	//  Should put in config file in case they do change
	private static final String ALT_VAR = "altitude_above_msl";
	private static final String NLDN_VAR = "LightningDensityNLDN5min_altitude_above_msl";
	private static final String TIME_VAR = "time";
	private static final String X_VAR = "x";
	private static final String Y_VAR = "y";

	GridDataset	dataset;
	Variable crsVar;
	private NetcdfFile ncFile;
	private ProjectionImpl proj;
	Path filepath;
	
	public NldnReader(String path) throws IOException {
		filepath = Paths.get(path);
		dataset = GridDataset.open(path);
		ncFile = dataset.getNetcdfFile();
		GridCoordSystem gcs =  dataset.getGrids().get(0).getCoordinateSystem();
		proj = gcs.getProjection();
	}

	// Make this a Util method
	public void dumpInfo() throws Exception {
		List<GridDatatype> gridTypes = dataset.getGrids();
		List<Gridset> gridSets = dataset.getGridsets();
		List<Variable> vars = ncFile.getVariables();

		crsVar = ncFile.findVariable("LambertConformal_Projection");
		for(Variable var: vars) {
			System.err.println(var);
		}
	}

	public double readAlt() throws IOException {
		Variable valt = ncFile.findVariable(ALT_VAR);
		Array altArr = valt.read();
		//		System.err.println(altArr);
		double alt = altArr.getDouble(0);
		return alt;
	}

	//	public float[][] readNldn() throws IOException {
	//		Variable vnldn= ncFile.findVariable(NLDN_VAR);
	//		Array nldnArr = vnldn.read();
	//		Array nldnReduce = nldnArr.reduce();
	//		float [][] nldn= (float [][])nldnReduce. copyToNDJavaArray();
	//		return nldn;
	//	}

	public NldnRecord readNldn() throws IOException {
		NldnRecord nldnRec = new NldnRecord();
		// time
		nldnRec.timeUtc = EarthcastUtil.computeTime(filepath.getFileName().toString());

		//  Proj info
		Variable vx = ncFile.findVariable(X_VAR);
		Variable vy = ncFile.findVariable(Y_VAR);
		Array ax = vx.read();
		Array ay = vy.read();
		float [] projx = (float [] )ax.getStorage();
		float [] projy =  (float [] )ay.getStorage();

		Variable vnldn= ncFile.findVariable(NLDN_VAR);
		Array nldnArr = vnldn.read();
		//		System.err.println(meshArr);
		Array nldnReduce = nldnArr.reduce();
		float [][] nldn = (float [][])nldnReduce. copyToNDJavaArray();
		int width = ax.getShape()[0];
		int height = ay.getShape()[0];
		for(int  j=0; j<height; j++) {
			for(int i=0; i<width; i++) {
				//				System.err.println(i + "," + j + "," + mesh[j][i]);
				if(nldn[j][i] != 0) {
					LatLonPoint llpt = proj.projToLatLon(projx[i], projy[j]);
					NldnPoint nldnPt = nldnRec.new NldnPoint((float)llpt.getLatitude(), (float)llpt.getLongitude(), nldn[j][i]);
					nldnRec.addMeshPoint(nldnPt);
				}
			}
		}
		return nldnRec;
	}

	/**
	 * Base Time is stored in the "units field of the time var, NOT the actual storage. 
	 * Need to extract it and possible add "minutes offset" value in storage (so far always 0)
	 * i.e. double time(time=1);
  			:units = "Minute since 2017-08-04T21:46:39Z";
	 * @throws IOException
	 */
	@Deprecated // Use timestamp encoded in filename
	public long readTime() throws IOException {
		Variable vtime = ncFile.findVariable(TIME_VAR);
		String ustr = vtime.getUnitsString();
		String [] uArr = ustr.split(" ");
		String tstr = uArr[2];
		Array atime = vtime.read();
		double timeMin = atime.getDouble(0);
		//		System.err.println(timeMin);
		Instant instant = Instant.parse( tstr );
		long time = instant.getEpochSecond();
		long timeUtc = time + ((long)timeMin * TimeUnit.MINUTES.toSeconds(1L) );
		return timeUtc;
	}

	public static void main(String[] args) throws Exception {
		//		MeshReader reader = new MeshReader("C:/Data/sensorhub/delta/MESH/ECT_NCST_DELTA_MESH_6_5km.201709111220.grb2");
		NldnReader reader = new NldnReader("C:/home/tcook/osh/mesh/data/NLDN/ECT_NCST_DELTA_NLDN_CG_6_5km.201710251830.grb2");
		NldnRecord r = reader.readNldn();
		System.err.println(r.timeUtc);

		//				MeshRecord rec = reader.createMeshRecord();

		//		reader.toLatLon();
		//		reader.readTime();
		//		reader.readAlt();
		//		reader.readMesh();

		//		NetcdfFile ncFile = NetcdfFile.open("C:/Data/sensorhub/delta/MESH/ECT_NCST_DELTA_MESH_6_5km.201709111220.nc");
		//		UcarUtil.dumpAttributeInfo(ncFile);
		//		UcarUtil.dumpVariableInfo(ncFile);
	}

}
