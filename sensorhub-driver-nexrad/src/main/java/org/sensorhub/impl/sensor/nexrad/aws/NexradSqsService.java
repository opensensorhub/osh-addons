package org.sensorhub.impl.sensor.nexrad.aws;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sensorhub.impl.sensor.nexrad.aws.sqs.AwsSqsService;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkPathQueue;
import org.sensorhub.impl.sensor.nexrad.aws.sqs.QueueFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;

/**
 * <p>Title: NexradSqsService.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Apr 15, 2016
 */
public class NexradSqsService
{
	private int numThreads = 4;  // config can and usually should override this
	static final String topicArn = "arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object";
	private String queueName;
	private List<String> sites;
	private AwsSqsService sqsService;
	private ExecutorService execService;
	ChunkPathQueue chunkQueue;  // local queue of filenames on disk- NexradSensor creates and passes this in- a bit clumsy so revisit later

	//  S3 Client needs to be created only once
	private AmazonS3Client s3client;


	public NexradSqsService(String queueName, List<String> sites) throws IOException {
		this.sites = sites;
		this.queueName = queueName;

		createS3Client();
	}

	private void createS3Client() {
		try {
			AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
			s3client = new AmazonS3Client(credentials);
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (~/.aws/credentials), and is in valid format.",
							e);
		}
	}

	public void start() {
		assert chunkQueue != null;
		String queueUrl  = QueueFactory.createAndSubscribeQueue(topicArn, queueName);

		execService = Executors.newFixedThreadPool(numThreads);
		sqsService = new AwsSqsService(queueUrl);

		for(int i=0; i<numThreads; i++) {
			execService.execute(new ProcessMessageThread(sqsService, s3client, sites, chunkQueue));
		}
	}

	public void stop() {
		execService.shutdownNow();
		QueueFactory.deleteQueue(queueName);
	}

	public AmazonS3Client getS3client() {
		return s3client;
	}

	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	public void setChunkQueue(ChunkPathQueue chunkQueue) {
		this.chunkQueue = chunkQueue;
	}

}
