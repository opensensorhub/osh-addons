/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.vectornav;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.opengis.sensorml.v20.ClassifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Driver for XSens MTi Inertial Motion Unit
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since July 1, 2015
 */
public class VN200Sensor extends AbstractSensorModule<VN200Config>
{
    static final Logger log = LoggerFactory.getLogger(VN200Sensor.class);
    
    protected final static String CRS_ID = "SENSOR_FRAME";
    protected final static byte SYNC = (byte)0xFA;
    protected final static double BASE_FREQ = 800.0;
    
    ICommProvider<?> commProvider;
    VN200QuatOutput quatOutput;
    VN200GpsOutput gpsOutput;
    
    boolean started;
    DataInputStream dataIn;
    ByteBuffer readBuffer = ByteBuffer.allocate(32);
    
    
    public VN200Sensor()
    {       
    }


    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate IDs
        generateUniqueID("urn:vectornav:imu:", null);
        generateXmlID("VNAV_INS_", null);
        
        // create data interfaces
        quatOutput = new VN200QuatOutput(this, config.attSamplingFactor / BASE_FREQ);
        quatOutput.init();
        addOutput(quatOutput, false);
                
        gpsOutput = new VN200GpsOutput(this, config.gpsSamplingFactor / BASE_FREQ);
        gpsOutput.init();
        addOutput(gpsOutput, false);        
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setDescription("VectorNav VN200 GPS Aided Inertial Navigation System");
            
            ClassifierList classif = smlFac.newClassifierList();
            sensorDescription.getClassificationList().add(classif);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("VectorNav Technologies");
            classif.addClassifier(term);
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
            term.setLabel("Model Number");
            term.setValue("VN200");
            classif.addClassifier(term);
            
            SpatialFrame localRefFrame = smlFac.newSpatialFrame();
            localRefFrame.setId(CRS_ID);
            localRefFrame.setOrigin("Position of Accelerometers (as marked on the plastic box of the device)");
            localRefFrame.addAxis("X", "The X axis is in the plane of the aluminum mounting plate, parallel to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Y", "The Y axis is in the plane of the aluminum mounting plate, orthogonal to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Z", "The Z axis is orthogonal to the aluminum mounting plate, so that the frame is direct (as marked on the plastic box of the device)");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
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
            // TODO do that in updateConfig
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
        
        try
        {
            // init sensor config
            sendInitCommands();
                
            // connect to data stream
            dataIn = new DataInputStream(commProvider.getInputStream());
            VN200Sensor.log.info("Connected to IMU data stream");
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
                    processNextMessage();
                }                

                dataIn = null;
            }
        });
        t.start();
        
        started = true;
    }
    
    
    protected void sendInitCommands() throws IOException
    {
        OutputStream os = commProvider.getOutputStream();
        StringBuilder buf;
        
        // enable attitude message
        buf = new StringBuilder();
        buf.append("$VNWRG,75,1,") // async data on serial port 1
           .append(config.attSamplingFactor).append(',') // output rate
           .append(0x10) // enable output group 4 
           .append(0x04) // enable field 'Quaternion' 
           .append("*XX");
        os.write(buf.toString().getBytes(StandardCharsets.US_ASCII));
        
        // enable GPS message
        buf = new StringBuilder();
        buf.append("$VNWRG,76,1,") // async data on serial port 1
           .append(config.gpsSamplingFactor).append(',') // output rate
           .append(0x08) // enable output group 4 
           .append(0x21) // enable fields 'UTC' and 'PosLla'
           .append("*XX");
        os.write(buf.toString().getBytes(StandardCharsets.US_ASCII));
    }
    
    
    protected boolean processNextMessage()
    {
        try
        {
            // wait for sync
            byte b = 0;
            while (b != SYNC)
                b = dataIn.readByte();
            
            // prepare for reading message
            readBuffer.clear();
            long timeStamp = System.currentTimeMillis();
            VN200AbstractOutput output;
            int payloadLength;
            
            // read group config and select corresponding output
            byte groups = dataIn.readByte();
            readBuffer.put(groups);
            switch (groups)
            {
                case 8:
                    payloadLength = 32+4;
                    output = gpsOutput;
                    break;
                    
                case 16:
                    payloadLength = 16+4;
                    output = quatOutput;
                    break;
                    
                default:
                    VN200Sensor.log.debug("Unexpected group config: " + Integer.toHexString(groups));
                    return false;
            }
            
            // read data to buffer
            dataIn.read(readBuffer.array(), 1, payloadLength);
            readBuffer.limit(payloadLength);
            
            // check CRC
            if (!checkCRC(readBuffer))
            {
                VN200Sensor.log.debug("Wrong message CRC");
                return false;
            }
            
            // let output class decode the payload
            readBuffer.position(3);
            output.decodeAndSendMeasurement(timeStamp, readBuffer);            
        }
        catch (IOException e)
        {
            // log error except when stopping voluntarily
            if (started)
                VN200Sensor.log.error("Error while decoding INS stream. Stopping", e);
            started = false;
            return false;
        }
        
        return true;
    }
    
    
    protected boolean checkCRC(ByteBuffer readBuffer)
    {
        readBuffer.flip();
        int crc = 0;
        
        // compute message CRC
        for (int i = 0; i < readBuffer.limit()-2; i++)
        {
            crc = (crc >> 8) | (crc << 8);
            crc ^= readBuffer.get() & 0xff;
            crc ^= (crc & 0xff) >> 4;
            crc ^= crc << 12;
            crc ^= (crc & 0xff) << 5;
            crc &= 0xFFFF;            
        }
        
        // compare with embedded CRC
        int readCrc = readBuffer.getShort() & 0xFFFF;
        if (readCrc != crc)
            return false;
        
        return true;
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
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
        return (commProvider != null); // TODO also send ID command to check that sensor is really there
    }
}