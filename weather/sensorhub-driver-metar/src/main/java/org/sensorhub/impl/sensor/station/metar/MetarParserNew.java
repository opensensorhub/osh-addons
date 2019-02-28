package org.sensorhub.impl.sensor.station.metar;


import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Arrays;

import org.sensorhub.impl.sensor.station.metar.MetarConstants.Modifier;
import org.sensorhub.impl.sensor.station.metar.RunwayVisualRange.Range;


/**
 * 
 * @author tcook
 *  Slash strings from Gen3.5: 
 *  It means "A report from a fully automated AWS that does not include information from sensors for 
 *  visibility, weather or cloud will report ///, // or ///// respectively in lieu of these parameters."

 *
 */

public class MetarParserNew 
{
	// Records should already be cleaned (MetarUtil) before being sent here
	public Metar parseMetar(String line) {
		Metar metar = new Metar();
		metar.reportString = line;
		System.err.println(line);

		try {
			String [] sarr = line.split(" ");
			int fieldCnt = 0;

			//  if first field is METAR/SPECI, record that.  Some of our sources don't include this always
			String s = sarr[fieldCnt];

			if(s.equals("METAR"))  fieldCnt++;  //we can ignore this
			if(s.equals("SPECI"))  {
				metar.isSpeci = true;
				fieldCnt++;
			}

			// next field must be stationID
			//			System.err.println("Must be station: " + sarr[fieldCnt]);
			metar.stationID = sarr[fieldCnt++];

			// next field must be date: ddhhmmZ
			//			System.err.println("Must be date: " + sarr[fieldCnt]);
			metar.dateString = sarr[fieldCnt++];

			// AUTO or COR optional
			s = sarr[fieldCnt];
			if(s.equals("AUTO") ) {
				fieldCnt++;  // ignore
			} else if(s.equals("COR") ) {
				metar.isCorrection = true;
				fieldCnt++;
			} else if(s.startsWith("CC") && s.length() == 3) {  //  CCA CCB etc.- Canadian metar correction flag
				metar.isCorrection = true;
				fieldCnt++;
			}

			// Wind string(s)
			// TODO - support MPS
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else if(s.endsWith("KT") || s.endsWith("MPS")) {
				parseWind(metar, s);
				s = sarr[++fieldCnt];
				if(s.length() == 7 && s.charAt(3) == 'V') {
					parseVariableWindDir(metar, s);
					fieldCnt++;
				}
			}

			//  Vis - /// indicates missing in most refs I Could find
			//  international seems mostly to use 4 digit meters value without M appended,
			//  but can technically have 8 compass directions abbv.  appended (N,NE...NW)
			//  and can also theoretically have a second value to indicate min and max
			//  TODO support if this case ever arises in the real world. For now,
			//       sticking with a single value in meters for int'l
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else if((s.length() == 4 && isNumeric(s)) || (s.length() == 5 && isNumeric(s.substring(0,4)) && s.endsWith("M"))) {  //  International in Meters
				if(s.equals("9999")) {
					metar.setVisibilityKilometers(10.0);
				} else {
					double meters = Double.parseDouble(s.substring(0, 4));
					metar.setVisibilityKilometers(meters/1000.0);
				}
				fieldCnt++;
			} else if(s.equals(MetarConstants.CAVOK)) {  
				metar.isCavok = true;
				metar.setVisibilityKilometers(10.0);
				fieldCnt++;
			} else if (s.endsWith("M")) { // either KM or SM- think this is safe but could be conditions where it fails
				parseVis(metar, sarr[fieldCnt]);
				fieldCnt++;
			} else if (fieldCnt+1 < sarr.length && sarr[fieldCnt + 1].endsWith("M")) {
				parseVis(metar, s, sarr[fieldCnt + 1]);
				fieldCnt += 2;
			} 
			//			System.err.println(metar.getVisibilityMiles());
			//			System.err.println(metar.getVisibilityKilometers());
			//			System.err.println(metar.isCavok);
			//			System.err.println(metar.visibilityLessThan);

			//  RunwayVis- can have multiple in theory
			if(fieldCnt >= sarr.length)
				return metar;
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else { 
				while(!s.equals("RMK") && !s.equals("RA") && s.startsWith("R")) {  // Not ReMarK and not RAin- should be runway
					//				System.err.println(sarr[fieldCnt]);
					metar.addRunwayVisualRange(parseRunwayVis(sarr[fieldCnt++]));
					if(fieldCnt >= sarr.length)
						return metar;
					s = sarr[++fieldCnt];
				}
			}

			// Present Weather
			if(fieldCnt >= sarr.length)
				return metar;
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else {
				while(PresentWeather.isPresentWeather(s)) {
					PresentWeather pw = PresentWeather.parsePresentWeather(s);
					System.err.println("PW: " + s + ": " +   pw);
					metar.addPresentWeather(pw);
					if(fieldCnt >= sarr.length)
						return metar;
					s = sarr[++fieldCnt];
				}
			}


			// Cloud Cover
			if(fieldCnt >= sarr.length)
				return metar;
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else {
				while(SkyCondition.isSkyCondition(s)) {
					SkyCondition sc = SkyCondition.parseSkyCondition(s);
					System.err.println("SC: " + s + ": " +   sc);
					metar.addSkyCondition(sc);
					if(fieldCnt >= sarr.length)
						return metar;
					s = sarr[++fieldCnt];
				}
			}

			// Temp/dewPt  (T'T'/T'dT'd) - prefixed with M means 'minus'
			//  12/7  M4/M2
			//  23/  M4/  Missing dewpt
			//  /14  /M3   Missing tmp
			if(fieldCnt >= sarr.length)
				return metar;
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else if(s.contains("/")) {
				int slashIdx = s.indexOf('/');
				if(slashIdx == 0)  {// dewPt only
					String d = s.substring(1);
					metar.setDewPointC(parseTempDew(d));
				} else if (slashIdx == s.length() - 1) {  // temp only
					String d = s.substring(0, slashIdx);
					metar.setTemperatureC(parseTempDew(d));
				} else {  // T and Td
					String [] tdstr = s.split("/");
					metar.setTemperatureC(parseTempDew(tdstr[0]));
					metar.setDewPointC(parseTempDew(tdstr[1]));
				}
				s = sarr[++fieldCnt];
				System.err.println("T/TD: " + metar.getTemperature() + "/" + metar.getDewPoint());
			}

			// Altimeter APPPP - Altimeter in inches of Mercury * 100 (mainly US)
			if(fieldCnt >= sarr.length)
				return metar;
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else 	if(s.startsWith("A")) {
				s = s.substring(1);
				metar.altimeter = Integer.parseInt(s) / 100.0;
				fieldCnt++;
				System.err.println("Alt: " + metar.altimeter);
			}

			// Pressure QPPPP - Pressure in hPa/mb  (mainly interntnl)
			if(fieldCnt >= sarr.length)
				return metar;
			s = sarr[fieldCnt];
			if(isMissing(s)) {
				fieldCnt++;
			} else 	if(s.startsWith("Q")) {
				s = s.substring(1);
				metar.pressure = Double.parseDouble(s);
				fieldCnt++;
				System.err.println("Pressure: " + metar.pressure);
			}

			// Remarks - RMK
			if(fieldCnt >= sarr.length)
				return metar;
			s = sarr[fieldCnt];
			if(s.equals("RMK")) {
				String [] subarr = Arrays.copyOfRange(sarr, ++fieldCnt, sarr.length); 
				parseRemarks(metar, subarr);
			}

			System.err.println("");
		} catch (Exception e) {
			System.err.println("FAILED: " + line);
			e.printStackTrace(System.err);
		}
		return metar;
	}

