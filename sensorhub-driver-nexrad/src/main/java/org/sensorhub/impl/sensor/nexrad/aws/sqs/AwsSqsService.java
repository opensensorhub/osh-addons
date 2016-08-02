package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;


/**
 * <p>Title: AmazonSqsService.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 2, 2016
 */
public class AwsSqsService {

	private AmazonSQS sqs;
	AWSCredentials credentials;
//	private static final String QUEUE_URL = 	"https://sqs.us-east-1.amazonaws.com/633354997535/NexradRealtimeQueue";
	private String queueUrl; 
	
	public AwsSqsService(String queueUrl) {
		this.queueUrl = queueUrl;

		//  create sqs client
		credentials = new ProfileCredentialsProvider().getCredentials();
		sqs = new AmazonSQSClient(credentials);
//		AmazonSQSAsync sqsAsync = new AmazonSQSAsyncClient (credentials ) ;
//		sqs  = new AmazonSQSBufferedAsyncClient(sqsAsync);
		
		Region usEast1 = Region.getRegion(Regions.US_EAST_1);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		sqs.setRegion(usEast1);
		sqs.setEndpoint("sdb.amazonaws.com");
	}
	
	
	public List<Message> receiveMessages() {
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl).
				withMaxNumberOfMessages(10); //withWaitTimeSeconds(1).
		try {
			List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
			return messages;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new ArrayList<Message>();
		}
	}
	
	public void deleteMessage(Message msg) {
		sqs.deleteMessage(new DeleteMessageRequest(queueUrl, msg.getReceiptHandle()));
	}
	
	public void deleteMessages(List<Message> messages) {
		if(messages.size() == 0)
			return;
		List<DeleteMessageBatchRequestEntry> entries = new ArrayList<>();
		for(Message message : messages){
			DeleteMessageBatchRequestEntry entry = new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle());
			entries.add(entry);
		}
		DeleteMessageBatchResult result = sqs.deleteMessageBatch(new DeleteMessageBatchRequest(queueUrl, entries));
		List<BatchResultErrorEntry> failed = result.getFailed();
		for(BatchResultErrorEntry entry: failed) {
			System.err.println("Failed to delete: " + entry.getId() + " : " + entry.getCode());
		}
	}
	
	// Don't think we will need to call this
	public void shutdown() {
		sqs.shutdown();
	}

	//  Should only create these once and hand them out as needed 
	public AWSCredentials getCredentials() {
		return credentials;
	}
}
