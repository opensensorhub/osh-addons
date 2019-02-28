/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.bno055;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.sensorml.v20.ClassifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLFactory;
import org.vast.swe.DataInputStreamLI;
import org.vast.swe.DataOutputStreamLI;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Driver for XSens MTi Inertial Motion Unit
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since July 1, 2015
 */
public class Bno055Sensor extends AbstractSensorModule<Bno055Config>
{
    protected final static String CRS_ID = "SENSOR_FRAME";
    
    private final static String STATE_CALIB_DATA = "calib_data";
    
    private final static byte[] READ_CALIB_STAT_CMD =
    {
        Bno055Constants.START_BYTE,
        Bno055Constants.DATA_READ,
        Bno055Constants.CALIB_STAT_ADDR,
        1
    };
    
    private final static byte[] READ_CALIB_DATA_CMD =
    {
        Bno055Constants.START_BYTE,
        Bno055Constants.DATA_READ,
        Bno055Constants.CALIB_ADDR,
        Bno055Constants.CALIB_SIZE
    };
    
    
    ICommProvider<?> commProvider;
    DataInputStreamLI dataIn;
    DataOutputStreamLI dataOut;
    Bno055Output dataInterface;
    byte[] calibData;
    Timer calibTimer;
    
    
    public Bno055Sensor()
    {
    }


    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate identifiers: use serial number from config or first characters of local ID
        generateUniqueID("urn:bosch:bno055:", config.serialNumber);
        generateXmlID("BOSCH_BNO055_", config.serialNumber);
        
