/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Original Code is the "SensorML DataProcessing Engine".

 The Initial Developer of the Original Code is the VAST team at the University of Alabama in Huntsville (UAH). <http://vast.uah.edu> Portions created by the Initial Developer are Copyright (C) 2007 the Initial Developer. All Rights Reserved. Please Contact Mike Botts <mike.botts@uah.edu> for more information.

 Contributor(s):
 Alexandre Robin <robin@nsstc.uah.edu>

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.algo.sat.orbit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.vast.util.Asserts;


/**
 * <p>
 * Class for parsing TLE data from a file and storing in TLEInfo structure
 * </p>
 *
 * @author Mike Botts, Alexandre Robin
 * @since 10/14/98
 */
public class TLEParser implements TLEProvider
{   
    private static final double SECONDS_PER_DAY = 86400.0;
    private static final double SECONDS_PER_YEAR = 31536000.0;      // no leapseconds
    private static final double SECONDS_PER_LEAPYEAR = 31622400.0;  // leapseconds
    private static final double DTR = Math.PI / 180.0;
    
    protected URL tleFileUrl;
    protected BufferedReader tleReader;
    protected int lineNumber = 0;
    protected String currentLine1, previousLine1, nextLine1;
    protected String currentLine2, previousLine2, nextLine2;
    protected double currentTime, nextTime;
    protected TLEInfo previousTLE;
    
    
    public TLEParser(URL tleFileUrl)
    {
        this.tleFileUrl = tleFileUrl;
        reset();
    }


