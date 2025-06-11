package com.sample.impl.sensor.BNO085;

public class Bno085ConstantsI2C {

    public final static String SENSOR_NAME = "bno085";
    public final static int I2C_BUS = 1;

    // TYPICAL PORT ADDRESS
    public final static int SENSOR_ADDRESS_LOW = 0x4A; // = 0101000
    public final static int SENSOR_ADDRESS_HIGH = 0x4B;

    // HEADER CONSTANTS
    // HEADER INFO (4 byte header)
    // 0 = Length LSB
    // 1 = Length MSB
    // 2 = Channel
    // 3 = SeqNum
    public final static int SHTP_HEADER_LENGTH = 4;

    // CONTROL CHANNELS
    public static class CHANNEL {
        public final static int SH_COMMAND = 0;                 // CHANNEL 0
        public final static int EXECUTABLE = 1;                 // CHANNEL 1
        public final static int SH_CONTROL = 2;                 // CHANNEL 2
        public final static int INPUT_SENSOR_REPORTS = 3;       // CHANNEL 3
        public final static int WAKE_INPUT_SENSOR_REPORTS = 4;  // CHANNEL 4
        public final static int GYRO_ROTATION_VECTOR = 5;       // CHANNEL 5
    }

    // COMMAND REPORTS (Channel 0)
    public final static byte ADVERTISEMENT_PKG = 0X00;

    // EXECUTABLE REPORTS (Channel 1)

    // CONTROL REPORTS
    ///  RESPONSES
    public final static byte COMMAND_RESPONSE = (byte) 0XF1;
    public final static byte PRODUCT_RESPONSE_ID = (byte) 0xF8;
    public final static byte FEATURE_RESPONSE_ID = (byte) 0xFC;
    /// REQUESTS/COMMANDS
    public final static byte PRODUCT_ID_REQUEST = (byte) 0xF9;
    public final static byte SET_FEATURE_COMMAND = (byte) 0xFD;
    public final static byte COMMAND_REQUEST = (byte) 0xF2;
    public final static byte RESET_CMD = (byte) 0x0B;

    // BNO085 CHIP CONSTANTS
    public final static byte GET_FEATURE_REQUEST = (byte) 0xFE;
    public final static byte INITIALIZE_CMD = (byte) 0X04;

    // SENSOR INPUT REPORTS
    public final static byte BASE_TIMESTAMP_REFERENCE_ID = (byte) 0xFB;
    public final static byte TIMESTAMP_REBASE_ID = (byte) 0xFA;
    public final static byte ACCELEROMETER_ID = (byte) 0x01;
    public final static byte LINEAR_ACCELERATION_ID = (byte) 0x04;
    public final static byte GRAVITY_ID = (byte) 0X06;
    public final static byte GYROSCOP_CALIBRATED_ID = (byte) 0X02;
    public final static byte GYROSCOP_UNCALIBRATED_ID = (byte) 0X07;
    public final static byte MAGNETIC_FIELD_CALIBRATED_ID = (byte) 0X03;
    public final static byte MAGNETIC_FIELD_UNCALIBRATED_ID = (byte) 0X0F;
    public final static byte ROTATION_VECTOR_ID = (byte) 0X05;


}
