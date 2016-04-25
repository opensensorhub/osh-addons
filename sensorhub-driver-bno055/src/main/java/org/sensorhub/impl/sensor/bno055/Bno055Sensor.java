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
import java.util.Arrays;
import net.opengis.sensorml.v20.ClassifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
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
    protected final static String STATE_CALIB_DATA = "calib_data";
    
    ICommProvider<? super CommConfig> commProvider;
    DataInputStreamLI dataIn;
    DataOutputStreamLI dataOut;
    Bno055Output dataInterface;
    byte[] calibData;
    
    
    public Bno055Sensor()
    {
    }


    @Override
    public void init(Bno055Config config) throws SensorHubException
    {
        super.init(config);
        
        // generate identifiers: use serial number from config or first characters of local ID
        String sensorID = config.serialNumber;
        if (sensorID == null)
        {
            int endIndex = Math.min(config.id.length(), 8);
            sensorID = config.id.substring(0, endIndex);
        }
        this.uniqueID = "urn:bosch:bno055:" + sensorID;
        this.xmlID = "BOSCH_BNO055_" + sensorID.toUpperCase();
        
        // create main data interface
        dataInterface = new Bno055Output(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
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
                setOperationMode(Bno055Constants.OPERATION_MODE_CONFIG);
                Thread.sleep(650);
                setPowerMode(Bno055Constants.POWER_MODE_NORMAL);
                //readCalibration(); // read calibration from sensor
                //Thread.sleep(10);
                setTriggerMode((byte)0);
                setOperationMode(Bno055Constants.OPERATION_MODE_NDOF);
            }
            catch (Exception e)
            {
                commProvider.stop();
                commProvider = null;
                throw new SensorHubException("Error sending init commands", e);
            }
        }
        
        if (config.decimFactor > 0)
            dataInterface.decimFactor = config.decimFactor;
        dataInterface.start(commProvider);
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
    }
    
    
    protected void setMode(byte address, byte mode) throws IOException
    {
        // build command
        byte[] setModeCmd = new byte[] {
            Bno055Constants.START_BYTE,
            Bno055Constants.DATA_WRITE,
            address,
            1,
            mode
        };
        
        // write command to serial port
        OutputStream os = commProvider.getOutputStream();
        os.write(setModeCmd);
        os.flush();
        
        // check ACK
        InputStream is = commProvider.getInputStream();
        int b0 = is.read();
        int b1 = is.read();
        if (b0 != (0xEE & 0xFF) && b1 != 1)
            throw new IOException("Register Write Error");
        
        // wait 30ms for mode switch to complete
        try { Thread.sleep(30); }
        catch (InterruptedException e) { }
    }
    
    
    protected void loadCalibration() throws IOException
    {
        
    }
    
    
    /* read calibration from sensor */
    protected void readCalibration()
    {
        try
        {
            // build command
            byte[] readCalibCmd =
            {
                Bno055Constants.START_BYTE,
                Bno055Constants.DATA_READ,
                Bno055Constants.CALIB_ADDR,
                Bno055Constants.CALIB_SIZE
            };
            
            sendReadCommand(readCalibCmd);
            
            // skip length byte and read 22 bytes
            dataIn.read();
            calibData = new byte[22];
            dataIn.read(calibData);
            
            getLogger().info("Calibration data is {}", Arrays.toString(calibData));
        }
        catch (Exception e)
        {
            getLogger().error("Cannot read calibration data from sensor", e);
        }
    }
    
    
    protected void sendReadCommand(byte[] readCmd) throws IOException
    {
        dataOut.write(readCmd);
        dataOut.flush();
        
        // check for error
        int firstByte = dataIn.read();
        if (firstByte != (Bno055Constants.ACK_BYTE & 0xFF))
            throw new IOException(String.format("Register Read Error: %02X", firstByte));
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
                is.read(calibData);
            }
        }
        catch (Exception e)
        {
            getLogger().error("Cannot load calibration data", e);
        }        
    }


    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        super.saveState(saver);        
        
        if (calibData != null)
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
    
    
    protected Logger getLogger()
    {
        return super.getLogger();
    }
}