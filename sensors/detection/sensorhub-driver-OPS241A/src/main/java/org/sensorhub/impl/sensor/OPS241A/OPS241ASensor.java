/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.OPS241A;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import org.sensorhub.impl.sensor.OPS241A.config.OPS241AConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// REQUIRED IF USING OSH RXTX CONFIGURATION
//import org.sensorhub.api.comm.ICommProvider;
import org.vast.swe.DataInputStreamLI;
import org.vast.swe.DataOutputStreamLI;

// REQUIRED FOR HANDLING MY RXTX CONNECTION
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class OPS241ASensor extends AbstractSensorModule<OPS241AConfig> implements Runnable{
    static final String UID_PREFIX = "osh:OPS241A:";
    static final String XML_PREFIX = "OPS241A";

    private static final Logger logger = LoggerFactory.getLogger(OPS241ASensor.class);

    /// GLOBAL VARIABLES FOR SENSOR OPERATION
    SerialPort serialPort;
    InputStream dataIn;
    OutputStream dataOut;
    OPS241AOutput ops241aOutput;

    String uom;
    String unitCommand;

    // Local variables
    private volatile boolean keepRunning = false;

    // Cosmetic debugging Variables Used to make font bold
    String BoldOn = "\033[1m";  // ANSI code to turn on bold
    String BoldOff = "\033[0m"; // ANSI code to turn on bold

    ///  INITIALIZE
    @Override
    public void doInit() throws SensorHubException  {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // ASSIGN LOCAL VARIABLES uom and unitCommand for Sensor and Output configuration
        establishUnits(config.unit);

        // INITIALIZE OUTPUT INSTANCE
        ops241aOutput = new OPS241AOutput(this);
        addOutput(ops241aOutput, false);
        ops241aOutput.doInit();

    }

    private void establishUnits(OPS241AConfig.Units unit) {
        switch (unit){
            case METERS_PER_SECOND:
                unitCommand = OPS241aConstants.M_PER_SEC_CMD;
                uom = OPS241AConfig.Units.METERS_PER_SECOND.toString();
                break;
            case CENTIMETERS_PER_SECOND:
                unitCommand = OPS241aConstants.CM_PER_SEC_CMD;
                uom = OPS241AConfig.Units.CENTIMETERS_PER_SECOND.toString();
                break;
            case FEET_PER_SECOND:
                unitCommand = OPS241aConstants.FT_PER_SEC_CMD;
                uom = OPS241AConfig.Units.FEET_PER_SECOND.toString();
                break;
            case KILOMETERS_PER_HOUR:
                unitCommand = OPS241aConstants.KM_PER_HR_CMD;
                uom = OPS241AConfig.Units.KILOMETERS_PER_HOUR.toString();
                break;
            case MILES_PER_HOUR:
                unitCommand = OPS241aConstants.MILES_PER_HR_CMD;
                uom = OPS241AConfig.Units.MILES_PER_HOUR.toString();
                break;
        }
    }

    @Override
    public void doStart() throws SensorHubException, InterruptedException {
        super.doStart();

        try {
            establishRxTxConnection(config.RxTxSettings.portAddress, config.RxTxSettings.baudRate); // ESTABLISH RxTx CONNECTION
            sendSensorCommand(OPS241aConstants.RESET_SETTINGS_CMD);                             // RESET SENSOR TO REMOVE ANY PREVIOUS CONFIGURATION
            sendSensorCommand(unitCommand);                                                     // Set Sensor Units
        } catch (IOException | UnsupportedCommOperationException | PortInUseException e) {
            throw new RuntimeException(e);
        }

        // Set variable to continue readings
        keepRunning = true;

        // CREATE THREAD THAT CONTINUALLY READS SENSOR REPORT
        Thread readOPS241A = new Thread( this, "OPS241A Worker");
        readOPS241A.start();    // This starts the the run() method

    }

    @Override
    public void doStop() throws SensorHubException, InterruptedException {
        super.doStop();
        keepRunning = false;
        if (serialPort != null){
            serialPort.close();
        }
    }

    @Override
    public boolean isConnected() {
        return true; //output.isAlive()
    }

    ///  THE FOLLOWING (2) METHODS HANDLE SENSOR READINGS. THE SENSOR WILL SEND NUMERICAL VALUES INDICATING READINGS
    /// OR THE SENSOR WILL SEND JSON STYLE MESSAGES
    public void handleJsonMsg(String jsonMsg) {
        System.out.println(jsonMsg);
        logger.info("Sensor Message: {}", jsonMsg);
    }

    public void handleSensorReading (String sensorReading) {
        try {
//            System.out.println(Double.parseDouble(sensorReading));
            ops241aOutput.SetData(Double.parseDouble(sensorReading));
        } catch (NumberFormatException e) {
            System.out.println("Unknown reading: " + sensorReading);
        }
    }

    // SEND A COMMAND TO THE SENSOR TO UPDATE SETTINGS OR RETRIEVE INFORMATION
    public void sendSensorCommand(String cmd) throws IOException {
        dataOut.write((cmd+ "\r\n").getBytes());
        dataOut.flush();
    }
    // CREATE A RXTX CONNECTION
    public void establishRxTxConnection(String portAddress, int baudRate) throws IOException, PortInUseException, UnsupportedCommOperationException {
        // ESTABLISH RXTX CONNECTION
        Enumeration<?> portList = CommPortIdentifier.getPortIdentifiers();
        CommPortIdentifier portId = null;

        System.out.println(BoldOn + "PORT LIST: " + BoldOff + portList );
        System.out.println(BoldOn + "EACH ITEM: " + BoldOff );

        while(portList.hasMoreElements()){
            CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
            System.out.println("id: " + id + "\nid name: " + id.getName());
            if(id.getName().equals(portAddress)){
                portId = id;
                break;
            }
        }

        if (portId == null){
            System.out.println("Could not find '" + portAddress + "'");
            return;
        }

        serialPort = portId.open("OPS241Reader", 2000);
        serialPort.setSerialPortParams(
            baudRate,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE
        );

        dataIn = serialPort.getInputStream();
        dataOut = serialPort.getOutputStream();

        dataOut.write((OPS241aConstants.RESET_SETTINGS_CMD + "\r\n").getBytes());


    }

    @Override
    public void run() {
        ByteArrayOutputStream reportLine = new ByteArrayOutputStream();
        int b;

        while (keepRunning){
            try {
                if ((b = dataIn.read()) != -1) {
                    if (b == '\n' || b == '\r') {
                        if (reportLine.size() > 0) {
                            String line = reportLine.toString().trim();
                            reportLine.reset();

                            if(line.startsWith("{") && line.endsWith("}")){
                                handleJsonMsg(line);
                            } else {
                                handleSensorReading(line);
                            }
                        }
                    } else {
                        reportLine.write(b);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

}
