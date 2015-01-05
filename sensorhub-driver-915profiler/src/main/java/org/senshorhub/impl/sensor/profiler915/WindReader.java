package org.senshorhub.impl.sensor.profiler915;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;


/**
 * <p>Title: WindReader.java</p>
 * <p>Description: Read 915 Profiler Wind data files </p>
 * @author Tony Cook
 * @since Jun 29, 2010
 */

public class WindReader
{
	public String testfile = "C:\\tcook\\kk\\915\\W10094.CNS";
	BufferedReader reader;
	int recCnt = 0;
	List<WindRecord> windRecords;  //  move

	public WindReader(){

	}

	//  Set wind file name based on currently selected data file
	//	public void setFilepath(String path){
	//		int lastSlashIdx = path.lastIndexOf("\\");
	//	}

	public void openFile(String filepath) throws IOException {
		reader = new BufferedReader(new FileReader(filepath));
	}
	//	
	//	public void getNextRecord() throws IOException {
	//		readHeader();
	//	}

	public WindRecord getNextRecord() throws IOException {
		String inline = null;
		boolean eoh = false;
		WindRecord wrTmp = null;
		String [] stmp;
		StringTokenizer st;
		int numBeams;

		try {
			while(!eoh) {
				inline = reader.readLine();  // MIPS
				if(inline == null) {
					break;
				}
				inline = inline.trim(); 
				int r;
				if(inline.length() == 0 || inline.equals("$")) 
					continue;
				wrTmp = new WindRecord();

				inline = reader.readLine();  //  WINDS
				inline = reader.readLine();  // LLA
				inline = inline.trim();
				st = new StringTokenizer(inline, " ");
				wrTmp.lat = Double.parseDouble(st.nextToken());
				wrTmp.lon = Double.parseDouble(st.nextToken());
				wrTmp.alt = Double.parseDouble(st.nextToken()); //  maybe int
				inline = reader.readLine();  // YMDHMS_z
				inline = inline.trim();
				wrTmp.time = parseTime(inline);
				inline = reader.readLine();  // avgTime,NumBeams,Rangegate
				inline = inline.trim();
				st = new StringTokenizer(inline, " ");
				wrTmp.averagingTime = Integer.parseInt(st.nextToken());
				numBeams = Integer.parseInt(st.nextToken());
				wrTmp.setNumBeams(numBeams);
				wrTmp.rangeGate = Integer.parseInt(st.nextToken()); 
				inline = reader.readLine();  // cons,totRec,windowSize
				inline = inline.trim();
				//			st = new StringTokenizer(inline, " ");
				//			for(int i = 0; i<numBeams; i++) {
				//				wrTmp.consensus[i] = Integer.parseInt(st.nextToken());
				//				wrTmp.totalRecords[i] = Integer.parseInt(st.nextToken());
				//				wrTmp.windowSize[i] = Integer.parseInt(st.nextToken());
				//			}
				inline = reader.readLine();  // numCodedCells,numSpectra,pulseWidth,interPulsePP[2]
				inline = inline.trim();
				st = new StringTokenizer(inline, " ");
				wrTmp.numCodedCells[0] = Integer.parseInt(st.nextToken());
				wrTmp.numCodedCells[1] = Integer.parseInt(st.nextToken());
				wrTmp.numSpectra[0] = Integer.parseInt(st.nextToken());
				wrTmp.numSpectra[1] = Integer.parseInt(st.nextToken());
				wrTmp.pulseWidth[0] = Integer.parseInt(st.nextToken());
				wrTmp.pulseWidth[1] = Integer.parseInt(st.nextToken());
				wrTmp.interPulsePd[0] = Integer.parseInt(st.nextToken());
				wrTmp.interPulsePd[1] = Integer.parseInt(st.nextToken());
				inline = reader.readLine();  // velocity...
				inline = inline.trim();
				st = new StringTokenizer(inline, " ");
				wrTmp.velocity[0] = Double.parseDouble(st.nextToken());
				wrTmp.velocity[1] = Double.parseDouble(st.nextToken());
				wrTmp.verticalCorrection = Integer.parseInt(st.nextToken());
				wrTmp.delay[0] = Integer.parseInt(st.nextToken());
				wrTmp.delay[1] = Integer.parseInt(st.nextToken());
				wrTmp.numGates[0] = Integer.parseInt(st.nextToken());
				wrTmp.numGates[1] = Integer.parseInt(st.nextToken());
				wrTmp.gateSpacing[0] = Integer.parseInt(st.nextToken());
				wrTmp.gateSpacing[1] = Integer.parseInt(st.nextToken());
				inline = reader.readLine();  //  az,el[]
				inline = inline.trim();
				st = new StringTokenizer(inline, " ");
				for(int i = 0; i<numBeams; i++){
					wrTmp.azimuth[i] = Double.parseDouble(st.nextToken());
					wrTmp.elevation[i] = Double.parseDouble(st.nextToken());
				}
				inline = reader.readLine();  // COLUMN HEADINGS LINE
				boolean eod = false;
				while (!eod) {
					//  must check for partial records- maybe even above this loop
					inline = reader.readLine();  //  ht,sp,dir,rad[],rec[],snr[]
					if(inline == null) {
						//					feof = true;
						break;
					}
					inline = inline.trim();
					if(inline.charAt(0) == '$') {
						break;
					}
					WindData wdTmp = parseWindData(inline, numBeams);
					wrTmp.windData.add(wdTmp);
				}

				recCnt++;
				return wrTmp;
			}
		} catch (NoSuchElementException e) {
			return null;
		}
		return null;
	}

