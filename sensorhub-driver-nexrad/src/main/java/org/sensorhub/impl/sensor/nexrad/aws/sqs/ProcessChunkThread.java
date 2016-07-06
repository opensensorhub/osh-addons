package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * <p>Title: ProcessChunkThread.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 5, 2016
 */
public class ProcessChunkThread implements Runnable {
//	public static final String REALTIME_AWS_NEXRAD_URL = "http://unidata-nexrad-level2-chunks.s3.amazonaws.com/";

	// Should pass this in as resource
	public static final String BUCKET_NAME = "unidata-nexrad-level2-chunks";

	String path;
	private AmazonS3Client s3client;
	private ChunkHandler chunkHandler;
	
	public ProcessChunkThread(AmazonS3Client client, ChunkHandler handler, String path) {
		this.s3client = client;
		this.path = path;
		this.chunkHandler = handler;
	}

	@Override
	public void run() {
		
		try (S3Object chunk = s3client.getObject(new GetObjectRequest(BUCKET_NAME, path))){
			chunkHandler.handleChunk(chunk);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
