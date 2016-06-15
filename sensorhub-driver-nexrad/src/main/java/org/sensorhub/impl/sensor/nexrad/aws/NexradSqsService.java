package org.sensorhub.aws.nexrad;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sensorhub.aws.sqs.AwsSqsService;
import org.sensorhub.aws.sqs.QueueFactory;

/**
 * <p>Title: NexradSqsService.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Apr 15, 2016
 */
public class NexradSqsService
{
	// allow to be configurable
	static final int NUM_THREADS = 5;
	static final String topicArn = "arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object";
	private String queueName;
	private List<String> sites;
	private AwsSqsService sqsService;
	
	public NexradSqsService(List<String> sites) {
		this.sites = sites;
		queueName = "NexradQueue_SensorHub";
	}
	
	public void start() {
		
		String queueUrl  = QueueFactory.createAndSubscribeQueue(topicArn, queueName);
		
		ExecutorService execService = Executors.newFixedThreadPool(NUM_THREADS);
		sqsService = new AwsSqsService(queueUrl);

		for(int i=0; i<NUM_THREADS; i++) {
			execService.execute(new ProcessMessageThread(sqsService, sites));
		}

		execService.shutdown();
	}
	
	public void stop() {
		sqsService.shutdown();
		QueueFactory.deleteQueue(queueName);
	}

}
