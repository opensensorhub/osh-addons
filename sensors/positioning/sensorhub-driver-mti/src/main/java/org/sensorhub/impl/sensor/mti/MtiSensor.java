/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mti;

import net.opengis.sensorml.v20.IdentifierList;
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
public class MtiSensor extends AbstractSensorModule<MtiConfig>
{
    static final Logger log = LoggerFactory.getLogger(MtiSensor.class);
    protected final static String CRS_ID = "SENSOR_FRAME";
        
    ICommProvider<?> commProvider;
    MtiOutput dataInterface;
    
    
    public MtiSensor()
    {       
    }


    @Override
    public void init() throws SensorHubException
    {
        super.init();
        
        // generate IDs
        generateUniqueID("urn:xsens:imu:mti:", null);
        generateXmlID("XSENS_IMU_", null);
                
        // create main data interface
        dataInterface = new MtiOutput(this);
        addOutput(dataInterface, false);
        dataInterface.init();
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            SMLFactory smlFac = new SMLFactory();
            sensorDescription.setDescription("XSens MTi Inertial Motion Unit");
            
            IdentifierList identifiers = smlFac.newIdentifierList();
            sensorDescription.getIdentificationList().add(identifiers);
            Term term;
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("Manufacturer"));
            term.setLabel("Manufacturer Name");
            term.setValue("XSens");
            identifiers.addIdentifier2(term);
            
            term = smlFac.newTerm();
            term.setDefinition(SWEHelper.getPropertyUri("ModelNumber"));
            term.setLabel("Model Number");
            term.setValue("MTi 28A53G35");
            identifiers.addIdentifier2(term);
            
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
        
        if (config.decimFactor > 0)
            dataInterface.decimFactor = config.decimFactor;
        dataInterface.start(commProvider);
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
}