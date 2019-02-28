package org.sensorhub.impl.sensor.nexrad.ucar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.nexrad.NexradConfig;
import org.sensorhub.impl.sensor.nexrad.RadialProvider;
import org.sensorhub.impl.sensor.nexrad.aws.AwsNexradUtil;
import org.sensorhub.impl.sensor.nexrad.aws.LdmRadial;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * <p>Title: ArchiveRadarProvider.java</p>
 * <p>Description: </p>
 *
 * @author tcook
 * @date Sep 17, 2016
 */
public class ArchiveRadialProvider implements RadialProvider {

	UcarLevel2Reader reader = new UcarLevel2Reader();
	private AmazonS3Client s3client;
	private List<S3ObjectSummary> summaries; 
	Path rootFolder;
	String site;
	int volumeIndex = 0;

	public ArchiveRadialProvider(NexradConfig config) throws SensorHubException {
		try {
			this.rootFolder = Paths.get(config.rootFolder);
			if(!Files.isDirectory(rootFolder))
				throw new SensorHubException("Configured rootFolder does not exist or is not a directory" + config.rootFolder);
			this.site = config.siteIds.get(0);
			Path sitePath = Paths.get(rootFolder.toString(), site);
			FileUtils.forceMkdir(sitePath.toFile());
		} catch (IOException e) {
			throw new SensorHubException(e.getMessage(), e);
		}
		s3client = AwsNexradUtil.createS3Client();
		summaries = AwsNexradUtil.listFiles(s3client, site, config.archiveStartTime, config.archiveStopTime);
	}

	public File getFile(S3ObjectSummary s) throws IOException {
		String key = s.getKey();
		S3Object obj = AwsNexradUtil.getChunk(s3client, AwsNexradUtil.ARCHIVE_BUCKET_NAME, key);
		int lastSlash = key.lastIndexOf("/");
		assert lastSlash != -1;
		String filename = key.substring(lastSlash + 1);
		Path pout = Paths.get(rootFolder.toString(), site);
		Path fout = Paths.get(pout.toString(), filename);
		String gzFilename;
		if(filename.endsWith(".gz"))
			gzFilename = filename.replaceAll(".gz", ".88d");
		else 
			gzFilename = filename + ".88d";
		Path gzOut = Paths.get(pout.toString(), gzFilename);
		try {
			AwsNexradUtil.dumpChunkToFile(obj, fout);
			String fileLocation = AwsNexradUtil.gunzipFile(fout.toString(), gzOut.toString());
			return new File(fileLocation);
		} catch (IOException e) {
			throw new IOException(e);
		}

	}

	/* (non-Javadoc)
	 * @see org.sensorhub.impl.sensor.nexrad.RadialProvider#getNextRadial()
	 */
	@Override
	public LdmRadial getNextRadial() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.sensorhub.impl.sensor.nexrad.RadialProvider#getNextRadials()
	 */
	@Override
	public List<LdmRadial> getNextRadials() throws IOException {
		if(volumeIndex >= summaries.size())
			return null;  // no more data
		S3ObjectSummary s = summaries.get(volumeIndex++);
		File file = getFile(s);
		UcarLevel2Reader reader = new UcarLevel2Reader(file);
		return reader.read();
	}
	
	public List<LdmRadial> getNextRadials(String site) throws IOException {
		return null;
	}

}
