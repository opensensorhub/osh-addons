/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.BNO085;

import org.sensorhub.impl.sensor.BNO085.config.Bno085Config;
import org.sensorhub.impl.sensor.BNO085.outputs.*;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Pi4J Variables:
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class Bno085Sensor extends AbstractSensorModule<Bno085Config> implements Runnable {
    static final String UID_PREFIX = "osh:bno085:";
    static final String XML_PREFIX = "BNO085";

    private static final Logger logger = LoggerFactory.getLogger(Bno085Sensor.class);

    ///  REQUIRED VARIABLES FOR SENSOR OPERATION
    // I2C Initialization Variables from Pi4j
    I2C i2c;
    Context pi4j;
    I2CProvider i2CProvider;

    // Class Variables to handle output operations during the sensor's primary readSensor() method
    AccelerometerOutput accelerometerOutput;
    GravityOutput gravityOutput;
    GyroCalibratedOutput gyroCalOutput;
    MagFieldCalibratedOutput magFieldCalOutput;
    RotationOutput rotationOutput;

    // Local variables
    private volatile boolean keepRunning = false;
    List<Byte> activeReportIds = new ArrayList<>();

    // Cosmetic debugging Variables Used to make font bold
    String BoldOn = "\033[1m";  // ANSI code to turn on bold
    String BoldOff = "\033[0m"; // ANSI code to turn on bold

    ///  INITIALIZE BNO085 SENSOR OVER I2C
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create I2C connection
        createI2cConnection();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // Initialize Sensor Outputs selected in the Admin Panel
        createConfiguredOutputs();
        try {
            resetSensor();  // Reset Sensor after initialization to clean/flush any past data
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void doStart() throws SensorHubException, InterruptedException {
        super.doStart();
        byte[] timeIntervalArray;
        long timeIntervalMicro = config.outputs.timeIntervalSeconds * 1000000L; // BNO085 Sensor uses µS to set sensor features
        timeIntervalArray = convertTo4ByteArray(timeIntervalMicro);

        for (byte id: activeReportIds){
            logger.info("{}Setting Sensor on BNO085: {}0x{}", BoldOn, BoldOff, Integer.toHexString(id & 0xFF));
            setFeature(id,timeIntervalArray[3],timeIntervalArray[2],timeIntervalArray[1],timeIntervalArray[0]);
        }

        keepRunning = true;
        Thread readBNO085 = new Thread(this, "BNO085 Worker");
        readBNO085.start();
    }

    @Override
    public void doStop() throws SensorHubException, InterruptedException {
        super.doStop();
        keepRunning = false;

        for (byte id: activeReportIds){
            logger.info("{}Turning off Sensor on BNO085{}", BoldOn, BoldOff);
            setFeature(id,(byte)0,(byte)0,(byte)0,(byte)0);
        }

        resetSensor();
    }

    @Override
    public boolean isConnected() {
        return true; //output.isAlive()
    }

    @Override
    public void run() {
        while (keepRunning){
            readSensor();

            try {
                Thread.sleep(config.outputs.timeIntervalSeconds * 1000L); // Thread uses milliseconds
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    // BNO085 SENSOR SPECIFIC METHODS
    /// UTILITY METHODS
    public static byte[] convertTo4ByteArray(long number){
        return new byte[] {
                (byte) (number >>> 24),
                (byte) (number >>> 16),
                (byte) (number >>> 8),
                (byte) number
        };
    }

    public void printBytes(byte[] byteArray){
        StringBuilder results = new StringBuilder(
                "0:\t" + (byteArray[0] & 0xFF) + "\t(LSB Length)" +
                        "\n1:\t" +  (byteArray[1] & 0xFF) +"\t(MSB Length)" +
                        "\n2:\t" +  (byteArray[2] & 0xFF) +"\t(Channel)" +
                        "\n3:\t" +  (byteArray[3] & 0xFF) + "\t(Sequence Number)\n"
        );

        if (byteArray.length>4){
            results.append(4).append(":\t0x").append(Integer.toHexString(byteArray[4] & 0xFF)).append("\t(Report ID)\n");
        }

        if(byteArray.length > 5){
            for (int i = 5; i<byteArray.length; i++){
                results.append(i).append(":\t").append(byteArray[i] & 0xFF).append("\n");
            }
        }
//        System.out.println(results);
    }

    public void createConfiguredOutputs() {

        if(config.outputs.isAccelerometer){
            accelerometerOutput = new AccelerometerOutput(this);
            addOutput(accelerometerOutput, false);
            accelerometerOutput.doInit();

            activeReportIds.add(Bno085ConstantsI2C.ACCELEROMETER_ID);
        }
        if(config.outputs.isGravity){
            gravityOutput = new GravityOutput(this);
            addOutput(gravityOutput, false);
            gravityOutput.doInit();

            activeReportIds.add(Bno085ConstantsI2C.GRAVITY_ID);
        }
        if(config.outputs.isGyroCal){
            gyroCalOutput = new GyroCalibratedOutput(this);
            addOutput(gyroCalOutput, false);
            gyroCalOutput.doInit();

            activeReportIds.add(Bno085ConstantsI2C.GYROSCOP_CALIBRATED_ID);
        }
        if(config.outputs.isMagFieldCal){
            magFieldCalOutput = new MagFieldCalibratedOutput(this);
            addOutput(magFieldCalOutput, false);
            magFieldCalOutput.doInit();

            activeReportIds.add(Bno085ConstantsI2C.MAGNETIC_FIELD_CALIBRATED_ID);
        }
        if(config.outputs.isRotation){
            rotationOutput = new RotationOutput(this);
            addOutput(rotationOutput, false);
            rotationOutput.doInit();

            activeReportIds.add(Bno085ConstantsI2C.ROTATION_VECTOR_ID);
        }
    }

    /// REQUEST/COMMAND METHODS
    public void requestProductInfo() throws InterruptedException {
        System.out.println("Sending Product ID Request...");
        // Build a SHTP Packet
        byte[] packet = new byte[5]; // HEADER + 1-byte Payload for command

        // CREATE BYTE ARRAY FOR WRITE COMMAND
        byte[] productIDRequestMsg = new byte[]{
                (byte) packet.length,                           // Header 1: Length of Message LSB
                0,                                              // Header 2: Length of Message MSB
                Bno085ConstantsI2C.CHANNEL.SH_CONTROL,          // Header 3: Channel
                0,                                              // Header 4: Sequence Number (does not appear to reset for some reason)
                Bno085ConstantsI2C.PRODUCT_ID_REQUEST           // Cargo 1: ID Request from
        };

        //SEND PACKAGE:
        this.i2c.write(productIDRequestMsg);
    }

    public void resetSensor() throws InterruptedException {
        System.out.println(BoldOn + "Resetting Sensor..." + BoldOff);
        logger.info("Resetting Sensor...");
        byte[] setRequest = new byte[21];                               // Create a byte array to hold header info (4) and Set Feature Command Cargo (17)

        setRequest[0] = (byte) 0x15;                                    // Header 1: Length of Message LSB
        setRequest[1] = (byte) 0x00;                                    // Header 2: Length of Message MSB
        setRequest[2] = Bno085ConstantsI2C.CHANNEL.SH_CONTROL;          // Header 3: Channel
        setRequest[3] = (byte)0x00;                                     // Header 4: Sequence Number (does not appear to reset for some reason)
        setRequest[4] = Bno085ConstantsI2C.COMMAND_REQUEST;             // Cargo 0: REPORT ID
        setRequest[5] = (byte)0;                                        // Cargo 1: Sequence Number
        setRequest[6] = Bno085ConstantsI2C.RESET_CMD;                   // Cargo 2: Command
        setRequest[7] = (byte)0;                                        // Cargo 3: P0
        setRequest[8] = (byte)0;                                        // Cargo 4: P1
        setRequest[9] = (byte)0;                                        // Cargo 5: P2
        setRequest[10] = (byte)0;                                       // Cargo 6: P3
        setRequest[11] = (byte)0;                                       // Cargo 7: P4
        setRequest[12] = (byte)0;                                       // Cargo 8: P5
        setRequest[13] = (byte)0;                                       // Cargo 9: P6
        setRequest[14] = (byte)0;                                       // Cargo 10: P7
        setRequest[15] = (byte)0;                                       // Cargo 11: P8


        this.i2c.write(setRequest);
        Thread.sleep(200);
        readSensor();
        System.out.println(BoldOn + "Sensor Reset Complete..." + BoldOff);
        logger.info("Sensor Reset Complete...");
    }

    public void setFeature(byte ReportID, byte time_byte_LSB, byte time_byte_2, byte time_byte_1, byte time_byte_MSB) {
        byte[] setRequest = new byte[21];                               // Create a byte array to hold header info (4) and Set Feature Command Cargo (17)

        setRequest[0] = (byte) 0x15;                                    // Header 1: Length of Message LSB
        setRequest[1] = (byte) 0x00;                                    // Header 2: Length of Message MSB
        setRequest[2] = Bno085ConstantsI2C.CHANNEL.SH_CONTROL;          // Header 3: Channel
        setRequest[3] = (byte)0x00;                                     // Header 4: Sequence Number (does not appear to reset for some reason)
        setRequest[4] = Bno085ConstantsI2C.SET_FEATURE_COMMAND;         // Cargo 0: REPORT ID
        setRequest[5] = ReportID;                                       // Cargo 1: Feature REPORT ID
        setRequest[6] = (byte)0;                                        // Cargo 2: Feature Flags
        setRequest[7] = (byte)0;                                        // Cargo 3: Change Sensitivity LSB
        setRequest[8] = (byte)0;                                        // Cargo 4: Change Sensitivity MSB
        setRequest[9] = time_byte_LSB;                                  // Cargo 5: REPORT INTERVAL LSB
        setRequest[10] = time_byte_2;                                   // Cargo 6: REPORT INTERVAL
        setRequest[11] = time_byte_1;                                   // Cargo 7: REPORT INTERVAL
        setRequest[12] = time_byte_MSB;                                 // Cargo 8: REPORT INTERVAL MSB
        setRequest[13] = (byte)0;                                       // Cargo 9: Batch Interval LSB
        setRequest[14] = (byte)0;                                       // Cargo 10: Batch Interval
        setRequest[15] = (byte)0;                                       // Cargo 11: Batch Interval
        setRequest[16] = (byte)0;                                       // Cargo 12: Batch Interval MSB
        setRequest[17] = (byte)0;                                       // Cargo 13: Sensor configuration word LSB
        setRequest[18] = (byte)0;                                       // Cargo 14: Sensor config word
        setRequest[19] = (byte)0;                                       // Cargo 15: Sensor config word
        setRequest[20] = (byte)0;                                       // Cargo 16: Sensor Configuration word MSB

        this.i2c.write(setRequest);
    }

    /// READ REPORT METHODS
    public void readUnknownReport(byte reportID, int prevMessageLength){
        byte[] unknownReport = new byte[prevMessageLength-1]; // When passed thus function, report ID has already been removed from stream
        this.i2c.read(unknownReport);

        StringBuilder results = new StringBuilder(
                BoldOn + "Uknown Report ID\n" + BoldOff +
                        "0:\t" + (unknownReport[0] & 0xFF) + "\t(LSB Length)\n" +
                        "1:\t" + (unknownReport[1] & 0xFF) +"\t(MSB Length)\n" +
                        "2:\t" + (unknownReport[2] & 0xFF) +"\t(Channel)\n" +
                        "3:\t" + (unknownReport[3] & 0xFF) + "\t(Sequence Number)\n" +
                        "4:\t" + (reportID & 0xFF) + "\t(Unknown Report ID)\n"
        );
        if(unknownReport.length > 5){
            for (int i = 5; i<unknownReport.length; i++){
                results.append(i).append(":\t").append(unknownReport[i] & 0xFF).append("\n");
            }
        }
        System.out.println(results);
    }

    public void readAdPkg(int prevMsgLength) throws InterruptedException {
        byte[] response = new byte[prevMsgLength + 3];          // Account for new header but also removing report ID from previous message (4-1=3)
        this.i2c.read(response);

        StringBuilder results = new StringBuilder(
                BoldOn + "Advertisement Package\n" + BoldOff +
                        "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
                        "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
                        "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
                        "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
                        "4:\t0x00\t(Advertisement Response ID)\n"
        );

        if (response.length>4){
            for (int i = 4; i<response.length; i++){
                results.append(i+1).append(":\t").append(response[i] & 0xFF).append("\n");
            }
        }

//        System.out.println(results);
    }

    // 0XFA --- TIme Stamp Rebase Report
    public void readTimestampRebase() throws InterruptedException {
        byte[] response = new byte[8];          // Length of initial timestamp (4 header + 5 Timestamp reference - ID )
        this.i2c.read(response);

        // Print Time Stamp Info
        String timeStampResponse =
                "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
                        "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
                        "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
                        "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
                        "4:\t0xFA\t(Timestamp Rebase ID)\n" +
                        "5:\t" +  (response[4] & 0xFF) + "\t(Base Delta LSB)\n" +
                        "6:\t" +  (response[5] & 0xFF) + "\t(Base Delta...\n" +
                        "7:\t" +  (response[6] & 0xFF) + "\t(Base Delta...)\n" +
                        "8:\t" +  (response[7] & 0xFF) + "\t(Base Delta MSB)\n";

//        System.out.println(BoldOn + "Timestamp Rebase\n" + BoldOff + timeStampResponse);
    }

    // 0XFB --- Base Time Stamp Report
    public void readBaseTimeStampReference() throws InterruptedException {
        byte[] response = new byte[8];          // Length of initial timestamp (4 header + 5 Timestamp reference - ID )
        this.i2c.read(response);
        // Print Time Stamp Info
        String timeStampResponse =
                "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
                        "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
                        "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
                        "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
                        "4:\t0xFB\t(Base Timestamp Reference ID)\n" +
                        "5:\t" +  (response[4] & 0xFF) + "\t(Base Delta LSB)\n" +
                        "6:\t" +  (response[5] & 0xFF) + "\t(Base Delta...\n" +
                        "7:\t" +  (response[6] & 0xFF) + "\t(Base Delta...)\n" +
                        "8:\t" +  (response[7] & 0xFF) + "\t(Base Delta MSB)\n";

//        System.out.println(BoldOn + "Base Timestamp Reference\n" + BoldOff + timeStampResponse);
    }

    // 0xFC --- FEATURE RESPONSE
    public void getFeatureResponse() throws InterruptedException {
        byte[] response = new byte[20]; // Feature Response is always 21 (4 header + 17 Response)
        this.i2c.read(response);
        // Print out Feature Response to terminal:
        String featureResponse =
                BoldOn + "Feature Response\n" + BoldOff +
                        "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
                        "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
                        "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
                        "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
                        "4:\t0xFC\t(Feature Response)\n" +
                        "5:\t0x" +  (Integer.toHexString(response[4] & 0xFF)) + "\t(Feature Report ID)\n" +
                        "6:\t" +  (response[5] & 0xFF) + "\t(Feature Flags)\n" +
                        "7:\t" +  (response[6] & 0xFF) + "\t(Change Sensitivity LSB)\n" +
                        "8:\t" +  (response[7] & 0xFF) + "\t(Change Sensitivity MSB)\n" +
                        "9:\t" +  (response[8] & 0xFF) + "\t(Report Interval LSB)\n" +
                        "10:\t" +  (response[9] & 0xFF) + "\t(Report Interval...)\n" +
                        "11:\t" +  (response[10] & 0xFF) + "\t(Report Interval...)\n" +
                        "12:\t" +  (response[11] & 0xFF) + "\t(Report Interval MSB)\n" +
                        "13:\t" +  (response[12] & 0xFF) + "\t(Batch Interval LSB)\n" +
                        "14:\t" +  (response[13] & 0xFF) + "\t(Batch Interval...)\n" +
                        "15:\t" +  (response[14] & 0xFF) + "\t(Batch Interval...)\n" +
                        "16:\t" +  (response[15] & 0xFF) + "\t(Batch Interval MSB)\n" +
                        "17:\t" +  (response[16] & 0xFF) + "\t(Config word LSB)\n" +
                        "18:\t" +  (response[17] & 0xFF) + "\t(Config word...)\n" +
                        "19:\t" +  (response[18] & 0xFF) + "\t(Config word...)\n" +
                        "20:\t" +  (response[19] & 0xFF) + "\t(Config word MSB)\n";

//        System.out.println(featureResponse);

    }

    // 0xF1 --- COMMAND RESPONSE
    public void ReadCommandResponse(){
        byte[] byteArray = new byte[19]; // Command Response Length -1 + Header length (16 - 1 + 4 = 19)
        this.i2c.read(byteArray);

        // THIS METHOD IS SPECIFICALLY FOR PRINTING THE RESULTS OF THE PRODUCT ID RESPONSES
        String results =
                BoldOn + "Command Response RESPONSE\n" + BoldOff +
                        "0:\t" +(byteArray[0] & 0xFF) + "\t\t(LSB Length)\n" +
                        "1:\t" +  (byteArray[1] & 0xFF) +"\t\t(MSB Length)\n" +
                        "2:\t" +  (byteArray[2] & 0xFF) +"\t\t(Channel)\n" +
                        "3:\t" +  (byteArray[3] & 0xFF) + "\t\t(Sequence Number)\n"+
                        "4:\t0xF1\t\t(Report ID)\n" +
                        "5:\t" +  (byteArray[4] & 0xFF) + "\t\t(Sequence Number)\n" +
                        "6:\t0x" +  Integer.toHexString(byteArray[5] & 0xFF) + "\t\t(Command Report ID)\n" +
                        "7:\t" +  (byteArray[6] & 0xFF) + "\t\t(Command Sequence Number)\n" +
                        "8:\t" +  (byteArray[7] & 0xFF) + "\t\t(Response Sequence Number)\n" +
                        "9:\t" +  (byteArray[8] & 0xFF) + "\t\t(R0)\n" +
                        "10:\t" +  (byteArray[9] & 0xFF) + "\t\t(R1)\n" +
                        "11:\t" +  (byteArray[10] & 0xFF) + "\t\t(R2)\n" +
                        "12:\t" +  (byteArray[11] & 0xFF) + "\t\t(R3)\n" +
                        "13:\t" +  (byteArray[12] & 0xFF) + "\t\t(R4)\n" +
                        "14:\t" +  (byteArray[13] & 0xFF) + "\t\t(R5)\n" +
                        "15:\t" +  (byteArray[14] & 0xFF) + "\t\t(R6)\n" +
                        "16:\t" +  (byteArray[15] & 0xFF) + "\t\t(R7)\n" +
                        "17:\t" +  (byteArray[16] & 0xFF) + "\t\t(R8)\n" +
                        "18:\t" +  (byteArray[17] & 0xFF) + "\t\t(R9)\n" +
                        "19:\t" +  (byteArray[18] & 0xFF) + "\t\t(R10)\n"
                ;

//        System.out.println(results);
    }

    // 0XF8 --- PRODUCT ID RESPONSE
    public void ReadProductInfo(){
        byte[] byteArray = new byte[19];
        this.i2c.read(byteArray);

        // THIS METHOD IS SPECIFICALLY FOR PRINTING THE RESULTS OF THE PRODUCT ID RESPONSES
        String results =
                BoldOn + "PRODUCT ID RESPONSE\n" + BoldOff +
                        "0:\t" +(byteArray[0] & 0xFF) + "\t\t(LSB Length)\n" +
                        "1:\t" +  (byteArray[1] & 0xFF) +"\t\t(MSB Length)\n" +
                        "2:\t" +  (byteArray[2] & 0xFF) +"\t\t(Channel)\n" +
                        "3:\t" +  (byteArray[3] & 0xFF) + "\t\t(Sequence Number)\n"+
                        "4:\t0xF8\t\t(Report ID)\n" +
                        "5:\t" +  (byteArray[4] & 0xFF) + "\t\t(Reset Cause)\n" +
                        "6-7:\t" +  (((byteArray[6] & 0xFF)<<8) | (byteArray[5] & 0xFF)) + "\t\t(SW Version)\n" +
                        "8-11:\t" +  (((byteArray[10] & 0xFF)<<24) | ((byteArray[9] & 0xFF)<<16) | ((byteArray[8] & 0xFF)<<8) | (byteArray[7] & 0xFF)) + "\t(SW Part Number)\n" +
                        "12-15:\t" +  (((byteArray[14] & 0xFF)<<24) | ((byteArray[13] & 0xFF)<<16) | ((byteArray[12] & 0xFF)<<8) | (byteArray[11] & 0xFF)) + "\t\t(SW Build Number)\n" +
                        "16-17:\t" +  (((byteArray[16] & 0xFF)<<8) | (byteArray[15] & 0xFF)) + "\t\t(SW Version Patch)\n" +
                        "18:\t" +(byteArray[17] & 0xFF) + "\t\t(Reserved)\n" +
                        "19:\t" +  (byteArray[18] & 0xFF) +"\t\t(Reserved)\n"
                ;

//        System.out.println(results);
    }

    // 0X01, 0X04, 0X06 --- ACCELEROMETER. LINEAR ACCELERATION, AND GRAVITY
    public void readAccelerationReport(byte reportId) throws InterruptedException {
        String idName = switch (reportId) {
            case 0x01 -> "Accelerometer";
            case 0x04 -> "Linear Acceleration";
            case 0x06 -> "Gravity";
            default -> "Error";
        };

        byte[] response = new byte[13];            // Header (4) + Input Report (10) - REPORT ID
        this.i2c.read(response);

        int rawX = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawY = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawZ = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);

        // Divide by 256 because values are in Q point 8
        float x = (short) rawX/256.0f;
        float y = (short) rawY/256.0f;
        float z = (short) rawZ/256.0f;

        // Print GRAVITY FEATURE
        String featureResponse =
            "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
            "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
            "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
            "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
            "4:\t0x" + Integer.toHexString(reportId) + "\t(Report ID)\n" +
            "5:\t" +  (response[4] & 0xFF) + "\t(Sequence Number)\n" +
            "6:\t" +  (response[5] & 0xFF) + "\t(Status)\n" +
            "7:\t" +  (response[6] & 0xFF) + "\t(Delay)\n" +
            "8-9:\t"  +  String.format("%.2f", x) + " m/s^2\t(" + idName + " Axis X)\n" + // Must divide by 256 because Q point is 8
            "10-11:\t" + String.format("%.2f", y) + " m/s^2\t(" + idName + " Axis Y)\n" + // Must divide by 256 because Q point is 8
            "12-13:\t" + String.format("%.2f", z) + " m/s^2\t(" + idName + " Axis Z)\n";// Must divide by 256 because Q point is 8  //Must devide by 256 because Q point is 8

//        System.out.println(BoldOn + idName + " Report\n" + BoldOff + featureResponse);
        switch (reportId){
            case Bno085ConstantsI2C.ACCELEROMETER_ID:
                accelerometerOutput.SetData(x,y,z);
                break;
            case Bno085ConstantsI2C.GRAVITY_ID:
                gravityOutput.SetData(x,y,z);
                break;
        }
    }

    // 0X02 ---- CALIBRATED GYROSCOPE
    public void readGyrCalReport(){
        byte[] response = new byte[13];            // Header (4) + Input Report (10) - REPORT ID
        this.i2c.read(response);

        int rawX = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawY = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawZ = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);

        // Divide by 512 because values are in Q point 9
        float x = (short) rawX/512.0f;
        float y = (short) rawY/512.0f;
        float z = (short) rawZ/512.0f;

        // Print GRAVITY FEATURE
        String featureResponse =
            "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
            "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
            "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
            "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
            "4:\t0x02\t(Gyroscope Calibrated ID)\n" +
            "5:\t" +  (response[4] & 0xFF) + "\t(Sequence Number)\n" +
            "6:\t" +  (response[5] & 0xFF) + "\t(Status)\n" +
            "7:\t" +  (response[6] & 0xFF) + "\t(Delay)\n" +
            "8-9:\t"  +  String.format("%.2f", x) + " rad/s\t(Calibrated Axis X)\n" + // Must divide by 256 because Q point is 8
            "10-11:\t" + String.format("%.2f", y) + " rad/s\t(Calibrated Axis Y)\n" + // Must divide by 256 because Q point is 8
            "12-13:\t" + String.format("%.2f", z) + " rad/s\t(Calibrated Axis Z)\n";// Must divide by 256 because Q point is 8  //Must devide by 256 because Q point is 8

//        System.out.println(BoldOn + "Gyroscope Calibrated Report\n" + BoldOff + featureResponse);
        gyroCalOutput.SetData(x,y,z);
    }

    // 0X03 ---- CALIBRATED MAGNETIC FIELD
    public void readMagFieldCalReport(){
        byte[] response = new byte[13];            // Header (4) + Input Report (10) - REPORT ID
        this.i2c.read(response);

        int rawX = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawY = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawZ = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);

        float q = 16.0f;// Divide by 16 because values are in Q point 4

        float x = (short) rawX/q;
        float y = (short) rawY/q;
        float z = (short) rawZ/q;

        // Print GRAVITY FEATURE
        String featureResponse =
            "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
            "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
            "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
            "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
            "4:\t0x03\t(Magnetic Field Calibrated ID)\n" +
            "5:\t" +  (response[4] & 0xFF) + "\t(Sequence Number)\n" +
            "6:\t" +  (response[5] & 0xFF) + "\t(Status)\n" +
            "7:\t" +  (response[6] & 0xFF) + "\t(Delay)\n" +
            "8-9:\t"  +  String.format("%.2f", x) + " µT\t(Calibrated Axis X)\n" +
            "10-11:\t" + String.format("%.2f", y) + " µT\t(Calibrated Axis Y)\n" +
            "12-13:\t" + String.format("%.2f", z) + " µT\t(Calibrated Axis Z)\n";

//        System.out.println(BoldOn + "Magnetic Field Calibrated Report\n" + BoldOff + featureResponse);

        magFieldCalOutput.SetData(x,y,z);
    }

    // 0X05 ---- ROTATION VECTOR
    public void readRotationReport(){
        byte[] response = new byte[17];            // Header (4) + Input Report (14) - REPORT ID
        this.i2c.read(response);

        int rawI = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawJ = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawK = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);
        int rawR = ((response[14] & 0xFF)<<8) | (response[13] & 0xFF);
        int rawA = ((response[16] & 0xFF)<<8) | (response[15] & 0xFF);

        float q1 = 16384.0f;// Q point is 14
        float q2 = 4096.0f;// Q point 12

        float i = (short) rawI/q1;
        float j = (short) rawJ/q1;
        float k = (short) rawK/q1;
        float r = (short) rawR/q1;
        float a = (short) rawA/q2;

        // Print GRAVITY FEATURE
        String featureResponse =
                "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
                        "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
                        "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
                        "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
                        "4:\t0x05\t(Rotation Vector ID)\n" +
                        "5:\t" +  (response[4] & 0xFF) + "\t(Sequence Number)\n" +
                        "6:\t" +  (response[5] & 0xFF) + "\t(Status)\n" +
                        "7:\t" +  (response[6] & 0xFF) + "\t(Delay)\n" +
                        "8-9:\t"  +  String.format("%.2f", i) + "\t(Quaternion i)\n" +
                        "10-11:\t" + String.format("%.2f", j) + "\t(Quaternion j)\n" +
                        "12-13:\t" + String.format("%.2f", k) + "\t(Quaternion K)\n" +
                        "14-15:\t" + String.format("%.2f", r) + "\t(Quaternion Real)\n" +
                        "16-17:\t" + String.format("%.2f", a) + " rad\t(Accuracy Estimate)\n";

//        System.out.println(BoldOn + "Rotation Vector Report\n" + BoldOff + featureResponse);

        rotationOutput.SetData(i, j, k, r, a);
    }

    // 0X07 - UNCALIBRATED GYROSCOPE
    public void readGyrUnCalReport(){
        byte[] response = new byte[19];            // Header (4) + Input Report (16) - REPORT ID
        this.i2c.read(response);

        int rawX = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawY = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawZ = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);
        int rawXbias = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawYbias = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawZbias = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);

        float q = 512.0f; // Divide by 512 because values are in Q point 9

        float x = (short) rawX/q;
        float y = (short) rawY/q;
        float z = (short) rawZ/q;
        float xbias = (short) rawXbias/q;
        float ybias = (short) rawYbias/q;
        float zbias = (short) rawZbias/q;

        // Print GRAVITY FEATURE
        String featureResponse =
            "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
            "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
            "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
            "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
            "4:\t0x07\t(Gyroscope Uncalibrated ID)\n" +
            "5:\t" +  (response[4] & 0xFF) + "\t(Sequence Number)\n" +
            "6:\t" +  (response[5] & 0xFF) + "\t(Status)\n" +
            "7:\t" +  (response[6] & 0xFF) + "\t(Delay)\n" +
            "8-9:\t"  +  String.format("%.2f", x) + " rad/s\t(Axis X)\n" +
            "10-11:\t" + String.format("%.2f", y) + " rad/s\t(Axis Y)\n" +
            "12-13:\t" + String.format("%.2f", z) + " rad/s\t(Axis Z)\n" +
            "14-15:\t"  +  String.format("%.2f", xbias) + " rad/s\t(bias Axis X)\n" +
            "16-17:\t" + String.format("%.2f", ybias) + " rad/s\t(bias Axis Y)\n" +
            "18-19:\t" + String.format("%.2f", zbias) + " rad/s\t(bias Axis Z)\n";

