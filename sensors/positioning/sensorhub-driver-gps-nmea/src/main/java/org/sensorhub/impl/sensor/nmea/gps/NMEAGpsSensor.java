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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * Driver implementation for NMEA 0183 compatible GPS units
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public class NMEAGpsSensor extends AbstractSensorModule<NMEAGpsConfig>
{
    public static final String GLL_MSG = "GLL";
    public static final String GGA_MSG = "GGA";
    public static final String GSA_MSG = "GSA";
    public static final String RMC_MSG = "RMC";
    public static final String VTG_MSG = "VTG";
    public static final String ZDA_MSG = "ZDA";
    public static final String HDT_MSG = "HDT";
    
    ICommProvider<?> commProvider;
    BufferedReader reader;
    volatile boolean started;
    
    HashSet<String> activeMessages = new HashSet<String>();
    double lastFixUtcTime = Double.NaN;
    
    
    public NMEAGpsSensor()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:osh:sensor:nmea-gps:", config.serialNumber);
        generateXmlID("GPS_SENSOR_", config.serialNumber);
        
        // create outputs depending on selected sentences
        if (config.activeSentences.contains(GLL_MSG) ||
            config.activeSentences.contains(GGA_MSG))
        {
            LLALocationOutput dataInterface = new LLALocationOutput(this);
            addOutput(dataInterface, false);
            dataInterface.init();
        }
        
        if (config.activeSentences.contains(GSA_MSG))
        {
            GPSQualityOutput dataInterface = new GPSQualityOutput(this);
            addOutput(dataInterface, true);
            dataInterface.init();
        }
        
        if (config.activeSentences.contains(VTG_MSG) ||
            config.activeSentences.contains(HDT_MSG))
        {
            NEDVelocityOutput dataInterface = new NEDVelocityOutput(this);
            addOutput(dataInterface, false);
            dataInterface.init();
        }
        
        this.activeMessages.addAll(config.activeSentences);
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
           
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("NMEA 0183 compatible GNSS receiver");
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        if (started)
            return;
        
        // init comm provider
        if (commProvider == null)
        {
            // we need to recreate comm provider here because it can be changed by UI
            try
            {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");
                
                commProvider = config.commSettings.getProvider();
                commProvider.start();
            }
            catch (Exception e)
            {
                commProvider = null;
                throw e;
            }
        }
        
        // connect to data stream
        try
        {
            reader = new BufferedReader(new InputStreamReader(commProvider.getInputStream(), StandardCharsets.US_ASCII));
            getLogger().info("Connected to NMEA data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }
        
        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (started)
                {
                    pollAndSendMeasurement();
                }
                
                reader = null;
            }
        });
        
        started = true;
        t.start();
    }
    
    
    private void pollAndSendMeasurement()
    {
        String msg = null;
        
        try
        {
            // read next message
            msg = reader.readLine();
            long msgTime = System.currentTimeMillis();
            
            // if null, it's EOF
            if (msg == null)
                return;            
            
            getLogger().trace("Received message: {}", msg);
            
            // discard messages not starting with $ or with wrong checksum
            if (msg.charAt(0) != '$' || !validateChecksum(msg))
            {
                getLogger().warn("Skipping invalid message: {}", msg);
                return;
            }
            
            // extract NMEA message type (remove $TalkerID prefix)
            int firstSep = msg.indexOf(',');
            String msgID = msg.substring(3, firstSep);
            
            // let each registered output handle this message
            if (activeMessages.contains(msgID))
            {
                for (ISensorDataInterface output: this.getAllOutputs().values())
                {
                    NMEAGpsOutput nmeaOut = (NMEAGpsOutput)output;
                    nmeaOut.handleMessage(msgTime, msgID, msg);
                }
            }
        }
        catch (EOFException e)
        {
            // do nothing
            // this happens when reader is closed in stop() method
            started = false;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while parsing NMEA stream", e);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while parsing NMEA message: " + msg, e);
        }
    }
    
    
    /*
     * Check message is error free
     */
    protected boolean validateChecksum(String msg)
    {
        int checkSumIndex = msg.lastIndexOf('*');
        if (checkSumIndex > 0)
        {
            // extract message checksum
            int msgCheckSum = Integer.parseInt(msg.substring(checkSumIndex + 1), 16);
            
            // compute our own checksum
            int checkSum = 0;
            for (int i = 1; i < checkSumIndex; i++)
                checkSum ^= (byte)(msg.charAt(i) & 0xFF);
                        
            // warn and return false if not equal
            if (checkSum != msgCheckSum)
            {
                getLogger().warn("Wrong checksum {} for message: {}", checkSum, msg);
                return false;
            }
        }
        
        return true;
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        started = false;
        
        if (reader != null)
        {
            try { reader.close(); }
            catch (IOException e) { }
        }
        
        if (commProvider != null)
        {
            commProvider.stop();
            commProvider = null;
        }
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return (commProvider != null);
    }
}
