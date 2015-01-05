package org.senshorhub.impl.sensor.profiler915;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * <p>Title: Profiler915_Util.java</p>
 * <p>Description:  </p>
 * @author Tony Cook
 * @since Sep 14, 2010
 */

public class Profiler915_Util
{
	public static int getMaxGates(List<Profile> profiles) {
		int maxGates = 0;
		int numGates;
		for(Profile profTmp: profiles) {
			numGates = profTmp.getNumGates();
			maxGates = (numGates>maxGates) ? numGates : maxGates;
			//			System.err.println("maxGates, Ht " + maxGates + " " + profTmp.getMaxHeight());
		}
		return maxGates;
	}

	public static double getMaxHeight(List<Profile> profiles) {
		double maxHt = 0.0;
		double htTmp;
		for(Profile profTmp: profiles) {
			htTmp = profTmp.getMaxHeight();
			maxHt = (htTmp>maxHt) ? htTmp : maxHt;
		}
		return maxHt*1000.0;
	}
	
	public static int getGateIndex(double htKm, List<Profile> profiles) {
		//  Go thru all profiles and find *highest* gate index for requested ht
		//  Needed for when numGates changes in the mddle of a file
		int maxGateNum = 0;
		int gateTmp;
		for(Profile profTmp: profiles) {
			gateTmp = profTmp.getGateNum(htKm);
			maxGateNum = (gateTmp > maxGateNum) ? gateTmp : maxGateNum;
		}
		
		return maxGateNum;
	}
	
	public static double [] getTimes(List<Profile> profiles){
		if(profiles == null)
			return null;
		double [] time = new double[profiles.size()];
		int cnt = 0;

		for(Profile profTmp:profiles){
			time[cnt++] = (double)profTmp.getTime();
		}
		
		return time;
	}
	
	public static int getMaxWindGates(List<WindRecord> windRecords) {
		int maxGates = 0;
		int numGates;
		for(WindRecord wrTmp: windRecords) {
			numGates = wrTmp.getWindData().size();
			maxGates = (numGates>maxGates) ? numGates : maxGates;
		}
		return maxGates;
	}

	public static double getMaxWindHeight(List<WindRecord> windRecords) {
		double maxHt = 0.0;
		double htTmp;
		for(WindRecord wrTmp: windRecords) {
			htTmp = wrTmp.getMaxHeight();
			maxHt = (htTmp>maxHt) ? htTmp : maxHt;
		}
		return maxHt*1000.0;
	}

	public static double [] getWindStartStopTimes(List<WindRecord> windRecords) {
		double [] times = new double[2];
		WindRecord wrTmp = windRecords.get(0);
		times[0] = wrTmp.getTime();
		wrTmp = windRecords.get(windRecords.size() - 1);
		times[1] = wrTmp.getTime();

		return times;
	}

	public static double [] getWindTimes(List<WindRecord> windRecords) {
		double [] times = new double[windRecords.size()];
		int i=0;
		for(WindRecord wrTmp: windRecords) {
			times[i++] = wrTmp.getTime();
		}

		return times;
	}
	
	public static String buildDefaultCurrentOutputName(String momentStr){
		String fn = "915"  + momentStr + "_current.png";
		
		return fn;
	}
	
	public static String buildDefaultFullOutputName(String infile, String momentStr){
		int lastSlashIdx = infile.lastIndexOf(System.getProperty("file.separator"));
		String infileShort = infile.substring(lastSlashIdx + 1);
		String yrStr = "20" + infileShort.substring(1,3);  // y2k
		int yr = Integer.parseInt(yrStr);
		String doyStr = infileShort.substring(3,6);
		int doy = Integer.parseInt(doyStr);
		Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.set(Calendar.YEAR, yr);
		cal.set(Calendar.DAY_OF_YEAR, doy);
		int month = cal.get(Calendar.MONTH) + 1;
		int dom = cal.get(Calendar.DAY_OF_MONTH);

		String fn = "D" + getTwoDigitInt(month) + getTwoDigitInt(dom) + yrStr + "_" + momentStr + "_full.png";
		
		return fn;
	}
	
