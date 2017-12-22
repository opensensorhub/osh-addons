package org.sensorhub.impl.sensor.flightAware;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.sensor.flightAware.FlightPlan.Waypoint;
import org.sensorhub.impl.sensor.flightAware.geom.GribUtil;
import org.sensorhub.impl.sensor.flightAware.geom.LatLonAlt;
import org.sensorhub.impl.sensor.mesh.EarthcastUtil;
import org.sensorhub.impl.sensor.mesh.UcarUtil;
import org.sensorhub.impl.sensor.navDb.NavDbEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.algorithm.locate.IndexedPointInAreaLocator;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Location;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

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
//	private static final String TURB_VAR = "Turbulence_Potential_Forecast_Index_altitude_above_msl";
	private static final String TURB_VAR = "Clear_air_turbulence_CAT_altitude_above_msl";
	private static final String TIME_VAR = "time";
	private static final String X_VAR = "x";
	private static final String Y_VAR = "y";

	GridDataset	dataset;
	Variable crsVar;
	private NetcdfFile ncFile;
	private ProjectionImpl proj;
	GridCoordSystem gridCoordSystem;
	private Variable turbVar;
	private float [][][] turbData;
	private Array transArr;
	float [] projx;
	float [] projy;
	long time = 0L;

	static final Logger log = LoggerFactory.getLogger(TurbulenceReader.class);


	public TurbulenceReader(String path) throws IOException {
		initFile(path);
	}

	public TurbulenceReader() {
	}

	/**
	 * Load all the data into memory that we will need to perform
	 * on the fly computation of Turbulence profiles
	 * 
	 * @param path 
	 * @throws IOException
	 */
	public void initFile(String path) throws IOException {
		dataset = GridDataset.open(path);
		ncFile = dataset.getNetcdfFile();
		List<String> vn = UcarUtil.getVariableNames(ncFile);
		for(String n: vn)  System.err.println(n);
		//  Get projection info
		gridCoordSystem = dataset.getGrids().get(0).getCoordinateSystem();
		proj = gridCoordSystem.getProjection();
		//	Compute time based on filename- this is the preferred method
		//  as the time in the time variable may not be accurate
		Path p = Paths.get(path);
		time = EarthcastUtil.computeTime(p.getFileName().toString());

		//  load XY variable info- we'll need this to do any forward/reverse projection
		Variable vx = ncFile.findVariable(X_VAR);
		Variable vy = ncFile.findVariable(Y_VAR);
		Array ax = vx.read();
		Array ay = vy.read();
		projx = (float [] )ax.getStorage();
		projy =  (float [] )ay.getStorage();

		//  and load TurbulenceData
		loadTurbulenceData();

		// remove NaNs here for now.  JSON does not like them
		removeNaNs();
	}

	private Variable findTurbulence() {
		List<Variable> vars = ncFile.getVariables();
		for (Variable v: vars)
			if(v.getShortName().toLowerCase().contains("turbulence"))
				return v;
		return null;
	}
	
	private void loadTurbulenceData() throws IOException {
//		turbVar = ncFile.findVariable(TURB_VAR);
		turbVar = findTurbulence();
		if (turbVar == null)
			throw new IOException("TurbulenceReader could not find turbulence variable: " + TURB_VAR);

		// Transpose array for efficient access of vertical profiles
		Array turbArr4d = turbVar.read();
		Array turbArr3d = turbArr4d.reduce();
		transArr = turbArr3d.transpose(0, 2);
		int []shape = transArr.getShape();

		turbData = (float[][][])transArr.copyToNDJavaArray();
	}

	private void removeNaNs() {
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

	private int findWaypoint(Coordinate [] projWaypt, Coordinate point) {
		for(int i=0; i<projWaypt.length; i++) {
			if(point.equals2D(projWaypt[i]))
				return i;
		}
		return -1;
	}

	public List<TurbulenceRecord>  getTurbulence(FlightPlan plan) throws IOException, InvalidRangeException {
		Coordinate [] waypointsXY = GribUtil.getProjectedWaypoints(gridCoordSystem, plan.getLats(), plan.getLons());
		Coordinate [] path = GribUtil.getPathIndices(gridCoordSystem, plan.getLats(), plan.getLons());
		List<TurbulenceRecord> recs = new ArrayList<>();
		List<Waypoint> waypoints = plan.waypoints;

		for(Coordinate pathPt: path) {
			TurbulenceRecord rec = new TurbulenceRecord();
			rec.time = time;
			LatLonPoint ll = proj.projToLatLon(projx[(int)pathPt.x], projy[(int)pathPt.y]);
			rec.lat = ll.getLatitude();
			rec.lon = ll.getLongitude();
			rec.turbulence = turbData[(int)pathPt.x][(int)pathPt.y];
			int waypointIndexSafe = findWaypoint(waypointsXY, pathPt);
			if(waypointIndexSafe >= 0) {
				if(waypointIndexSafe < waypoints.size()) {
					Waypoint waypt = plan.waypoints.get(waypointIndexSafe);
					rec.waypointName = waypt.name;
					//					System.err.println("\twaypt: " + waypointIndexSafe + "," + waypt.name);
				} else {
					log.debug("Waypoint index out of range: idx, id" , waypointIndexSafe, plan.oshFlightId);
				}
			}
			recs.add(rec);
		}

		return recs;
	}

	private float []  readAlt() throws IOException {
		Variable vtime = ncFile.findVariable(ALT_VAR);
		Array atime = vtime.read();
		float [] alt = (float [])atime.copyTo1DJavaArray();
		return alt;
	}

	//	brTop:29.83344563844328, -100.0, 12000.0
	//	blTop:30.16655436155672, -100.0, 12000.0
	//	flTop:30.135117172238807, -97.11580280188991, 12000.0
	//	frTop:29.802008449125367, -97.11580280188991, 12000.0
	//	brBottom:29.83344563844328, -100.0, 8000.0
	//	blBottom:30.16655436155672, -100.0, 8000.0
	//	flBottom:30.135117172238807, -97.11580280188991, 8000.0
	//	frBottom:29.802008449125367, -97.11580280188991, 8000.0
	public LawBox getLawBox(FlightObject pos) throws IOException {
		return getLawBox(pos, null, null);
	}

		
	/**
	 * 
	 * @param pos
	 * @param origin (can be null)
	 * @param destination (can be null)
	 * @return  LawBox object based on airports
	 * @throws IOException
	 */
	public LawBox getLawBox(FlightObject pos, NavDbEntry origin, NavDbEntry destination) throws IOException {
		//		LawBoxGeometry geom = new LawBoxGeometry(pos);
		LawBox lawBox = new LawBox(pos);
		lawBox.computeBox(origin, destination);
//		System.err.println(pos);
//		System.err.println(lawBox);
		
		// to XY for 4 corners
		Coordinate br = GribUtil.latLonToXY(gridCoordSystem, lawBox.brBottomLla);
		Coordinate bl = GribUtil.latLonToXY(gridCoordSystem, lawBox.blBottomLla);
		Coordinate fr = GribUtil.latLonToXY(gridCoordSystem, lawBox.frBottomLla);
		Coordinate fl = GribUtil.latLonToXY(gridCoordSystem, lawBox.flBottomLla);
//		System.err.println(br);
//		System.err.println(bl);
//		System.err.println(fr);
//		System.err.println(fl);

		//  Check every pixel that could possibly be in the polygon defined by the four cornesr
		PrecisionModel pm = new PrecisionModel(1.0);
		GeometryFactory fac = new GeometryFactory(pm);
		Coordinate [] corners = new Coordinate[] {br, fr, fl, bl, br};
		Polygon poly = fac.createPolygon(corners);
		int minx = (int)GribUtil.min(corners, Coordinate.X);
		int miny = (int)GribUtil.min(corners, Coordinate.Y);
		int maxx = (int)GribUtil.max(corners, Coordinate.X);
		int maxy = (int)GribUtil.max(corners, Coordinate.Y);
//		System.err.println(minx + "," + miny + " ==> " + maxx + "," + maxy);
		int bottom = getLevel((int)lawBox.brBottomLla.alt);
		int top = getLevel((int)lawBox.brTopLla.alt);

		// 
		lawBox.maxTurb = 0.f;
		Coordinate maxCoord;
		IndexedPointInAreaLocator ipl = new IndexedPointInAreaLocator(poly);
		for(int y = miny; y<=maxy; y++) {
			for(int x = minx; x<=maxx; x++) {
				Coordinate c = new Coordinate(x,y);
				Point p = fac.createPoint(c);
				int loc = ipl.locate(c);
//				System.err.println(c.x + "," + c.y + " : " + l);
				if(!(loc == Location.BOUNDARY || loc == Location.INTERIOR))
					continue;
				for(int z = bottom; z <= top; z++) {
//					System.err.println(c.x + "," + c.y + "," + z + " : " + turbData[(int)c.x][(int)c.y][z]);
					if(turbData[(int)c.x][(int)c.y][z] > lawBox.maxTurb) {
						lawBox.maxTurb = turbData[(int)c.x][(int)c.y][z];
						lawBox.maxCoordXYZ = new Coordinate((int)c.x, (int)c.y, (int)z);
//						System.err.println("new max: " + lawBox.maxTurb + " @ " + lawBox.maxCoordXYZ);
					}
				}

			}			
		}
		LatLonPoint ll = gridCoordSystem.getLatLon((int)lawBox.maxCoordXYZ.x, (int)lawBox.maxCoordXYZ.y);
		lawBox.maxCoordLla = new LatLonAlt(ll.getLatitude(), ll.getLongitude(), getAltitude((int)lawBox.maxCoordXYZ.z));
//		System.err.println("Final maxTurb, maxLoc: " + lawBox.maxTurb + "," + lawBox.maxCoordLla);
		return lawBox;
	}

	//  Alt is stored in meters natively
	//  level 0 = 0 m
	//  level 1 = 30 m
	//  level 2 = ... = 1 kft
	int getLevel(int altFeet) {
		if (altFeet < 97)  return 0;
		if (altFeet < 997)  return 1;
		return (altFeet/1000) + 1;
	}

	int getAltitude(int level) {
		// take from actual alt variable, but for now, it is fixed
		if(level == 0)  return 0;
		if(level == 1)  return 100;
		return (level - 1) * 1000;
	}

	// ECT_NCST_DELTA_GTGTURB_6_5km.201710181800.grb2
	public GridCoordSystem getGridCoordSystem() {
		return gridCoordSystem;
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

	public static  void testFill()  throws Exception {
		// TODO Auto-generated method stub
		TurbulenceReader reader = new TurbulenceReader("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710082130.grb2");
		double [] lat = {30.0, 32.0, 34.0, 36.0, 38.0, 40.0};
		double [] lon = {-90., -92., -94., -96., -98., -100.};
	}

	public static void testProf(String[] args) throws Exception {
		TurbulenceReader reader = new TurbulenceReader("C:/home/tcook/osh/mesh/data/GTGTURB/ECT_NCST_DELTA_GTGTURB_6_5km.201710251815.grb2");
		reader.readAlt();
		//		reader.loadTurbulenceData();
		FlightAwareApi api = new FlightAwareApi();
		FlightPlan plan = api.getFlightPlan("DAL2520-1508217988-airline-0410");
		//		FlightPlan plan = FlightPlan.getSamplePlan();

		List<TurbulenceRecord> recs = reader.getTurbulence(plan);
		for(TurbulenceRecord rec:recs) {
			System.err.println(rec.lat + "," + rec.lon + "," + rec.waypointName);
		}
		reader.dumpInfo();
	}

	public static void testGeom(String[] args) throws Exception {
		Coordinate [] carr = new Coordinate[] {
				new Coordinate(0,0,0),
				new Coordinate(10,0,0),
				new Coordinate(10,10,0),
				new Coordinate(0,10,0),
				new Coordinate(0,0,0)
		};
		PrecisionModel pm = new PrecisionModel(1.0);
		GeometryFactory fac = new GeometryFactory(pm);
		Polygon poly = fac.createPolygon(carr);
	
		IndexedPointInAreaLocator ipl = new IndexedPointInAreaLocator(poly);
		System.err.println(Location.BOUNDARY + " " + Location.INTERIOR + " " + Location.EXTERIOR + " " + Location.NONE);
		for (int i = -2; i<=12; i++) {
			for (int j = -2; j<=12; j++) {
				Point p = fac.createPoint(new Coordinate(j,i));
				boolean contains = poly.contains(p);
				boolean within = p.within(poly);
				int loc  = ipl.locate(new Coordinate(j,i));
				if(loc == Location.BOUNDARY || loc == Location.INTERIOR)
					System.err.println( j + "," + i + " : " + contains + " : " + within + " : " + loc);
			}
		}
	}


	public static void main(String[] args) throws Exception {
		//		double lat = 25.;
		//		double lon = -100.;
		double lat = 45.0;
		double lon = -75.;
		double alt = 10_000.;
		double groundSpeed = 300.;
		double verticalRate = -8000;
		double heading = 225.;

		LawBoxGeometry lbGeom = new LawBoxGeometry(lat, lon, alt, groundSpeed, verticalRate, heading);
		TurbulenceReader reader = new TurbulenceReader("C:/home/tcook/osh/mesh/data/ECT_NCST_DELTA_GTGTURB_6_5km.201712190815.grb2");
		FlightObject obj = new FlightObject();
		obj.clock = "" + System.currentTimeMillis()/1000;
		obj.lat="32.43";
		obj.lon="-88.89";
		obj.alt = "32000";
		obj.gs = "350";
		obj.heading = "235";
		obj.verticalChange = 2000.;
		
		LawBox box = reader.getLawBox(obj);
		System.err.println(box);

	}

}
