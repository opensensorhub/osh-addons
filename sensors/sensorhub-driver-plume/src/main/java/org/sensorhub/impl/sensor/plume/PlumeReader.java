package org.sensorhub.impl.sensor.plume;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 
 * @author Tony Cook
 *
 */

/*

/**
3. Output-PARTICLE.OUT
 Line 1: WRITE(JUPRT,form) LON0,LAT0,TIMEMOD,RUNTIM,DTMOV,ITYPREL,NSORCE
 Line 2~NSORCE+1: to list all the sources
   DO ISRC=1,NSORCE
      WRITE(JUPRT,form) SX(ISRC), SY(ISRC),SZ(ISRC),tdelay(isrc),pconc(ISRC)
   ENDDO
 *(SX,SY,SZ) are original stack location in (x,y,z) or (Lon, Lat, z) depending on IPCOOR

 Repeat it=1,nstep_output
   DO ISRC=1,NSORCE
      WRITE(JUPRT,form)' SOURCE',ISRC,TIMEMOD+DTMOV,RUNTIM,TDELAY(ISRC),NPART(ISRC)
      DO IP=1,NPART(ISRC)
        WRITE(JUPRT,form) X(IP,ISRC), Y(IP,ISRC), Z(IP,ISRC)
      ENDDO
   ENDDO
 End of repeat

 *TIMEMOD model timing, is RUNTIME 
 *RUNTIME model run time in [sec]
 *TDELAY is the time (in [sec]) between the starting release from a source and the particle model starting time
 *NPART  is no. of particles in this timestep for a source
  Note that if NPART(isrc)=0 (not released yet), then no (x,y,z) location is printed for the source for the timestep 
 *Ignort pconc which is not used

 */
public class PlumeReader 
{
	static final double START_TIME = 1441918699.;  // Actual start time doesn't seem to be in the model-
												  //  Ask YuLing about this	
	
	// TODO: add support for multiple sources
	public static Plume read(File f) throws IOException {
		assert f.exists() && f.isFile();

		Plume plume = new Plume();
		try(BufferedReader reader = new BufferedReader(new FileReader(f))) {

			//  Run header line
			String inline = reader.readLine().trim();
			String [] vals = inline.split("\\s+");
			double lon0 = Double.parseDouble(vals[0]);
			double lat0 = Double.parseDouble(vals[1]);
			double timeMod = Double.parseDouble(vals[2]);
			double runtime = Double.parseDouble(vals[3]);
			double timeStep = Double.parseDouble(vals[4]);
			double releaseType = Double.parseDouble(vals[5]);
			double numSources = Double.parseDouble(vals[6]);

			// source header lines
			for(int i=0; i<numSources; i++) {
				inline = reader.readLine().trim();
				vals = inline.split("\\s+");
				double srcLon = Double.parseDouble(vals[0]);
				double srcLat  = Double.parseDouble(vals[1]);
				double srcHeight = Double.parseDouble(vals[2]);
				double tdelay = Double.parseDouble(vals[3]);

				if(i == 0) {
					plume.sourceLat = srcLat;
					plume.sourceLon = srcLon;
					plume.sourceHeight = srcHeight;
				}
			}

			// read plume data
			boolean eof = false;
			while(!eof) {
				for(int i=0; i<numSources; i++) {
					//  one header line per source
					inline = reader.readLine();
					if(inline == null || inline.trim().length() == 0) {
						//System.err.println("EOF reached");
						eof = true;
						break;
					}
					vals = inline.trim().split("\\s+");
					timeMod = Double.parseDouble(vals[2]) + START_TIME;
					runtime = Double.parseDouble(vals[3]);
					double tdelay = Double.parseDouble(vals[4]);
					int numParticles = (int)(Double.parseDouble(vals[5]));
//					double[][] points = new double[numParticles][3];
					double[] points1d = new double[numParticles * 3];

//					for(int j = 0; j<numParticles; j++) {
					for(int j = 0; j<numParticles*3; j+=3) {
						inline = reader.readLine().trim();
						vals = inline.split("\\s+");
						double lon = Double.parseDouble(vals[0]);
						double lat  = Double.parseDouble(vals[1]);
						double alt = Double.parseDouble(vals[2]);	
						points1d[j] = lat;
						points1d[j+1] = lon;
						points1d[j+2] = alt;
					}
					if(i == 0 && numParticles > 0) {
						PlumeStep step = new PlumeStep(timeMod, numParticles, points1d);
						plume.addStep(step);
					}
				}
			}
		}
		
		return plume;
	}

	public static void main(String[] args) throws IOException {
		File pfile = new File("C:/Data/sensorhub/plume/ppdm_ind_ms_v2/run/PARTICLE.OUT_p0");
		Plume plume = PlumeReader.read(pfile);
		System.err.println(plume);
	}
}
