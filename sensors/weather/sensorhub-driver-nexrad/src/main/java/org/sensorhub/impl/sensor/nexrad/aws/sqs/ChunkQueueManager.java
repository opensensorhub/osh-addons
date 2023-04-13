package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.nexrad.NexradConfig;
import org.sensorhub.impl.sensor.nexrad.NexradSensor;
import org.sensorhub.impl.sensor.nexrad.NexradSite;

import com.amazonaws.services.s3.AmazonS3Client;

public class ChunkQueueManager {
	Map<String, ChunkPathQueue> queueMap;

	public ChunkQueueManager(NexradSensor sensor) throws SensorHubException {
		initQueueMap(sensor);
	}
	
	public void initQueueMap(NexradSensor sensor) throws SensorHubException {
		try {
			queueMap = new HashMap<>();
			NexradConfig config = sensor.getConfiguration();
			Path rootPath = Paths.get(config.rootFolder); 
			if(!Files.isDirectory(rootPath))
				throw new SensorHubException("Configured rootFolder does not exist or is not a directory" + config.rootFolder);
			
//			for(String site: config.siteIds) {
			for(NexradSite site: sensor.getEnabledSites()) {
				ChunkPathQueue queue = new ChunkPathQueue(Paths.get(config.rootFolder), site.id);
				queueMap.put(site.id, queue);
//				nexradSqsService.setChunkQueue(queue);   
//				queue.setS3client(sensor.getS3client());  

			}
		} catch (IOException e) {
			throw new SensorHubException(e.getMessage(), e);
		}
	}
	
	public void addChunkPath(String site, String path) {
		ChunkPathQueue queue = getChunkQueue(site);
		queue.add(path);
	}
	
	public ChunkPathQueue getChunkQueue(String site) {
		return queueMap.get(site);
	}

	// TODO - best way to use config root folder vs direct from S3Object
	public void enableSite(String siteId) {
		if(queueMap.containsKey(queueMap))
			return;
//		ChunkPathQueue queue = new ChunkPathQueue(Paths.get(config.rootFolder), siteId);
//		queueMap.put(siteId, queue);
	}
	
	public void disableSite(String siteId) {
		queueMap.remove(siteId);
	}
	public void setS3Client(AmazonS3Client s3client) {
		for(Map.Entry<String, ChunkPathQueue> entry: queueMap.entrySet()) {
			ChunkPathQueue queue = entry.getValue();
			queue.setS3client(s3client);
		}
	}
}
