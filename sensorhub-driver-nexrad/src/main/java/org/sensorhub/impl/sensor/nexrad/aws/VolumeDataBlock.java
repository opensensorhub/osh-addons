package org.sensorhub.impl.sensor.nexrad.aws;

/**
 * <p>Title: VolumeDataBlock.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 16, 2016
 */
public class VolumeDataBlock {
	String dataName;  // RVOL always
	short blockSize; // bytes
	int majorVersionNum;
	int minorVersionNum;
	float latitude;
	float longitude;
	short siteHeightAboveSeaLevelMeters;
	short feedhornHeightAboveGroundMeters;
	float calibrationConstant;
	float transmitterPowerHorizontalKw;
	float transmitterPowerVerticalKw;
	float zdrCalibaration;
	float initialDifferentialPhase;
	short volumeCoveragePattern;
	short processingStatus;
}
