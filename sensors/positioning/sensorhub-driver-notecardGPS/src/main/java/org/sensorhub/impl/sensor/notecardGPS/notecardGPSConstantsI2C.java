package org.sensorhub.impl.sensor.notecardGPS;

public class notecardGPSConstantsI2C {

    public final static String SENSOR_NAME = "notecardGPS";
    public final static int I2C_BUS = 1;
    public final static int SENSOR_ADDRESS = 0x17;

    public final static String RESTORE = "{\"req\":\"card.restore\",\"delete\":"+true+"}";
    public final static String ENABLE_ACCELEROMETER = "{\"req\":\"card.motion.mode\",\"start\":true}";
    public final static String DISABLE_ACCELEROMETER = "{\"req\":\"card.motion.mode\",\"stop\":true}";

    public final static String HUBSET = "{\"req\":\"hub.set\",\"product\":\"com.botts-inc.bill.brown:my0card0test\",\"mode\":\"periodic\",\"outbound\":5,\"inbound\":6}";
    public final static String HUBSET_MIN = "{\"req\":\"hub.set\",\"product\":\"com.botts-inc.bill.brown:gps\",\"mode\":\"minimum\"}";
    public final static String HUBSET_OFF = "{\"req\":\"hub.set\",\"product\":\"com.botts-inc.bill.brown:gps\",\"mode\":\"off\"}";
    public final static String CHECK_SIGNAL = "{\"req\":\"hub.signal\"}";
    public final static String CHECK_STATUS = "{\"req\":\"hub.status\"}";
    public final static String HUB_SYNC = "{\"req\":\"hub.sync\"}";
    public final static String GET_VERSION  = "{\"req\":\"card.version\"}";
    public final static String GET_LOC      = "{\"req\":\"card.location\"}";
    public final static String GET_TEMP      = "{\"req\":\"card.temp\"}";
    public final static String GET_MOTION      = "{\"req\":\"card.motion\"}";
    public final static String SET_TIME = "{\"req\":\"card.time\",\"time\":"+System.currentTimeMillis()/1000+"}}";
    public final static String GET_TIME = "{\"req\":\"card.time\"}";
    public final static String TURNON_TRIANGLULATION = "{\"req\":\"card.triangulate\",\"mode\":\"cell\",\"on\":true,\"set\":true}";
    public final static String TURNOFF_TRIANGLULATION = "{\"req\":\"card.triangulate\",\"mode\":\"-\"}";
    public final static String CHECK_TRIANGLULATION = "{\"req\":\"card.triangulate\"}";
    public final static String CONNECTIVITY = "{\"req\":\"card.transport\"}";
    public final static String SET_LOCATION_PER = "{\"req\":\"card.location.mode\",\"mode\":\"periodic\",\"seconds\":5}";
    public final static String SET_LOCATION_CONT = "{\"req\":\"card.location.mode\",\"mode\":\"continuous\"}";
    public final static String DISABLE_GPS = "{\"req\":\"card.location.mode\",\"mode\":\"off\"}";


}
