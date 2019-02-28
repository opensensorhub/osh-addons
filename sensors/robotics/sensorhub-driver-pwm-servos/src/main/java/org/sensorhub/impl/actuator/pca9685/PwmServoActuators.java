/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.actuator.pca9685;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.dio.JdkDioI2CCommProvider;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Driver implementation to control servos connected to Adafruit PCA9685
 * 16-channels PWM daughter board using I2C commands.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public class PwmServoActuators extends AbstractSensorModule<PwmServosConfig>
{
    static final Logger log = LoggerFactory.getLogger(PwmServoActuators.class);
    
    JdkDioI2CCommProvider i2cCommProvider;
    volatile boolean started;
    
    
    public PwmServoActuators()
    {        
    }
    
    
    @Override
    public void init() throws SensorHubException
    {
        super.init(config);
        
        // generate IDs
        generateUniqueID("urn:osh:actuator:servos:", null);
        generateXmlID("PWM_SERVOS_", null);      
        
        // create control inputs
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            sensorDescription.setDescription("Adafruit PCA9685 16-channels PWM servo board");
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        if (started)
            return;
                
        // init comm provider
        if (i2cCommProvider == null)
        {
            // we need to recreate comm provider here because it can be changed by UI
            try
            {
                if (config.commSettings == null)
                    throw new SensorHubException("No communication settings specified");
                
                i2cCommProvider = (JdkDioI2CCommProvider)config.commSettings.getProvider();
                i2cCommProvider.start();
            }
            catch (Exception e)
            {
                i2cCommProvider = null;
                throw e;
            }
        }
        
        
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        started = false;
        
        if (i2cCommProvider != null)
        {
            i2cCommProvider.stop();
            i2cCommProvider = null;
        }
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return (i2cCommProvider != null);
    }
}
