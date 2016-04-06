package org.sensorhub.impl.sensor.nexrad;

import java.io.InputStream;
import java.nio.file.WatchService;
import java.util.concurrent.PriorityBlockingQueue;

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
	public String dataFolder; // = "C:/Data/sensorhub/Level2/test/KARX";


	public LdmFilesProviderConfig()
	{
		this.moduleClass = LdmFilesProviderConfig.class.getCanonicalName();
		System.err.println("LdmProvConf: " + dataFolder);
	}


}
