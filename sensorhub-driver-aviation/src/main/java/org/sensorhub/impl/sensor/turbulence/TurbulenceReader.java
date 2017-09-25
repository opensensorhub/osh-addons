package org.sensorhub.impl.sensor.turbulence;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.geotiff.GeoTiff;

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
	GridDataset	dataset;
	
	public TurbulenceReader(String path) throws IOException {
		dataset = GridDataset.open(path);
	}
	
	public double [] getProfile(double lat, double lon) {
		//  Convert LatLon coord to spherical mercator proj. (or use gdal to convert entire file to 4326?
		
		//  slice data as appropriate- will need multiple slices to interpolate
		
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
		TurbulenceReader reader = new TurbulenceReader("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201709071815.grb2");
		reader.dumpInfo();
	}
	
	public static void main_tiff(String[] args) throws Exception {
		String path = "C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201709071815.tiff";
//		String path = "C:/Users/tcook/root/sensorHub/geoserver/time_geotiff/MRMS_MergedReflectivityQComposite_00.00_20170726-172841.grib2";
		GeoTiff gt = new GeoTiff(path);
		gt.read();
		gt.showInfo(new PrintWriter(System.err));
	}
	
	public static void main_(String[] args) throws Exception {
//		String path = "C:/Data/sensorhub/delta/edr/DeltaEdr.20170816.2010.nc";
		String path = "C:/Data/sensorhub/delta/ECT_NCST_DELTA_GTGTURB_6_5km.201708042145.grb2";
		NetcdfFile f = NetcdfFile.open(path);
		List<Variable> vars = f.getVariables();
		for(Variable var: vars) {
			System.err.println(var);
		}
	}
}
