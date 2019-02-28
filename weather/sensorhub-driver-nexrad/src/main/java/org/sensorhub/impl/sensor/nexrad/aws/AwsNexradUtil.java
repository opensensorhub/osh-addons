package org.sensorhub.impl.sensor.nexrad.aws;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

		https://noaa-nexrad-level2.s3.amazonaws.com/2016/09/02/KABR/KABR20160902_230405_V06
 * 
 */
public class AwsNexradUtil {
	public static final String AWS_NEXRAD_URL = "http://noaa-nexrad-level2.s3.amazonaws.com/";
	public static final String REALTIME_AWS_NEXRAD_URL = "http://unidata-nexrad-level2-chunks.s3.amazonaws.com/";
	public static final String ARCHIVE_BUCKET_NAME = "noaa-nexrad-level2";
	public static final String BUCKET_NAME = "unidata-nexrad-level2-chunks";

	public static AmazonS3Client createS3Client() {
		AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
		AmazonS3Client s3 = new AmazonS3Client(credentials);
		return s3;
	}

	//  Need to figure out why realtime format value of daysSince70 seems to be one day too many
	public static long toJulianTime(long daysSince70, long msSinceMidnight) {
//		return TimeUnit.DAYS.toMillis(daysSince70 - 1) + msSinceMidnight;
		return TimeUnit.DAYS.toMillis(daysSince70) + msSinceMidnight;
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

	public static S3Object getChunk(AmazonS3Client s3client, String bucketName, String chunkPath) {
		return s3client.getObject(new GetObjectRequest(bucketName, chunkPath));
	}

	public static void dumpChunkToFile(S3Object chunk, Path pout) throws IOException {
		S3ObjectInputStream s3is = chunk.getObjectContent();
		BufferedInputStream is = new BufferedInputStream(s3is, 8192);
		try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(pout.toFile()))) {
			byte [] b = new byte[8192];
			while(true) {
				int numBytes = is.read(b);
				if(numBytes == -1)  break;
				os.write(b,0, numBytes);
			}
		}
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

	public static List<S3ObjectSummary> listFiles(AmazonS3Client s3, String site, String date) {
		return listFiles(s3, site, date + "_00", date + "_24");
	}

	public static List<S3ObjectSummary> listFiles(AmazonS3Client s3, String site, String startTime, String stopTime) {
		//  create sqs client
		String startSlash = startTime.substring(0,4) + "/" + startTime.substring(4,6) + "/" + startTime.substring(6,8);
		String startPrefix = startSlash + "/" + site + "/" + site + startTime.substring(0,8);
		String startTimeCompare = startSlash + "/" + site + "/" + site + startTime;
		String stopTimeCompare = startSlash + "/" + site + "/" + site + stopTime;
		//  Get all listings for entire day
		ObjectListing listing = s3.listObjects( ARCHIVE_BUCKET_NAME, startPrefix);
		List<S3ObjectSummary> summaries = listing.getObjectSummaries();
		while (listing.isTruncated()) {
			System.err.println("something");
			listing = s3.listNextBatchOfObjects (listing);
			summaries.addAll (listing.getObjectSummaries());
		}

		//  Now filter based on start and stopTime
		List<S3ObjectSummary> matches = new ArrayList<>();
		for(S3ObjectSummary s: summaries) {
			if(s.getKey().compareTo(startTimeCompare) >= 0  && s.getKey().compareTo(stopTimeCompare) <= 0) 
				matches.add(s);
		}

		return matches;
	}

	public static String gunzipFile(String compressedFile, String decompressedFile) throws IOException {
		byte[] buffer = new byte[8096];
		try {
			FileInputStream fileIn = new FileInputStream(compressedFile);
			GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);
			FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile);
			int bytes_read;
			while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, bytes_read);
			}
			gZIPInputStream.close();
			fileOutputStream.close();
			return decompressedFile;
		} catch (IOException ex) {
			throw new IOException(ex);
		}
	}


	public static void main(String[] args) throws Exception {
		AWSCredentials credentials = new ProfileCredentialsProvider().getCredentials();
		AmazonS3Client s3 = new AmazonS3Client(credentials);
		//		List<S3ObjectSummary> matches = listFiles(s3, "KEWX", "20160901");
//		List<S3ObjectSummary> matches = listFiles(s3, "KEWX", "20160901_121314", "20160901_131314");
		List<S3ObjectSummary> matches = listFiles(s3, "KHTX", "20110427_1800", "20110427_2200");
		for(S3ObjectSummary s: matches) {
			System.err.println(s.getKey());
			S3Object obj = getChunk(s3, ARCHIVE_BUCKET_NAME, s.getKey());
			int lastSlash = s.getKey().lastIndexOf("/");
			assert lastSlash != -1;
			String filename = s.getKey().substring(lastSlash + 1);
			Path pout = Paths.get("C:/Data/sensorhub/Level2/archive/KHTX");
			Path fout = Paths.get(pout.toString(), filename);
			String gzFilename;
			AwsNexradUtil.dumpChunkToFile(obj, fout);
			gunzipFile(fout.toString(), fout.toString() + ".88d");
		}


		//		S3ObjectInputStream is - obj.get

		//		listFiles(s3, "KEWX", "20160901_00", "20160901_24");
	}

}
