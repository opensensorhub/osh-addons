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
import org.sensorhub.impl.comm.UARTConfig;
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
public class RxtxSerialCommProvider extends AbstractModule<RxtxSerialCommProviderConfig> implements ICommProvider<RxtxSerialCommProviderConfig>
{
    static final Logger log = LoggerFactory.getLogger(RxtxSerialCommProvider.class);
    
    SerialPort serialPort;
    InputStream is;
    OutputStream os;
    
    
    public RxtxSerialCommProvider() 
    {
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        UARTConfig config = this.config.protocol;
        
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
                    
                    // get parity code
                    int parity;                    
                    if (config.parity == null)
                    {
                        parity = SerialPort.PARITY_NONE;
                    }
                    else
                    {
                        switch (config.parity)
                        {
                            case PARITY_EVEN:
                                parity = SerialPort.PARITY_EVEN;
                                break;
                                
                            case PARITY_ODD:
                                parity = SerialPort.PARITY_ODD;
                                break;
                                
                            case PARITY_MARK:
                                parity = SerialPort.PARITY_MARK;
                                break;
                                
                            case PARITY_SPACE:
                                parity = SerialPort.PARITY_SPACE;
                                break;
                                
                            default:
                                parity = SerialPort.PARITY_NONE;
                        }
                    }
                                        
                    // configure serial port
                    serialPort.setSerialPortParams(
                            config.baudRate,
                            config.dataBits,
                            config.stopBits,
                            parity);
                    
                    // set read thresholds
                    if (config.receiveTimeout >= 0)
                        serialPort.enableReceiveTimeout(config.receiveTimeout);
                    else
                        serialPort.disableReceiveTimeout();
                    
                    if (config.receiveThreshold >= 0)
                        serialPort.enableReceiveThreshold(config.receiveThreshold);
                    else
                        serialPort.disableReceiveThreshold();
                        
                    // obtain input/output streams
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
            throw new SensorHubException("Invalid serial port configuration for " + config.portName);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot connect to serial port " + config.portName);
        }
        catch (UnsatisfiedLinkError e)
        {
            throw new SensorHubException("Cannot load RX/TX native library", e);
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
