package org.sensorhub.impl.sensor.mesh;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;

//  TODO: Move to utility package

public class UcarUtil
{
	/**
	 * Convert 2D X/Y to lat/lon for OSH processing
	 * @param x
	 * @param y
	 * @throws IOException
	 */
	public static float[][] toLatLon(NetcdfFile ncFile, GridDataset dataset, String xvar, String yvar) throws IOException {
	    GridCoordSystem gcs =  dataset.getGrids().get(0).getCoordinateSystem();
	    ProjectionImpl proj = gcs.getProjection();
	    Variable vx = ncFile.findVariable(xvar);
	    Variable vy = ncFile.findVariable(yvar);
	    Array ax = vx.read();
	    Array ay = vy.read();
	    float [] projx = (float [] )ax.getStorage();
	    float [] projy =  (float [] )ay.getStorage();
	    float [] lats = new float[projy.length];
	    float [] lons = new float[projx.length];
	    float [][] latLons = {lats, lons}; 
	    for (int j=0; j<projy.length; j++)
	       for (int i=0; i<projx.length; i++){
	    	  LatLonPoint pt = proj.projToLatLon(projx[i], projy[j]);
	    	  lats[j] = (float)pt.getLatitude();
	    	  lons[i] = (float)pt.getLongitude();
//	    	  System.err.println(projx[i] + "," + projy[j] + " ==> " + lon[i] + "," + lat[j]);
	    }        
	    return latLons;
	}

	public static List<String> getVariableNames(NetcdfFile file) {
		List<String> vnames = new ArrayList<>();
		List<Variable> vars = file.getVariables();
		for (Variable v: vars)
			vnames.add(v.getShortName());
		return vnames;
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
	
	public static void main(String[] args) throws Exception {
		NetcdfDataset ds = NetcdfDataset.openDataset("C:/Data/sensorhub/delta/gtgturb/ECT_NCST_DELTA_GTGTURB_6_5km.201710041430.grb2");
		dumpAttributeInfo(ds);
		List<String> vnames = getVariableNames(ds);
		for(String s: vnames)
			System.err.println(s);
	}


}

