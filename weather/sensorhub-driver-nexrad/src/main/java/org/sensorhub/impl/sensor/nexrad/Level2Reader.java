package org.sensorhub.impl.sensor.nexrad;



/**
 *<p>Title: Level2Reader.java</p>
 * <p>Description:  Read Level II archive format using UCAR libs into Sweeps and Radials</p>
 * @author Tony Cook
 * @since March 2015
 * 
 *  NOTE:  a lot of the UCAR methods are deprecated. They are based on TypedDataset, which is deprecated.  
 *        But RadialDatasetSweep extends TypedDataset and is not deprecated.  Not worrying about it for now. 
 */
@Deprecated  // Use UcarLevel2Reader
public class Level2Reader {
	
//	public  Sweep readSweep(String source, Level2Product prod) throws IOException{
//		return readSweep(source, prod.toString());
//	}
//
//	public Sweep readSweep(String source, String variableName) throws IOException{
//		//  These used to be class vars- moved to this method since this is the only place they are being used
//		//  If I need to provide more interactive progress feedback, these would need to be class vars again
//		RadialDatasetSweep radialDataset = null;
//		RadialDatasetSweep.RadialVariable radialVar = null;
//		Sweep sweep = new Sweep();
//
//		CancelTask cancelTask = new CancelTask() {
//			public boolean isCancel() {
//				return false;
//			}
//			public void setError(String msg) {
//			}
//			@Override
//			public void setProgress(String msg, int progress) {
//			}
//		};
//		Formatter formatter = new Formatter();
//		
//		try {
//			radialDataset = (RadialDatasetSweep) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.RADIAL, source, cancelTask, formatter);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//		//  Get Radial Variable
////		java.util.List<VariableSimpleIF> vars = radialDataset.getDataVariables();
//		radialVar = (RadialDatasetSweep.RadialVariable) radialDataset.getDataVariable(variableName);
//		if(radialVar == null) {
//			String vName = variableName + "_HI"; // Some files have _HI appended to the variable name
//			radialVar = (RadialDatasetSweep.RadialVariable) radialDataset.getDataVariable(vName);
//			if (radialVar == null) {
//				throw new IOException("The variable '"+ variableName+"' was not found.");
//			}
//		}
//		RadialDatasetSweep.Sweep rdsSweep = radialVar.getSweep(0);
//		int numRadials = rdsSweep.getRadialNumber();
//		for(int i=0; i<numRadials; i++) {
//			Radial r = new Radial();
//			r.azimuth = rdsSweep.getAzimuth(i);
//			r.elevation = rdsSweep.getElevation(i);
//			sweep.rangeToFirstGate = rdsSweep.getRangeToFirstGate();
//			sweep.gateSpacing = rdsSweep.getGateSize();
////			r.radialStartTime = rdsSweep.getTime(i);
//			sweep.numGates =  rdsSweep.getGateNumber(); 
//			r.dataFloat = rdsSweep.readData(i);
//			sweep.addRadial(r);
//		}
//		
//		double rangeFoldedValue = Double.NEGATIVE_INFINITY;
//		Date dateTime = radialDataset.getStartDate();
//		long startTimeMs = dateTime.getTime();
//		sweep.setStartTimeMs(startTimeMs);
//		
//		return sweep;
//	}

//	public static void main(String[] args) throws IOException {
////		String testFile = "C:/Users/tcook/root/sensorHub/doppler/ldm/20160328-200427-031-I";
//		String testFile = "C:/Users/tcook/root/sensorHub/doppler/ldm/20160328-200427-031-I";
//		Level2Reader reader = new Level2Reader();
//		Sweep sweep = reader.readSweep(testFile, Level2Product.REFLECTIVITY);
////		Sweep sweep = reader.readSweep(testFile, Level2Product.VELOCITY);
//		System.err.println(sweep);
//	}
}
