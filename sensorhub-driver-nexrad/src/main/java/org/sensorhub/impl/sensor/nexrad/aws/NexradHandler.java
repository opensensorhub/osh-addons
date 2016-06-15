package org.sensorhub.impl.sensor.nexrad.aws;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

/**
 * <p>Title: NexradClass.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Feb 18, 2016
 */
public class NexradHandler implements RequestHandler<SNSEvent, Object>
{
	@Override
	public Object handleRequest(SNSEvent event, Context context) {
		LambdaLogger logger = context.getLogger();
		for (SNSEvent.SNSRecord record : event.getRecords()) {
            SNSEvent.SNS sns = record.getSNS();
            logger.log("SNS message " + sns.getMessage());
        }
		
//        S3EventNotificationRecord record = event.getRecords().get(0);  // NPE
//        String evtName = record.getEventName();
//        record.getUserIdentity();
//        logger.log("evtName: " + evtName);
//        logger.log("numRecs: " + event.getRecords().size());
//        S3Entity s3 = record.getS3();
//        logger.log("Record: " + record + " ===== s3: " + s3);
//        S3ObjectEntity obj  =  s3.getObject();
//        String key = obj.getKey();
//        logger.log("Key: " + key);
        
		//			String srcBucket = record.getS3().getBucket().getName();
		// Object key may have spaces or unicode non-ASCII characters.
		//			String srcKey = record.getS3().getObject().getKey().replace('+', ' ');
		//			srcKey = URLDecoder.decode(srcKey, "UTF-8");

		//			System.err.println(srcKey);
		// Download the image from S3 into a stream
		//			AmazonS3 s3Client = new AmazonS3Client();
		//			S3Object s3Object = s3Client.getObject(new GetObjectRequest(srcBucket, srcKey));
		//			InputStream objectData = s3Object.getObjectContent();
		return null;
	}
}
