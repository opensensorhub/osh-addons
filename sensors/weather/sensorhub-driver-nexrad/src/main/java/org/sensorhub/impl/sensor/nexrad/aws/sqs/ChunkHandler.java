package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.io.IOException;

import com.amazonaws.services.s3.model.S3Object;

/**
 * <p>Title: ChunkHandler.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 28, 2016
 */
public interface ChunkHandler {
	public void handleChunk(S3Object o) throws IOException;
}
