package org.sensorhub.impl.sensor.nexrad.ucar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.sensor.nexrad.RadialProvider;
import org.sensorhub.impl.sensor.nexrad.VCP;
import org.sensorhub.impl.sensor.nexrad.aws.DataHeader;
import org.sensorhub.impl.sensor.nexrad.aws.LdmRadial;
import org.sensorhub.impl.sensor.nexrad.aws.MomentDataBlock;

import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * <p>Title: UcarLevel2Reader.java </p>
 * <p>Description:  Reads Level II radial data by using Netcdf library from UCAR</p>
 * @author Tony Cook
 * @since Sep 8, 2016
 */

public class UcarLevel2Reader
{
	NetcdfDataset netCdf;
	
	public UcarLevel2Reader() {
	}
	
	public UcarLevel2Reader(File f) throws IOException {
		netCdf = NetcdfDataset.openDataset(f.getCanonicalPath());
	}

	
	private Array getArray(String name) throws IOException {
		Variable dataVar = getVariable(name);
		if(dataVar == null)
			return null;
		return dataVar.read();
	}
	
	private Variable getVariable(String name) throws IOException {
		return netCdf.findVariable(name);
	}
	
	public int [] get1dIntData(String name) throws IOException {
		Array dataArr = getArray(name);
		if(dataArr == null)
			return null;
		int [] data = (int [])dataArr.copyToNDJavaArray();
		return data;
	}
	
	public float [] get1dFloatData(String name) throws IOException {
		Array dataArr = getArray(name);
		if(dataArr == null)
			return null;
		float [] data = (float [])dataArr.copyToNDJavaArray();
		return data;
	}
	
	public int [][] getIntData(String name) throws IOException {
		Array dataArr = getArray(name);
		if(dataArr == null)
			return null;
		int [][] data = (int [][])dataArr.copyToNDJavaArray();
		return data;
	}
	
	public float [][] get2dFloatData(String name) throws IOException {
		Array dataArr = getArray(name);
		if(dataArr == null)
			return null;
		float [][] data = (float [][])dataArr.copyToNDJavaArray();
		return data;
	}

	public float[][][] getFloatData(String name) throws IOException {
		Variable dataVar = getVariable(name);
		if(dataVar == null)
			return null;
		Array dataArr = dataVar.read();
		int [] shape = dataVar.getShape();
		int numSweeps = shape[0];
		int numRadials = shape[1];
		int numGates = shape[2];
		System.err.println("Current numSweeps, numRadials: " + numSweeps + ", " + numRadials + "," + numGates);
		float[][][] data  = (float[][][])dataArr.copyToNDJavaArray();

		return data;
	}	
	
	public short getRangeSampleInterval(float distance[]) {
		Float frange = (distance[distance.length-1] - distance[0]) / (distance.length-1);
		return frange.shortValue();
	}

