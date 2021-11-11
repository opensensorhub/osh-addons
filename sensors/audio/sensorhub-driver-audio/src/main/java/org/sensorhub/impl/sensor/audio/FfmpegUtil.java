package org.sensorhub.impl.sensor.audio;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacpp.Loader;

public class FfmpegUtil {

	// Brute force method to get codec, numChannels, and sampleRate from file
	// There is probably a much cleaner way to do this, but it seems to work
	// for most audio files with a single stream
	public static AudioMetadata ffProbe(Path inputFile) throws IOException, InterruptedException {
		String ffprobe = Loader.load(org.bytedeco.ffmpeg.ffprobe.class);
		ProcessBuilder pb = new ProcessBuilder(ffprobe, inputFile.toString());
		Process p = pb.start();
		p.waitFor();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		StringBuilder builder = new StringBuilder();
		String line = null;
		AudioMetadata metadata = new AudioMetadata();
		while ((line = reader.readLine()) != null) {
  		    builder.append(line + System.getProperty("line.separator"));
			line = line.trim();
			//    Stream #0:0: Audio: pcm_s16le ([1][0][0][0] / 0x0001), 44100 Hz, 2 channels, s16, 1411 kb/s
			//    Stream #0:0: Audio: mp3 (U[0][0][0] / 0x0055), 16000 Hz, mono, fltp, 20 kb/s    
			if(line.startsWith("Duration")) {
				String [] sarr = line.split(",");
				String [] subarr = sarr[0].split(" ");
				String sduration = subarr[1].trim();
				subarr = sduration.split(":");
				long hours = 3600 * Long.parseLong(subarr[0]);
				long minutes = 60 * Long.parseLong(subarr[1]);
				metadata.duration = hours + minutes + Double.parseDouble(subarr[2]);
				subarr = sarr[1].split(":");
				metadata.bitrate = subarr[1].trim();
				
			} else if (line.startsWith("Stream")) {
				String [] sarr = line.split(",");
				String [] subarr = sarr[0].split(" ");
				boolean codecNext = false;
				// codec
				for(String s: subarr) {
					if(codecNext) {
						metadata.codec = s.trim();
						break;
					}
					if(s.equalsIgnoreCase("Audio:")) {
						codecNext = true;
					}
				}
				subarr = sarr[1].trim().split(" ");
				metadata.sampleRate = Integer.parseInt(subarr[0].trim());
				subarr = sarr[2].trim().split(" ");
				if("mono".equalsIgnoreCase(subarr[0])) 
					metadata.numChannels = 1;
				else if("stereo".equalsIgnoreCase(subarr[0]))
					metadata.numChannels = 2;
				else 
					metadata.numChannels = Integer.parseInt(subarr[0]);
			}
		}
//		System.err.println(builder.toString());
		return metadata;
	}

	public static void main(String[] args) throws Exception {
//		Path infile = Paths.get("C:/Data/sensorhub/audio/silentEchoDemo/GUI_Decode_2019-03-27 141801.wav");
//		AudioMetadata md = ffProbe(infile);
//		System.err.println(md);
//
//		infile = Paths.get("C:/Data/sensorhub/audio/Mail06_2019-03-27 141801.wav");
//		md = ffProbe(infile);
//		System.err.println(md);
//		
//		infile = Paths.get("C:/Data/sensorhub/audio/guitar/D_Bouz_Pickin_2019-03-27 141801.wav");
//		md = ffProbe(infile);
//		System.err.println(md);

		Path p = Paths.get("C:/Data/sensorhub/audio/snippets");
		assert Files.exists(p);
		Iterator<File> it = WavDirectoryIterator.getFileIterator(p, "wav");
		while(it.hasNext()) {
			File infile = it.next();
			AudioMetadata md = ffProbe(infile.toPath());
			System.err.println(infile.getName() + "\n" + md);
		}
	}

}
