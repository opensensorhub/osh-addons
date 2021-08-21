package org.sensorhub.impl.sensor.audio;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;

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
	
	public static void main(String[] args) {
		Path p = Paths.get("C:/Users/tcook/root/sensorHub/socom/mastodon/data/20190327_SRSE_Vignette_Data/20190327_SRSE_Vignette_Data/Silent_Echo_Data/Audio_Decodes");
		Iterator<File> it = getFileIterator(p, "wav");
		while(it.hasNext())
			System.err.println(it.next());

	}
}