	//  If hasSplitCuts is true and hiRes also true, use only the 
	//  even-numbered elevations for Reflectivity, and all elevations
	//  for vel and sw
	public List<LdmRadial> read(boolean hiRes) throws IOException {
		VCP vcp = UcarUtil.getVcp(netCdf);
		boolean hasSplitCuts = false;
		if(vcp != null)
			hasSplitCuts = vcp.hasSplitCuts();
			
		float [][][] ref = getFloatData(UcarUtil.REFLECTIVITY + (hiRes ? "_HI" : ""));
		float [][][] vel = getFloatData(UcarUtil.VELOCITY + (hiRes ? "_HI" : ""));
		float [][][] sw = getFloatData(UcarUtil.SPECTRUM_WIDTH + (hiRes ? "_HI" : ""));

		int [][] timeR = getIntData(UcarUtil.TIME + "R" + (hiRes ? "_HI" : "") );
		int [] numRadialsR = get1dIntData(UcarUtil.NUM_RADIALS + "R" + (hiRes ? "_HI" : "") );
		int [] numGatesR = get1dIntData(UcarUtil.NUM_GATES + "R"  + (hiRes ? "_HI" : "") );
		float [][] elevationR = get2dFloatData(UcarUtil.ELEVATION + "R"  + (hiRes ? "_HI" : "") );
		float [][] azimuthR = get2dFloatData(UcarUtil.AZIMUTH + "R" + (hiRes ? "_HI" : "") );
		float [] distanceR = get1dFloatData(UcarUtil.DISTANCE + "R"  + (hiRes ? "_HI" : "") ); // distance of each gate?

		int [][] timeV = getIntData(UcarUtil.TIME + "V" + (hiRes ? "_HI" : "") );
		int [] numRadialsV = get1dIntData(UcarUtil.NUM_RADIALS + "V" + (hiRes ? "_HI" : "") );
		int [] numGatesV = get1dIntData(UcarUtil.NUM_GATES + "V" + (hiRes ? "_HI" : "") );
		float [][] elevationV = get2dFloatData(UcarUtil.ELEVATION + "V" + (hiRes ? "_HI" : "") );
		float [][] azimuthV = get2dFloatData(UcarUtil.AZIMUTH + "V" + (hiRes ? "_HI" : "") );
		float [] distanceV = get1dFloatData(UcarUtil.DISTANCE + "V" + (hiRes ? "_HI" : "") ); // distance of each gate?
		//   appears SW has same az/el/numRadials/etc as Velocity
		
		short daysSince1970 = getDaysSince1970();
		
		List<LdmRadial> radials = new ArrayList<>();
		for(int i=0; i<numRadialsV.length; i++){
			for(int j=0; j<numRadialsR[i]; j++) {
				
				LdmRadial radial = new LdmRadial();
				radial.dataHeader = new DataHeader();
				radial.dataHeader.daysSince1970 = daysSince1970;
				radial.dataHeader.msSinceMidnight = timeR[i][j];
				radial.dataHeader.azimuthAngle = azimuthV[i][j];
				radial.dataHeader.elevationAngle = elevationV[i][j];
				
				MomentDataBlock refBlock = new MomentDataBlock("REF");
				int elevation = (hasSplitCuts && hiRes) ? i*2 : i; 
				refBlock.numGates = (short)numGatesR[elevation];
				refBlock.rangeToCenterOfFirstGate = (short)distanceR[0];
				refBlock.rangeSampleInterval = getRangeSampleInterval(distanceR);
				refBlock.setData(ref[elevation][j]);
				float []  data = refBlock.getData();
				radial.momentData.put(refBlock.blockName, refBlock);

				MomentDataBlock velBlock = new MomentDataBlock("VEL");
				velBlock.numGates = (short)numGatesV[i];
				velBlock.rangeToCenterOfFirstGate = (short)distanceV[0];
				velBlock.rangeSampleInterval = getRangeSampleInterval(distanceV);
				velBlock.setData(vel[i][j]);
//				if(j%100  == 0)  System.err.println(elevationV[i][j]);
				radial.momentData.put(velBlock.blockName, velBlock);

				
				MomentDataBlock swBlock = new MomentDataBlock("SW");
				swBlock.numGates = (short)numGatesV[i];
				swBlock.rangeToCenterOfFirstGate = (short)distanceV[0];
				swBlock.rangeSampleInterval = velBlock.rangeSampleInterval;
				swBlock.setData(sw[i][j]);
				radial.momentData.put(swBlock.blockName, swBlock);
				radials.add(radial);
			}
		}
		return radials;
	}
	
	public List<LdmRadial> read() throws IOException {
		List<LdmRadial> rads = new ArrayList<>();
		if(UcarUtil.hasSuperRes(netCdf)) {
			List<LdmRadial> hiResRads = read(true);
			rads.addAll(hiResRads);
		}
		List<LdmRadial> standardRaesRads = read(false);
		rads.addAll(standardRaesRads);

		return rads;
	}
	
	private short getDaysSince1970() {
		short days = UcarUtil.getDaysSince1970(netCdf);
		return days;
	}
	
	public static void main(String[] args) throws Exception {
		File f = new File("C:/Data/sensorhub/Level2/archive/KBMX/kbmxTest2");
		UcarLevel2Reader reader = new UcarLevel2Reader(f);
		UcarUtil.dumpAttributeInfo(reader.netCdf);
		List<LdmRadial> rads = new ArrayList<>();
		if(UcarUtil.hasSuperRes(reader.netCdf)) {
			List<LdmRadial> hiResRads = reader.read(true);
			rads.addAll(hiResRads);
		}
		List<LdmRadial> standardRaesRads = reader.read(false);
		rads.addAll(standardRaesRads);
		for(LdmRadial rad: rads)
			System.err.println(rad);
		System.err.println("Done");
	}
}