	//  There are numerous possible fields and variations of encodings in the remarks section
	//  Initially I am only going to support THE FOLLOWING:
	//  SLP (Pressure)  :  SLPppp - pressure in 10ths of mb- SLP982 = 998.2; SLP142 = 1014.2;  SLP426 = 1042.6
	//  temp/dew        :  Tttttdddd - temp/dew in 10ths Celcius; first digit 1 is negative
	//  wind gust		:  PK WND dddff(f)/(hh)mm - ddd=direction; ff(f)= speed; (hh)mm= hour/minute
	//  hourly precip   :  Ppppp - precip in 100ths of inches - should P000 be trace?
	//  24 hour precip  :  7pppp - 24 hour precip (TODO) 
	public static void parseRemarks(Metar metar, String [] sarr ) {
		System.err.print("REMARKS: ");
		int fieldCnt = 0;
		for(int i=fieldCnt; i<sarr.length; i++) {
			//			System.err.println(sarr[i]);
			if(sarr[i].startsWith("SLP")) {
				if(sarr[i].length() == 6) {
					// assumes 900 < P < 1089.9 - I think this is reasonable and have to assume something to parse 
					String ps = sarr[i].substring(3, 6);
					if(ps.startsWith("9"))
						metar.pressure = Double.parseDouble(ps)/10. + 900.;  
					else 
						metar.pressure = Double.parseDouble(ps)/10. + 1000.;
					System.err.println("Prss: " + metar.pressure + "... " + sarr[i]);
				}
			} else if(sarr[i].startsWith("T")) {
				if(sarr[i].length() == 9) {  // T/Td in 10ths
					String ts = sarr[i].substring(1,5);
					String td = sarr[i].substring(5,9);
					double valT = Double.parseDouble(ts.substring(1)) / 10.0;
					double valTd = Double.parseDouble(td.substring(1)) / 10.0;
					double tempP = ts.charAt(0) == '1' ? -1. * valT : valT;
					metar.setTemperaturePrecise(tempP);
					double dewP = td.charAt(0) == '1' ? -1. * valTd : valTd;
					metar.setDewPointPrecise(dewP);

					System.err.println("Prc T/Td: " + metar.getTemperature() + "/" + metar.getDewPoint() + " ... " + sarr[i]);
				} // something else starting with T
			} else if (sarr[i].equals("PK")) {
				if(sarr[i+1].equals("WND" )) {
					i+=2;
					String pws = sarr[i];
					int slashIdx = pws.indexOf('/');
					if(slashIdx == -1 )
						continue;
					String dirsp = pws.substring(0, slashIdx);
					metar.windDirectionGust = Integer.parseInt(dirsp.substring(0,3));
					metar.windGust = Double.parseDouble(dirsp.substring(3));
					String ts = pws.substring(slashIdx + 1);
					Integer hr = null, min = null;
					if(ts.length() == 2) {
						min = Integer.parseInt(ts);
					} else {
						hr = Integer.parseInt(ts.substring(0, 2));
						min = Integer.parseInt(ts.substring(2));
					}
					//  TODO set peakWind time
					System.err.println("PeakWind: " + pws+ " ... " + metar.windDirectionGust + "/" + metar.windGust + "/" + hr + ":" + min);
				}
			} else if(sarr[i].startsWith("6") && sarr[i].length() == 5 && isNumeric(sarr[i])) {
				System.err.println("Six hour precip: " + sarr[i]);
			} else if(sarr[i].startsWith("7") && sarr[i].length() == 5 && isNumeric(sarr[i].substring(1))) {
				System.err.println("24 hr Prcp: " + sarr[i]);
			} else if(sarr[i].startsWith("P") && sarr[i].length() == 5 && isNumeric(sarr[i].substring(1))) {
				metar.hourlyPrecipInches = Double.parseDouble(sarr[i].substring(1)) / 100.0;
				System.err.println("Hourly Prcp: " + sarr[i] + " ... " + metar.hourlyPrecipInches);
			} else if(sarr[i].startsWith("PCPN")) {
				System.err.println("PCPN: " + sarr[i]);
			}
		}

	}

