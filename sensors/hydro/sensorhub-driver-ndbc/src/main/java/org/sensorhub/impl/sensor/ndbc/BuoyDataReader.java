package org.sensorhub.impl.sensor.ndbc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */

public class BuoyDataReader 
{
	static final String MISSING = "MM";
	static final Logger logger = LoggerFactory.getLogger(BuoyDataReader.class);
	
	public static List<BuoyDataRecord> read(String url) throws IOException {
		List<BuoyDataRecord> recs = new ArrayList<>();
		URL dataUrl = new URL(url);
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(dataUrl.openStream()))) {
			// Skip Header lines
			String header = reader.readLine();
			boolean headerOk = checkHeader(header);
			if(!headerOk) {
				throw new IOException("Unexpected header format: " + header);
			}
			String units = reader.readLine();
			boolean unitsOk = checkUnits(units);
			if(!unitsOk) {
				throw new IOException("Unexpected units format: " + units);
			}
			while(true) {
				String inline = reader.readLine();
				try {
					if(inline == null)  break;
					if(inline.isBlank())  continue;
				
					String [] values = inline.split("\\s+");
					//	#STN       LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE
					//	#text      deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     ft
					BuoyDataRecord rec = new BuoyDataRecord();
					rec.id = values[0];
					rec.lat = parseDouble(values[1]);
					rec.lon = parseDouble(values[2]);
					// time 
					Integer year = parseInt(values[3]);
					Integer month = parseInt(values[4]);
					Integer day = parseInt(values[5]);
					Integer hour = parseInt(values[6]);
					Integer minute = parseInt(values[7]);
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
					logger.error("Error parsing line: {}" , inline, e);
				}
			}

			return recs;
		} catch(Exception e) {
			throw new IOException("Error reading data file: " + url, e);
		}
	}
	
	//	#STN       LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE
	public static boolean checkHeader(String header) {
		// TODO check fields are as expected
		String [] fields = header.split("\\s+");
		return (fields.length == 22);
	}
	
	//	#text      deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     ft
	public static boolean checkUnits(String units) {
		String [] fields = units.split("\\s+");
		// TODO check units are as expected
		return (fields.length == 22);
	}

	// Default URL is https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt
	public static boolean testObsFileIsReachable(String url) throws IOException {
		URL dataUrl = new URL(url);
		try(InputStream is = dataUrl.openStream()) {
			return true;
		} catch (IOException e) {
			logger.error("URL {} is not reachable" , url, e);
			return false;
		}
	}

	public static Double parseDouble(String s) {
		s = s.trim();
		if(s.isEmpty())
			return null;
		if(MISSING.equals(s)) {
			return null;
		}
		try {
			 return Double.parseDouble(s);
		} catch (NumberFormatException e) {
			logger.error("", e);
			return null;
		}
	}

	public static Integer parseInt(String s) {
		s = s.trim();
		if(s.isEmpty())
			return null;
		if(MISSING.equals(s)) {
			return null;
		}
		try {
			 return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			logger.error("", e);
			return null;
		}
	}

	public static void testReader() throws IOException {
		String dataFile = "https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt"; 
		logger.debug("Reading data from {}", dataFile);
		List<BuoyDataRecord> recs  = read(dataFile);
		for(BuoyDataRecord rec: recs) {
			logger.debug(rec.toString());
		}
	}
}
