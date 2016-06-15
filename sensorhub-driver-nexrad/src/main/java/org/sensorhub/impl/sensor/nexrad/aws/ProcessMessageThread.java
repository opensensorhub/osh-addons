package org.sensorhub.aws.nexrad;

import java.util.List;

import org.sensorhub.aws.sqs.AwsSqsService;
import org.sensorhub.aws.sqs.ChunkHandler;
import org.sensorhub.aws.sqs.ProcessChunkThread;

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
	
	public ProcessMessageThread(AwsSqsService sqsService, List<String> sites) {
		this.sqsService = sqsService;
		this.sitesToKeep = sites;
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
			String path = AwsNexradUtil.getChunkPath(body);
			String time = AwsNexradUtil.getEventTime(body);
			String site = path.substring(0, 4);
//				System.err.println(site);
			if(sitesToKeep.size() == 0 || sitesToKeep.contains(site)) {
				System.err.println("**** " + path);
				ProcessChunkThread t = new ProcessChunkThread(new LdmLevel2FileWriter(),path);
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
