/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nmea.gps;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Vector;
import org.vast.swe.SWEConstants;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.util.DateTimeFormat;


/**
 * <p>
 * Output providing GNSS receiver location in EPSG:4979.<br/>
 * This class tries to extract location data from either GGA, GLL or RMC
 * messages. Since some location messages only contain UTC time (and no date),
 * we also try to read UTC day from RMC or ZDA messages when available.*
 * Otherwise UTC day is obtained from system clock.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public class LLALocationOutput extends NMEAGpsOutput
{
    static final long SECONDS_PER_DAY = 24*3600L;
    static final long MILLIS_PER_DAY = SECONDS_PER_DAY*1000L;
    
    double lastFixUtcDateTime = Double.NaN;
    double lastFixUtcTimeValue = Double.NaN;
    GregorianCalendar cal;
    
    
    public LLALocationOutput(NMEAGpsSensor parentSensor)
    {
        super(parentSensor);
        this.samplingPeriod = 1.0; // default to 1Hz on startup
        
        this.cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    
    @Override
    public String getName()
    {
        return "gpsLocation";
    }

    
    protected void init()
    {
        GeoPosHelper fac = new GeoPosHelper();
        
        // SWE Common data structure
        dataStruct = fac.newDataRecord(3);
        dataStruct.setName(getName());
        dataStruct.addComponent("time", fac.newTimeStampIsoUTC());
        
        Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
        locVector.setLabel("Location");
        locVector.setDescription("Location measured by GPS device");
        dataStruct.addComponent("location", locVector);
        
        dataEncoding = fac.newTextEncoding(",", "\n");
    }
    
    
    protected void handleMessage(long msgTime, String msgID, String msg)
    {
        DataBlock dataBlock = null;
                
        // init with system date if no GNSS message has been received for a while
        // this is needed to properly compute unix time of messages that contain only the time of day
        double now = System.currentTimeMillis() / 1000.;
        if (Double.isNaN(lastFixUtcDateTime) || (now - lastFixUtcDateTime) > 600.0)
            lastFixUtcDateTime = now;
        
        // process different message types
        if (msgID.equals(NMEAGpsSensor.GGA_MSG))
        {
            String[] tokens = msg.split(NMEA_SEP_REGEX);
                        
            // skip if position fix not available
            if (tokens[6].charAt(0) == '0')
            {
                log.debug("GGA: No position fix");
                return;
            }
            
            // skip if location was already processed for this fix time
            String utcTimeToken = tokens[1];
            double utcTime = Double.parseDouble(utcTimeToken);
            if (utcTime == lastFixUtcTimeValue)
                return;
            
            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, toJulianTime(utcTimeToken));
            dataBlock.setDoubleValue(1, toDecimalDegrees(tokens[2], tokens[3], false)); // lat
            dataBlock.setDoubleValue(2, toDecimalDegrees(tokens[4], tokens[5], true)); // lon
            dataBlock.setDoubleValue(3, toEllipsoidalHeight(tokens[9], tokens[11])); // alt           
        }
        
        else if (msgID.equals(NMEAGpsSensor.RMC_MSG))
        {
            String[] tokens = msg.split(NMEA_SEP_REGEX);
            
            // skip if data is marked as invalid
            if (tokens[2].charAt(0) != 'A')
            {
                log.debug("RMC: No position fix");
                return;
            }
            
            // skip if location was already processed
            String utcTimeToken = tokens[1];
            double utcTime = Double.parseDouble(utcTimeToken);
            if (utcTime == lastFixUtcTimeValue)
                return;
            
            // populate datablock
            dataBlock = getNewDataBlock();
            dataBlock.setDoubleValue(0, toJulianTime(utcTimeToken, tokens[9]));
            dataBlock.setDoubleValue(1, toDecimalDegrees(tokens[3], tokens[4], false)); // lat
            dataBlock.setDoubleValue(2, toDecimalDegrees(tokens[5], tokens[6], true)); // lon 
            dataBlock.setDoubleValue(3, Double.NaN); // alt
        }
        
        else if (msgID.equals(NMEAGpsSensor.ZDA_MSG))
        {
            String[] tokens = msg.split(NMEA_SEP_REGEX);
            
            // UTC date
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(tokens[2]));
            cal.set(Calendar.MONTH, Integer.parseInt(tokens[3])-1);
            cal.set(Calendar.YEAR, Integer.parseInt(tokens[4]));
            
            // UTC time
            String utcTimeToken = tokens[1];
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(utcTimeToken.substring(0, 2)));
            cal.set(Calendar.MINUTE, Integer.parseInt(utcTimeToken.substring(2, 4)));            
            double seconds = Double.parseDouble(utcTimeToken.substring(4));
            cal.set(Calendar.SECOND, (int)seconds);
            double subSeconds = seconds - Math.floor(seconds);
            cal.set(Calendar.MILLISECOND, (int)(subSeconds*1000));
            
            setLastFixUtcDateTime(cal.getTimeInMillis() / 1000.0);         
        }
        
        if (dataBlock != null)
            sendOutput(msgTime, dataBlock);
    }
    
    
    protected double toJulianTime(String utcTime)
    {
        // UTC time of day
        int hours = Integer.parseInt(utcTime.substring(0, 2));
        int minutes = Integer.parseInt(utcTime.substring(2, 4));
        double seconds = Double.parseDouble(utcTime.substring(4));
        
        // combine with current day
        double secondsAtBeginingOfDay = Math.floor(lastFixUtcDateTime / SECONDS_PER_DAY) * SECONDS_PER_DAY;
        double fixDateTime = secondsAtBeginingOfDay + hours*3600. + minutes*60. + seconds;
        
        // handle change of day
        if (fixDateTime < lastFixUtcDateTime)
            fixDateTime += SECONDS_PER_DAY;
        
        lastFixUtcTimeValue = Double.parseDouble(utcTime);
        return setLastFixUtcDateTime(fixDateTime);
    }
    
    
    protected double toJulianTime(String utcTime, String utcDate)
    {
        // UTC date
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(utcDate.substring(0, 2)));
        cal.set(Calendar.MONTH, Integer.parseInt(utcDate.substring(2, 4))-1);
        cal.set(Calendar.YEAR, 2000 + Integer.parseInt(utcDate.substring(4, 6)));
        
        // UTC time
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(utcTime.substring(0, 2)));
        cal.set(Calendar.MINUTE, Integer.parseInt(utcTime.substring(2, 4)));        
        double seconds = Double.parseDouble(utcTime.substring(4));
        cal.set(Calendar.SECOND, (int)seconds);
        double subSeconds = seconds - Math.floor(seconds);
        cal.set(Calendar.MILLISECOND, (int)(subSeconds*1000));
        
        lastFixUtcTimeValue = Double.parseDouble(utcTime);
        return setLastFixUtcDateTime(cal.getTimeInMillis() / 1000.0);
    }
    
    
    protected double toDecimalDegrees(String latLon, String signIndicator, boolean lon)
    {
        double val = Double.NaN;
        
        // convert to decimal degrees
        int sep = lon ? 3 : 2;
        String integerDegrees = latLon.substring(0, sep);
        String decimalMinutes = latLon.substring(sep);
        val = Double.parseDouble(integerDegrees) + Double.parseDouble(decimalMinutes) / 60.;
                
        // handle sign according to N/S or E/W indicator
        char dir = signIndicator.charAt(0);
        if (dir == 'S' || dir == 'W')
            return -val;
        else
            return val;
    }
    
    
    protected double setLastFixUtcDateTime(double fixDateTime)
    {
        // log date of first fix or all
        if (Double.isNaN(lastFixUtcDateTime))
            log.info("First GPS Fix on {}", new DateTimeFormat().formatIso(fixDateTime, 0));
        else if (log.isTraceEnabled())
            log.trace("GPS Fix on {}", new DateTimeFormat().formatIso(fixDateTime, 0));
        
        lastFixUtcDateTime = fixDateTime;
        parentSensor.lastFixUtcTime = lastFixUtcDateTime;
        
        return fixDateTime;
    }
    
    
    protected double toEllipsoidalHeight(String mslAlt, String geoidSep)
    {
        return Double.parseDouble(mslAlt) + Double.parseDouble(geoidSep);
    }
}