	public static String buildDefaultInputName(){
		//  D10094a.SPC
		//  Dyyddda.SPC
		long currentTime = System.currentTimeMillis();
		Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis(currentTime);
		int yr = cal.get(Calendar.YEAR) - 2000;
		int doy = cal.get(Calendar.DAY_OF_YEAR);
		String yrStr = getTwoDigitInt(yr);
		String doyStr = getThreeDigitInt(doy);
		String fname = "D" + yr + doyStr + "a.SPC";  //  account for .b, .c, etc?
		
		return fname;
	}
	
	//  Roll the day and return resulting valid 915 filename
	public static String getAdjacentDayPath(String filename, int upDown) {
		File f = new File(filename);
		String dir = "";
		String fileTmp = f.getName();
		
		String yrStr = fileTmp.substring(1,3);
		int yr = Integer.parseInt(yrStr) + 2000;
		String doyStr = fileTmp.substring(3,6);
		int doy = Integer.parseInt(doyStr);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis(0);
		
		cal.set(Calendar.YEAR, yr);
		cal.set(Calendar.DAY_OF_YEAR, doy + upDown);
		int newYear = cal.get(Calendar.YEAR) - 2000;
		int newDoy = cal.get(Calendar.DAY_OF_YEAR);
		String returnFile = dir + "D" + getTwoDigitInt(newYear) + getThreeDigitInt(newDoy) + "a.SPC";
		
		return returnFile;
	}
	
	public static double getFileStartTime(String filename){

		int lastSlash = filename.lastIndexOf(System.getProperty("file.separator"));
		if(lastSlash != -1) {
			filename = filename.substring(lastSlash + 1, filename.length());
		}
			
		
		String yrStr = filename.substring(1,3);
		int yr = Integer.parseInt(yrStr) + 2000;
		String doyStr = filename.substring(3,6);
		int doy= Integer.parseInt(doyStr);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis(0);
		cal.set(Calendar.YEAR, yr);
		cal.set(Calendar.DAY_OF_YEAR, doy);
		
		return (double)cal.getTimeInMillis()/1000.0;
	}
	
	//  Should check for exists also? 
	//  Convert data filepath to windfilepath (assumes both data and wind in same dir- reasses this assumption after testing)
	//  
	public static String getWindPath(String file){
	//  Also need to build windFilepath here
		String slash = System.getProperty("file.separator");
		int lastSlashIdx = file.lastIndexOf(slash);
		int lastDotIdx = file.lastIndexOf(".");

		char [] windC = file.toCharArray();
		windC[lastSlashIdx+1] = 'W';
		String tempStr = new String(windC);
		tempStr = tempStr.substring(0, lastDotIdx-1);
		//  Dicey way to build filename- assumes 'a' or 'b' in data file name - ck. for consistency w/Dustin
		String windFilepath = new String(tempStr + ".CNS");
		return windFilepath;
	}
	
	//  Convert relative Profiler data filename () to wind filename - no paths!
	//  Use for Batcher, so Dustin can specify windDir separately
	public static String getWindFilename(String filename) {
		char [] windC = filename.toCharArray();
		windC[0] = 'W';
		String wtmp = new String(windC);
		int dotIdx = wtmp.lastIndexOf('.');
		String wfile = wtmp.substring(0, dotIdx-1) + ".CNS";
		return new String(wfile);
	}
	
	
	public static String getTwoDigitInt(int itmp){
		if (itmp >=10)
			return "" + itmp;
		else 
			return "0" + itmp;
			
	}
	
	public static String getThreeDigitInt(int doy){
		if (doy >=100)
			return "" + doy;
		else if (doy >= 10)
			return "0" + doy;
		else
			return "00" + doy;
	}
	
	public static void main(String[] args) {
		String testPath = "C:\\users\\tcook\\root\\kk\\915\\data\\D10094a.SPC";
		testPath = "D09001a.SPC";
		String nextDay = Profiler915_Util.getAdjacentDayPath(testPath, -1);
		System.out.println("Next: " + nextDay);
	}
}
