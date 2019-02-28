package org.sensorhub.impl.sensor.nexrad;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


/**
 * <p>Title: NexradUtil.java</p>
 * <p>Description:  Some stuff for accessing site info and scan start times.  Probably will move this around for SensorHub project</p>
 * @author Tony Cook
 * @since Jul 15, 2011
 */

public class NexradUtil
{	
	private static NexradTable nexradTable;

	private NexradUtil() {
	}

	public static NexradSite getSite(String id){
		if(!ensureTable())
			return null;
		if(id.length() == 3) {
			id = "K" + id;  // Hawaii radars start with P, so this is really a bad assumption.  Could just force clients to use 4 character id
		}
		return nexradTable.getSite(id);
	}

	public static Collection <NexradSite> getAllSites() {
		if(!ensureTable())
			return null;
		return nexradTable.getAllSites();
	}

	private static boolean ensureTable() {
		try {
			if(nexradTable == null)
				nexradTable = NexradTable.getInstance();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("NexradUtil.getSite():  NexradTable could not be instantiated- see stack trace.");
			return false;
		}

		return true;
	}

	//  A more reliable way to get startTime than what UcarUtil provides.  Specifically, 
	//  NWS files for KTLX and KDGF did not work with the UCAR libs.  This is adapted 
	//  from WCT DecodeL3_Header class.  Needs more testing and it could be made more 
	//  efficient.  I elected to not use DecodeL3_header directly as it loads a bunch 
	//  of static resources upon initialization.  I should clean up his decoder and use 
	//  it for more stuff!
	public static long getScanStartTime(String path) throws IOException {

		DataInputStream is = null;
		try {
			is = new DataInputStream(new FileInputStream(path));

			while (is.readShort() != -1) {
				System.err.println("");
			}
			//         System.out.println("--FIRST BREAKPOINT-- FILE POINTER LOCATION = " + is.getFilePointer());
			double lat = is.readInt() / 1000.0;
			double lon = is.readInt() / 1000.0;

			// Decode Radar Site Altitude
			short alt = is.readShort();
			// Get product code )
			short pcode = is.readShort();

			//----
			// Get operational mode
			short opmode = is.readShort();
			// Get volume coverage pattern
			short vcp = is.readShort();
			// Get sequence number
			short seqnumber = is.readShort();
			// Get volume scan number
			short scannumber = is.readShort();
			// Get volume scan date
			short scandate = is.readShort();
			// Get volume scan time
			int scantime = is.readInt();
			// Get product generation date
			short gendate = is.readShort();
			// Get product generation time
			int gentime = is.readInt();
			int yyyymmdd = NexradUtil.convertJulianDate(scandate);
			String str = new Integer(yyyymmdd).toString();
			int year = Integer.parseInt(str.substring(0, 4));
			int month = Integer.parseInt(str.substring(4, 6));
			int day = Integer.parseInt(str.substring(6, 8));
			int hour = scantime/3600;
			int minute = (scantime/60)%60;
			int seconds = scantime - hour*3600 - minute*60;
			Calendar prodGenCal = Calendar.getInstance();
			prodGenCal.setTimeZone(TimeZone.getTimeZone("GMT"));
			prodGenCal.set(year, month-1, day, hour, minute, seconds);
			prodGenCal.set(Calendar.MILLISECOND, 0);

			return prodGenCal.getTimeInMillis();
		} catch (IOException e) {
			throw e;
		} finally {
			if(is != null)
				try {
					is.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	/**
	 *  Converts julian date (days since 1970) into YYYYMMDD format integer
	 *
	 * @param  jdate  Julian Date
	 * @return        Date in YYYYMMDD format
	 *    Taken from WCT toolkit source
	 */
	public static int convertJulianDate(int jdate) {

		int days[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
		int year;
		int month = 0;
		int ndays;
		int day = 0;
		int total;
		int len;
		int i;
		boolean leap_year;

		total = 0;
		for (year = 1970; year < 2098; year++) {
			leap_year = false;
			if (year % 4 == 0) {
				leap_year = true;
			}

			for (month = 0; month < 12; month++) {
				total = total + days[month];
				if (month == 1 && leap_year) {
					total++;
				}
				if (total >= jdate) {
					ndays = days[month];
					if (month == 1 && leap_year) {
						ndays++;
					}
					day = ndays - (total - jdate);
					month = month + 1;
					return (year * 10000 + month * 100 + day);
				}
			}
		}
		return -999;
	}
	// END convertJulianDate

	//  Modified from DecodeL3Header from WCT.  I needed a fast way to get the scan time
	//  out of NIDS format for NOAAport processing/rendering nexrad data, and this is much 
	//  faster than using Netcdf Java from UCAR.  It is very specific to NIDS format though
	//  and not tested much. As long as that format doesn't change should be okay
	public static long getNidsScanTime(File f) throws IOException {
		DataInputStream is = null;
		is = new DataInputStream(new FileInputStream(f));
		byte[] awips = new byte[61];

		is.read(awips);

		// Decode Lat and Lon
		double  lat = is.readInt() / 1000.0;
		double lon = is.readInt() / 1000.0;
		double alt = is.readShort();
		// Get product code 
		is.readShort();
		is.readShort();
		// Get volume coverage pattern
		is.readShort();
		// Get sequence number
		is.readShort();
		// Get volume scan number
		is.readShort();
		// Get volume scan date (days since 1970)
		short scandate = is.readShort();
		// Get volume scan time (seconds of day_
		int  scantime = is.readInt();
		is.close();
		return ((scandate-1) * TimeUnit.DAYS.toMillis(1)) + (scantime * 1000);
	}

	public static void main(String[] args) throws Exception {
		String nidsPath = "C:/Users/tcook/root/AnythingWx/noaaport/radar/n0qgrk_20140509_1757.nids";
		long time = NexradUtil.getNidsScanTime(new File(nidsPath));
	}
}
