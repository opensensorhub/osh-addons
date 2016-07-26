package org.sensorhub.impl.sensor.nexrad.aws;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.util.Topics;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * <p>Title: AwsNexradUtil.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Feb 3, 2016
 * 
 * Each chunk of each volume scan file is its own object in Amazon S3. The basic data format is the following:
		/<Site>/<Volume_number>/<YYYYMMDD-HHMMSS-CHUNKNUM-CHUNKTYPE>
		Where:
		<Site> is the NEXRAD ground station (map of ground stations)
		<Volume_number> is the volume id number (cycles from 0 to 999)
		<YYYYMMDD> is the date of the volume scan
		<HHMMSS> is the time of the volume scan
		<CHUNKNUM> is the chunk number
		<CHUNKTYPE> is the chunk type
 * 
 */
public class AwsNexradUtil {
	public static final String AWS_NEXRAD_URL = "http://noaa-nexrad-level2.s3.amazonaws.com/";
	public static final String REALTIME_AWS_NEXRAD_URL = "http://unidata-nexrad-level2-chunks.s3.amazonaws.com/";
	public static final String dataBucketStr = "noaa-nexrad-level2";

	public static long toJulianTime(long daysSince70, long msSinceMidnight) {
		//		return TimeUnit.DAYS.toMillis(daysSince70) + msSinceMidnight;
		return TimeUnit.DAYS.toMillis(daysSince70 - 1) + msSinceMidnight;

	}

	public static String getChunkPath(String message) {
		//  Find instance of "key":
		int keyIdx = message.indexOf("key\\\"");
		String s = message.substring(keyIdx);
		int startIdx = s.indexOf("\\\"");
		s = s.substring(startIdx + 5);
		int stopIdx = s.indexOf("\\\"");
		s = s.substring(0, stopIdx);

		return s;
	}

	public static String getChunkPathJson(String message) {
		//  Find instance of "key":
		Gson gson = new Gson();

		NexradMessage msg = gson.fromJson(message, NexradMessage.class); 
		String s = msg.Message.replaceAll("\\\\","");
		JsonParser parser = new JsonParser();
		JsonObject jobject = (JsonObject) parser.parse(s);
		JsonArray jarray =  jobject.getAsJsonArray("Records");

		jobject = jarray.get(0).getAsJsonObject();
		jobject = jobject.getAsJsonObject("s3");
		jobject = jobject.getAsJsonObject("object");
		JsonPrimitive prim = jobject.getAsJsonPrimitive("key");
		String key = prim.getAsString();
		return key;

	}
	
	class NexradMessage {
		String Message;
		String Type;
	}

	class Records {
		String eventVersion;
		S3 s3;

		class S3 {
			String s3SchemaVersion;
			S3Object object;

			class S3Object {
				String key;
			}
		}
	}

	public static String getEventTime(String message) {
		//  Find instance of "key":
		int keyIdx = message.indexOf("Time\\\"");
		String s = message.substring(keyIdx);
		int startIdx = s.indexOf("\\\"");
		s = s.substring(startIdx + 5);
		int stopIdx = s.indexOf("\\\"");
		s = s.substring(0, stopIdx);

		return s;
	}


	public static void main(String[] args) throws Exception {

		String s = new String(Files.readAllBytes(Paths.get("C:/Users/tcook/root/sensorHub/doppler/awsMessage.json")));

		long t1 = System.currentTimeMillis();
		for(int i=0; i<10000; i++) {
//			String key = getChunkPathIndexOf(s);
			String key = getChunkPath(s);
			if (i%1000==0)
				System.err.println(i);
		}

		long t2 = System.currentTimeMillis();
		System.err.println(t2 - t1);
	}

	public static void main_(String[] args) throws IOException, InterruptedException {
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException(
					"Cannot load the credentials from the credential profiles file. " +
							"Please make sure that your credentials file is at the correct " +
							"location (~/.aws/credentials), and is in valid format.",
							e);
		}

		AmazonSQS sqs = new AmazonSQSClient(credentials);
		AmazonSNSClient sns = new AmazonSNSClient(credentials);
		Region usWest2 = Region.getRegion(Regions.US_WEST_2);
		sqs.setRegion(usWest2);

		try {
			//			ListQueuesResult r = sqs.listQueues();
			//			System.err.println(r.toString());
			//			GetQueueAttributesRequest qar = new GetQueueAttributesRequest(queueUrl);
			// Create a queue
			System.out.println("Creating a new SQS queue.\n");
			CreateQueueRequest createQueueRequest = new CreateQueueRequest("NexradQueueTest");
			String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
			//			sqs.
			String topicArn = "arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object";
			//			String topicArn = "arn:aws:sns:us-east-1:811054952067:NewNEXRADLevel2Archive";
			Topics.subscribeQueue(sns, sqs, topicArn, myQueueUrl);

			// Receive messages
			System.out.println("Receiving messages from MyQueue.\n");
			int cnt = 0;
			while(cnt++ < 1000) {
				ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl).
						withWaitTimeSeconds(0).withMaxNumberOfMessages(10);
				List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
				for (Message message : messages) {
					//					System.out.println("  Message");
					//					System.out.println("    MessageId:     " + message.getMessageId());
					//					System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
					//					System.out.println("    MD5OfBody:     " + message.getMD5OfBody());
					//					System.out.println("    Body:          " + message.getBody());
					String body = message.getBody();
					String path = getChunkPath(body);
					String time = getEventTime(body);
					//					if(path.contains("KLGX"))
					System.err.println(path);
					//					for (Entry<String, String> entry : message.getAttributes().entrySet()) {
					//						System.out.println("  Attribute");
					//						System.out.println("    Name:  " + entry.getKey());
					//						System.out.println("    Value: " + entry.getValue());
					//					}
					//					MessageAttributeValue val = message.getMessageAttributes().get("key");
					//					System.err.println(val.getStringValue());
					// delete message
					sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl, message.getReceiptHandle()));
				}
				System.out.println("NumMsgs = " + messages.size());

				//				Thread.sleep(100L);

			}
			//  destory queue 
			if (sqs != null && myQueueUrl != null) {
				sqs.deleteQueue(new DeleteQueueRequest(myQueueUrl));
				System.out.println("Deleted the queue.");
				sqs.shutdown();
			}
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which means your request made it " +
					"to Amazon SQS, but was rejected with an error response for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means the client encountered " +
					"a serious internal problem while trying to communicate with SQS, such as not " +
					"being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}
}
