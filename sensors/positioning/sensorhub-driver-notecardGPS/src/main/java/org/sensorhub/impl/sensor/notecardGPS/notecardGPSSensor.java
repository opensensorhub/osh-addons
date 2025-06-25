/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.notecardGPS;

import org.sensorhub.impl.sensor.notecardGPS.config.Config;
import org.sensorhub.impl.sensor.notecardGPS.outputs.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

// Pi4J Variables:
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;


/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class notecardGPSSensor extends AbstractSensorModule<Config> implements Runnable {
    static final String UID_PREFIX = "osh:notecardGPS:";
    static final String XML_PREFIX = "notecardGPS";

    private static final Logger logger = LoggerFactory.getLogger(notecardGPSSensor.class);

    ///  REQUIRED VARIABLES FOR SENSOR OPERATION
    // I2C Initialization Variables from Pi4j
    I2C i2c;
    Context pi4j;
    I2CProvider i2CProvider;

    // Class Variables to handle output operations during the sensor's primary readSensor() method
    notecardGPSOutput notecardGPSOutput;

    // Local variables
    private volatile boolean keepRunning = false;

    // Cosmetic debugging Variables Used to make font bold
    String BoldOn = "\033[1m";  // ANSI code to turn on bold
    String BoldOff = "\033[0m"; // ANSI code to turn on bold

    ///  INITIALIZE Notecard OVER I2C
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create I2C connection
        createI2cConnection();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // Initialize Sensor Outputs
        notecardGPSOutput = new notecardGPSOutput(this);
        addOutput(notecardGPSOutput, false);
        notecardGPSOutput.doInit();

        // RESTORE NOTECARD TO FACTOR SETTINGS
        Transaction(notecardGPSConstantsI2C.RESTORE);
    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        String HubSet = "{\"req\":\"hub.set\",\"product\":\"" + config.NCconfig.NHproductUID + "\",\"mode\":\"minimum\"}";

        // CONFIGURE THE NOTECARD
        Transaction(HubSet);
        Transaction(notecardGPSConstantsI2C.ENABLE_ACCELEROMETER);
        Transaction(notecardGPSConstantsI2C.TURNON_TRIANGLULATION);
        Transaction(notecardGPSConstantsI2C.SET_LOCATION_CONT);

        keepRunning = true;
        Thread readNoteCardworker = new Thread(this, "Notecard Worker");
        readNoteCardworker.start();
    }

    @Override
    public void doStop() throws SensorHubException {
        super.doStop();
        keepRunning = false;
        Transaction(notecardGPSConstantsI2C.HUBSET_OFF);
        Transaction(notecardGPSConstantsI2C.DISABLE_ACCELEROMETER);
        Transaction(notecardGPSConstantsI2C.TURNOFF_TRIANGLULATION);
        Transaction(notecardGPSConstantsI2C.DISABLE_GPS);
    }

    @Override
    public boolean isConnected() {
        return true; //output.isAlive()
    }

    @Override
    public void run() {
        while (keepRunning){
            try {
                Transaction(notecardGPSConstantsI2C.GET_LOC);
                Thread.sleep(config.NCconfig.gpsSampleRate * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    // SENSOR SPECIFIC METHODS
    public void Transaction(String jsonMsg){

        System.out.println(BoldOn + "Sending Message: " + BoldOff + jsonMsg);

        try {
            // Using the json message provided, create a byte array for transaction over i2c
            byte[] messageBytes = (jsonMsg + "\n").getBytes(StandardCharsets.UTF_8);    // Must add '\n' to the end of the message
            int length = messageBytes.length;                                           // Length is required when sending a message transaction to notecard

            // Create a new Byte Array [<length of array>, <each byte of array>]
            byte[] reqBytes = new byte[length + 1];
            reqBytes[0] = (byte) length;                                                // prepend the length
            System.arraycopy(messageBytes, 0, reqBytes, 1, length);     // create combined array with length at the beginning

            // Send Message to Notecard
            i2c.write(reqBytes);
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        readNotecard(jsonMsg);
    }

    public void readNotecard(String prevReq) {
        // Poll the Notecard  and retrieve the length of stored message in the i2c que
        int msgLength = getMsgQueLength();
        StringBuilder response = new StringBuilder();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        if(msgLength<=0){
            response.append("No Data in Que");
        }

        while(msgLength>0){
            try {
                // SEND REQUEST TO NOTECARD THAT A READ IS ABOUT TO HAPPEN
                byte[]readReq = new byte[]{0x00, (byte) (msgLength)};
                i2c.write(readReq);
                Thread.sleep(300);

                // READ DESIGNATED DATA FROM NOTECARD
                byte[] buffer = new byte[msgLength+2];    // in addition to the message length, the notecard sends a prefix [<how much is left>, <how much in this message>]
                i2c.read(buffer);
                Thread.sleep(300);

                // CONTINUE TO WRITE BUFFER MESSAGES TO OUTPUT
                out.write(buffer,2,buffer.length-2); // skip prefix of buffer

                // UPDATE MSG LENGTH TO CONTINUE LOOP OR COMPLETE
                msgLength = getMsgQueLength();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        byte[] jsonBytes = out.toByteArray();
        String json = convertSignedBytesToJson(jsonBytes);

        /// IF REQUEST IS CARD.LOCATION, THEN GATHER DATA FOR OUTPUT
        if(!notecardGPSConstantsI2C.GET_LOC.equals(prevReq)){
            // Convert JSON to Object and Send Output
            Gson gson = new Gson();
            GPSmsg gpsmsg = gson.fromJson(json,GPSmsg.class);

            notecardGPSOutput.SetData( gpsmsg.status, gpsmsg.mode, gpsmsg.time, gpsmsg.lat, gpsmsg.lon );

        }

        response.append(json);
        System.out.println(BoldOn + "RESPONSE: \n" + BoldOff + response);
    }

    public String convertSignedBytesToJson(byte[] buffer){
        StringBuilder json = new StringBuilder();
        for (byte b : buffer) {
            char c = (char) (b & 0xFF);
            json.append(c);
        }

        return json.toString();
    }

    public int getMsgQueLength(){
        int msgLength;

        try {
            // SEND AN EMPTY READ REQUEST TO NOTECARD. THIS WILL LINE UP THE READ QUE WITH LENGTH OF MESSAGE (IF ANY)
            byte[] requestRead = new byte[]{0x00, (byte) 0};
            i2c.write(requestRead);

            Thread.sleep(300);

            // NOW READ A SINGLE BYTE FROM THE QUE
            byte[] buffer = new byte[1];
            i2c.read(buffer);
            Thread.sleep(300);

            msgLength = (buffer[0] & 0xFF); // convert to unsigned byte so that value remains positive between 0 and 255.

        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return msgLength;
    }

    static class GPSmsg {
        public String status;
        public String mode;
        public float lat;
        public float lon;
        public long time;
    }


    public void createI2cConnection(){
        // Initialize pi4j and i2c configuration
        pi4j = Pi4J.newAutoContext();
        i2CProvider = pi4j.provider("linuxfs-i2c");

        I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
                .bus(config.connection.I2C_BUS_NUMBER)
                .device(config.connection.SENSOR_ADDRESS)
                .name("notecardGPS")
                .build();
        System.out.println("Building I2C Connection...");
        logger.info("Building I2C Connection...");
        try {
            this.i2c = i2CProvider.create(i2cConfig);
            System.out.println(BoldOn + "I2C Connection Established" + BoldOff + "\n\tBus/Port:" + config.connection.I2C_BUS_NUMBER + "\n\tSensor Address: 0x" + Integer.toHexString(config.connection.SENSOR_ADDRESS));
            logger.info("{}I2C Connection Established{}\n\tBus/Port:{}\n\tSensor Address: 0x{}", BoldOn, BoldOff, config.connection.I2C_BUS_NUMBER, Integer.toHexString(config.connection.SENSOR_ADDRESS));

        } catch (Exception e) {
            System.out.println("I2C Connection Failed...check I2C bus and Sensor Address");
            logger.error("I2C Connection Failed...check I2C bus and Sensor Address");
            throw new RuntimeException(e);
        }
    }

}