	public static Double parseTempDew(String s) {
		double sign = 1.;
		if(s.length() > 1 && s.startsWith("M")) {
			sign = -1.;
			s = s.substring(1);
		}
		return  sign * Double.parseDouble(s);
	}

	//  1 1/2SM
	//  3/4SM  4800M
	//  9SM
	//  M1/4SM  Less than whatever follows
	public static void parseVis(Metar m, String... s) throws NumberFormatException {
		double val = 0;
		if(s[0].startsWith("M")) {
			m.visibilityLessThan = true;
			val = parseFraction(s[0].substring(1, s[0].length()-2));
		} else if(s.length == 1) {
			val = parseFraction(s[0].substring(0, s[0].length()-2));
		} else {
			double whole = Double.parseDouble(s[0]);
			val = whole + parseFraction(s[1].substring(0, s[1].length()-2));
		}
		if(s[s.length - 1].endsWith("SM"))
			m.setVisibilityMiles(val);
		else if (s[s.length -1].endsWith("KM"))
			m.setVisibilityKilometers(val);
		else 
			throw new NumberFormatException("Cannot Parse Metar Visibility String. Should end in SM or KM: " + s[0]);
	}

	private static double parseFraction(String s) throws NumberFormatException {
		int idx = s.indexOf('/');
		if(idx == -1)
			return Double.parseDouble(s);
		double num = Double.parseDouble(s.substring(0, idx));
		double den = Double.parseDouble(s.substring(idx + 1));
		return num/den;
	}

