package org.sensorhub.impl.sensor.FlightAware;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sensorhub.impl.sensor.FlightAware.FlightAwareApi;
import org.sensorhub.impl.sensor.FlightAware.FlightPlan;
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
	private float [][][] turbData;
	private Array transArr;

	public TurbulenceReader(String path) throws IOException {
		dataset = GridDataset.open(path);
		ncFile = dataset.getNetcdfFile();
		gcs = dataset.getGrids().get(0).getCoordinateSystem();
		proj = gcs.getProjection();
		// Force to lat-lon space
		UcarUtil.toLatLon(ncFile, dataset, X_VAR, Y_VAR, lats, lons);
	}
	
	public void ingestFullFile() throws IOException {
		turbVar = ncFile.findVariable(TURB_VAR);
		if (turbVar == null)
			throw new IOException("TurbulenceReader could not find turbulence variable: " + TURB_VAR);

		// Transpose array for efficient access of vertical profiles
		Array turbArr4d = turbVar.read();
		Array turbArr3d = turbArr4d.reduce();
		transArr = turbArr3d.transpose(0, 2);
		int []shape = transArr.getShape();
		System.err.println(shape[0] + "  " + shape[1] + " " + shape[2]);
		
		turbData = (float[][][])transArr.copyToNDJavaArray();
		removeNaNs();
	}
	
	public void removeNaNs() {
		int []shape = transArr.getShape();
		System.err.println(shape[0] + "  " + shape[1] + " " + shape[2]);
		
		for(int x=0; x<shape[0]; x++) {
			for(int y=0; y<shape[1]; y++) {
				for(int z=0; z<shape[2]; z++) {
					if (Float.isNaN(turbData[x][y][z]))
						turbData[x][y][z] = 0.0f;
				}
			}
		}
	}

	public List<TurbulenceRecord>  getTurbulence(float [] lat, float [] lon) throws IOException, InvalidRangeException {
		return getTurbulence(lat, lon, null);
	}
	
	public List<TurbulenceRecord>  getTurbulence(float [] lat, float [] lon, String [] names) throws IOException, InvalidRangeException {
			
		List<TurbulenceRecord> recs = new ArrayList<>();
		for(int i=0; i<lat.length; i++) {
			TurbulenceRecord rec = new TurbulenceRecord();
			rec.time = readTime(); 
			rec.lat = lat[i];
			rec.lon = lon[i];
			rec.turbulence = getProfileT(lat[i], lon[i]);
			if(names != null)
				rec.waypointName = names[i];
			System.err.println(rec.lat + "," + rec.lon + ":" + rec.turbulence[0] );
			recs.add(rec);
		}

		return recs;
	}

	@Deprecated //  Inefficient- use transpose arrays 
	public float [] getProfile(double lat, double lon) throws IOException, InvalidRangeException {
		//  Convert LatLon coord to spherical mercator proj. (or use gdal to convert entire file to 4326?
		int [] result = null;
		int[] idx = gcs.findXYindexFromLatLon(lat, lon, null);
		if(idx == null || idx[0] == - 1 ||  idx[1] == -1) {
			throw new IOException("Projection toLatLon failed");
		}
//		System.err.println("XY index = " + idx[0] + "," + idx[1]);
		// And actual lat lon of that point (if we need to know how far we are from the requested lat/lon)
		//		LatLonPoint actualLL = gcs.getLatLon(idx[0], idx[1]);
		//		System.err.println("Actual LL: " + actualLL.getLatitude() + "," + actualLL.getLongitude());

		//(time=1, altitude_above_msl=52, y=674, x=902);
		int[] origin = new int[] {0, 0, idx[1], idx[0]};
		int[] size = new int[] {1, 52, 1, 1};
		Array profArr = turbVar.read(origin, size);
		Array profReduce = profArr.reduce();
		float [] prof = (float []) profReduce.copyTo1DJavaArray();
		for(int i=0; i<prof.length; i++) {
//			System.err.println("prof[" + i + "] = " + prof[i]);
			if(Float.isNaN(prof[i]))  // play nice
				prof[i] = 0.0f;
		}

		return prof;
	}

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

	public float [] getProfileT(double lat, double lon) throws IOException, InvalidRangeException {
		//  Convert LatLon coord to spherical mercator proj. (or use gdal to convert entire file to 4326?
		int [] result = null;
		int[] idx = gcs.findXYindexFromLatLon(lat, lon, null);
		if(idx == null || idx[0] == - 1 ||  idx[1] == -1) {
			throw new IOException("Projection toLatLon failed");
		}
		return turbData[idx[0]][idx[1]];
		
	}
	
	public static  void main(String [] argss)  throws Exception {
		// TODO Auto-generated method stub
		TurbulenceReader reader = new TurbulenceReader("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710082130.grb2");
		UcarUtil.dumpVariableInfo(reader.ncFile);

		reader.ingestFullFile();
		reader.removeNaNs();
	}
	
	public static void main_(String[] args) throws Exception {
		TurbulenceReader reader = new TurbulenceReader("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710082130.grb2");
		reader.dumpInfo();
		FlightAwareApi api = new FlightAwareApi();
		FlightPlan plan = api.getFlightPlan("DAL1487-1506921959-airline-0651");

//		reader.dumpInfo();
		List<TurbulenceRecord> recs = reader.getTurbulence(plan.getLats(), plan.getLons());
		for(TurbulenceRecord r: recs)
			System.err.println(r);
		
//		for(int i=0;i<100;i++ ) {
//			System.err.println(i);
//			double lat = 30. + Math.random() * 10;
//			double lon = -90. + Math.random() * 10;
//			reader.getProfile(lat, lon);
//		}
	}
}
