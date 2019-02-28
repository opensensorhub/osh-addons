/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.dio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.DeviceNotFoundException;
import jdk.dio.InvalidDeviceConfigException;
import jdk.dio.UnavailableDeviceException;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.I2CConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Communication provider for exchanging data with a slave device connected
 * to the I2C bus. This driver is based no JDK Device I/O.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 29, 2015
 */
public class JdkDioI2CCommProvider extends AbstractModule<JdkDioI2CCommProviderConfig> implements ICommProvider<JdkDioI2CCommProviderConfig>
{
    static final Logger log = LoggerFactory.getLogger(JdkDioI2CCommProvider.class);
    
    I2CDevice i2c;
    InputStream is;
    OutputStream os;
    
    
    public JdkDioI2CCommProvider() 
    {
    }
    
    
    @Override
    public void start() throws SensorHubException
    {        
        I2CConfig config = this.config.protocol;
        String deviceString = String.format("I2C device (Bus %d, Addr %H)", config.busNumber, config.deviceAddress);
        
        try
        {
            I2CDeviceConfig i2cConf = new I2CDeviceConfig(
                    config.busNumber,
                    config.deviceAddress,
                    DeviceConfig.DEFAULT,
                    DeviceConfig.DEFAULT);
            
            // open I2C device
            i2c = DeviceManager.<I2CDevice>open(i2cConf);
            
            // obtain input/output streams
            is = Channels.newInputStream(i2c);
            os = Channels.newOutputStream(i2c);
            
            log.info("Connected to {}", deviceString);
        }
        catch (InvalidDeviceConfigException e)
        {
            throw new SensorHubException("Invalid configuration for " + deviceString, e);
        }
        catch (DeviceNotFoundException e)
        {
            throw new SensorHubException("Unknown " + deviceString, e);
        }
        catch (UnavailableDeviceException e)
        {
            throw new SensorHubException(deviceString + " is currently in use", e);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot connect to " + deviceString, e);
        }
        catch (UnsatisfiedLinkError e)
        {
            throw new SensorHubException("Cannot load Device I/O native library", e);
        }
    }
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return is;
    }


    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return os;
    }    


    @Override
    public void stop() throws SensorHubException
    {
        if (i2c != null)
        {
            try
            {
                i2c.close();
            }
            catch (IOException e)
            {
            }
            
            i2c = null;
        }
        
        is = null;
        os = null;
    }
    
    
    public I2CDevice getI2CDevice()
    {
        return i2c;
    }


    @Override
    public void cleanup() throws SensorHubException
    {        
    }
}
