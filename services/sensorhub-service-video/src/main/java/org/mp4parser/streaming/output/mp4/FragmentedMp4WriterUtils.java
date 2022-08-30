package org.mp4parser.streaming.output.mp4;

import java.util.Date;

/**
 * This is a helper class that serves as a hack to allow access to some protected members of the FragmentedMp4Writer
 * class.
 * 
 * This is the lesser of two evils, in our opinion. (The alternative is to include the mp4parser library's source in
 * this project and modify the FragmentedMp4Writer class to add a setCreationTime(...) method.)
 */
public interface FragmentedMp4WriterUtils {
	public static void setCreationTime(FragmentedMp4Writer fragmentedMp4Writer, Date creationTime) {
		if (fragmentedMp4Writer != null) {
			fragmentedMp4Writer.creationTime = creationTime;
		}
	}
}
