/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm.rxtx;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.RS232Config;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Communication provider for serial ports
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since July 2, 2015
 */
public class RxtxSerialCommProvider extends AbstractModule<RS232Config> implements ICommProvider<RS232Config>
{
    static final Logger log = LoggerFactory.getLogger(RxtxSerialCommProvider.class.getSimpleName());
    
    SerialPort serialPort;
    InputStream is;
    OutputStream os;
    
    
    public RxtxSerialCommProvider() 
    {
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        try
        {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(config.portName);
                
            if (portIdentifier.isCurrentlyOwned())
            {
                throw new PortInUseException();
            }
            else
            {
                CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);
                
                if (commPort instanceof SerialPort)
                {
                    serialPort = (SerialPort) commPort;
                                        
                    // configure serial port
                    serialPort.setSerialPortParams(
                            config.baudRate,
                            config.dataBits,
                            config.stopBits,
                            SerialPort.PARITY_NONE);
                    
                    is = serialPort.getInputStream();
                    os = serialPort.getOutputStream();
                    
                    log.info("Connected to serial port {}", config.portName);
                }
                else
                {
                    log.error("Port {} is not a serial port", config.portName);
                }
            }
        }
        catch (NoSuchPortException e)
        {
            throw new SensorHubException("Invalid serial port " + config.portName);
        }
        catch (PortInUseException e)
        {
            throw new SensorHubException("Port " + config.portName + " is currently in use");
        }
        catch (UnsupportedCommOperationException e)
        {
            throw new SensorHubException("Invalid serial port configuration");
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot connect to serial port " + config.portName);
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
        if (serialPort != null)
        {
            serialPort.close();
            serialPort = null;
        }
        
        is = null;
        os = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {        
    }
}
