package org.sensorhub.impl.sensor.nexrad.aws.sqs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.sensorhub.impl.sensor.nexrad.RadialProvider;
import org.sensorhub.impl.sensor.nexrad.aws.AwsNexradUtil;
import org.sensorhub.impl.sensor.nexrad.aws.Level2Reader;
import org.sensorhub.impl.sensor.nexrad.aws.Radial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;


/**
 * <p>Title: ChunkPathQueue.java</p>
 * <p>Description: This class was originally written to ensure nexrad S3 objects were read and 
 * processed in order. From observation, the initial messages that we get from the Nexrad SNS
 * were not strictly in order. Now, I don't think we really need this. It seems after the first
 * few messages, subsequent messages are in strict order. Also, should not really matter if there
 * are a few out of order messages. Clients should still be able to construct volumes in that case.
 * 
 * Will probably end up dropping this mechanism as it complicates the ingest process.
 * - Tony, 2023-03-17
 * </p>
 *
 * @author T
 * @date Jul 27, 2016
 */
public class ChunkPathQueue 
{
	// Create priorityQueue for each site
	//  either this class manages them all, or multiple instances one per site

	Logger logger = LoggerFactory.getLogger(ChunkPathQueue.class);
	PriorityBlockingQueue<String> queue;
	AmazonS3Client s3client;
	Path siteFolder; 
	String site;
	int vol, chunk;
	char type;
	boolean first = true;
	static final int START_SIZE = 3;  // allow settable
	static final int SIZE_LIMIT = 8;

	public ChunkPathQueue(Path rootFolder, String site) throws IOException {
		this(site);
		this.siteFolder = Paths.get(rootFolder.toString(), site);
		//  Make sure the target folder exists
		FileUtils.forceMkdir(this.siteFolder.toFile());
	}
	
	public ChunkPathQueue(String site) throws IOException {
		this.site = site;
		queue = new PriorityBlockingQueue<>();
	}

	public void add(String chunkPath) {
		queue.add(chunkPath);
	}

	void dump(BlockingQueue<String> queue) {
		String [] sarr = (String[]) queue.toArray(new String [] {});
		Arrays.sort(sarr);
		logger.trace("QUEUE: ");
		for (String st: sarr)
			logger.trace("\t" + st);

	}

	boolean isNext(int v, int c, char t) {
		boolean isNext;
		if(type == 'E') {
			isNext = (v == vol + 1) && (c == 1);
		} else {
			isNext = (v == vol) && (c == chunk + 1);
		}
		if(isNext) {
			vol = v;
			chunk = c;
			type = t;
		}
		//System.err.println(isNext + ": " + v +"," + c + "," + t);
		return isNext;
	}

	// If a force take, need to ensure that any previous chunks that come in later are not added to the queue
	private String next() throws InterruptedException {
		boolean next = false;
		while(!next) {
			if(first) {
				String f = queue.take();
				String [] sarr = f.split("/");
				vol = Integer.parseInt(sarr[1]);
				int dashIdx = f.lastIndexOf('-');
				assert (dashIdx > 10);
				String s = f.substring(dashIdx - 3, dashIdx);
				chunk = Integer.parseInt(s);
				type = f.charAt(f.length() - 1);
				first = false;
				continue;
			}

			String chunkName = queue.peek();
			if(chunkName == null)
				continue;
			String [] sarr = chunkName.split("/");
			int v = Integer.parseInt(sarr[1]);
			int dashIdx = chunkName.lastIndexOf('-');
			assert (dashIdx > 10);
			String s = chunkName.substring(dashIdx - 3, dashIdx);
			int c = Integer.parseInt(s);
			char t = chunkName.charAt(chunkName.length() - 1);
			if (isNext(v,c,t)) {
				chunkName = queue.take();
				logger.debug("Take that from queue: {}" , chunkName);
				return chunkName;
			} else if(queue.size() > SIZE_LIMIT) {
				chunkName = queue.take(); 
				logger.debug("Force take from queue: {}" , chunkName);
				chunk = c;
				vol = v;
				type = t;
			}
			dump(queue);					
			Thread.sleep(500L);
		}

		return null;
	}

	public S3Object nextObject() throws IOException
	{
		assert s3client != null;
		try
		{
			String nextFile = next();
			logger.debug("NextFile: {}", nextFile);
			S3Object chunk = AwsNexradUtil.getChunk(s3client, AwsNexradUtil.BUCKET_NAME, nextFile);
			
			return chunk;
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}
	
	public Path nextFile() throws IOException
	{
		assert s3client != null;
		try
		{
//			System.err.println("*** Checking nextFile");
			String nextFile = next();
			S3Object chunk = AwsNexradUtil.getChunk(s3client, AwsNexradUtil.BUCKET_NAME, nextFile);
			nextFile = nextFile.replaceAll("/", "_");

			Path pout = Paths.get(siteFolder.toString(), nextFile);
			//  If I thread writing of file, I will have to put in a mechanism to notify the listener (NexradOutput)
			//  when the file writing is complete.  Right now, I don't think it is needed. 
			AwsNexradUtil.dumpChunkToFile(chunk, pout);
			return pout;
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}

	public void setS3client(AmazonS3Client s3client) {
		this.s3client = s3client;
	}
}