//        System.out.println(BoldOn + "Gyroscope Uncalibrated Report\n" + BoldOff + featureResponse);

    }

    // 0X0F - UNCALIBRATED Magnetic Field
    public void readMagFieldUnCalReport(){
        byte[] response = new byte[19];            // Header (4) + Input Report (16) - REPORT ID
        this.i2c.read(response);

        int rawX = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawY = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawZ = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);
        int rawXbias = ((response[8] & 0xFF)<<8) | (response[7] & 0xFF);
        int rawYbias = ((response[10] & 0xFF)<<8) | (response[9] & 0xFF);
        int rawZbias = ((response[12] & 0xFF)<<8) | (response[11] & 0xFF);

        float q = 16.0f; // Divide by 16 because values are in Q point 4

        float x = (short) rawX/q;
        float y = (short) rawY/q;
        float z = (short) rawZ/q;
        float xbias = (short) rawXbias/q;
        float ybias = (short) rawYbias/q;
        float zbias = (short) rawZbias/q;

        String featureResponse =
                "0:\t" + (response[0] & 0xFF) + "\t(LSB Length)\n" +
                        "1:\t" +  (response[1] & 0xFF) +"\t(MSB Length)\n" +
                        "2:\t" +  (response[2] & 0xFF) +"\t(Channel)\n" +
                        "3:\t" +  (response[3] & 0xFF) + "\t(Sequence Number)\n" +
                        "4:\t0x0F\t(Magnetic Field Uncalibrated ID)\n" +
                        "5:\t" +  (response[4] & 0xFF) + "\t(Sequence Number)\n" +
                        "6:\t" +  (response[5] & 0xFF) + "\t(Status)\n" +
                        "7:\t" +  (response[6] & 0xFF) + "\t(Delay)\n" +
                        "8-9:\t"  +  String.format("%.2f", x) + " µT\t(uncalibrated Axis X)\n" +
                        "10-11:\t" + String.format("%.2f", y) + " µT\t(uncalibrated Axis Y)\n" +
                        "12-13:\t" + String.format("%.2f", z) + " µT\t(uncalibrated Axis Z)\n" +
                        "14-15:\t"  +  String.format("%.2f", xbias) + " µT\t(uncalibrated hard iron bias Axis X)\n" +
                        "16-17:\t" + String.format("%.2f", ybias) + " µT\t(uncalibrated hard iron bias Axis Y)\n" +
                        "18-19:\t" + String.format("%.2f", zbias) + " µT\t(uncalibrated hard iron bias Axis Z)\n";

