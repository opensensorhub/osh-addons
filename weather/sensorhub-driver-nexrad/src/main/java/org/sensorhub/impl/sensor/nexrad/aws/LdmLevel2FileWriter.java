package org.sensorhub.impl.sensor.nexrad.aws;

import java.io.BufferedInputStream;
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
@Deprecated  // Moved this functionality to AwsNedxradUtil- remove on next code cleanup
public class LdmLevel2FileWriter implements ChunkHandler {
	private String level2Path ;
	private static final int BUFFER_SIZE = 8192;

	public LdmLevel2FileWriter(String outputPath) {
		this.level2Path = outputPath;
	}
	
	@Override
	public void handleChunk(S3Object s3object) throws IOException {
		String key = s3object.getKey();
		String name = key.replaceAll("/", "_");
		String site = key.substring(0, 4);
		Path sitePath = Paths.get(level2Path, site);
		boolean ok = sitePath.toFile().mkdirs();
		Path outPath = Paths.get(sitePath.toString(), name);

		try(S3ObjectInputStream is = s3object.getObjectContent()) {
			dumpChunkToFile(is, outPath);
		}
	}

	public void dumpChunkToFile(InputStream s3is, Path pout) throws IOException {
		BufferedInputStream is = new BufferedInputStream(s3is, BUFFER_SIZE);
		Path ptmp = Paths.get(pout.toString() + ".tmp");
		try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(ptmp.toFile()))) {
			byte [] b = new byte[BUFFER_SIZE];
			while(true) {
				int numBytes = is.read(b);
				if(numBytes == -1)  break;
				os.write(b,0, numBytes);
			}
		}
		Files.move(ptmp.toFile(), pout.toFile());
	}

}
