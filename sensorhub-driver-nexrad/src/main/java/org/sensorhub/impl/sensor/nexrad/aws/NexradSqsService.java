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
	private int numThreads = 4;  // config can and usually should override this
	static final String topicArn = "arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object";
	private String queueName;
	private List<String> sites;
	private AwsSqsService sqsService;
	private ExecutorService execService;
	private String outputPath;
	
	public NexradSqsService(List<String> sites, String outputPath, int numThreads) {
		this.sites = sites;
		this.numThreads = numThreads;
		this.outputPath = outputPath;
		queueName = "NexradQueue_SensorHub";
	}
	
	public void start() {
		
		String queueUrl  = QueueFactory.createAndSubscribeQueue(topicArn, queueName);
		
		execService = Executors.newFixedThreadPool(numThreads);
		sqsService = new AwsSqsService(queueUrl);

		for(int i=0; i<numThreads; i++) {
			execService.execute(new ProcessMessageThread(sqsService, sites, outputPath));
		}

//		execService.shutdown();
	}
	
	public void stop() {
		execService.shutdownNow();
		QueueFactory.deleteQueue(queueName);
	}

}
