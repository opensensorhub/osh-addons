package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.nexrad.NexradSensor;
import org.sensorhub.impl.sensor.nexrad.RadialProvider;
import org.sensorhub.impl.sensor.nexrad.aws.Level2Reader;
import org.sensorhub.impl.sensor.nexrad.aws.Radial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.model.S3Object;

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
	public Radial getNextRadial() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Radial> getNextRadials() throws IOException {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.sensorhub.impl.sensor.nexrad.RadialProvider#getNextRadials()
	 */
//	@Override
	public List<Radial> getNextRadialsFile(String site) throws IOException {
		// This won't work for dynamically adding/removing sites- Need event interface
		ChunkPathQueue chunkQueue = chunkQueueManager.getChunkQueue(site);
		try {
			Path p = chunkQueue.nextFile();
			logger.debug("Reading File {}" , p.toString());
			Level2Reader reader = new Level2Reader();
			List<Radial> radials = reader.read(p.toFile());
//			List<LdmRadial> radials = new ArrayList<>();
			return radials;
		} catch (IOException e) {
			e.printStackTrace(System.err);
			logger.error(e.getMessage());
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.sensorhub.impl.sensor.nexrad.RadialProvider#getNextRadials()
	 */
	@Override
	public List<Radial> getNextRadials(String site) throws IOException {
		// This won't work for dynamically adding/removing sites- Need event interface
		ChunkPathQueue chunkQueue = chunkQueueManager.getChunkQueue(site);
		try {
			S3Object object = chunkQueue.nextObject();
			//S3ObjectInputStream s3is = object.getObjectContent();
			BufferedInputStream s3is = new BufferedInputStream(object.getObjectContent());
			logger.debug("Reading object {}" , object.getKey());
			Level2Reader reader = new Level2Reader();
			List<Radial> radials = reader.read(s3is, object.getKey());
			
//			List<LdmRadial> radials = new ArrayList<>();
			return radials;
		} catch (IOException e) {
			e.printStackTrace(System.err);
			logger.error(e.getMessage());
			return null;
		}
	}

}
