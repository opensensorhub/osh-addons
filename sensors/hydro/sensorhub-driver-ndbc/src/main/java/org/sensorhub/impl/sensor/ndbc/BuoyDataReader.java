package org.sensorhub.impl.sensor.ndbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


public class BuoyDataReader 
{
	static final String MISSING = "MM";
	
	public static List<BuoyDataRecord> read(String url) throws IOException {

		List<BuoyDataRecord> recs = new ArrayList<>();
		
		try {
			URL dataUrl = new URL(url);
			BufferedReader reader = new BufferedReader(new InputStreamReader(dataUrl.openStream()));
			// Skip Header lines
			reader.readLine();
			reader.readLine();
			while(true) {
				String inline = reader.readLine();
				try {
					if(inline == null)  break;
					if(inline.trim().length() == 0)  continue;
				
					String [] values = inline.split("\\s+");
				// #STN       LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE
				// #text      deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     f
					BuoyDataRecord rec = new BuoyDataRecord();
					rec.id = values[0];
					rec.lat = parseDouble(values[1]);
					rec.lon = parseDouble(values[2]);
					// time 
					int year = parseInt(values[3]);
					int month = parseInt(values[4]);
					int day = parseInt(values[5]);
					int hour = parseInt(values[6]);
					int minute = parseInt(values[7]);
					LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, minute);
					ZonedDateTime zdt = ldt.atZone(ZoneId.of("GMT"));
					rec.timeMs = zdt.toInstant().toEpochMilli();
					rec.windDir = parseInt(values[8]);
					rec.windSpeed = parseDouble(values[9]);
					rec.windGust = parseDouble(values[10]);
					rec.wvht = parseDouble(values[11]);
					rec.dpd = parseDouble(values[12]);
					rec.apd = parseDouble(values[13]);
					rec.mwd = parseDouble(values[14]);
					rec.pressure = parseDouble(values[15]);
					rec.ptdy = parseDouble(values[16]);
					rec.airTemp = parseDouble(values[17]);
					rec.waterTemp = parseDouble(values[18]);
					rec.dewPt = parseDouble(values[19]);
					rec.visibility = parseDouble(values[20]);
					rec.tide = parseDouble(values[21]);
					recs.add(rec);
				} catch (Exception e) {
					// If any exception, skip this record and continue
					e.printStackTrace(System.err);
				}
			}

			return recs;
		} catch(Exception e) {
			throw new IOException("Error reading data file: " + url, e);
		}
	}
	
	public static Double parseDouble(String s) {
		s = s.trim();
		if(s.length() == 0)
			return null;
		if(MISSING.equals(s)) {
			return null;
		}
		try {
			 Double val = Double.parseDouble(s);
			 return val;
		} catch (NumberFormatException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static Integer parseInt(String s) {
		s = s.trim();
		if(s.length() == 0)
			return null;
		if(MISSING.equals(s)) {
			return null;
		}
		try {
			 Integer val = Integer.parseInt(s);
			 return val;
		} catch (NumberFormatException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static void testReader() throws IOException {
		String dataFile = "https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt"; 
		System.out.println("Reading data from " + dataFile);
		List<BuoyDataRecord> recs  = read(dataFile);
		for(BuoyDataRecord rec: recs) {
			System.out.println(rec);
		}
	}
}