	//  1 1/2SM
	//  3/4SM  4800M
	//  9SM
	//  M1/4SM  Less than whatever follows
	public static void testVis(String[] args) throws Exception {
		//		System.err.println(parseFraction("1/2"));
		//		System.err.println(parseFraction("6"));

		Metar m = new Metar();
		//		parseVis(m, "4800");  // test with file
		//		parseVis(m, "4800M");  // test with file
		//		parseVis(m, "CAVOK");   // test with file
		//		parseVis(m, "M1/4SM");
		//		parseVis(m, "1/4SM");
		//		parseVis(m, "6SM");
		//		parseVis(m, "1", "1/2SM");
		parseVis(m, "11", "3/8KM");
		System.err.println(m.getVisibilityMiles());
		System.err.println(m.getVisibilityKilometers());
		System.err.println(m.isCavok);
		System.err.println(m.visibilityLessThan);
	}

	/**
	 * Format:	dddkkKT
 	dddkkGkkKT
 	dddkkKT dddVddd
 	VRBkkKT
ddd - the wind direction. 90 degrees is encoded 090.
kk - the wind speed in knots. CAN BE 2 or 3 digits
Gkk - if the wind is gusting, the gust speed in knots is added. (as in theis example).  CAN BE 2 or 3 digits
dddVddd - If the wind direction is variable and the wind speed is greater than 6 knots, give the direction range. e.g. 23013KT 210V250
VRBkk - If the wind speed is variable and less than 6 knots, use VRB05KT.
Example: 36023G33KT 360 degrees heading, 23 kts speed with gusts to 33 kts.
	TODO: ///06KT - support this!
	 **/
	public static final void parseWind(Metar metar, String s) {
		if(s.startsWith("VRB")) {
			metar.windDirectionIsVariable = true;
			String speedStr = s.substring(3, 5);
			metar.setWindSpeed(Double.parseDouble(speedStr));
			//			System.err.println("IS VRB: " + metar.getWindSpeed());
			return;
		}
		int endIdx = s.indexOf("K");  // MPS

		String windStr = s.substring(0, endIdx);
		//		System.err.println(s + ":  " + windStr);
		int endWindIdx;
		if(windStr.contains("G")) {
			endWindIdx = s.indexOf("G");
			int startGustIdx = endWindIdx + 1;
			String gustStr = windStr.substring(startGustIdx, windStr.length());
			//			metar.setWindGusts(Double.parseDouble(gustStr));
			metar.windGust = Double.parseDouble(gustStr);
			//			System.err.println("GUST:  " + gustStr);
		} else {
			endWindIdx = windStr.length();
		}
		String windDirStr = windStr.substring(0, 3);
		String windSpeedStr = windStr.substring(3, endWindIdx);
		metar.windDirection = Integer.parseInt(windDirStr);
		metar.setWindSpeed(Double.parseDouble(windSpeedStr));
		//		System.err.println(windDirStr + "/" + windSpeedStr);
	}

