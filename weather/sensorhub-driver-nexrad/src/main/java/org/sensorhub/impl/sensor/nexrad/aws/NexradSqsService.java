package org.sensorhub.impl.sensor.nexrad.aws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.sensorhub.impl.sensor.nexrad.aws.sqs.AwsSqsService;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkPathQueue;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkQueueManager;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.QueueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * <p>Title: NexradSqsService.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Apr 15, 2016
 */
public class NexradSqsService
{
	static final Logger logger = LoggerFactory.getLogger(NexradSqsService.class);
	private int numThreads = 4;  // config can and usually should override this
	static final String topicArn = "arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object";
	private String queueName;
	private List<String> sites;
	private AwsSqsService sqsService;
	private ExecutorService execService;
	// local queue of filenames on disk- NexradSensor creates and passes this in- a bit clumsy so revisit later
//	ChunkPathQueue chunkQueue;  
	ChunkQueueManager chunkQueueManager;

	//  S3 Client needs to be created only once
	private AmazonS3Client s3client;
	//  We need to keep threads in order to dynamically add/remove sites once that is supported
	List<ProcessMessageThread> messageThreads = new ArrayList<>();

	//  Relocating queue control to this class, where it makes more sense
	static final long QUEUE_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);
	long idleStartTime;  // the last time data was requested from the any listener
	long idleTimeMillis;  // how long in milliseconds to allow queue to be idle (no requests) before disabling
	boolean queueActive = false;

	public NexradSqsService(String queueName, List<String> sites) throws IOException {
		this.sites = sites;
		this.queueName = queueName;

		createS3Client();
	}

	private void createS3Client() {
		try {
			s3client = AwsNexradUtil.createS3Client();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (~/.aws/credentials), and is in valid format.",
							e);
		}
	}

	public void start() {
//		assert chunkQueue != null;
		assert chunkQueueManager != null;
		String queueUrl  = QueueFactory.createAndSubscribeQueue(topicArn, queueName);

		execService = Executors.newFixedThreadPool(numThreads);
		sqsService = new AwsSqsService(queueUrl);

		for(int i=0; i<numThreads; i++) {
			ProcessMessageThread t = new ProcessMessageThread(sqsService, s3client, sites, chunkQueueManager);
			messageThreads.add(t);
			execService.execute(t);
		}
		
		Timer queueTimer = new Timer();  //
		queueTimer.scheduleAtFixedRate(new CheckQueueStatus(), 0, QUEUE_CHECK_INTERVAL); //delay in milliseconds
	}

	public void stop() {
		if(queueActive) {
			for(ProcessMessageThread t: messageThreads) {
				t.setProcessing(false);
			}
			shutdownAndAwaitTermination(execService);
			QueueFactory.deleteQueue(queueName);
		}
	}
	
	void shutdownAndAwaitTermination(ExecutorService pool) {
	    pool.shutdown(); // Disable new tasks from being submitted
	    try {
	        // Wait a while for existing tasks to terminate
	        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
	            pool.shutdownNow(); // Cancel currently executing tasks
	            // Wait a while for tasks to respond to being cancelled
	            if (!pool.awaitTermination(10, TimeUnit.SECONDS))
	                System.err.println("Pool did not terminate");
	        }
	    } catch (InterruptedException ie) {
	        // (Re-)Cancel if current thread also interrupted
	        pool.shutdownNow();
	        // Preserve interrupt status
	        Thread.currentThread().interrupt();
	    }
	}

	public void setQueueActive() throws IOException {
		if(!queueActive) {
			// May need to recreate amazonSqs with new name to avoid reusing queue issues
//			sqsService = new AwsSqsService(queueUrl);
			
			
			// design issue here in that nexradSqs needs chunkQueue and chunkQueue needs s3client.  
//			nexradSqs.setChunkQueue(chunkQueue);  // 
//			chunkQueue.setS3client(nexradSqs.getS3client());  //
//			idleTimeMillis = TimeUnit.MINUTES.toMillis(config.queueIdleTimeMinutes);
			idleStartTime = System.currentTimeMillis();

			start();
			queueActive = true;
		} 
	}

	public void setQueueIdle() {
		if(!queueActive)
			return;
		idleStartTime = System.currentTimeMillis();
	}

	class CheckQueueStatus extends TimerTask {
		@Override
		public void run() {
			logger.debug("Check queue.  QueueActive = {}" , queueActive);
			if(!queueActive)
				return;
			if(System.currentTimeMillis() - idleStartTime > idleTimeMillis) {
				logger.debug("Check Queue. Stopping unused queue... ");
				stop();
				queueActive = false;
			}
		}

	}

	public long getQueueIdleTime() {
		return idleStartTime;
	}

	public void setQueueIdleTime(long queueIdleTime) {
		this.idleStartTime = queueIdleTime;
	}

	public boolean isQueueActive() {
		return queueActive;
	}

	public void setQueueActive(boolean queueActive) {
		this.queueActive = queueActive;
	}

	public long getQueueIdleTimeMillis() {
		return idleTimeMillis;
	}

	public void setQueueIdleTimeMillis(long queueIdleTimeMillis) {
		this.idleTimeMillis = queueIdleTimeMillis;
		logger.debug("{} QueueIdleTimeMinutes: {}", idleTimeMillis);
	}
	
	public AmazonS3Client getS3client() {
		return s3client;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	public void setChunkQueueManager(ChunkQueueManager chunkQueueManager) {
		this.chunkQueueManager = chunkQueueManager;
	}
}
