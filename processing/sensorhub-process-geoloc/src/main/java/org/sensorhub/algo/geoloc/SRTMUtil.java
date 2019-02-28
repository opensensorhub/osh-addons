
package org.sensorhub.algo.geoloc;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.sensorhub.algo.vecmath.BilinearInterpolation;
import org.sensorhub.algo.vecmath.Vect3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Utility class to lookup DEM height value from SRTM data. Interpolation 
 * is done when location falls between grid points.
 * </p>
 * <p>
 * <b>This class is NOT thread-safe</b>
 * </p>
 * 
 * @author Tony Cook, Alex Robin
 * @since Nov 14, 2008
 */
public class SRTMUtil
{
    private static Logger log = LoggerFactory.getLogger(SRTMUtil.class);
    
    static final double RES_1ARCSEC = 1.0 / 3600.0; // .0027777777
    static int NUM_ROWS_FILE = 3601, NUM_COLS_FILE = 3601;
    static int BYTES_PER_PIXEL = 2;
    
    String dataRoot;
    String filename;
    RandomAccessFile file = null;
    BilinearInterpolation bi = new BilinearInterpolation();
    double lat0, lon0; // origin of current file
    Vect3d c1, c2, c3, c4; // corner location and values


    public SRTMUtil(String dataRoot)
    {
        if (!dataRoot.endsWith(File.separator))
            dataRoot += File.separator;
        this.dataRoot = dataRoot;
        
        this.c1 = new Vect3d();
        this.c2 = new Vect3d();
        this.c3 = new Vect3d();
        this.c4 = new Vect3d();
    }


    public double getInterpolatedElevation(double lat, double lon) throws IOException
    {
        // open file containing lat/lon point
        openFile(lat, lon);

        // compute the 4 corners containing this lat/lon
        getCorners(lat, lon);
        bi.setCorners(c1, c2, c3, c4);
        
        // interpolate at the exact lat/lon location
        return bi.interpolate(lon, lat);
    }


    public void getCorners(double lat, double lon) throws IOException
    {
        // compute floating pt pixel index of lat-lon
        double deltaLat = lat - lat0;
        double deltaLon = lon - lon0;
        double latIndexD = deltaLat / RES_1ARCSEC;
        double lonIndexD = deltaLon / RES_1ARCSEC;
        
        // compute corners
        int x1 = (int) lonIndexD;
        int x2 = (int) lonIndexD + 1;
        int y1 = (int) latIndexD;
        int y2 = (int) latIndexD + 1;
        
        // get elevations for corners
        seek(x1, NUM_ROWS_FILE - y1 - 1);
        short z11 = readShort();
        short z21 = readShort();
        seek(x1, NUM_ROWS_FILE - y2 - 1);
        short z12 = readShort();
        short z22 = readShort();
        
        c1.set(lon0 + (double) x1 / (NUM_COLS_FILE), lat0 + (double) y1 / (NUM_ROWS_FILE), z11);
        c2.set(lon0 + (double) x1 / (NUM_COLS_FILE), lat0 + (double) y2 / (NUM_ROWS_FILE), z12);
        c3.set(lon0 + (double) x2 / (NUM_COLS_FILE), lat0 + (double) y1 / (NUM_ROWS_FILE), z21);
        c4.set(lon0 + (double) x2 / (NUM_COLS_FILE), lat0 + (double) y2 / (NUM_ROWS_FILE), z22);
    }


    private void openFile(String filepath) throws IOException
    {
        file = new RandomAccessFile(filepath, "r");
    }
    
    
    private void seek(int x, int y) throws IOException
    {
        long pos = (y * NUM_COLS_FILE + x) * BYTES_PER_PIXEL;
        file.seek(pos);
    }


    public String openFile(double lat, double lon) throws IOException
    {
        lat0 = (int) Math.floor(lat);
        lon0 = (int) Math.floor(lon);
        
        // latitude tile identifier
        String latString;
        if (lat0 >= 0)
            latString = "N" + toTwoChar((int)lat0);
        else
            latString = "S" + toTwoChar((int)-lat0);
        
        // longitude tile identifier
        String lonString;
        if (lon0 >= 0)
            lonString = "E" + toThreeChar(Math.abs((int)lon0));
        else
            lonString = "W" + toThreeChar(Math.abs((int)-lon0));
        
        String filename = latString + lonString + ".hgt";
        log.debug("Using file " + filename);

        // reuse or open file
        if (!filename.equals(this.filename))
        {
            if (file != null)
                file.close();

            this.filename = filename;
            openFile(dataRoot + filename);
        }

        return dataRoot + filename;
    }


    private final short readShort() throws IOException
    {
        byte b1 = file.readByte();
        byte b2 = file.readByte();
        int retVal = ((b1 << 8) & 0x0000ff00) | (b2 & 0x000000ff);
        return (short) retVal;
    }


    private final String toTwoChar(int i)
    {
        if (i < 0 || i > 90)
            throw new IllegalArgumentException("Invalid integer latitude value");
        
        if (i < 10)
            return "0" + i;
        else
            return "" + i;
    }


    private final String toThreeChar(int i)
    {
        if (i < 0 || i > 180)
            throw new IllegalArgumentException("Invalid integer longitude value");
        
        if (i < 10)
            return "00" + i;
        else if (i < 100)
            return "0" + i;
        else
            return "" + i;
    }


    public static void main(String[] args) throws IOException
    {
        SRTMUtil util = new SRTMUtil("/media/alex/Backup500/Data/SRTM/US/1arcsec");
        double lat = 35.0, lon = -114.5;
        double result = util.getInterpolatedElevation(lat, lon);
        System.out.println(util.c1);
        System.out.println(util.c2);
        System.out.println(util.c3);
        System.out.println(util.c4);
        System.out.println("Result = " + result);
    }
}
