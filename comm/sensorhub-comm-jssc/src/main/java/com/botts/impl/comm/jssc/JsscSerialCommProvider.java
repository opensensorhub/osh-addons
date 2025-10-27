/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.comm.jssc;

import jssc.SerialPort;
import jssc.SerialPortException;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.UARTConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * <p>
 * Communication provider for serial ports
 * </p>
 *
 * @author Alex Robin
 * @since July 2, 2015
 */
public class JsscSerialCommProvider extends AbstractModule<JsscSerialCommProviderConfig> implements ICommProvider<JsscSerialCommProviderConfig>
{
    static final Logger log = LoggerFactory.getLogger(JsscSerialCommProvider.class);
    
    SerialPort serialPort;
    InputStream is;
    OutputStream os;
    
    
    public JsscSerialCommProvider()
    {
    }
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
        UARTConfig config = this.config.protocol;

        serialPort = new SerialPort(config.portName);

        try
        {
            if (!serialPort.openPort())
                throw new SensorHubException("Failed to open serial port " + config.portName);

            // get parity code
            int parity = SerialPort.PARITY_NONE;
            if (config.parity != null) {
                switch (config.parity) {
                    case PARITY_EVEN -> parity = SerialPort.PARITY_EVEN;
                    case PARITY_ODD -> parity = SerialPort.PARITY_ODD;
                    case PARITY_MARK -> parity = SerialPort.PARITY_MARK;
                    case PARITY_SPACE -> parity = SerialPort.PARITY_SPACE;
                }
            }

            // configure serial port
            serialPort.setParams(
                    config.baudRate,
                    config.dataBits,
                    config.stopBits,
                    parity);

            serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);

            // obtain input/output streams
            is = new SerialInputStream(serialPort);
            os = new SerialOutputStream(serialPort);

            getLogger().info("Serial port {} opened: {} baud, {} data bits, {} stop bits, parity {}",
                    config.portName, config.baudRate, config.dataBits, config.stopBits, config.parity);

        } catch (SerialPortException e) {
            throw new SensorHubException("Failed to open serial port " + config.portName);
        }
    }
    
    
    @Override
    public InputStream getInputStream()
    {
        return is;
    }


    @Override
    public OutputStream getOutputStream()
    {
        return os;
    }    


    @Override
    protected void doStop() throws SensorHubException
    {
        if (serialPort != null) {
            try {
                if (serialPort.isOpened()) {
                    serialPort.closePort();
                    getLogger().info("Serial port closed");
                }
            } catch (SerialPortException e) {
                throw new SensorHubException("Error closing serial port", e);
            } finally {
                serialPort = null;
            }
        }
        is = null;
        os = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {        
    }
}
