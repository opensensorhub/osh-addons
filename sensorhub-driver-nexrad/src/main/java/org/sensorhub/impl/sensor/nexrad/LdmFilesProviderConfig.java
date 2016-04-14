package org.sensorhub.impl.sensor.nexrad;

import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.config.DisplayInfo;

/**
 * <p>Title: LdmFilesProviderConfig.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 29, 2016
 */
public class LdmFilesProviderConfig extends CommConfig {
	
	@DisplayInfo(desc="Folder for real-time incoming Nexrad LDM-formatted chunks")
	public String dataFolder; 

	public LdmFilesProviderConfig()
	{
		this.moduleClass = LdmFilesProviderConfig.class.getCanonicalName();
		System.err.println("LdmProvConf: " + dataFolder);
	}


}
