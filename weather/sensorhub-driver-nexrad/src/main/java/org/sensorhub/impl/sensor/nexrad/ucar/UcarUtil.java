package org.sensorhub.impl.sensor.nexrad.ucar;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.sensorhub.impl.sensor.nexrad.VCP;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.unidata.geoloc.EarthLocation;

public class UcarUtil
{
	// Standard base names for fields we need. will need to verify these do not change 
	public static final String REFLECTIVITY = "Reflectivity";
	public static final String VELOCITY = "RadialVelocity";
	public static final String SPECTRUM_WIDTH = "SpectrumWidth";
	public static final String NUM_RADIALS = "numRadials";
	public static final String NUM_GATES  = "numGates";
	public static final String TIME = "time";
	public static final String ELEVATION  = "elevation";
	public static final String AZIMUTH = "azimuth";
	public static final String DISTANCE = "distance";
	
	public UcarUtil(){
	}

	/**
	 * TODO - allow range limit factor
	 * 
	 * @param radialDataset
	 * @return BoundingBox of radar (based on product- pulled from NetcdfFile globalAtts internally)
	 * @throws IOException 
	 */

	public static Rectangle2D.Double getBounds(NetcdfFile ncFile) {
		List<Attribute> globalAtts = ncFile.getGlobalAttributes();
		double minLat = 0.0, maxLat = 0.0, minLon = 0.0, maxLon = 0.0;
		for(Attribute att: globalAtts) {
			if(att.getShortName().equals("geospatial_lat_min")) {
				minLat = att.getNumericValue().doubleValue();
			} else if(att.getShortName().equals("geospatial_lat_max")) {
				maxLat = att.getNumericValue().doubleValue();
			} else if(att.getShortName().equals("geospatial_lon_min")) {
				minLon = att.getNumericValue().doubleValue();
			} else if(att.getShortName().equals("geospatial_lon_max")) {
				maxLon = att.getNumericValue().doubleValue();
			}
		}
		//  Force correct coord order
		// Is this really a problem from the netCDF files?
		if(minLon > maxLon) {
			double tmp = minLon;
			minLon = maxLon;
			maxLon = tmp;
		}
		
		Rectangle2D.Double bounds = new Rectangle2D.Double(minLon, minLat, maxLon - minLon, maxLat - minLat);
		return bounds;
	}
	
	public static double[] getLocation(RadialDatasetSweep radialDataset) {
		EarthLocation location = radialDataset.getCommonOrigin();
		if(location == null) {
			System.err.println("UCAR_Util.getLocation:  location is null.  Common origin problem?");
			return null;			
		}
		
		return new double [] {location.getLatitude(), location.getLongitude(), location.getAltitude()};
	}
	
