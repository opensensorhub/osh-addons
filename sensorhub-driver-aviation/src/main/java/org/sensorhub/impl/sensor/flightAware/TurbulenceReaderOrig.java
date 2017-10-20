package org.sensorhub.impl.sensor.flightAware;

import java.awt.Point;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.sensorhub.impl.sensor.flightAware.FlightPlan.Waypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class TurbulenceReaderOrig
{
	// TODO allow var names to be set in config
	private static final String ALT_VAR = "altitude_above_msl";
	private static final String TURB_VAR = "Turbulence_Potential_Forecast_Index_altitude_above_msl";
//	private static final String TIME_VAR = "forecast_reference_time";
	private static final String TIME_VAR = "time";
	private static final String X_VAR = "x";
	private static final String Y_VAR = "y";

	GridDataset	dataset;
	Variable crsVar;
	private NetcdfFile ncFile;
	private ProjectionImpl proj;
	private GridCoordSystem gcs;
	private Variable turbVar;
	private float [][][] turbData;
	private Array transArr;
	float [] projx;
	float [] projy;
	private List<Integer> wayptIndices = new ArrayList<>();  // when we create full path, need to create these so they can be included in response 
	long time = 0L;

	static final Logger log = LoggerFactory.getLogger(TurbulenceReaderOrig.class);
	
	public TurbulenceReaderOrig(String path) throws IOException {
		dataset = GridDataset.open(path);
		ncFile = dataset.getNetcdfFile();
		gcs = dataset.getGrids().get(0).getCoordinateSystem();
		proj = gcs.getProjection();
//		time = readTime();
		time = computeTime(path);
		
	    Variable vx = ncFile.findVariable(X_VAR);
	    Variable vy = ncFile.findVariable(Y_VAR);
	    Array ax = vx.read();
	    Array ay = vy.read();
	    projx = (float [] )ax.getStorage();
	    projy =  (float [] )ay.getStorage();

//		float[][] latLons = UcarUtil.toLatLon(ncFile, dataset, X_VAR, Y_VAR);
//		lats = latLons[0];
//		lons = latLons[1];
//		System.err.println(lats);
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
//		System.err.println(shape[0] + "  " + shape[1] + " " + shape[2]);

		turbData = (float[][][])transArr.copyToNDJavaArray();
//		removeNaNs();
	}

	public void removeNaNs() {
		int []shape = transArr.getShape();
//		System.err.println(shape[0] + "  " + shape[1] + " " + shape[2]);

		for(int x=0; x<shape[0]; x++) {
			for(int y=0; y<shape[1]; y++) {
				for(int z=0; z<shape[2]; z++) {
					if (Float.isNaN(turbData[x][y][z]))
						turbData[x][y][z] = 0.0f;
				}
			}
		}
	}

//	public List<TurbulenceRecord>  getTurbulence(float [] lat, float [] lon) throws IOException, InvalidRangeException {
//		return getTurbulence(lat, lon, null);
//	}
//
//	public List<TurbulenceRecord>  getTurbulence(float [] lat, float [] lon, String [] names) throws IOException, InvalidRangeException {
//		List<TurbulenceRecord> recs = new ArrayList<>();
//		for(int i=0; i<lat.length; i++) {
//			TurbulenceRecord rec = new TurbulenceRecord();
//			rec.time = readTime(); 
//			rec.lat = lat[i];
//			rec.lon = lon[i];
//			rec.turbulence = getProfileT(lat[i], lon[i]);
//			if(names != null)
//				rec.waypointName = names[i];
////			System.err.println(rec.lat + "," + rec.lon + ":" + rec.turbulence[0] );
//			recs.add(rec);
//		}
//
//		return recs;
//	}
//
	public List<TurbulenceRecord>  getTurbulence(FlightPlan plan) throws IOException, InvalidRangeException {
		List<Point> path = getPathIndices(plan.getLats(), plan.getLons());
		List<TurbulenceRecord> recs = new ArrayList<>();
		List<Waypoint> waypoints = plan.waypoints;

		int pointCnt = 0;
		int waypointCnt = 0;
		Integer wayptIdx = wayptIndices.get(waypointCnt);
		System.err.println("** Starting path loop for id, time, numWaypts" + plan.oshFlightId + "," + plan.getTimeStr() + "," + plan.waypoints.size());
		for(Point pt: path) {
			TurbulenceRecord rec = new TurbulenceRecord();
			rec.time = time;
			LatLonPoint ll = proj.projToLatLon(projx[pt.x], projy[pt.y]);
			rec.lat = ll.getLatitude();
			rec.lon = ll.getLongitude();
			rec.turbulence = turbData[pt.x][pt.y];
			if(wayptIdx == pointCnt) {
				Waypoint waypt = plan.waypoints.get(waypointCnt);
				rec.waypointName = waypt.name;
//				System.err.println(waypt.name + ", " + waypt.type);
				if(++waypointCnt < wayptIndices.size())
					wayptIdx = wayptIndices.get(waypointCnt);
			}
//			System.err.println(rec.lat + "," + rec.lon + " : " + rec.turbulence[0] );
			recs.add(rec);
			pointCnt++;
		}

		return recs;
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

	// ECT_NCST_DELTA_GTGTURB_6_5km.201710181800.grb2
	public long computeTime(String filename) throws IOException {
		log.debug("computeTime called for " + filename);
		int dotIdx = filename.indexOf('.');
		if(dotIdx == -1) {
			log.error("Could not compute timestamp from filename: {}.  Using timestamp from time variable", filename);
			return readTime();
		}
		String datestr = filename.substring(dotIdx + 1);
		String yrs = datestr.substring(0,4);
		String mons = datestr.substring(4,6);
		String days = datestr.substring(6,8);
		String hrs = datestr.substring(8,10);
		String mins = datestr.substring(10,12);
		int yr = Integer.parseInt(yrs);
		int mon = Integer.parseInt(mons);
		int day = Integer.parseInt(days);
		int hr = Integer.parseInt(hrs);
		int min = Integer.parseInt(mins);
		//  Surely there is an easier way to get the timestamp... 
		//  partial issue is that ZonedDateTime can't seem to see toEpochMilli() method, 
		//  likely due to clash with joda time dependencies
		LocalDateTime date = LocalDateTime.of(yr, mon, day, hr, min);
		ZonedDateTime gmtDate = ZonedDateTime.of(date, ZoneId.of("UTC"));
		Instant instant = Instant.from(gmtDate);
		long time = instant.toEpochMilli();
		return time/1000;
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

//	public float [] getProfileT(double lat, double lon) throws IOException, InvalidRangeException {
//		//  Convert LatLon coord to spherical mercator proj. (or use gdal to convert entire file to 4326?
//		int [] result = null;
//		int[] idx = gcs.findXYindexFromLatLon(lat, lon, null);
//		if(idx == null || idx[0] == - 1 ||  idx[1] == -1) {
//			throw new IOException("Projection toLatLon failed");
//		}
//		return turbData[idx[0]][idx[1]];
//
//	}

	/**
	 * 
	 * @param lat - array of wayPoint latitudes
	 * @param lon - array of wayPoint longitudes
	 * @return  a List of x/y indices of all points between and including first lat/lon to last lat/lon
	 * @throws IOException
	 */
	public List<Point> getPathIndices(float [] lat, float [] lon) throws IOException {
		Point [] pts = new Point [lat.length];
		
		//  Translate the input data into XY indexes
		for(int i=0; i<lat.length; i++) {
			int[] idx = gcs.findXYindexFromLatLon(lat[i], lon[i], null);
			if(idx == null || idx[0] == - 1 ||  idx[1] == -1) {
				throw new IOException("Projection fromLatLon failed. Point in path is off the available grid: " + lat[i] + "," + lon[i]);
			}
			pts[i] = new Point(idx[0], idx[1]);
//			System.err.println(" *** " + pts[i].x + "," + pts[i].y + " : " + lat[i] + "," + lon[i]);
		}

		//  Build new list between each point
		List<Point> path = new ArrayList<>();
		Point prevPt = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
		int cnt = 0;
		for(int i=0; i<pts.length-1; i++) {
			Point waypt = new Point(pts[i].x, pts[i].y);
			wayptIndices.add(cnt);
//			if(!prevPt.equals(waypt)) {
				path.add(waypt);  // add the actual point
				cnt++;
//			}
			//  distance
			double d = Math.sqrt( Math.pow((double)(pts[i+1].x - pts[i].x), 2.0) + Math.pow((double)(pts[i+1].y - pts[i].y), 2.0) );
//			System.err.println("\t\t" + pts[i].x + "," + pts[i].y + " - " + pts[i+1].x + "," + pts[i+1].y + ";   disatnce = " + d);
			// deltas
			double  delx = Math.abs(pts[i+1].x - pts[i].x);
			double  dely = Math.abs(pts[i+1].y - pts[i].y);
			double idx = 1.0;
			int ix = Integer.MIN_VALUE, iy = Integer.MIN_VALUE;
			int sanityCnt = 0, sanity = 1000;  // *should* never happen, but let's ensure no infinite loop
			while (idx<d && sanityCnt++ < sanity) {
				if(sanityCnt > sanity)
					throw new IOException("TurbulenceReader went off the tracks tryign to generate full path");
				
				double t = idx/d;
				double x = (1.0 - t) * pts[i].x + t*pts[i+1].x;
				double y = (1.0 - t) * pts[i].y + t*pts[i+1].y;
				ix = (int)Math.round(x);
				iy = (int)Math.round(y);
//				System.err.println(ix + "," + iy + " .. " + x + "," + y);
				Point p = new Point(ix, iy);
				if(!p.equals(prevPt)) {
					path.add(p);
					cnt++;
				}
				prevPt = p;
				idx += 1.0;  //  what should increment be?
			}
		}
		//  Add last waypointIndex
		Point waypt = new Point(pts[pts.length - 1].x, pts[pts.length - 1].y);
		wayptIndices.add(cnt);
		// And last point if it is different
//		if(!prevPt.equals(waypt)) {
			path.add(waypt);  // add the actual point
			cnt++;
//		}
		
//		for(Point p: path)
//			System.err.println(p);
		return path;
	}

	public static  void testFill()  throws Exception {
		// TODO Auto-generated method stub
		TurbulenceReaderOrig reader = new TurbulenceReaderOrig("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710082130.grb2");
		//		reader.ingestFullFile();
		//		reader.removeNaNs();
//		double [] lat = {30.0, 32.0, 34.0, 36.0, 38.0, 40.0};
//		double [] lon = {-95., -95., -95., -95., -95., -95.};

//				double [] lat = {30.0, 30.0, 30.0, 30.0, 30.0, 30.0};
//				double [] lon = {-90., -92., -94., -96., -98., -100.};
		//
				double [] lat = {30.0, 32.0, 34.0, 36.0, 38.0, 40.0};
				double [] lon = {-90., -92., -94., -96., -98., -100.};

//		double [] lat = {30.0, 30.001, 30.002, 30.003, 30.004, 30.005};
//		double [] lon = {-90.0, -90.001, -90.002, -90.003, -90.004, -90.005};

//		List<Point> path = reader.getPathIndices(lat, lon);
	}
	
	public static void main(String[] args) throws Exception {
		TurbulenceReaderOrig reader = new TurbulenceReaderOrig("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710191915.grb2");
		reader.ingestFullFile();
		FlightAwareApi api = new FlightAwareApi();
		FlightPlan plan = api.getFlightPlan("DAL2520-1508217988-airline-0410");
//		FlightPlan plan = FlightPlan.getSamplePlan();

		List<TurbulenceRecord> recs = reader.getTurbulence(plan);
		reader.dumpInfo();
	}
	
	
	public static void mainWaypointsOnly(String[] args) throws Exception {
		TurbulenceReaderOrig reader = new TurbulenceReaderOrig("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710082130.grb2");
		reader.dumpInfo();
		FlightAwareApi api = new FlightAwareApi();
		FlightPlan plan = api.getFlightPlan("DAL1487-1506921959-airline-0651");

				reader.dumpInfo();
//		List<TurbulenceRecord> recs = reader.getTurbulence(plan.getLats(), plan.getLons());
//		for(TurbulenceRecord r: recs)
//			System.err.println(r);
//
		//		for(int i=0;i<100;i++ ) {
		//			System.err.println(i);
		//			double lat = 30. + Math.random() * 10;
		//			double lon = -90. + Math.random() * 10;
		//			reader.getProfile(lat, lon);
		//		}
	}
}