//        System.out.println(BoldOn + "Magnetic Field Uncalibrated Report\n" + BoldOff + featureResponse);

    }

    ///  HANDLER METHODS
    public void readInputSensorResponse() throws InterruptedException {
        //RETRIEVE REPORT ID
        byte[] header = new byte[5];        // HEADER TO INCLUDE REPORT ID
        this.i2c.read(header);

        int msgLength = (header[0] & 0xFF);
        byte reportID = header[4];

        switch (reportID) {
            case Bno085ConstantsI2C.TIMESTAMP_REBASE_ID:
                readTimestampRebase();
                break;
            case Bno085ConstantsI2C.BASE_TIMESTAMP_REFERENCE_ID:
                readBaseTimeStampReference();
                break;
            case Bno085ConstantsI2C.ACCELEROMETER_ID,
                 Bno085ConstantsI2C.LINEAR_ACCELERATION_ID,
                 Bno085ConstantsI2C.GRAVITY_ID:
                readAccelerationReport(reportID);
                break;
            case Bno085ConstantsI2C.GYROSCOP_CALIBRATED_ID:
                readGyrCalReport();
                break;
            case Bno085ConstantsI2C.GYROSCOP_UNCALIBRATED_ID:
                readGyrUnCalReport();
                break;
            case Bno085ConstantsI2C.MAGNETIC_FIELD_CALIBRATED_ID:
                readMagFieldCalReport();
                break;
            case Bno085ConstantsI2C.MAGNETIC_FIELD_UNCALIBRATED_ID:
                readMagFieldUnCalReport();
                break;
            case Bno085ConstantsI2C.ROTATION_VECTOR_ID:
                readRotationReport();
                break;
            default:
                readUnknownReport(reportID, msgLength);
                break;
        }
    }

    public void readControlResponse() throws InterruptedException {
        //RETRIEVE REPORT ID
        byte[] header = new byte[5];        // HEADER TO INCLUDE REPORT ID
        this.i2c.read(header);

        int ctrlMsgLength = ((header[1] & 0x7F)<<8) | (header[0] & 0xFF);
        byte ctrlReportID = header[4];

        switch (ctrlReportID) {
            case Bno085ConstantsI2C.COMMAND_RESPONSE:
                ReadCommandResponse();
                break;
            case Bno085ConstantsI2C.PRODUCT_RESPONSE_ID:
                ReadProductInfo();
                break;
            case Bno085ConstantsI2C.FEATURE_RESPONSE_ID:
                getFeatureResponse();
                break;
            default:
                readUnknownReport(ctrlReportID, ctrlMsgLength);
                break;
        }
    }

    public void readExecutableResponse(int msgLength){
        byte[] executableMsg = new byte[msgLength + 4];
        this.i2c.read(executableMsg);
//        System.out.println(BoldOn + "EXECUTABLE RESPONSE" + BoldOff);
        printBytes(executableMsg);
    }

    public void readWakeInputSensorReports(int msgLength){
        byte[] executableMsg = new byte[msgLength + 4];
        this.i2c.read(executableMsg);
//        System.out.println(BoldOn + "WAKE INPUT SENSOR REPORT" + BoldOff);
        printBytes(executableMsg);
    }

    public void readGyroRotationVectorResponse(int msgLength){
        byte[] executableMsg = new byte[msgLength + 4];
        this.i2c.read(executableMsg);
//        System.out.println(BoldOn + "GYRO ROTATION VECTOR RESPONSE" + BoldOff);
        printBytes(executableMsg);
    }

    public void readCommandResponse() throws InterruptedException {
        //RETRIEVE REPORT ID
        byte[] header = new byte[5];        // HEADER TO INCLUDE REPORT ID
        this.i2c.read(header);

        int cmdMsgLength = ((header[1] & 0x7F)<<8) | (header[0] & 0xFF);
        int cmdReportID =(header[4]&0xFF);

        switch (cmdReportID){
            case Bno085ConstantsI2C.ADVERTISEMENT_PKG:
                readAdPkg(cmdMsgLength);
                break;
            default:
                readUnknownReport((byte) cmdReportID,cmdMsgLength);
        }

    }

    ///  MAIN METHOD THAT READS ALL DATA IN SENSOR'S DATASTREAM
    public void readSensor() {
        //RETRIEVE HEADER AND DESTRUCTURE INFORMATION
        byte[] header = new byte[4];
        this.i2c.read(header);

        int msgLength = ((header[1] & 0x7F) << 8) | (header[0] & 0xFF); // combine LSB and MSB message, 0x7F hides first bit and 0xFF makes signed byte to unsigned byte
        int channel = header[2] & 0xFF; //Convert to unsigned byte

        // DIGEST MESSAGE HEADER AND CHECK THE CHANNEL AND IF MESSAGE CONTAINS ANY LENGTH
        while (msgLength > 0) {
            switch (channel) {
                case Bno085ConstantsI2C.CHANNEL.SH_COMMAND:
                    try {
                        readCommandResponse();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case Bno085ConstantsI2C.CHANNEL.EXECUTABLE:
                    readExecutableResponse(msgLength);
                    break;
                case Bno085ConstantsI2C.CHANNEL.SH_CONTROL:
                    try {
                        readControlResponse();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case Bno085ConstantsI2C.CHANNEL.INPUT_SENSOR_REPORTS:
                    try {
                        readInputSensorResponse();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case Bno085ConstantsI2C.CHANNEL.WAKE_INPUT_SENSOR_REPORTS:
                    readWakeInputSensorReports(msgLength);
                    break;
                case Bno085ConstantsI2C.CHANNEL.GYRO_ROTATION_VECTOR:
                    readGyroRotationVectorResponse(msgLength);
                    break;
                default:
                    // UNRECOGNIZED HEADER INFORMATION
                    String unrecognizedHeader =
                        BoldOn + "UnrecognizedHeader:\n" + BoldOff +
                        "0:\t" + msgLength + "\t(LSB Length)\n" +
                        "1:\t" + (header[1] & 0xFF) + "\t(MSB Length)\n" +
                        "2:\t" + channel + "\t(Channel)\n" +
                        "3:\t" + (header[3] & 0xFF) + "\t(Sequence Number)\n";
                    System.out.println(unrecognizedHeader);
                    return;
            }

            // READ NEXT HEADER
            this.i2c.read(header);
            msgLength = ((header[1] & 0x7F) << 8) | (header[0] & 0xFF);
            channel = header[2] & 0xFF;
        }
    }

    public void createI2cConnection(){
        // Initialize pi4j and i2c configuration
        pi4j = Pi4J.newAutoContext();
        i2CProvider = pi4j.provider("linuxfs-i2c");

        I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
                .bus(config.connection.I2C_BUS_NUMBER)
                .device(config.connection.SENSOR_ADDRESS)
                .name("bno055")
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