        // create main data interface
        dataInterface = new Bno055Output(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Bosch BNO055 absolute orientation sensor");
            
            SMLFactory smlFac = new SMLFactory();
            ClassifierList classif = smlFac.newClassifierList();
            sensorDescription.getClassificationList().add(classif);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("Bosch");
            classif.addClassifier(term);
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
            term.setLabel("Model Number");
            term.setValue("BNO055");
            classif.addClassifier(term);
            
            SpatialFrame localRefFrame = smlFac.newSpatialFrame();
            localRefFrame.setId(CRS_ID);
            localRefFrame.setOrigin("Position of Accelerometers (as marked on the plastic box of the device)");
            localRefFrame.addAxis("X", "The X axis is in the plane of the circuit board");
            localRefFrame.addAxis("Y", "The Y axis is in the plane of the circuit board");
            localRefFrame.addAxis("Z", "The Z axis is orthogonal to circuit board, pointing outward from the component face");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        // init comm provider
        if (commProvider == null)
        {
            // we need to recreate comm provider here because it can be changed by UI
            if (config.commSettings == null)
                throw new SensorHubException("No communication settings specified");
            commProvider = config.commSettings.getProvider();
            commProvider.start();

            // connect to comm data streams
            try
            {   
                dataIn = new DataInputStreamLI(commProvider.getInputStream());
                dataOut = new DataOutputStreamLI(commProvider.getOutputStream());
                getLogger().info("Connected to IMU data stream");
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error while initializing communications ", e);
            }
            
            // send init commands
            try
            {
                reset();
                
                setOperationMode(Bno055Constants.OPERATION_MODE_CONFIG);
                
                setPowerMode(Bno055Constants.POWER_MODE_NORMAL);
                setTriggerMode((byte)0);
                
                // load saved calibration coefs
                if (calibData != null)
                    loadCalibration();
                
                setOperationMode(Bno055Constants.OPERATION_MODE_NDOF);
            }
            catch (Exception e)
            {
                commProvider.stop();
                commProvider = null;
                throw new SensorHubException("Error sending init commands", e);
            }
            
            // monitor calibration status
            if (getLogger().isTraceEnabled())
            {
                calibTimer = new Timer();
                calibTimer.schedule(new TimerTask()
                {
                    public void run()
                    {
                        boolean calibOk = showCalibStatus();
                        if (calibOk)
                            cancel();
                    }
                }, 20L, 500L);
            }
        }
        
        dataInterface.start(commProvider);
    }
    
    
    protected void reset() throws IOException
    {
        byte[] resetCmd = new byte[] {
            Bno055Constants.START_BYTE,
            Bno055Constants.DATA_WRITE,
            Bno055Constants.SYS_TRIGGER_ADDR,
            1,
            0x20
        };
        
        sendWriteCommand(resetCmd, false);
        
        try { Thread.sleep(650); }
        catch (InterruptedException e) { }
    }
    
    
    protected void setPowerMode(byte mode) throws IOException
    {
        setMode(Bno055Constants.POWER_MODE_ADDR, mode);
    }
    
    
    protected void setTriggerMode(byte mode) throws IOException
    {
        setMode(Bno055Constants.SYS_TRIGGER_ADDR, mode);
    }
    
    
    protected void setOperationMode(byte mode) throws IOException
    {
        setMode(Bno055Constants.OPERATION_MODE_ADDR, mode);
        try { Thread.sleep(50); }
        catch (InterruptedException e) { }
    }
    
    
    protected void setMode(byte address, byte mode) throws IOException
    {
        // wait for previous command to complete
        try { Thread.sleep(30); }
        catch (InterruptedException e) { }
        
        // build command
        byte[] setModeCmd = new byte[] {
            Bno055Constants.START_BYTE,
            Bno055Constants.DATA_WRITE,
            address,
            1,
            mode
        };
        
        sendWriteCommand(setModeCmd, true);
        
        // wait for mode switch to complete
        try { Thread.sleep(30); }
        catch (InterruptedException e) { }
    }
    
    
    protected boolean showCalibStatus()
    {
        try
        {
            ByteBuffer resp = sendReadCommand(READ_CALIB_STAT_CMD);
            
            // read calib status byte
            byte calStatus = resp.get();
            int sys = (calStatus >> 6) & 0x03;
            int gyro = (calStatus >> 4) & 0x03;
            int accel = (calStatus >> 2) & 0x03;
            int mag = calStatus & 0x03;
            
            getLogger().trace("Calib Status: sys={}, gyro={}, accel={}, mag={}", sys, gyro, accel, mag);
            
            if (sys == 3 && gyro == 3 && accel == 3 && mag == 3)
                return true;
        }
        catch (IOException e)
        {            
        }
        
        return false;
    }
    
    
    /* load calibration data to sensor */
    protected void loadCalibration() throws Exception
    {
        try
        {
            if (calibData.length != Bno055Constants.CALIB_SIZE)
                throw new IOException("Calibration data must be " + Bno055Constants.CALIB_SIZE + " bytes");
            
            // build command
            byte[] setCalCmd = new byte[4 + Bno055Constants.CALIB_SIZE];
            setCalCmd[0] = Bno055Constants.START_BYTE;
            setCalCmd[1] = Bno055Constants.DATA_WRITE;
            setCalCmd[2] = Bno055Constants.CALIB_ADDR;
            setCalCmd[3] = Bno055Constants.CALIB_SIZE;
            System.arraycopy(calibData, 0, setCalCmd, 4, Bno055Constants.CALIB_SIZE);
            
            sendWriteCommand(setCalCmd, true);            
            getLogger().debug("Loaded calibration data: {}", Arrays.toString(calibData));
        }
        catch (IOException e)
        {
            throw new SensorHubException("Error loading calibration table", e);
        }
    }
    
    
    /* read calibration data from sensor */
    protected void readCalibration()
    {
        try
        {
            setOperationMode(Bno055Constants.OPERATION_MODE_CONFIG);
            
            ByteBuffer resp = sendReadCommand(READ_CALIB_DATA_CMD);
            calibData = resp.array();          
            getLogger().debug("Read calibration data: {}", Arrays.toString(calibData));
        }
        catch (Exception e)
        {
            getLogger().error("Cannot read calibration data from sensor", e);
        }
    }
    
    
    protected synchronized ByteBuffer sendReadCommand(byte[] readCmd) throws IOException
    {
        // flush any pending received data to get into a clean state
        while (dataIn.available() > 0)
            dataIn.read();
        
        dataOut.write(readCmd);
        dataOut.flush();
        
        // check for error
        int b0 = dataIn.read();
        if (b0 != (Bno055Constants.ACK_BYTE & 0xFF))
            throw new IOException(String.format("Register Read Error: %02X", dataIn.read()));
        
        // read response
        int length = dataIn.read();
        byte[] response = new byte[length];
        dataIn.readFully(response);
        ByteBuffer buf = ByteBuffer.wrap(response);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf;
    }
    
    
    protected synchronized void sendWriteCommand(byte[] writeCmd, boolean checkAck) throws IOException
    {
        int nAttempts = 0;
        int maxAttempts = 5;
        while (nAttempts < maxAttempts)
        {
            nAttempts++;
            
            // flush any pending received data to get into a clean state
            while (dataIn.available() > 0)
                dataIn.read();
            
            // write command to serial port
            dataOut.write(writeCmd);
            dataOut.flush();
            
            // check ACK
            if (checkAck)
            {
                int b0 = dataIn.read();
                int b1 = dataIn.read();
                
                if (b0 != (0xEE & 0xFF) || b1 != 0x01)
                {
                    String msg = String.format("Register Write Error: 0x%02X 0x%02X (%d)", b0, b1, nAttempts);
                    if (b1 != 0x07 || nAttempts >= maxAttempts)
                        throw new IOException(msg);
                    getLogger().warn(msg);
                    continue;
                }
            }
            
            return;
        }
    }
    

    @Override
    public void loadState(IModuleStateManager loader) throws SensorHubException
    {
        super.loadState(loader);
        
        try
        {
            InputStream is = loader.getAsInputStream(STATE_CALIB_DATA);
            if (is != null)
            {
                calibData = new byte[22];
                int nBytes = is.read(calibData);
                if (nBytes != Bno055Constants.CALIB_SIZE)
                    throw new IOException("Calibration data must be " + Bno055Constants.CALIB_SIZE + " bytes");
            }
        }
        catch (Exception e) 
        {
            getLogger().error("Cannot load calibration data", e);
            calibData = null;
        }        
    }


    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        super.saveState(saver);        
        
        if (calibData != null && calibData.length == Bno055Constants.CALIB_SIZE)
        {
            try
            {
                OutputStream os = saver.getOutputStream(STATE_CALIB_DATA);
                os.write(calibData);
                os.flush();
            }
            catch (IOException e)
            {
                getLogger().error("Cannot save calibration data", e);
            }
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (calibTimer != null)
            calibTimer.cancel();
        
        if (dataInterface != null)
            dataInterface.stop();
                        
        if (commProvider != null)
        {
            readCalibration();
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