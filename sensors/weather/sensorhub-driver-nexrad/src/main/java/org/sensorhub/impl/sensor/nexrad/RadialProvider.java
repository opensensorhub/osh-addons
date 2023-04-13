package org.sensorhub.impl.sensor.nexrad;

import java.io.IOException;
import java.util.List;

import org.sensorhub.impl.sensor.nexrad.aws.Radial;

/**
 * <p>Title: RadialProvider.java</p>
 * <p>Description: </p>
 *
 * @author tcook
 * @date Sep 14, 2016
 * 
 * Probably won't go this route
 * 
 */

public interface RadialProvider {
	// Ingest data without retaining files locally
	public List<Radial>  getNextRadials(String site) throws IOException;
	// Ingest data and save files locally
	public List<Radial>  getNextRadialsFile(String site) throws IOException;
}
