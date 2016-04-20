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
import net.opengis.sensorml.v20.ClassifierList;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.sensorml.v20.Term;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
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
public class Bno055Sensor extends AbstractSensorModule<Bno055Config>
{
    protected final static String CRS_ID = "SENSOR_FRAME";
        
    ICommProvider<? super CommConfig> commProvider;
    Bno055Output dataInterface;
    
    
    public Bno055Sensor()
    {       
    }


    @Override
    public void init(Bno055Config config) throws SensorHubException
    {
        super.init(config);
        
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
            
            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setId("Bosch_BNO055");
            sensorDescription.setDescription("Bosch BNO055 Absolute Orientation Sensor");
            
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
            localRefFrame.addAxis("X", "The X axis is in the plane of the aluminum mounting plate, parallel to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Y", "The Y axis is in the plane of the aluminum mounting plate, orthogonal to the serial connector (as marked on the plastic box of the device)");
            localRefFrame.addAxis("Z", "The Z axis is orthogonal to the aluminum mounting plate, so that the frame is direct (as marked on the plastic box of the device)");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        // init comm provider
        if (commProvider == null)
        {
            if (config.commSettings == null)
                throw new SensorHubException("No communication settings specified");
            commProvider = config.commSettings.getProvider();
            commProvider.start();
            
            // we need to recreate comm provider here because it can be changed by UI
            // TODO do that in updateConfig
            try
            {   
                // send init commands
                setOperationMode(Bno055Constants.OPERATION_MODE_CONFIG);
                Thread.sleep(650);
                setPowerMode(Bno055Constants.POWER_MODE_NORMAL);
                setTriggerMode((byte)0);
                setOperationMode(Bno055Constants.OPERATION_MODE_NDOF);
            }
            catch (Exception e)
            {
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
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        if (dataInterface != null)
            dataInterface.stop();
                    
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
    
    
    protected Logger getLogger()
    {
        return super.getLogger();
    }
}