    public void reset()
    {
        closeFile();
        
        currentTime = Double.NEGATIVE_INFINITY;
        nextTime = Double.NEGATIVE_INFINITY;
        currentLine1 = "";
        previousLine1 = "";
        nextLine1 = "";
        currentLine2 = "";
        previousLine2 = "";
        nextLine2 = "";
        lineNumber = 0;
        
        // reopen file at beginning!
        initReader();
    }
    
    
    protected void initReader()
    {
        try
        {
            tleReader = new BufferedReader(new InputStreamReader(tleFileUrl.openStream()));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    /**
     * Reads all TLEs for the given time range
     * @param satID ID of desired sat/object
     * @param startTime 
     * @param stopTime 
     * @return list of TLE objects
     * @throws IOException 
     */
    public List<TLEInfo> readTLEList(String satID, double startTime, double stopTime) throws IOException
    {
        List<TLEInfo> tleList = new ArrayList<>();
        
        TLEInfo tle = getClosestTLE(satID, startTime);
        tleList.add(tle);
        while ((tle = readNextTLE(satID, stopTime)) != null)
            tleList.add(tle);
        
        return tleList;
    }


    /**
     * Reads the next TLE
     * @param satID ID of desired sat/object
     * @param stopTime
     * @return TLEInfo object
     * @throws IOException
     */
    public TLEInfo readNextTLE(String satID, double stopTime)
    {
        boolean isLastEntry = readNextEntry(satID);
        if (isLastEntry)
            return null;
        
        double tleTime = getJulian(currentLine1);
        if (tleTime > stopTime)
            return null;
        
        return parseTLE(currentLine1, currentLine2);
    }
    
    
    /**
     * Reads the closest TLE to the given time 
     * @param satID ID of desired sat/object
     * @param desiredTime
     * @return TLEInfo object
     * @throws IOException
     */
    public TLEInfo getClosestTLE(String satID, double desiredTime) throws IOException
    {
        boolean isLastEntry = false;
        
        // reset if requested time is before previous TLE
        if (desiredTime < currentTime)
            reset();
        
        // return same TLE if time is within TLE range
        else if (desiredTime < nextTime)
            return previousTLE;
        
        // skip lines until we find first TLE after requested time
        while (desiredTime > nextTime)
        {
            // read next 2 lines
            isLastEntry = readNextEntry(satID);
            if (isLastEntry)
            {
                if (nextTime != Double.POSITIVE_INFINITY)
                {
                    currentTime = nextTime;
                    nextTime = Double.POSITIVE_INFINITY;
                }
                break;
            }
            
            // read next line time
            currentTime = nextTime;
            nextTime = getJulian(nextLine1);
        }
        
        // if no more lines, use last one
        if (!isLastEntry)
        {
            // now we have the first tle greater than the input time,
            // which currently lies between next and current positions,
            // determine which TLE is closer
            double currentDelta = Math.abs(currentTime - desiredTime);
            double nextDelta = Math.abs(nextTime - desiredTime);

            if (currentDelta > nextDelta)
                previousTLE = parseTLE(nextLine1, nextLine2);
            else
                previousTLE = parseTLE(currentLine1, currentLine2);
        }
        else
            previousTLE = parseTLE(currentLine1, currentLine2);
        
        return previousTLE;
    }
    
    
    /*protected boolean readNextEntry(String satID)
    {
        String lineSatID;
        
        // read until we find an entry for the desired satID
        do
        {
            boolean isLastEntry = readNextEntry();
            if (isLastEntry)
                return true;
            
            lineSatID = nextLine1.substring(2, 7);
        }
        while (!lineSatID.equals(satID));
        
        
        
        return false;
    }*/
    
    
    /**
     * Try to read next TLE entry
     * @return true if EOF
     */
    protected boolean readNextEntry(String satID)
    {
        Asserts.checkNotNull(satID, "satID");
        
        try
        {
            tleReader.mark(140);
            previousLine1 = currentLine1;
            previousLine2 = currentLine2;
            currentLine1 = nextLine1;
            currentLine2 = nextLine2;

            // skip blank lines and sat name lines and get line 1
            // also filter on sat ID
            String lineSatID = "";
            do
            {
                nextLine1 = tleReader.readLine();
                if (nextLine1 != null)
                    lineSatID = nextLine1.substring(2, 7);
            }
            while (nextLine1 != null && (nextLine1.length() < 69 || !satID.equals(lineSatID)));

            // return if EOF
            if (nextLine1 == null)
            {
                nextLine1 = currentLine1;
                nextLine2 = currentLine2;
                return true;
            }

            // skip blank lines and get line 2
            do
            {
                nextLine2 = tleReader.readLine();
            }
            while (nextLine2.length() == 0 && nextLine2.length() < 69);
            lineNumber++;
            
            return false;
        }
        catch (IOException e)
        {
            return true;
        }
    }
    
    
    /**
     * Parse one TLE entry and create a TLEInfo object 
     * @param lineBuffer1
     * @param lineBuffer2
     * @return
     */
    protected TLEInfo parseTLE(String lineBuffer1, String lineBuffer2)
    {
        TLEInfo tle = new TLEInfo();
        String text;
        
        // sat id
        text = lineBuffer1.substring(2, 7).trim();
        tle.satID = Integer.parseInt(text);
        
        // julian time
        tle.tleTime = getJulian(lineBuffer1);
        
        // bstar
        text = lineBuffer1.charAt(53) + "0." + lineBuffer1.substring(54,59).trim() + "e" + lineBuffer1.substring(59,61);
        tle.bstar = Double.parseDouble(text);
        
        // inclination
        text = lineBuffer2.substring(8,16).trim();
        tle.inclination = Double.parseDouble(text) * DTR;
        
        // right ascension
        text = lineBuffer2.substring(17,25).trim();
        tle.rightAscension = Double.parseDouble(text) * DTR;
        
        // eccentricity
        text = "0." + lineBuffer2.substring(26,33).trim();
        tle.eccentricity= Double.parseDouble(text);
        
        // arg of perigee
        text = lineBuffer2.substring(34,42).trim();
        tle.argOfPerigee = Double.parseDouble(text) * DTR;
        
        // mean anomaly
        text = lineBuffer2.substring(43,51).trim();
        tle.meanAnomaly = Double.parseDouble(text) * DTR;
        
        // mean motion
        text = lineBuffer2.substring(52,63).trim();
        tle.meanMotion = Double.parseDouble(text) * 2*Math.PI / SECONDS_PER_DAY; // convert to rad/s
        
        // rev number
        text = lineBuffer2.substring(63,68).trim();
        tle.revNumber = Integer.parseInt(text);
        
        return tle;
    }
    
    
    /**
     * Get Julian time for Line1 of TLE entry
     * @param lineBuffer
     * @return
     */
    protected double getJulian(String lineBuffer)
    {
        int year = Integer.valueOf(lineBuffer.substring(18, 20).trim()).intValue();
        double doyFrac = Double.valueOf(lineBuffer.substring(20, 32).trim()).doubleValue();

        // change to 4 digit year with Y2K check (good til 2050)
        year = (year < 50) ? 2000 + year : 1900 + year;

        // convert to julian time
        double julianTime = (doyFrac - 1.0) * 3600 * 24;
        for (int i = 1970; i < year; i++)
            julianTime += (isLeapYear(i)) ? SECONDS_PER_LEAPYEAR : SECONDS_PER_YEAR;

        return julianTime;
    }
    
    
    protected boolean isLeapYear(int year)
    {
        if((year%4)==0 && ( (year%100)!=0 || (year%400)==0 ))
            return true;
        else
            return false;
    }
    
    
    /**
     * Call to close the TLE data file when done
     */
    public void closeFile()
    {
        try
        {
            if (tleReader != null)
            {
                tleReader.close();
                tleReader = null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    protected void finalize()
    {
        closeFile();
    }
}