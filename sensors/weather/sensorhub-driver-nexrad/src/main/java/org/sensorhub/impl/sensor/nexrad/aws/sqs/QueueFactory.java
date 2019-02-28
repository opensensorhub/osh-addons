package org.sensorhub.impl.sensor.nexrad.aws.sqs;

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
		
	deleteQueue("https://sqs.us-west-2.amazonaws.com/384286541835/NexradQueue_SensorHub_00024");
	}
	
	public static void listQueues() {
		System.out.println("Listing all queues in your account.\n");
		for (String queueUrl : sqs.listQueues().getQueueUrls()) {
		    System.out.println("  QueueUrl: " + queueUrl);
		}
		System.out.println();
	}
	
	public static String createAndSubscribeQueue(String topicArn, String queueName)  {
		// Create a queue
		CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName);
		String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
		System.out.println("I am Creating a new SQS queue called " + queueName);
		// TODO - error check that queue was actually created
		Topics.subscribeQueue(sns, sqs, topicArn, myQueueUrl);
		return myQueueUrl;
	}

	public static void deleteQueue(String queueUrl) {
		if (sqs != null && queueUrl != null) {
			try {
				sqs.deleteQueue(new DeleteQueueRequest(queueUrl));
				System.out.println("Deleted queue: " + queueUrl);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			sqs.shutdown();
		}
	}
}
