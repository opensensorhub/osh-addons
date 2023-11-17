package org.sensorhub.impl.sensor.nexrad.aws;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Title: Radial.java
 * </p>
 * <p>
 * Description:
 * </p>
 *
 * @author T
 * @date Apr 6, 2016
 */
public class Radial {
	public DataHeader dataHeader;
	public VolumeDataBlock volumeDataBlock;
//	public long timeMsUtc;  // compute using daysSince70 and msSince midnight from dataHeader, but I am not really using it
	public Map<String, MomentDataBlock> momentData = new HashMap<>();

	public String toCsvString() {
		DecimalFormat df = new DecimalFormat("00");
		StringBuilder b = new StringBuilder();
		long utcTimeMs = getTime();
		Instant inst = Instant.ofEpochMilli(utcTimeMs);
		b.append("Radial, " + dataHeader.siteId + ", " + utcTimeMs + ", " + inst + ", ");
		b.append( df.format(dataHeader.elevationNum) +  ", " + dataHeader.elevationAngle + ", " + dataHeader.azimuthAngle);
		return b.toString();
	}
		
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		long utcTimeMs = getTime();
		Instant inst = Instant.ofEpochMilli(utcTimeMs);
		b.append("\n\tutcTimeMs: " + utcTimeMs + "," + inst + "\n");
		b.append("\televationNum, angke: " + dataHeader.elevationNum +  ", " + dataHeader.elevationAngle + "\n");
		b.append("\tazimuthAngle: " + dataHeader.azimuthAngle + "\n");

//		MomentDataBlock refMomentData = momentData.get("REF");
//		MomentDataBlock velMomentData = momentData.get("VEL");
//		MomentDataBlock swMomentData = momentData.get("SW ");
//
//		if (refMomentData == null) {
//			b.append("\tReflData null" + "\n");
//		} else {
//			b.append("\trangeToCenterOfFirstRefGate: " + refMomentData.rangeToCenterOfFirstGate + "\n");
//			b.append("\trefGateSize: " + refMomentData.rangeSampleInterval + "\n");
//			b.append("\tnumRefGates: " + refMomentData.numGates + "\n");
//		}
//		if (velMomentData == null) {
//			b.append("\tVelData null" + "\n");
//		} else {
//			b.append("\trangeToCenterOfFirstVelGate: " + velMomentData.rangeToCenterOfFirstGate + "\n");
//			b.append("\tvelGateSize: " + velMomentData.rangeSampleInterval + "\n");
//			b.append("\tnumVelGates: " + velMomentData.numGates + "\n");
//		}
//		if (swMomentData == null) {
//			b.append("\tSwData null" + "\n");
//		} else {
//			b.append("\trangeToCenterOfFirstSwGate: " + swMomentData.rangeToCenterOfFirstGate + "\n");
//			b.append("\tSwGateSize: " + swMomentData.rangeSampleInterval + "\n");
//			b.append("\tnumSwGates: " + swMomentData.numGates + "\n");
//		}

		return b.toString();
	}

	public long getTime() {
		long days = dataHeader.daysSince1970;
		long ms = dataHeader.msSinceMidnight;
//		double utcTime = (double)(AwsNexradUtil.toJulianTime(days, ms)/1000.);
		long utcTimeMs = AwsNexradUtil.toJulianTime(days, ms);
		Instant inst = Instant.ofEpochMilli(utcTimeMs);

		return utcTimeMs;
	}
}
