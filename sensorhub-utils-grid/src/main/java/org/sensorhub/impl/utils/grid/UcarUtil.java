/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2018 Delta Air Lines, Inc. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.utils.grid;

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
	
	private static Object  readVar(NetcdfFile ncFile, String varName) throws IOException {
		Variable vtime = ncFile.findVariable(varName);
		Array atime = vtime.read();
		float [] alt = (float [])atime.copyTo1DJavaArray();
		return alt;
	}

	
	public static void main(String[] args) throws Exception {
//		NetcdfDataset ds = NetcdfDataset.openDataset("C:/Data/sensorhub/delta/CLDTOP/ECT_NCST_DELTA_CLDTOP_6_5km.201711021840.grb2");
		NetcdfFile ncFile = NetcdfFile.open("C:/Data/sensorhub/delta/CLDTOP/ECT_NCST_DELTA_CLDTOP_6_5km.201711021930.grb2");
		List<String> vnames = getVariableNames(ncFile);
//		readVar(ncFile, "");
//		readVar(ncFile, "altitude_above_msl");
		float [] ctops = (float []) readVar(ncFile, "MergedReflectivityQComposite_altitude_above_msl");
		float min = Float.MAX_VALUE;
		float max = Float.MIN_VALUE;
		for(float f: ctops) {
			if(f < min)  min = f;
			if(f > max)  max = f;
		}
		System.err.println(min + "  " + max);
		for(String s: vnames)
			System.err.println(s);
	}


}

