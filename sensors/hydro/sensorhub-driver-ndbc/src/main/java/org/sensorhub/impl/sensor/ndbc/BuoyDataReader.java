/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

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
 * @author Tony Cook
 */

public class BuoyDataReader 
{
	static final String MISSING = "MM";
	static final Logger logger = LoggerFactory.getLogger(BuoyDataReader.class);
	static final String expectedFields = "#STN       LAT      LON  YYYY MM DD hh mm WDIR WSPD   GST WVHT  DPD APD MWD   PRES  PTDY  ATMP  WTMP  DEWP  VIS   TIDE";
	static final String expectedUnits = "#text      deg      deg   yr mo day hr mn degT  m/s   m/s   m   sec sec degT   hPa   hPa  degC  degC  degC  nmi     ft";
	
	public static List<BuoyDataRecord> read(String url) throws IOException {
		List<BuoyDataRecord> recs = new ArrayList<>();
		URL dataUrl = new URL(url);
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(dataUrl.openStream()))) {
			String fields = reader.readLine();
			boolean fieldsOk = checkFields(fields);
			if(!fieldsOk) {
				logger.warn("Fields in header do not match expected fields:\n\t{}", fields);
			}
			String units = reader.readLine();
			boolean unitsOk = checkUnits(units);
			if(!unitsOk) {
				logger.warn("Units in header do not match expected units:\n\t{}", units);
			}
			while(true) {
				String inline = reader.readLine();
				try {
					if(inline == null)  break;
					if(inline.isBlank())  continue;
				
					String [] values = inline.split("\\s+");
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
	
	public static boolean checkFields(String fields) {
		fields = fields.trim();
		return fields.equalsIgnoreCase(expectedFields);
	}
	
	public static boolean checkUnits(String units) {
		units = units.trim();
		return units.equalsIgnoreCase(expectedUnits);
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
			logger.error(e.getMessage(), e);
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
			logger.error(e.getMessage(), e);
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
