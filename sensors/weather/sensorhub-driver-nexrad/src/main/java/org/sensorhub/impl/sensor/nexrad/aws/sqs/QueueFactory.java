package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.PurgeQueueResult;

/**
 * <p>Title: QueueFactory.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 30, 2016
 */
public class QueueFactory
{
	static AWSCredentials credentials;
	static AmazonSQS sqs;
	static AmazonSNSClient sns;
	
	static {
		credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (~/.aws/credentials), and is in valid format.",
							e);
		}

		sqs = new AmazonSQSClient(credentials);
		sns = new AmazonSNSClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		sqs.setRegion(usWest2);
	}
	

	public static void main(String[] args) {
		listQueues();
//		deleteQueue("https://sqs.us-west-2.amazonaws.com/384286541835/NexradQueue_SensorHub_00018");
	}
	
	public static void main_(String[] args) {
		String topicArn = "arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object";
		String qName = "NexradQueue_SensorHub_00018";

		String queueUrl  = QueueFactory.createAndSubscribeQueue(topicArn, qName);
//		Topics.subscribeQueue(sns, sqs, topicArn, queueUrl);
		
//		GetQueueAttributesResult attributes = sqs.getQueueAttributes(queueUrl, Collection.singletonList("ApproximateNumberOfMessages"));
		List<String> atts = new ArrayList<>();
		atts.add("ApproximateNumberOfMessages");
		GetQueueAttributesResult attributes = sqs.getQueueAttributes(queueUrl, atts);
	    String size = attributes.getAttributes().get("ApproximateNumberOfMessages");
	    System.err.println("QueueSize: " + size);
	    PurgeQueueRequest purgeReq = new PurgeQueueRequest(queueUrl); 
	    PurgeQueueResult purgeRes = sqs.purgeQueue(purgeReq);
	    System.err.println(purgeRes);
	}
	
	public static void listQueues() {
		System.err.println("Listing all queues in your account.\n");
		for (String queueUrl : sqs.listQueues().getQueueUrls()) {
		    System.out.println("  QueueUrl: " + queueUrl);
		}
		System.out.println();
	}
	
	public static String createAndSubscribeQueue(String topicArn, String queueName)  {
		return createAndSubscribeQueue(topicArn, queueName, false);
	}
	
	/**
	 * Create a queue and subscribe to an Amazon SNS topic. If the queue already exists, 
	 * it will not be recreated.
	 * 
	 * @param topicArn - amazon resource name for the topic
	 * @param queueName  - name for the queue to create
	 * @param purgeExisting - if true, will purge any existing messages in the queue if it already exists
	 * @return url for the created queue
	 */
	public static String createAndSubscribeQueue(String topicArn, String queueName, boolean purgeExisting)  {
		// Create a queue
		CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
		String queueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
		// TODO - error check that queue was actually created
		Topics.subscribeQueue(sns, sqs, topicArn, queueUrl);
		
		if(purgeExisting) {
//			List<String> atts = new ArrayList<>();
//			atts.add("ApproximateNumberOfMessages");
//			GetQueueAttributesResult attributes = sqs.getQueueAttributes(queueUrl, atts);
//		    String size = attributes.getAttributes().get("ApproximateNumberOfMessages");
//		    System.err.println("QueueSize: " + size);
			// NOTE: Can only purge once every 60 s!!
		    PurgeQueueRequest purgeReq = new PurgeQueueRequest(queueUrl); 
		    PurgeQueueResult purgeRes = sqs.purgeQueue(purgeReq);
		}
		return queueUrl;
	}

	/**
	 * Note that a queue with the same url cannot be recreated for 60s after stopping
	 * @param queueUrl
	 */
	public static void deleteQueue(String queueUrl) {
		if (sqs != null && queueUrl != null) {
			try {
				sqs.deleteQueue(new DeleteQueueRequest(queueUrl));
			} catch (Exception e) {
				e.printStackTrace();
			}
//			sqs.shutdown();
		}
	}
}
 