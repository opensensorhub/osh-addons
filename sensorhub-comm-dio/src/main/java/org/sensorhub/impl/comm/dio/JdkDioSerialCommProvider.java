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
import jdk.dio.UnavailableDeviceException;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTConfig;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Communication provider for UART serial ports using JDK Device I/O
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 27, 2015
 */
public class JdkDioSerialCommProvider extends AbstractModule<JdkDioSerialCommProviderConfig> implements ICommProvider<JdkDioSerialCommProviderConfig>
{
    static final Logger log = LoggerFactory.getLogger(JdkDioSerialCommProvider.class);
    
    UART uart;
    InputStream is;
    OutputStream os;
    
    
    public JdkDioSerialCommProvider() 
    {
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        org.sensorhub.impl.comm.UARTConfig config = this.config.protocol;
        
        try
        {
            // figure out constant for parity setting
            int parity = UARTConfig.PARITY_NONE;
            if (config.parity != null)
            {
                switch (config.parity)
                {
                    case PARITY_EVEN:
                        parity = UARTConfig.PARITY_EVEN;
                        break;
                        
                    case PARITY_ODD:
                        parity = UARTConfig.PARITY_ODD;
                        break;
                        
                    case PARITY_MARK:
                        parity = UARTConfig.PARITY_MARK;
                        break;
                        
                    case PARITY_SPACE:
                        parity = UARTConfig.PARITY_SPACE;
                        break;
                        
                    default:
                }
            }
            
            // set UART parameters
            UARTConfig uartConf = new UARTConfig(
                    config.portName,
                    DeviceConfig.DEFAULT,
                    config.baudRate,
                    config.dataBits,
                    parity,
                    config.stopBits,
                    UARTConfig.FLOWCONTROL_NONE);
            
            // open UART
            uart = DeviceManager.<UART>open(uartConf);
            
            // set read thresholds
            if (config.receiveTimeout >= 0)
                uart.setReceiveTimeout(config.receiveTimeout);
            else
                uart.setReceiveTimeout(Integer.MAX_VALUE);
            
            if (config.receiveThreshold >= 0)
                uart.setReceiveTriggerLevel(config.receiveThreshold);
            else
                uart.setReceiveTriggerLevel(0);
            
            // obtain input/output streams
            is = Channels.newInputStream(uart);
            os = Channels.newOutputStream(uart);
            
            log.info("Connected to serial device {}", config.portName);
        }
        catch (DeviceNotFoundException e)
        {
            throw new SensorHubException("Unknown serial device " + config.portName, e);
        }
        catch (UnavailableDeviceException e)
        {
            throw new SensorHubException("Serial device " + config.portName + " is currently in use", e);
        }
        catch (UnsupportedOperationException e)
        {
            throw new SensorHubException("Invalid serial device configuration for " + config.portName, e);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot connect to serial device " + config.portName, e);
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
        if (uart != null)
        {
            try
            {
                uart.close();
            }
            catch (IOException e)
            {
            }
            
            uart = null;
        }
        
        is = null;
        os = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {        
    }
}
