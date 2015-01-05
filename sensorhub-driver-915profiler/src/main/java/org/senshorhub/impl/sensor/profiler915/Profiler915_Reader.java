package org.senshorhub.impl.sensor.profiler915;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class Profiler915_Reader implements ProfileProvider
{
	String filepath;
	String filepathDay1;
	String filepathDay2;
	Profiler915_HeaderReader headerReader;  //  need for access to Header table to look up numGates and numBins, etc...
	Profiler915_HeaderReader headerReaderDay2;  //  need for access to Header table to look up numGates and numBins, etc...
	Profiler915_Writer writer = null;
//	String windFilepath;  //  Note commenting this out without creating new reader WILL break batcher/legacy plotter
	DataInputStream is;
	RandomAccessFile rif;
//	List<Profile915> profiles;
	List<Profile> profiles;
	int recordCnt;
	double userStartTime;
	int bytePos;  //  file position 
	boolean dayForward = false;  //  Set if there is datafile for next day
	boolean dayBack = false;  //  true if there is datafile for previous day

	public Profiler915_Reader(){
		profiles = new ArrayList<Profile>(1000);
	}

	//  This method is a little overloaded for now, but needed
	//  Make sure requested File exists
	//  Check for file on adjacent day
	//  read header file(s)
	public boolean setFile(String fileStr) {
		this.filepath = fileStr;
		this.filepathDay1 = fileStr;
		File ftmp = new File(filepath);
	//  Go ahead and set fileDay2 if it exists 
		//  need to check forward and backerds!
		filepathDay2 = Profiler915_Util.getAdjacentDayPath(filepath, 1);
		File ftmpDay2 = new File(filepathDay2);
		String file;
		try {
			file = ftmp.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if(!ftmp.exists()) {
			return false;
		}	
		if(ftmpDay2.exists())
			dayForward = true;
		
		String headerName = getHeaderName(file);
//		String slash = System.getProperty("file.separator");
//		int lastSlashIdx = file.lastIndexOf(slash);
//		//int lastDotIdx = file.lastIndexOf(".");
//		char [] headerC = file.toCharArray();
//		headerC[lastSlashIdx+1] = 'H';
//		String headerName = new String(headerC);
		File htmp = new File(headerName);
		if(!htmp.exists()) {
			return false;
		}	
		//  Reconsider preload header file
		headerReader = readHeaderFile(headerName);
		if (headerReader == null)
			return false;
		
		return true;
	}
	
	private String getHeaderName(String fname) {
		String slash = System.getProperty("file.separator");
		int lastSlashIdx = fname.lastIndexOf(slash);
		//int lastDotIdx = file.lastIndexOf(".");
		char [] headerC = fname.toCharArray();
		headerC[lastSlashIdx+1] = 'H';
		String headerName = new String(headerC);
		
		return headerName;
	}
	
	private Profiler915_HeaderReader readHeaderFile(String headerName){
		Profiler915_HeaderReader hrTmp = new Profiler915_HeaderReader();
		hrTmp.setHeaderFile(headerName);
		try {
			hrTmp.readHeader();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return hrTmp;
	}

	public void openFile() throws IOException
	{
		FileInputStream fis = null;

		try {
			fis = (new FileInputStream(filepath));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			System.err.println("Profile915_Reader file: " + filepath + " not found...");
		}
		if(is != null) {
			try {
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		is = new DataInputStream(new BufferedInputStream(fis));
		recordCnt = 0;
		bytePos = 0;
		//  Problem - need to keep data resident for Spectra plots AND be able to render full data file
		//  
//		if(profiles.size() > 0)
//			clearProfiles();
	}

	public void closeFile(){
		try {
			is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Profile915 getNextProfile() {
		Profile915 profTmp;
		boolean profFound = false;

		while(!profFound) {
			try {
				profTmp = new Profile915();
				short recType;
				is.mark(100000);

				try {
					recType = ByteSwappedReader.readSwappedShort(is);
				} catch (EOFException e) {
					System.out.println("Profiler915_Reader:  EOF detected");
					return null;
				}
				int numBytes = ByteSwappedReader.readSwappedInt(is);
				short spectralDataInFile = ByteSwappedReader.readSwappedShort(is);
				if (spectralDataInFile !=1 ) {
					System.out.println("Profile915_reader.getNextProfile()- Not a moments file!:  recType, numBtes, sdif: " +
							recType + ", " + numBytes + ", " + spectralDataInFile);
					return null;
				}
				profTmp.headerStartByte = 	ByteSwappedReader.readSwappedInt(is);  
				Profiler915_Header header = headerReader.headerMap.get(profTmp.headerStartByte);
				if (header == null) {
					System.out.println("Profiler915_Reader.getNextProfile(): ");
					System.out.println("\tNo header found at byte "	+ profTmp.headerStartByte + ", Exiting...");
					return null;
				}

				profTmp.setHeader(header);
				profTmp.sysTime = ByteSwappedReader.readSwappedInt(is);
				//   Check sysTime here!!! -If we can skip bytes to next Record
				bytePos +=  16;
//				if(profTmp.sysTime < userStartTime) {
//					is.reset();
//					is.skipBytes(numBytes);
//					bytePos += numBytes;
//					continue;
//				}
				//System.err.println("sysTime = " + sysTime);
				profTmp.radarIndex = ByteSwappedReader.readSwappedShort(is);
				profTmp.beamArrayIndex = ByteSwappedReader.readSwappedShort(is);
				profTmp.numCoherentIntegrations = ByteSwappedReader.readSwappedShort(is);
				profTmp.numSpectraInAverage = ByteSwappedReader.readSwappedShort(is);
				bytePos += 8;
				
				
				//  Get numGateHts and numSpecBins from header
				//  NOTE: A lot of things will need to be fixed for multuple Beams!!!
				profTmp.initializeArrays(header.numGateHeights[0], header.numWindSpectralBins);

				//  doppler data
				int numGates = profTmp.getNumGates();
				for(int i = 0; i< numGates; i++) {
					profTmp.dopplerRaw[i] = (float)ByteSwappedReader.readSwappedShort(is) / 1.0e4f;  //  normalized val * 1e4
					profTmp.swRaw[i] = (float)ByteSwappedReader.readSwappedShort(is) / 1.0e4f;  //  normalized val * 1e4
					profTmp.snr[i] = (float)ByteSwappedReader.readSwappedShort(is) / 100.0f;  //  dB x 100
					profTmp.noise[i] = (float)ByteSwappedReader.readSwappedShort(is) / 100.0f;  // == 1000log10(noiseVal)
				}
				bytePos += 8 * numGates;
				
				//  power spectra data
				int numBins = profTmp.getNumSpectralBins();
				int numFloats = numGates * numBins;
				double specDataTmp;
				double varFac  = Math.pow(2.0, 18.0);
//				boolean skipSpectra = false;
				boolean skipSpectra = true;
				profTmp.spectralDataByteStart = bytePos;
				int bytesToSkip = numGates * numBins * 4;
				if(skipSpectra) {
					is.skipBytes(bytesToSkip);
				} else {
					for(int i=0; i<numGates; i++){
						for(int j = 0; j< numBins; j++) {
							specDataTmp =  ByteSwappedReader.readSwappedFloat(is);  // Varaince * 2^18
							//  Convert here?
							profTmp.spectralData[i][j] = (float)(specDataTmp / varFac);
							//  Uncomment for log space:
							//  bin_data=10.*alog10(bin_data/2.^18)
							//					profTmp.spectralData[i][j] = (float)(10.0 * Math.log10(specDataTmp/varFac));
						}
					}
				}
				bytePos += bytesToSkip;
				//			long filePos = fis.getChannel().position();
				//			if(recCnt>=3180)
				//				System.out.println("Rec#, filePos, filePosHex: " + 
				//						recCnt + ", " + filePos + ", " + Long.toHexString(filePos));	
				//			int numBytesAgain = ByteSwappedReader.readSwappedInt(is, (recCnt>3170));
				int numBytesAgain = ByteSwappedReader.readSwappedInt(is);
				//  Can separate writing at some point by having reader read and return one profile at a time
				//  skip any off-zenith (!=90) beams
				bytePos += 4;
				if(profTmp.sysTime < userStartTime) {
					continue;
				}
				if(header.beamElevationAngle[0] == 9000) {
					profTmp.computeParams();
					profiles.add(profTmp);
					recordCnt++;
					return profTmp;  //  profFound == true;
				}
			} catch (Exception e) {
				System.err.println("Profiler915_Reader.getNextProfile():  Unexpected EOF, but probably okay.");
			}
		}

		return null;
	}

	public int getNumProfiles(){
		return (profiles != null) ? profiles.size() : 0;
	}

	//  Approximate the period between *vertical* pulses, throwing out any delta that is not reasonable
	//  NOTE: optimize by doing this only once!
	public double getPulsePeriod(){
		double periodMin = 12, periodMax = 61;  //reasonable min max period between vertical pulses
		double [] times = Profiler915_Util.getTimes(profiles);
		if(times == null)
			return -1.0;
		double deltaT, sum = 0.0;
		int count = 0;
		for(int i=1; i< times.length; i++) {
			deltaT = times[i] - times[i-1];
			if(deltaT < periodMin || deltaT > periodMax)
				continue;
			sum += deltaT;
			count++;
		}
		
		double pulsePeriod = sum/(double)count;
		return pulsePeriod;
	}

//	@Override
//	public List<Profile915> getProfiles(){
////		return  null;
//	}
//	
	public List<Profile> getProfiles(){
		Profile profTmp = profiles.get(0);
		Profile915 ptmp = (Profile915)profTmp;
		return  profiles;
	}
	
	
	public Profile getTest() {
		return new Profile915();
	}

	public void setUserStartTime(double t){
		userStartTime = t;
	}

	/*****  END NEW ****/

	/** readData() - reads ALL profiles- used for Profile915Writer
	 *  NOTE: Use getNextProfile() to read only one profile at a time- should
	 *  call that method from this one at some point
	 *  GET RID of this by removing reference in ProfileWriter!!
	 */
	public void readData() throws IOException {
		if(writer==null){
			System.out.println("You must call reader.setProfileWriter(writer) before calling readData()!");
			System.exit(-1);
		}

		FileInputStream fis = (new FileInputStream(filepath));
		DataInputStream is = new DataInputStream(new BufferedInputStream(fis));

		boolean eof = false;
		int recCnt = 0;
		int totalFloatCnt = 0;
		Profile915 profTmp;
		System.out.println("Profiler915_Reader: reading Data...");
		int maxProfiles = writer.getNumberOfOutputProfiles();

		while(!eof) {
			profTmp = new Profile915();
			short recType;
			try {
				recType = ByteSwappedReader.readSwappedShort(is);
			} catch (EOFException e) {
				System.out.println("Profiler915_Reader:  EOF detected");
				break;
			}
			int numBytes = ByteSwappedReader.readSwappedInt(is);
			//			if(recCnt>3180 && recCnt<3185){
			//				System.out.println("RecCnt, numBytes: " + recCnt + ", " + numBytes);
			//			}
			short spectralDataInFile = ByteSwappedReader.readSwappedShort(is);
			if (spectralDataInFile !=1 ) {
				//				System.out.println("This is a moments only file.  Not going to work for Spectra...");
				System.out.println("recNum,  recType, numBtes, sdif: " +
						recCnt + ": " + recType + ", " + numBytes + ", " + spectralDataInFile);
				//				System.exit(-1);
			}
			profTmp.headerStartByte = 	ByteSwappedReader.readSwappedInt(is);  
			Profiler915_Header header = headerReader.headerMap.get(profTmp.headerStartByte);
			if (header == null) {
				System.out.println("Profiler915_Reader.readData(): ");
				System.out.println("\tNo header found at byte "
						+ profTmp.headerStartByte + ", Exiting...");
				System.exit(-1);
			}


			profTmp.setHeader(header);
			profTmp.sysTime = ByteSwappedReader.readSwappedInt(is);
			//System.err.println("sysTime = " + sysTime);
			profTmp.radarIndex = ByteSwappedReader.readSwappedShort(is);
			profTmp.beamArrayIndex = ByteSwappedReader.readSwappedShort(is);
			profTmp.numCoherentIntegrations = ByteSwappedReader.readSwappedShort(is);
			profTmp.numSpectraInAverage = ByteSwappedReader.readSwappedShort(is);

			//  Get numGateHts and numSpecBins from header
			//  NOTE: A lot of things will need to be fixed for multuple Beams!!!
			profTmp.initializeArrays(header.numGateHeights[0], header.numWindSpectralBins);

			//  doppler data
			int numGates = profTmp.getNumGates();
			for(int i = 0; i< numGates; i++) {
				profTmp.dopplerRaw[i] = (float)ByteSwappedReader.readSwappedShort(is) / 1.0e4f;  //  normalized val * 1e4
				profTmp.swRaw[i] = (float)ByteSwappedReader.readSwappedShort(is) / 1.0e4f;  //  normalized val * 1e4
				profTmp.snr[i] = (float)ByteSwappedReader.readSwappedShort(is) / 100.0f;  //  dB x 100
				profTmp.noise[i] = (float)ByteSwappedReader.readSwappedShort(is) / 100.0f;  // == 1000log10(noiseVal)
			}

			//  power spectra data
			int numBins = profTmp.getNumSpectralBins();
			int numFloats = numGates * numBins;
			totalFloatCnt += numFloats;
			double specDataTmp;
			double varFac  = Math.pow(2.0, 18.0);
			for(int i=0; i<numGates; i++){
				for(int j = 0; j< numBins; j++) {
					specDataTmp =  ByteSwappedReader.readSwappedFloat(is);  // Varaince * 2^18
					//  Convert here?
					profTmp.spectralData[i][j] = (float)(specDataTmp / varFac);
					//  Uncomment for log space:
					//  bin_data=10.*alog10(bin_data/2.^18)
					//					profTmp.spectralData[i][j] = (float)(10.0 * Math.log10(specDataTmp/varFac));
				}
			}
			//			long filePos = fis.getChannel().position();
			//			if(recCnt>=3180)
			//				System.out.println("Rec#, filePos, filePosHex: " + 
			//						recCnt + ", " + filePos + ", " + Long.toHexString(filePos));	
			//			int numBytesAgain = ByteSwappedReader.readSwappedInt(is, (recCnt>3170));
			int numBytesAgain = ByteSwappedReader.readSwappedInt(is);
			if(recCnt%100==0) {
				System.out.println("\t" + recCnt + " profiles processed... " + numBytesAgain);

			}

			//  Can separate writing at some point by having reader read and return one profile at a time
			//  skip any off-zenith (!=90) beams
			if(header.beamElevationAngle[0] == 9000) {
				writer.writeProfile(profTmp);
			}
			//			
			recCnt++;
			if(maxProfiles != -1 && recCnt >=maxProfiles)
				break;
			//			if(recCnt==3182) {
			//				is.readDouble();
			//				is.readDouble();
			//				is.readDouble();
			//			}
		}
		System.out.println("Total records, total spectralPoints read = " + recCnt + ", " + totalFloatCnt);
	}

	public void setWriter(Profiler915_Writer writer) {
		this.writer = writer;
	}

	public void setHeaderReader(Profiler915_HeaderReader headerReader) {
		this.headerReader = headerReader;
	}

//	//  Use for plotter- assumes Windfile in same location as Data
//	public String getWindFilepath() {
//		return windFilepath;
//	}
//
//	//  Use for Batcher, so Dustin can specify windDir separately
//	public String getWindFilename() {
//		String slash = System.getProperty("file.separator");
//		int lastSlashIdx = windFilepath.lastIndexOf(slash);	
//		
//		String windFile = windFilepath.substring(lastSlashIdx+1);
//		
//		return windFile;
//	}
//	
	public String getFilepath() {
		return filepath;
	}

	public void getSpectralData(Profile915 prof) throws IOException{
		//  file should be closed- ck if needed
		openFile();
		int specDataStart = prof.spectralDataByteStart;
		int bytesSkipped = is.skipBytes(specDataStart);
		if(bytesSkipped != specDataStart) {
			System.err.println("Prof915_reader.getSpecData():  bytesSkipped != spectralStartByte:\n "
					+ bytesSkipped + " , " + specDataStart);
			return;
		}
		double specDataTmp;
		double varFac  = Math.pow(2.0, 18.0);
		int numGates = prof.getNumGates();
		int numBins = prof.getNumSpectralBins();
		for(int i=0; i<numGates; i++){
			for(int j = 0; j< numBins; j++) {
				specDataTmp =  ByteSwappedReader.readSwappedFloat(is);  // Varaince * 2^18
				//  Convert here?
				prof.spectralData[i][j] = (float)(specDataTmp / varFac);
			}
		}
	}

	//  FIX!
	public int getEstimatedRayCount() {
		File file = new File(filepath);
		long fileSize = file.length();
		double raySize = 1000.0;
		
		int numRays = (int)(fileSize/raySize);
		return numRays;
	}

	//  Return the current byteOffset of the file
	public int getByteOffset() {
		return bytePos;
	}
	
	public void readFullFile(int momentNumber) throws IOException {
		openFile();
		
		List<Profile915> profiles = new ArrayList<Profile915>(100);
		Profile915 profile;
		boolean moreData = true;
		int profileCnt = 0;
		while(moreData)  {
			//  momentNum!
			profile = (Profile915)getNextProfile();
			if(profile == null)
				break;
			profileCnt++;
			profiles.add(profile);
		}
		closeFile();
	}
	
	//  load the requested number of Profiles into memory
	public void loadProfiles(int numToLoad) {
		Profile915 profile;
		boolean moreData = true;
		int profileCnt = 0;
		while(moreData && profileCnt < numToLoad) {
			profile = getNextProfile();
			if(profile == null) {
				break;
			}
			//  profile gets added IFF beam angle == 90.00
			if(profile.header.beamElevationAngle[0] == 9000) {
				profile.computeParams();
				profiles.add(profile);
				profileCnt++;
			}
		}
		if(!dayForward)
			return;
		filepath = filepathDay2;
		try {
			openFile();
			String headerName = getHeaderName(filepathDay2);
			headerReaderDay2 = readHeaderFile(headerName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		while(moreData && profileCnt < numToLoad) {
			profile = getNextProfile();
			if(profile == null) {
				break;
			}
			//  profile gets added IFF beam angle == 90.00
			if(profile.header.beamElevationAngle[0] == 9000) {
				profile.computeParams();
				profiles.add(profile);
				profileCnt++;
			}
		}
	}
	
	public void clearProfiles(){
		profiles.clear();
	}
	
	public static void main(String [] args) throws IOException 
	{
		//  Read data
		Profiler915_Reader reader = new Profiler915_Reader();
//		reader.setFile("C:\\Users\\tcook\\root\\kk\\915\\data\\D09103a_copy.SPC");
//		reader.setFile("C:\\Users\\tcook\\root\\kk\\915\\data\\D09103a.SPC");
		reader.setFile("C:\\Users\\tcook\\root\\kk\\915\\data\\D10094a.SPC");
		reader.openFile();
		boolean eof =  false;
		Profile915 profTmp;
		int recCnt = 0;
		while(!eof) {
			profTmp = reader.getNextProfile();
			if(profTmp == null)
				break;
			recCnt++;
			if(recCnt%500 == 0)
				System.out.println(recCnt + " records read...");
		}
		System.err.println("EOF reached.  " + reader.recordCnt + " records read");
	}

}

