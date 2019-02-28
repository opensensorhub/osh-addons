package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.nexrad.NexradConfig;
import org.sensorhub.impl.sensor.nexrad.NexradSensor;
import org.sensorhub.impl.sensor.nexrad.RadialProvider;
import org.sensorhub.impl.sensor.nexrad.aws.LdmLevel2Reader;
import org.sensorhub.impl.sensor.nexrad.aws.LdmRadial;
import org.sensorhub.impl.sensor.nexrad.aws.NexradSqsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: RealtimeRadialProvider.java</p>
 * <p>Description: </p>
 *
 * @author tcook
 * @date Sep 20, 2016
 */
public class RealtimeRadialProvider implements RadialProvider {
	NexradSensor sensor;
	static final Logger logger = LoggerFactory.getLogger(RealtimeRadialProvider.class);

	ChunkQueueManager chunkQueueManager;
	
	public RealtimeRadialProvider(NexradSensor sensor, ChunkQueueManager chunkManager) throws SensorHubException {
		this.sensor = sensor;
		this.chunkQueueManager = chunkManager;
	}


	/* (non-Javadoc)
	 * @see org.sensorhub.impl.sensor.nexrad.RadialProvider#getNextRadial()
	 */
	@Override
	public LdmRadial getNextRadial() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<LdmRadial> getNextRadials() throws IOException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.sensorhub.impl.sensor.nexrad.RadialProvider#getNextRadials()
	 */
	@Override
	public List<LdmRadial> getNextRadials(String site) throws IOException {
		// This won't work for dynamically adding/removing sites- Need event interface
		ChunkPathQueue chunkQueue = chunkQueueManager.getChunkQueue(site);
		try {
			Path p = chunkQueue.nextFile();
			logger.debug("Reading File {}" , p.toString());
			LdmLevel2Reader reader = new LdmLevel2Reader();
			List<LdmRadial> radials = reader.read(p.toFile());
//			List<LdmRadial> radials = new ArrayList<>();
			return radials;
		} catch (IOException e) {
			e.printStackTrace(System.err);
			logger.error(e.getMessage());
			return null;
		}
	}

}
