package org.sensorhub.impl.sensor.nexrad.aws;

import java.util.List;

import org.sensorhub.impl.sensor.nexrad.aws.sqs.AwsSqsService;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkHandler;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ProcessChunkThread;

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
	private String outputPath;
	
	public ProcessMessageThread(AwsSqsService sqsService, List<String> sites, String outputPath) {
		this.sqsService = sqsService;
		this.sitesToKeep = sites;
		this.outputPath = outputPath;
	}
	
	@Override
	public void run() {
		for(;;) {
			List<Message> messages = this.sqsService.receiveMessages();
			processMessages(messages);
		}
		
	}
	
	private void processMessages(List<Message> messages) {
		for(Message msg: messages) {
			String body = msg.getBody();
			String chunkPath = AwsNexradUtil.getChunkPath(body);
			String time = AwsNexradUtil.getEventTime(body);
			String site = chunkPath.substring(0, 4);
//				System.err.println(site);
			if(sitesToKeep.size() == 0 || sitesToKeep.contains(site)) {
//				System.err.println("**** " + path);
				ProcessChunkThread t = new ProcessChunkThread(new LdmLevel2FileWriter(outputPath), chunkPath);
				t.run();
			}
			
			sqsService.deleteMessage(msg);
			
		}
	}
	
	public void addSite(String site) {
		sitesToKeep.add(site);
	}
	
	public void removeSite(String site) {
		sitesToKeep.remove(site);
	}
}
