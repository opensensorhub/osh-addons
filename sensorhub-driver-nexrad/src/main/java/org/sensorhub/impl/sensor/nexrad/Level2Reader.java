package org.sensorhub.impl.sensor.nexrad;

import java.io.IOException;
import java.util.Date;
import java.util.Formatter;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.util.CancelTask;

public class Level2Reader {

	public synchronized Sweep readSweep(String source, String variableName) throws IOException{
		//  These used to be class vars- moved to this method since this is the only place they are being used
		//  If I need to provide more interactive progress feedback, these would need to be class vars again
		RadialDatasetSweep radialDataset = null;
		RadialDatasetSweep.RadialVariable radialVar = null;
		Sweep sweep = new Sweep();

		CancelTask cancelTask = new CancelTask() {
			public boolean isCancel() {
				return false;
			}
			public void setError(String msg) {
			}
			@Override
			public void setProgress(String msg, int progress) {
			}
		};
		Formatter formatter = new Formatter();
		
		//  NOTE- deprecated method- not sure why, but when I get time, investigate
		try {
			radialDataset = (RadialDatasetSweep) FeatureDatasetFactoryManager.open(ucar.nc2.constants.FeatureType.RADIAL, source, cancelTask, formatter);
		} catch (IOException e) {
			e.printStackTrace();
		}

		//  Get Radial Variable
		 java.util.List<VariableSimpleIF> vars = radialDataset.getDataVariables();
		radialVar = (RadialDatasetSweep.RadialVariable) radialDataset.getDataVariable(variableName);
		if(radialVar == null) {
			String vName = variableName + "_HI"; // Some files have _HI appended to the variable name
			radialVar = (RadialDatasetSweep.RadialVariable) radialDataset.getDataVariable(vName);
			if (radialVar == null) {
				throw new IOException("The variable '"+ variableName+"' was not found.");
			}
		}

		double rangeFoldedValue = Double.NEGATIVE_INFINITY;
		Date dateTime = radialDataset.getStartDate();
		long startTimeMs = dateTime.getTime();
		//			sweep.setStartTimeMs(startTimeMs);
		
		return sweep;
	}

	public static void main(String[] args) throws IOException {
		String testFile = "C:/Data/sensorhub/Level2/HTX/KHTX20110427_205716_V03";
//		String variableName = "Reflectivity";
		String variableName = "RadialVelocity";
//		String variableName = "SpectrumWidth";
		Level2Reader reader = new Level2Reader();
		reader.readSweep(testFile, variableName);
	}
}
