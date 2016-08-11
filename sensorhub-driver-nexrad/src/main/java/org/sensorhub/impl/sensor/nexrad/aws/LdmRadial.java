package org.sensorhub.impl.sensor.nexrad.aws;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: LdmRadisl.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Apr 6, 2016
 */
public class LdmRadial
{	
	public DataHeader dataHeader;
	public VolumeDataBlock volumeDataBlock;
	public Map<String, MomentDataBlock> momentData = new HashMap<>();

}
