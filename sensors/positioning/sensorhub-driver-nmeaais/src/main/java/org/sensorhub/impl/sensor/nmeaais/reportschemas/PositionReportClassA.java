package org.sensorhub.impl.sensor.nmeaais.reportschemas;

/**
 * Holds decoded fields from an AIS Class A Position Report (message types 1, 2, 3).
 */
public class PositionReportClassA {
    public int    messageId;     // 1, 2, or 3
    public int    repeat;        // 0–3
    public String mmsi;          // 9-digit MMSI, zero-padded
    public int    navStatus;     // 0–15
    public int    rot;           // signed, -128 to +127 deg/min
    public double sog;           // speed over ground in knots (raw / 10.0)
    public int    posAccuracy;   // 0 = low, 1 = high
    public double longitude;     // decimal degrees (raw / 600000.0)
    public double latitude;      // decimal degrees (raw / 600000.0)
    public double cog;           // course over ground in degrees (raw / 10.0)
    public int    heading;       // true heading 0–359, 511 = not available
    public int    timeStamp;     // UTC second 0–59, 60/61/62/63 = special
    public int    smi;           // special manoeuvre indicator 0–2
    public int    spare;         // 3 bits, reserved
    public int    raimFlag;          // 0 = not in use, 1 = in use
    public int    commState;     // 19-bit communication state
    public int    bits;          // total payload bits (always 168 for Class A)
}
