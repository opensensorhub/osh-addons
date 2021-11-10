package org.sensorhub.impl.sensor.audio;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author tcook
 *
 */
public class WavDirectoryIterator 
{
	public static Iterator<File> getFileIterator(Path dir, String ext) {
		File [] files = getFiles(dir, ext);
		Iterator<File> fileIterator = Arrays.stream(files).iterator();
		return fileIterator;
	}
	
	private static File[] getFiles(Path dir, String ext) {
		assert Files.isDirectory(dir);
		//  Get all .wav files, sort based on name
		File [] files = dir.toFile().listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(ext);
			}
		});
		
		return files;
	}
	
	//  GUI_Decode_2019-03-27 141801.wav
	// D_Bouz_Pickin_2019-03-27 141801.wav
	public static double filenameTimeParser(String fname, String timePattern) throws IOException {
        String strPattern = "\\d{4}-\\d{2}-\\d{2} \\d{6}";
        Pattern pattern = Pattern.compile(strPattern);
        Matcher matcher = pattern.matcher(fname);
        if(!matcher.find())
        	throw new IOException("Unrecognized time pattern in filename: " + timePattern + "," + fname);
        
		String dateStr = matcher.group();
//		String dateStr = fname.substring(11, fname.length() - 4);
		System.err.println(dateStr);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timePattern).withZone(ZoneId.of("GMT"));
		ZonedDateTime zdt = ZonedDateTime.parse(dateStr, formatter);
		long sec = zdt.toInstant().getEpochSecond();
		long nano = zdt.toInstant().getNano();
		double time = sec + nano;
		
		return time;
	}
	
	 public static void mainTestRegex(String[] args) {
	        
	        // String str = "The first version was released on 23-01-1996";
			String str = "GUI_Decode_2019-03-27 141801.wav";
//			String str = "D_Bouz_Pickin_2019-03-27 141801.wav";

	        String strPattern = "\\d{4}-\\d{2}-\\d{2} \\d{6}";
	        
	        Pattern pattern = Pattern.compile(strPattern);
	        Matcher matcher = pattern.matcher(str);
	        
	        while( matcher.find() ) {
	            System.out.println( matcher.group() );
	        }
	    }
	
	public static void main(String[] args) throws IOException {
//		Path p = Paths.get("C:/Users/tcook/root/sensorHub/socom/mastodon/data/20190327_SRSE_Vignette_Data/20190327_SRSE_Vignette_Data/Silent_Echo_Data/Audio_Decodes");
		//       "wavDir": "C:/Users/tcook/root/sensorHub/socom/mastodon/data/20190327_SRSE_Vignette_Data/20190327_SRSE_Vignette_Data/Silent_Echo_Data/Audio_Decodes_Subset",

		//    GUI_Decode_2019-03-27 141801.wav
		// D_Bouz_Pickin_2019-03-27 141801.wav
		Path p = Paths.get("C:/Data/sensorhub/audio");
//		Path p = Paths.get("C:/Data/sensorhub/audio/silentEchoDemo");
		Iterator<File> it = getFileIterator(p, "wav");
		while(it.hasNext()) {
			File f  = it.next();
			System.err.println(f);
			double t = filenameTimeParser(f.getName(), "yyyy-MM-dd HHmmss");
			System.err.println(t + ", " + f.getName());
		}

	}
}
