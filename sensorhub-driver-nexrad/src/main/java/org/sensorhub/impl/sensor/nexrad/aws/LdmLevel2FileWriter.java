package org.sensorhub.impl.sensor.nexrad.aws;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.sensorhub.impl.sensor.nexrad.aws.sqs.ChunkHandler;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.io.Files;

/**
 * <p>Title: Level2FileWriter.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 28, 2016
 */
public class LdmLevel2FileWriter implements ChunkHandler {
	private static final String LEVEL2_PATH = "C:/Data/sensorhub/Level2/test";
	private static final int BUFFER_SIZE = 8192;

	@Override
	public void handleChunk(S3Object s3object) throws IOException {
		String key = s3object.getKey();
		String name = key.replaceAll("/", "_");
		String site = key.substring(0, 4);
		Path sitePath = Paths.get(LEVEL2_PATH, site);
		boolean ok = sitePath.toFile().mkdirs();
		Path outPath = Paths.get(sitePath.toString(), name);

		try(S3ObjectInputStream is = s3object.getObjectContent()) {
			dumpChunkToFile(is, outPath);
		}
	}

	public void dumpChunkToFile(InputStream is, Path pout) throws IOException {
		Path ptmp = Paths.get(pout.toString() + ".tmp");
		try(FileOutputStream os = new FileOutputStream(ptmp.toFile())) {
			int cnt = 0;
			while(true) {
				int b = is.read();
				if(b == -1)  break;
				os.write(b);
				cnt++;
			}
		}
		Files.move(ptmp.toFile(), pout.toFile());
	}

}