	public static final void parseVariableWindDir(Metar metar, String s) {
		metar.windDirectionIsVariable = true;
		String minDir = s.substring(0, 3);
		String maxDir = s.substring(4, 7);
		metar.windDirectionMin = Integer.parseInt(minDir);
		metar.windDirectionMax = Integer.parseInt(maxDir);
		//		System.err.println("DIR variable: " + metar.windDirectionMin + "/" + metar.windDirectionMax);
	}

	// US Examples: (M or P may precede values to indicate less or greater respectively; always ends in FT)
	//   R26L/2400FT -- Runway 26 Left has a range of 2400 ft. 
	//   R08/0400V0800FT -- Runway 08 has a visual range between 400 and 800 feet.
	// International Examples: (values always meters; M or P as in US; may end in in U,D, or N to indicate Up, Down, or NoChange)
	//   R27/0600U 
	public static RunwayVisualRange parseRunwayVis(String s) throws NumberFormatException {
		int slashIdx = s.indexOf('/');
		if (slashIdx == -1)
			throw new NumberFormatException("No slash in Runway Vis field: " + s);
		RunwayVisualRange rvr = new RunwayVisualRange(s);
		rvr.runwayId = s.substring(0, slashIdx);

		String vis = s.substring(slashIdx + 1, s.length());
		if(vis.endsWith("FT")) {  //  US
			rvr.units = "feet";
			vis = vis.substring(0, vis.length() - 2);
		} else {  // interntnl
			rvr.units = "meters";
		}

		parseRunwayRange(rvr, vis);
		return rvr;
	}

	private static Modifier getModifier(char c) { //throws CharConversionException {
		switch(c) {
		case 'M':
			return Modifier.LESS_THAN;
		case 'P':
			return Modifier.GREATER_THAN;
		case 'U':
			return Modifier.INCREASING;
		case 'D':
			return Modifier.DECREASING;
		case 'N':
			return Modifier.NO_CHANGE;
		default:
			return Modifier.UNKNOWN;
		}
	}

	private static void parseRunwayRange(RunwayVisualRange rvr, String visStr) {
		if(visStr.contains("V")) {  // we have lower and upper bounds
			String [] range = visStr.split("V");
			rvr.lowerRange = parseRange(rvr, range[0]);
			rvr.upperRange = parseRange(rvr, range[1]);
		} else {
			rvr.range = parseRange(rvr, visStr);
		}


	}

	private static Range parseRange(RunwayVisualRange rvr, String s) {
		Range range = rvr.new Range();

		char c = s.charAt(0);
		if(Character.isLetter(c)) {
			range.addModifier(getModifier(c));
			s = s.substring(1);
		}
		c = s.charAt(s.length() - 1);
		if(Character.isLetter(c)) {
			range.addModifier(getModifier(c));
			s = s.substring(0, s.length() - 1);
		}
		range.value = Integer.parseInt(s);
		return range;
	}

