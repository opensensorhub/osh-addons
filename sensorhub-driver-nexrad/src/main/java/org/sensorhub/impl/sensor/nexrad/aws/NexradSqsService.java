package org.sensorhub.impl.sensor.nexrad.aws;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sensorhub.impl.sensor.nexrad.aws.sqs.AwsSqsService;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.QueueFactory;

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
	static final int NUM_THREADS = 10;  // allow config- test numThreads vs. sites that are being listened to
	static final String topicArn = "arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object";
	private String queueName;
	private List<String> sites;
	private AwsSqsService sqsService;
	private ExecutorService execService;
	
	public NexradSqsService(List<String> sites) {
		this.sites = sites;
		queueName = "NexradQueue_SensorHub";
	}
	
	public void start() {
		
		String queueUrl  = QueueFactory.createAndSubscribeQueue(topicArn, queueName);
		
		execService = Executors.newFixedThreadPool(NUM_THREADS);
		sqsService = new AwsSqsService(queueUrl);

		for(int i=0; i<NUM_THREADS; i++) {
			execService.execute(new ProcessMessageThread(sqsService, sites));
		}

//		execService.shutdown();
	}
	
	public void stop() {
		execService.shutdownNow();
		QueueFactory.deleteQueue(queueName);
	}

}
