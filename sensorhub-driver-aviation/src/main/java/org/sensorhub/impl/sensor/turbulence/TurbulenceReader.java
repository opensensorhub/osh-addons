package org.sensorhub.impl.sensor.turbulence;

import java.io.IOException;
import java.util.List;

import org.sensorhub.impl.sensor.mesh.UcarUtil;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
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
 *  3D Turbulence Nowcast
o	Source: ECT-derived turbulence product  GTG-NowCast heritage
o	Horizontal Resolution: 6.5 km x 6.5 km
o	Vertical Slices: 1,000ft - 45,000ft at 1000ft intervals
o	Update rate: 15 minutes
o	Data: Turbulence rapid update NWP combined w/ select observations
o	Display Color Table Format: Color shading using same 10 category colors as current FWV ''Turbulence" display:

PROJ.4 : '+proj=lcc +lat_1=25 +lat_2=25 +lat_0=25 +lon_0=265 +x_0=0 +y_0=0 +a=6371229 +b=6371229 +units=m +no_defs '
int LambertConformal_Projection;
  :grid_mapping_name = "lambert_conformal_conic";
  :latitude_of_projection_origin = 25.0; // double
  :longitude_of_central_meridian = 265.0; // double
  :standard_parallel = 25.0; // double
  :earth_radius = 6371229.0; // double
 *
 */

public class TurbulenceReader
{
	// TODO allow var names to be set in config
	private static final String ALT_VAR = "altitude_above_msl";
	private static final String TURB_VAR = "Turbulence_Potential_Forecast_Index_altitude_above_msl";
	private static final String TIME_VAR = "time";
	private static final String X_VAR = "x";
	private static final String Y_VAR = "y";

	GridDataset	dataset;
	Variable crsVar;
	private NetcdfFile ncFile;
	private ProjectionImpl proj;
	private GridCoordSystem gcs;
	private float [] lats;
	private float [] lons;
	private Variable turbVar;

	public TurbulenceReader(String path) throws IOException {
		dataset = GridDataset.open(path);
		ncFile = dataset.getNetcdfFile();
		gcs = dataset.getGrids().get(0).getCoordinateSystem();
		proj = gcs.getProjection();
		toLatLon();
		loadTurb();
	}

	public void toLatLon() throws IOException {
		UcarUtil.toLatLon(ncFile, dataset, X_VAR, Y_VAR, lats, lons);
	}

	private void loadTurb() {
		turbVar = ncFile.findVariable(TURB_VAR);
//		Array turbArr4d = turbVar.read();
//		System.err.println("1");
//		Array turbArr3d = turbArr4d.reduce();
//		System.err.println("2");

	}
	
	public float [] getProfile(double lat, double lon) throws IOException, InvalidRangeException {
		//  Convert LatLon coord to spherical mercator proj. (or use gdal to convert entire file to 4326?
		int [] result = null;
		int[] idx = gcs.findXYindexFromLatLon(lat, lon, null);
		if(idx == null || idx[0] == - 1 ||  idx[1] == -1) {
			throw new IOException("Projection toLatLon failed");
		}
		System.err.println("XY index = " + idx[0] + "," + idx[1]);
		// And actual lat lon of that point (if we need to know how far we are from the requested lat/lon)
//		LatLonPoint actualLL = gcs.getLatLon(idx[0], idx[1]);
//		System.err.println("Actual LL: " + actualLL.getLatitude() + "," + actualLL.getLongitude());
		
//		System.err.println(turbArr3d);
		//(time=1, altitude_above_msl=52, y=674, x=902);
		int[] origin = new int[] {0, 0, idx[1], idx[0]};
		int[] size = new int[] {1, 52, 1, 1};
		Array profArr = turbVar.read(origin, size);
		Array profReduce = profArr.reduce();
		float [] prof = (float []) profReduce.copyTo1DJavaArray();
		for(int i=0; i<prof.length; i++) {
			System.err.println("prof[" + i + "] = " + prof[i]);
		}
		//
		//		  Array data2D = data3D.reduce();

		//		//		System.err.println(meshArr);
		//		Array meshReduce = meshArr.reduce();
		//		float [][] mesh  = (float [][])meshReduce.copyToNDJavaArray();
		//		int width = ax.getShape()[0];
		//		int height = ay.getShape()[0];
		//		for(int  j=0; j<height; j++) {
		//			for(int i=0; i<width; i++) {
		//				//				System.err.println(i + "," + j + "," + mesh[j][i]);
		//				if(mesh[j][i] != 0) {
		//					LatLonPoint llpt = proj.projToLatLon(projx[i], projy[j]);
		//					MeshPoint meshPt = meshRec.new MeshPoint((float)llpt.getLatitude(), (float)llpt.getLongitude(), mesh[j][i]);
		//					meshRec.addMeshPoint(meshPt);
		//				}
		//			}
		//		}

		// interpolate

		return null;
	}

	public void dumpInfo() throws Exception {
		NetcdfDataset ncDataset = dataset.getNetcdfDataset();
		NetcdfFile ncFile = dataset.getNetcdfFile();
		List<GridDatatype> gridTypes = dataset.getGrids();
		List<Gridset> gridSets = dataset.getGridsets();
		List<Variable> vars = ncFile.getVariables();
		for(Variable var: vars) {
			System.err.println(var);
		}
	}

	public static void main(String[] args) throws Exception {
		TurbulenceReader reader = new TurbulenceReader("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710041430.grb2");
		reader.dumpInfo();
		for(int i=0;i<100;i++ ) {
			System.err.println(i);
			double lat = 30. + Math.random() * 10;
			double lon = -90. + Math.random() * 10;
			reader.getProfile(lat, lon);
		}
	}
}
