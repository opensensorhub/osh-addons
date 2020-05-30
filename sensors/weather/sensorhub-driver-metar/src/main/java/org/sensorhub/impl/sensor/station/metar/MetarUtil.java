package org.sensorhub.impl.sensor.station.metar;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * <p>Title: MetarUtil.java</p>
 * <p>Description: </p>
 *
 * @author T
 * @date Mar 2, 2016
 */
public class MetarUtil
{
	public static List<String> cleanFile(Path p) throws IOException {
		return cleanFile(p, false);
	}

	public static List<String> getLinesFromFile(String pathToFile) throws IOException {
		assert Files.exists(Paths.get(pathToFile));
		List<String> lines = Files.readAllLines(Paths.get(pathToFile),
				Charset.defaultCharset());
		return lines;
	}

	public static List<String> cleanFile(Path p, boolean isMarta) throws IOException {
		List<String> lines = getLinesFromFile(p.toString());
		List<String> linesOut = new ArrayList<>();
		boolean multiLine = false;
		StringBuilder multiLineBuffer = new StringBuilder(); 
		int lineCnt = -1;
		for(String line:lines) {
			if(++lineCnt < 7 && isMarta)  continue;  
			if(line.trim().length() == 0)  continue;
			String [] tokens = line.split(" ");
			if(!tokens[0].startsWith("METAR") && !tokens[0].startsWith("SPECI") 
					&& tokens[0].length() != 4 && !multiLine)
				continue;
			line = line.trim();
			if(multiLine) 
				multiLineBuffer.append(" ");
			multiLineBuffer.append(line);
			if(line.endsWith("=")) {
				if(multiLineBuffer.toString().length() > 4 && !line.endsWith("NIL=")) {
					multiLineBuffer.deleteCharAt(multiLineBuffer.length() - 1);  // get rid of the '=' terminating sign 
					linesOut.add(multiLineBuffer.toString());
				}
				multiLineBuffer = new StringBuilder();
				multiLine = false;
			} else {
				multiLine = true;
			}

		}
		// check if anything in multilineBuffer- last line in single entry wxMsg files does not end with '='
		// !!!
		if(multiLineBuffer.length() > 5)
			linesOut.add(multiLineBuffer.toString());


		return linesOut;
	}

	public static boolean isAlpha(String name) {
		return name.matches("[a-zA-Z]+");
	}

	public static final double f_to_c(double tempF) {
		return (tempF - 32.0) * 5.0 / 9.0;
	}

	public static final double c_to_f(double tempC) {
		return (tempC*1.8) + 32.0;
	}

	public static double computeRH(double tempC, double dewPtC) {
		double e = computeVaporPressure(dewPtC);
		double es = computeVaporPressure(tempC);
		double rh = e/es;
		return rh * 100.;
	}

	/**
	 * 
	 * @param tempC
	 * @return   Vapor Pressure in HPa
	 */
	public static double computeVaporPressure(double tempC) {
		//  Clauss/Clap
		//	    L = 2.453 * 10**6 J/kg, Rv = 461 J/kg. /Rv = 5321.04
		//	    LN(Es/6.11) = (L/Rv )(1/273 - 1/T)
		//	    es = 6.11 * exp( (L/Rv) * (1/273 - 1T )
		//		double tempK = tempC + 273.15;
		//		double A =  5321.04 * ( (1.0/273.0) - (1.0/tempK) );
		//		double e = 6.11 * Math.exp(A);

		//  Modified CC
		double A =  (7.5 * tempC) / (237.3 + tempC);
		double e = 6.11 * Math.pow(10.0, A);
		return e;
	}

	/**
	 * 
	 * @param year
	 * @param month
	 * @param dateStr - standard Metar report date String DDHHMMZ where DD = day of month, HH = hour of day, MM = minute
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

		// Java 8
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
		//  *Assumes* system clock is correct and always ahead of dateStr day/hour/minute fields, which *should* always be the case
		// For archive ingest, year and month must be provided by the caller using computeTimeUtc(year, month, dateStr)
		// JAVA 8
		//		LocalDateTime dtNow = LocalDateTime.now();
		//		int year = dtNow.get(ChronoField.YEAR);
		//		int month = dtNow.get(ChronoField.MONTH_OF_YEAR);
		//		int dayOfMonth = dtNow.get(ChronoField.DAY_OF_MONTH);

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		//		Calendar cal  = TimeUtil.getGMTCalendar(2017, 0, 1, 0, 0, 0);
		//		Calendar cal  = TimeUtil.getGMTCalendar(2017, 11, 1, 0, 0, 0);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

		if(iday > dayOfMonth) {
			if(--month == 0) {
				month = 12;
				--year;
			}
		}
		return computeTimeUtc(year, month, dateStr);
	}

	public static double millibarsToInches(double pressureMb) {
		return  (29.92 * pressureMb) / 1013.25;
	}
	
	public static double inchesToMillibars(double pressureInches) {
		return (pressureInches/29.92) * 1013.25;
	}

	public static void main_(String[] args) throws Exception {
		//		System.err.println(getWeatherscopePathFromTime(System.currentTimeMillis()));
		//		List<String> lines = cleanFile(Paths.get("C:/Data/station/metar/NAMSA_EURSA_621377820000.TXT"), true);
		List<String> lines = cleanFile(Paths.get("C:/Data/station/metar/wxMsg/SAHOURLY.TXT.1286"), false);
		//		List<String> lines = cleanFile(Paths.get("C:/Data/station/metar/wxMsg/SAHOURLY.TXT"));
		for(String line:lines)
			System.err.println(line);
		//
		//		String a = "~~~~";
		//		System.err.println(a.startsWith("~~~~"));
	}
}
