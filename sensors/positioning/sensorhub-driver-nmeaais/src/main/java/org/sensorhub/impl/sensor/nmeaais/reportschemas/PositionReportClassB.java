package org.sensorhub.impl.sensor.nmeaais.reportschemas;

/**
 * Holds decoded fields from an AIS Class A Position Report (message types 1, 2, 3).
 */
public class PositionReportClassB {
    public int  messageId;
    public String description;
    public int repeat;
    public String mmsi;
    public double sog;
    public int posAccuracy;
    public double longitude;
    public double latitude;
    public double cog;
    public int heading;
    public int timeStamp;
    public int unitFlag;
    public int displayFlag;
    public int dscFlag;
    public int bandFlag;
    public int message22Flag;
    public int modeFlag;
    public int raimFlag;
    public int commStateFlag;
    public int commState;
    public int bits;

}
