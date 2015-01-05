package org.senshorhub.impl.sensor.profiler915;

import java.io.IOException;
import java.util.List;

/**
 * <p>Title: ProfileProvider.java</p>
 * <p>Description:  interface for readers to provide profile data</p>
 * @author Tony Cook
 * @since Nov 8, 2010
 */

public interface ProfileProvider 
{
	public boolean setFile(String fielepath);
	public void openFile() throws IOException;
	public void closeFile();

	//   Removed getNextProfile in favor of loadProfiles- also took out 
	//   momentNumber in reading.  XPR may have to deal with that for performance
//	public Profile getNextProfile(int momentNum);
	public void loadProfiles(int numProfiles);
	public List<Profile> getProfiles();
	
	public double getPulsePeriod();
	public void setUserStartTime(double time);
}
