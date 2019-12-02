/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mesh;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.sensorhub.impl.sensor.mesh.MeshRecord.MeshPoint;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;


/**
 * <p>
 * Reader for MESH data files that can be downloaded from NOAA MRMS web site:
 * https://mrms.ncep.noaa.gov/data/2D/
 * </p>
 *
 * @author tcook
 * @since Aug 29, 2017
 * 
 *
int LatLon_Projection;
  :grid_mapping_name = "latitude_longitude";
  :earth_radius = 6371229.0; // double
  :semi_major_axis = 6378160.0; // double
  :semi_minor_axis = 6356684.713804713; // double
  :_CoordinateTransformType = "Projection";
  :_CoordinateAxisTypes = "GeoX GeoY";

float MESH_altitude_above_msl(time=1, altitude_above_msl=1, lat=3500, lon=7000);
  :long_name = "MESH @ Specific altitude above mean sea level";
  :units = "mm";
  :description = "Maximum Estimated Size of Hail (MESH)";
  :missing_value = -1.0f; // float
  :_FillValue = -3.0f; // float
  :grid_mapping = "LatLon_Projection";
  :coordinates = "reftime time altitude_above_msl lat lon ";
  :Grib_Variable_Id = "VAR_209-3-28_L102";
  :Grib2_Parameter = 209, 3, 28; // int
  :Grib2_Parameter_Name = "MESH";
  :Grib2_Level_Type = "Specific altitude above mean sea level";
  :Grib2_Generating_Process_Type = "Observation";

float lat(lat=3500);
  :units = "degrees_north";
  :_CoordinateAxisType = "Lat";

float lon(lon=7000);
  :units = "degrees_east";
  :_CoordinateAxisType = "Lon";

double reftime;
  :units = "Minute since 2019-11-22T01:48:35Z";
  :standard_name = "forecast_reference_time";
  :long_name = "GRIB reference time";
  :calendar = "proleptic_gregorian";
  :_CoordinateAxisType = "RunTime";

double time(time=1);
  :units = "Minute since 2019-11-22T01:48:35Z";
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
 */
public class MeshReader
{
	//  Var names we need from the Grib file.  Note, obviously we are going to have to rely on these to not change
	//  Should put in config file in case they do change
	private static final String MESH_VAR = "MESH_altitude_above_msl";
	private static final String TIME_VAR = "time";
    private static final String LON_VAR = "lon";
    private static final String LAT_VAR = "lat";
    private static final String X_VAR = "x";
    private static final String Y_VAR = "y";

	GridDataset	dataset;
	Variable crsVar;
	private NetcdfFile ncFile;
	private ProjectionImpl proj;
	Path filepath;
	

	public MeshReader(String path) throws IOException {
		this.filepath = Paths.get(path);
		dataset = GridDataset.open(path);
		ncFile = dataset.getNetcdfFile();
		GridCoordSystem gcs =  dataset.getGrids().get(0).getCoordinateSystem();
		proj = gcs.getProjection();
	}

	public void dumpInfo() {
		List<Variable> vars = ncFile.getVariables();
		for(Variable var: vars) {
			System.out.println(var);
		}
	}

	//	public float[][] readMesh() throws IOException {
	public MeshRecord readMesh() throws IOException {
		MeshRecord meshRec = new MeshRecord();
		
		// time
		meshRec.timeUtc = readTime();
		
		// get either lat/lon or proj x/y variables
		Variable vx = ncFile.findVariable(LON_VAR);
		if (vx == null)
		    vx = ncFile.findVariable(X_VAR);
		
		Variable vy = ncFile.findVariable(LAT_VAR);
		if (vy == null)
            vy = ncFile.findVariable(Y_VAR);
		
		Array ax = vx.read();
		Array ay = vy.read();
		float [] projx = (float[])ax.getStorage();
		float [] projy =  (float[])ay.getStorage();

		// read mesh data and associate to lat/lon locations
		Variable vmesh = ncFile.findVariable(MESH_VAR);
		vmesh.setCaching(false); // don't keep cache as it prevents the array to be garbage collected quickly
		Array meshArr = vmesh.read();
		Array meshReduce = meshArr.reduce();
		
		// read by iterating through index to avoid copying the array
		IndexIterator it = meshReduce.getIndexIterator();
		while (it.hasNext()) {
		    float val = it.getFloatNext();		    
		    if (!Float.isNaN(val) && val != 0) {
	            int[] dims = it.getCurrentCounter();
	            int i = dims[0];
	            int j = dims[1];
		        LatLonPoint llpt = proj.projToLatLon(projx[j], projy[i]);
                MeshPoint meshPt = meshRec.new MeshPoint((float)llpt.getLatitude(), (float)llpt.getLongitude(), val);
                meshRec.addMeshPoint(meshPt);
            }		    
		}
		
		return meshRec;
	}

	/**
	 * Base Time is stored in the "units field of the time var, NOT the actual storage. 
	 * Need to extract it and possible add "minutes offset" value in storage (so far always 0)
	 * i.e. double time(time=1);
  			:units = "Minute since 2017-08-04T21:46:39Z";
     * @return Unix time stamp, seconds since 01/01/1970 UTC
	 * @throws IOException
	 */
	public long readTime() throws IOException {
		Variable vtime = ncFile.findVariable(TIME_VAR);
		String ustr = vtime.getUnitsString();
		String [] uArr = ustr.split(" ");
		String tstr = uArr[2];
		Array atime = vtime.read();
		double timeMin = atime.getDouble(0);
		Instant instant = Instant.parse(tstr);
		long time = instant.getEpochSecond();
		return time + ((long)timeMin * TimeUnit.MINUTES.toSeconds(1L));
	}

}
