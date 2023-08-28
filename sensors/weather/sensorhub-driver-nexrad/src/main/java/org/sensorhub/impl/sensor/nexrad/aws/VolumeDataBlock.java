package org.sensorhub.impl.sensor.nexrad.aws;

/**
 * <p>Title: VolumeDataBlock.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 16, 2016
 */
public class VolumeDataBlock {
	public String dataName;  // RVOL always
	public short blockSize; // bytes
	public int majorVersionNum;
	public int minorVersionNum;
	public float latitude;
	public float longitude;
	public short siteHeightAboveSeaLevelMeters;
	public short feedhornHeightAboveGroundMeters;
	public float calibrationConstant;
	public float transmitterPowerHorizontalKw;
	public float transmitterPowerVerticalKw;
	public float zdrCalibaration;
	public float initialDifferentialPhase;
	public short volumeCoveragePattern;
	public short processingStatus;
	public short zdrBias;
}