	/**
	 * 
	 * @param year
	 * @param month
	 * @param dateStr - standard Metar report date String DDHHMM where DD = day of month, HH = hour of day, MM = minute
	 * @return epoch seconds since 1970-01-01
	 * @throws NumberFormatException
	 */
	public static long computeTimeUtc(int year, int month, String dateStr) throws NumberFormatException {
		if(dateStr.length() != 7)
			throw new NumberFormatException("Invalid dateString: " + dateStr);
		String day = dateStr.substring(0, 2);
		String hour = dateStr.substring(2, 4);
		String minute = dateStr.substring(4, 6);

		int iday = Integer.parseInt(day);
		int	ihour = Integer.parseInt(hour);
		int	iminute = Integer.parseInt(minute);

		LocalDateTime dt = LocalDateTime.of(year, month, iday, ihour, iminute, 0, 0);
		return dt.toEpochSecond(ZoneOffset.UTC);
	}
	
	/**
	 * 
	 * @param dateStr - standard Metar report date String DDHHMM where DD = day of month, HH = hour of day, MM = minute
	 * @return epoch seconds since 1970-01-01
	 * @throws NumberFormatException
	 */
	public static long computeTimeUtc(String dateStr) throws NumberFormatException {
		if(dateStr.length() != 7)
			throw new NumberFormatException("Invalid dateString: " + dateStr);
		String day = dateStr.substring(0, 2);
		int iday = Integer.parseInt(day);

		//  Check for month changeover using system time.  
		//  *Assumes* system clock is correct and always ahead of dateStr day/hour/minute fields
		//  We have to get year and month somehow, as they are not provided in the Metar record format.
		// For archive ingest, year and month must be provided by the caller using computeTimeUtc(year, month, dateStr)
		LocalDateTime dtNow = LocalDateTime.now();
		int year = dtNow.get(ChronoField.YEAR);
		int month = dtNow.get(ChronoField.MONTH_OF_YEAR);
		int dayOfMonth = dtNow.get(ChronoField.DAY_OF_MONTH);
		if(iday > dayOfMonth) {
			if(--month == 0) {
				month = 12;
				--year;
			}
		}
		return computeTimeUtc(year, month, dateStr);
	}
	
	/**
	 * 
	 * @param s
	 * @return true if input string is any string of slashes
	 */
	public static boolean isMissing(String s) {
		for(char c: s.toCharArray()) {
			if(c != '/')
				return false;
		}
		return true;
	}

	public static boolean isNumeric(String s) {
		return s.matches("[-+]?\\d+(\\.\\d+)?");
	}


	public static void testWind(String[] args) throws Exception {
		MetarParserNew mp = new MetarParserNew();
		//		parseWind(new Metar(), "220104KT");
		//		parseWind(new Metar(), "22010KT");
		//		parseWind(new Metar(), "220104G106KT");
		//		parseWind(new Metar(), "220104G16KT");
		//		parseWind(new Metar(), "22014G106KT");
		//		parseWind(new Metar(), "22014G16KT");
		//		parseWind(new Metar(), "VRB05KT");
		parseVariableWindDir(new Metar(), "130V190");
	}

	public static void main(String[] args) throws Exception {
		
		long dt = computeTimeUtc("312359Z");
		long dt2 = computeTimeUtc("010001Z");
		System.err.println(dt);
		System.err.println(LocalDateTime.ofEpochSecond(dt, 0, ZoneOffset.UTC));
		System.err.println(LocalDateTime.ofEpochSecond(dt2, 0, ZoneOffset.UTC));
		
		
//		MetarParserNew mp = new MetarParserNew();
//		//						File infile = new File("C:/Data/station/metar/wxMsg/SAHOURLY.TXT.3065");
//		File infile = new File("C:/Data/sensorhub/metar/SAHOURLY.TXT.2262");
//		//								File infile = new File("C:/Data/station/metar/NAMSA_EURSA_621377820000.TXT");
//		//				File infile = new File("C:/Data/station/metar/testMetar.txt");
//
//		try {
//			List<String> lines = MetarUtil.cleanFile(infile.toPath(), false);
////			lines = Files.readAllLines(infile.toPath(), Charset.defaultCharset());
//
//			for(String l: lines)
//				mp.parseMetar(l);
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

	}
}