	//  
	public static String getTimeCoverageString(String path) throws IOException {
		NetcdfFile ncFile = null;
		try {
			ncFile = NetcdfFile.open(path, 0 , null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		}
		Attribute timeAtt = ncFile.findGlobalAttribute("time_coverage_start");
		String tstr = timeAtt.getStringValue();
		ncFile.close();
		return tstr;
	}
	
	public static long getStartTime(String path) throws IOException {
		String timeStr = getTimeCoverageString(path);
		//return TimeUtil.getTimeMs(timeStr);
		return 0L;
	}
	
	public static void varInfo(String path) throws IOException {
		NetcdfFile file = NetcdfFile.open(path);
		UcarUtil.dumpVariableInfo(file);
	}
	
	public static float [][] getFloatData(String path, String varName)  throws IOException {
		NetcdfFile file = NetcdfFile.open(path);
		
		Variable var = file.findVariable(varName);
		List<ucar.nc2.Dimension> dims = var.getDimensions();
		if(dims.size() !=2) {
			System.err.println("UcarUtil.getFloatData(): Wrong dims (" + dims.size() + ") for requested variable: " + varName);
			return null;
		}
		
		Array arr = var.read();
		float [][] data = (float [][])arr.copyToNDJavaArray();
//		
//		float []minMax = ArrayUtil.minMax(data);
//		System.err.println("Min, max = " + minMax[0] + ", " + minMax[1]);
		
		return data;
	}
	
	public static List<String> getVariableNames(NetcdfFile file) {
		List<String> vnames = new ArrayList<>();
		List<Variable> vars = file.getVariables();
		for (Variable v: vars)
			vnames.add(v.getShortName());
		return vnames;
	}
	
	public static VCP getVcp(NetcdfFile file) {
		Attribute att = file.findGlobalAttribute("VolumeCoveragePattern");
		if (att == null)
			return null;
		Number num = att.getNumericValue();
		return VCP.getVCP(num.intValue());
	}
	
	public static short getDaysSince1970(NetcdfFile file) {
		Attribute dateAtt = file.findGlobalAttribute("base_date");
		if(dateAtt == null)
			return 0; // throw exception since we can't compute time without this attribute?
		String dateStr = dateAtt.getStringValue();
		int yr = Integer.parseInt(dateStr.substring(0,4));
		int month = Integer.parseInt(dateStr.substring(5,7));
		int day = Integer.parseInt(dateStr.substring(8,10));
		DateTime baseDt = new DateTime(yr, month, day, 0, 0, DateTimeZone.forID("UTC"));
		DateTime dt1970 = new DateTime(1970, 1, 1, 0, 0, DateTimeZone.forID("UTC"));
		Integer days = Days.daysBetween(dt1970, baseDt).getDays();
		return days.shortValue();
	}
	
	public static boolean hasSuperRes(NetcdfFile file) throws IOException {
		Variable var = file.findVariable("Reflectivity_HI");
		return (var == null) ? false: true;
	}
	
	public static void dumpAttributeInfo(NetcdfFile file) throws IOException {
		System.err.println("==========   ATTS  =================");
		List<Attribute> atts= file.getGlobalAttributes();
		for(Attribute attTmp:atts) {
			String name = attTmp.getShortName();
			DataType dt = attTmp.getDataType();
			switch(dt) {
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
				Number num = attTmp.getNumericValue();
				System.err.println(name + " = " + num);
				break;
			case STRING:
				String value = attTmp.getStringValue();
				System.err.println(name + " = " + value);
				break;
			default:
				System.err.println(name + " has unknown type" + dt);
				break;
			
			}
		}
	}
	
	public static void dumpVariableInfo(NetcdfFile file) throws IOException {
		List<ucar.nc2.Variable> vars = file.getVariables();
		Attribute att = file.findGlobalAttribute("time_coverage_start");
		System.err.println(att);
		att = file.findGlobalAttribute("time_coverage_end");
		System.err.println(att);
		System.err.println("==========   VARS  =================");

		Array  azimuthArr;
		Array reflArr;
		Array gridXarr;
		Array gridYarr;
		float [][] reflData;
		float [][] azimuthData;
		double [] gridXdata;
		double [] gridYdata;
		for(ucar.nc2.Variable var : vars) {
			System.err.println(var.getShortName());
			System.err.println(var.getDescription());
			List<ucar.nc2.Dimension> dims = var.getDimensions();
			System.err.print("Dims: ");
			for(Dimension dim: dims){
				System.err.print(dim);
			}
			System.err.println("\ntype = " + var.getDataType());
			System.err.println("rank = " + var.getRank());
			System.err.println(var.getShortName());
			if(var.getShortName().startsWith("azim")) {
				azimuthArr = var.read();
				azimuthData = (float [][])azimuthArr.copyToNDJavaArray();
				System.err.println("-------------------");

			}
			if(var.getShortName().equals("BaseReflectivity")) {
				reflArr = var.read();
				reflData = (float [][])reflArr.copyToNDJavaArray();
				System.err.println("-------------------");

			}
			System.err.println("-------------------");
		}
	}
	
	public static void printTimeStrings(String path) throws IOException {
		File dir = new File(path);
		File [] files = dir.listFiles();
		String tstr;
		for(File file: files) {
			tstr = UcarUtil.getTimeCoverageString(file.getAbsolutePath());
			System.err.println(tstr + ": " + file.getAbsolutePath());
		}
	}

	public static void main(String[] args) {
		DateTime baseDt = new DateTime(2016, 9, 16, 0, 0, DateTimeZone.forID("UTC"));
		DateTime dt1970 = new DateTime(2016,9,15, 0, 0, DateTimeZone.forID("UTC"));
		Integer days = Days.daysBetween(dt1970, baseDt).getDays();
		System.err.println(days);

	}
	
	public static void main_(String[] args) throws Exception {
		NetcdfDataset ds = NetcdfDataset.openDataset("C:/Data/sensorhub/Level2/archive/KBMX/kbmxTest2");
		dumpAttributeInfo(ds);
		System.err.println(hasSuperRes(ds));
		List<String> vnames = getVariableNames(ds);
		System.err.println(getDaysSince1970(ds));
//		for(String s: vnames)
//			System.err.println(s);
	}


}

