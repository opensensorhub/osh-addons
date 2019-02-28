package org.sensorhub.impl.sensor.nexrad.aws;

import java.util.List;

import org.sensorhub.impl.sensor.nexrad.aws.sqs.AwsSqsService;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkPathQueue;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkQueueManager;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.model.Message;

/**
 * <p>Title: MessageProcessingThread.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 2, 2016
 */
public class ProcessMessageThread implements Runnable {

	private AwsSqsService sqsService;
	List<String> sitesToKeep;
	ChunkPathQueue chunkQueue;
	ChunkQueueManager chunkQueueManager;
	boolean processing = true;
	
	public ProcessMessageThread(AwsSqsService sqsService, AmazonS3Client client, List<String> sites, ChunkQueueManager chunkQueueManager) {
		this.sqsService = sqsService;
		this.sitesToKeep = sites;
		this.chunkQueueManager = chunkQueueManager;
	}

	@Override
	public void run() {
		while(processing) {
			List<Message> messages = this.sqsService.receiveMessages();
			for(Message msg: messages) {
				String body = msg.getBody();
				String chunkPath = AwsNexradUtil.getChunkPath(body);
				//				String time = AwsNexradUtil.getEventTime(body);
				String site = chunkPath.substring(0, 4);
//				System.err.println(chunkPath);
				if(sitesToKeep.contains(site)) {
					chunkQueueManager.addChunkPath(site, chunkPath);
				}
			}
			sqsService.deleteMessages(messages);	
		}
	}

	public void setProcessing(boolean processing) {
		this.processing = processing;
	}

	public void addSite(String site) {
		sitesToKeep.add(site);
	}

	public void removeSite(String site) {
		sitesToKeep.remove(site);
	}
	
}
