package org.sensorhub.impl.sensor.station.metar;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

	public static boolean isEmwinMetarFile(String s) {
		if(!s.startsWith("SAHOURLY")) 
			return false;
		int lastDot = s.lastIndexOf('.');
		if(lastDot == -1)
			return false;
		String suffix = s.substring(lastDot + 1, s.length());
		if(suffix.length() == 0)
			return false;
		
	    boolean isInt = suffix.matches("\\d+");
	    if(suffix.equalsIgnoreCase("TXT") || isInt)
	    	return true;
	    return false;
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
