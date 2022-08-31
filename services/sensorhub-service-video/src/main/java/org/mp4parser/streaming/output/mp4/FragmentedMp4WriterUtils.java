package org.mp4parser.streaming.output.mp4;

import java.util.Date;

/**
 * This is a helper class that allows access to the protected {@link FragmentedMp4Writer#creationTime creationTime}
 * member of the {@link FragmentedMp4Writer} class. The <code>mp4parser</code> library does not expose a way to
 * set this value, but we want its value to match the observation time (instead of the default, which is to use the
 * time when the <code>FragmentedMp4Writer</code> was created).
 * <p>
 * Hacking a library in this way is generally not a good idea. But it seems to be a small evil, since it allows us to
 * use the third-party library as a dependency (rather than forking the library to add this one feature, and/or
 * including its source in this project).
 */
public interface FragmentedMp4WriterUtils {
	public static void setCreationTime(FragmentedMp4Writer fragmentedMp4Writer, Date creationTime) {
		if (fragmentedMp4Writer != null) {
			fragmentedMp4Writer.creationTime = creationTime;
		}
	}
}