	public double [] getTimes() {
		double [] times = new double[2];
		if(reader == null) {
			System.err.println("WindReader.getTimes():  You must call openFile(fname) before calling this method");
			return null;
		}
		WindRecord wrTmp = windRecords.get(0);
		times[0] = wrTmp.time;
		wrTmp = windRecords.get(windRecords.size() - 1);
		times[1] = wrTmp.time;
		return times;
	}

	public double parseTime(String tstr) {
		double time;

		StringTokenizer st = new StringTokenizer(tstr, " ");
		Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		int year, month, dayOfMonth, hour, minute, second, zoneOff;
		year = Integer.parseInt(st.nextToken()) + 2000;
		month = Integer.parseInt(st.nextToken()) - 1;
		dayOfMonth = Integer.parseInt(st.nextToken());
		hour = Integer.parseInt(st.nextToken());
		minute = Integer.parseInt(st.nextToken());
		second = Integer.parseInt(st.nextToken());
		zoneOff = Integer.parseInt(st.nextToken());
		cal.set(year, month, dayOfMonth, hour, minute, second);

		long timeMillis = cal.getTimeInMillis();
		time = timeMillis + (zoneOff * 3600.0);  // +/-?
		time = time/1000.0;

		return time;
	}

	public WindData parseWindData(String dataStr, int numBeams) {
		WindData wdTmp = new WindData(numBeams);
		StringTokenizer st = new StringTokenizer(dataStr," ");
		wdTmp.height = Double.parseDouble(st.nextToken());
		wdTmp.speed = Double.parseDouble(st.nextToken());
		wdTmp.direction = Double.parseDouble(st.nextToken());

		for(int i=0; i<numBeams; i++) {
			wdTmp.rad[i] =  Double.parseDouble(st.nextToken());
			wdTmp.cnt[i] =  Double.parseDouble(st.nextToken());
			wdTmp.snr[i] =  Double.parseDouble(st.nextToken());
		}

		return wdTmp;
	}

	public void close() throws IOException {
		reader.close();
	}

	public List<WindRecord> getWindRecords(){
		return windRecords;
	}

	public void loadAllData() throws IOException {
		if(reader == null) {
			System.err.println("WindReader.loadAllData():  You must call openFile(fname) before calling this method");
			return;
		}

		windRecords = new ArrayList<WindRecord>(48);
		WindRecord wrTmp = null;
		do {
			wrTmp = getNextRecord();
			if(wrTmp != null) {
				windRecords.add(wrTmp);
			}
		} while (wrTmp != null);
	}

	public static void main(String[] args) throws IOException {
		WindReader reader = new WindReader();
		reader.openFile(reader.testfile);
		WindRecord wrTmp = null;
		do {
			wrTmp = reader.getNextRecord();
		} while (wrTmp != null);
		reader.close();
		System.err.println("Done, recCnt = " + reader.recCnt);
	}
}
