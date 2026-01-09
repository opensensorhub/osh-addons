package org.sensorhub.impl.sensor.OPS241A;

public class OPS241aConstants {

    public final static String portAddress = "/dev/ttyACM0";
    public final static int baudRate = 9600;

    //READ COMMANDS
    public final static String SENSOR_PART_NUMBER_CMD = "?P";
    public final static String SERIAL_NUM_CMD = "?N";
    public final static String BUILD_DATE_CMD = "?D";
    public final static String GET_SENSOR_LABEL = "L?";
    public final static String GET_UNITS_CMD = "U?";


    // UPDATE SENSOR COMMANDS
    public final static String RESET_SETTINGS_CMD = "AX";
    public final static String SENSOR_LABEL_WRITE = "L="; // + s where s is whatever you want. can be up to 15 characters;
    public final static String CM_PER_SEC_CMD = "UC";
    public final static String FT_PER_SEC_CMD = "UF";
    public final static String KM_PER_HR_CMD = "UK";
    public final static String M_PER_SEC_CMD = "UM";
    public final static String MILES_PER_HR_CMD = "US";




}
