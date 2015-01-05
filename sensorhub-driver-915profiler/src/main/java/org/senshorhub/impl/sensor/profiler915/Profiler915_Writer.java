package org.senshorhub.impl.sensor.profiler915;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <p>Title: Profiler915_Writer.java</p>
 * <p>Description: Writes Profiler915 data as ASCII </p>
 * @author Tony Cook
 * @since Feb, 2010
 */

public class Profiler915_Writer {
	private BufferedWriter writer;
	private Profile915 profile;
	private String outputDir;
	private int numberOfOutputProfiles = -1;
	
	int tempCnt = 0;

	public Profiler915_Writer() {
	}

	public void writeProfile(Profile915 profile) throws IOException {
		tempCnt++;
		this.profile = profile;
		// filenamePattern = Dyyddd_hhmmss.txt
		// where dd = dayOfyear

		String filename = outputDir + getFilename(profile);
		writer = new BufferedWriter(new FileWriter(filename));

		writeHeader();

		// Compute params needed for the rest of the data
		profile.computeParams();
		writeDopplerData();

		writeSpectralData();

		writer.close();
	}

	private String getFilename(Profile915 profile){
		Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis((long)(profile.sysTime * 1000.0));
		int year = cal.get(Calendar.YEAR);  
		// Y2K !!!  THis won't be used for data b4 2000 will it?
		if(year>1999)
			year = year - 2000;
		else 
			year = year - 1000;
		int doy = cal.get(Calendar.DAY_OF_YEAR);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		String yearStr = (year <10) ? "0" + year : "" + year;
		String hrStr = (hour <10) ? "0" + hour : "" + hour;
		String minStr = (min <10) ? "0" + min : "" + min;
		String secStr = (sec <10) ? "0" + sec : "" + sec;
		String doyStr;
		if(doy < 10)  
			doyStr = "00" + doy;
		else if(doy < 100)
			doyStr = "0" + doy;
		else
			doyStr = "" + doy;
		
		return "T" + yearStr + doyStr + "_" + hrStr + minStr + secStr + ".txt";
	}

	// For now, just data, but could add other params
	private void writeHeader() throws IOException {
		Calendar cal = GregorianCalendar.getInstance(TimeZone
				.getTimeZone("GMT"));
		cal.setTimeInMillis((long) (profile.sysTime * 1000.0));
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setCalendar(cal);

		String dateStr = "Date:  " + df.format(cal.getTime()) + " UTC\n";
		writer.write(dateStr);

		writer.flush();
	}

	private void writeDopplerData() throws IOException {
		int numBins = profile.getNumSpectralBins();
		int numGates = profile.getNumGates();
		String numBinsStr = "NumGates, NumBins:  " + numGates + ", " + numBins
				+ "\n";
		writer.write(numBinsStr);

		// Nyquist: <Nyq>
		String nyqStr = ("Nyquist:  " + profile.nyquist + "\n");
		writer.write(nyqStr);

		// Height: <gateAgl[n]>
		StringBuffer buffTmp = new StringBuffer(512);
		buffTmp.append("Height AGL:  ");
		for (int i = 0; i < numGates; i++) {
			buffTmp.append(profile.gateAgl[i] + " ");
		}
		buffTmp.append("\n");
		writer.write(buffTmp.toString());

		// SNR: <[n]>
		buffTmp = new StringBuffer(512);
		buffTmp.append("SNR:  ");
		for (int i = 0; i < numGates; i++) {
			buffTmp.append(profile.snr[i] + " ");
		}
		buffTmp.append("\n");
		writer.write(buffTmp.toString());

		// VEL: <[n]>
		buffTmp = new StringBuffer(512);
		buffTmp.append("Velocity:  ");
		for (int i = 0; i < numGates; i++) {
			// use reverse-convention computed value here
			buffTmp.append(profile.dopplerVelocity[i] + " ");
		}
		buffTmp.append("\n");
		writer.write(buffTmp.toString());

		// CORR_VEL: <[n]>
		buffTmp = new StringBuffer(512);
		buffTmp.append("CorrVel:  ");
		for (int i = 0; i < numGates; i++) {
			buffTmp.append(profile.corrVel[i] + " ");
		}
		buffTmp.append("\n");
		writer.write(buffTmp.toString());

		// SW: <[n]>
		buffTmp = new StringBuffer(512);
		buffTmp.append("Spectrum Width:  ");
		for (int i = 0; i < numGates; i++) {
			buffTmp.append(profile.spectralWidth[i] + " ");
		}
		buffTmp.append("\n");
		writer.write(buffTmp.toString());

		// Noise: <[n]>
		buffTmp = new StringBuffer(512);
		buffTmp.append("Noise:  ");
		for (int i = 0; i < numGates; i++) {
			buffTmp.append(profile.noise[i] + " ");
		}
		buffTmp.append("\n");
		writer.write(buffTmp.toString());

		writer.flush();
	}

	private void writeSpectralData() throws IOException {
		int numGates = profile.getNumGates();
		int numBins = profile.getNumSpectralBins();

		StringBuffer buffTmp;
		for (int i = 0; i < numGates; i++) {
			writer.write("Gate #" + (i + 1) + ": ");
			buffTmp = new StringBuffer(512);
			for (int j = 0; j < numBins; j++) {
				buffTmp.append(profile.spectralData[i][j] + " ");
			}
			writer.write(buffTmp.toString());
			writer.write("\n");
		}

		writer.flush();
	}

//	public void setHeaderReader(Profiler915_HeaderReader headerReader) {
//		this.headerReader = headerReader;
//	}
//	
	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public void setNumberOfOutputProfiles(int numberOfOutputProfiles) {
		this.numberOfOutputProfiles = numberOfOutputProfiles;
	}

	public int getNumberOfOutputProfiles() {
		return numberOfOutputProfiles;
	}
	
	public static void main(String[] args) throws IOException {
		Profiler915_Writer writer = new Profiler915_Writer();
		//  read args
		if(args.length <2 || args.length > 3) {
			System.out.println("Usage: java -jar P915_Writer.jar inputFile  outputDir [numberOfProfiles]");
			System.exit(-1);
		}
		String dataFile = args[0];
//		String slash = System.getProperty("file.separator");
		String slash = "/";  
		int lastSlashIdx = dataFile.lastIndexOf(slash);
		char [] headerC = dataFile.toCharArray();
		headerC[lastSlashIdx + 1] = 'H';
		String headerFile = new String(headerC);
		
		String outputDir = args[1];
		if(!outputDir.endsWith("/"))
			outputDir = outputDir + "/";
		writer.setOutputDir(outputDir);
		
		//  numerOfOutputProfiles
		if(args.length == 3) {
			try {
				int numProfs = Integer.parseInt(args[2]);
				writer.setNumberOfOutputProfiles(numProfs);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				System.out.println("Profiler915Writer error:  Illegel numberOfProfiles = " + args[2]);
				System.exit(-1);
			}
		}
		
		// Read header
		Profiler915_HeaderReader headerReader = new Profiler915_HeaderReader();
		headerReader.setHeaderFile(headerFile);
		headerReader.readHeader();

		// Read data
		Profiler915_Reader reader = new Profiler915_Reader();
		reader.setFile(dataFile);
		reader.setWriter(writer);
		reader.setHeaderReader(headerReader);
		reader.readData();
	}
}
