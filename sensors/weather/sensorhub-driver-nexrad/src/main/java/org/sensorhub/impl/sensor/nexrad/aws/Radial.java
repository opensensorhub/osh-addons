package org.sensorhub.impl.sensor.nexrad.aws;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: Radisl.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Apr 6, 2016
 */
public class Radial
{	
	public DataHeader dataHeader;
	public VolumeDataBlock volumeDataBlock;
	public long timeMsUtc;  // compute using daysSince70 and msSince midnight from dataHeader, but I am not really using it
	public Map<String, MomentDataBlock> momentData = new HashMap<>();
}
