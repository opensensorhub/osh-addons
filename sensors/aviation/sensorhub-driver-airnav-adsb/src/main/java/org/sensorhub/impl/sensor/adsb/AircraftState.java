package org.sensorhub.impl.sensor.adsb;

public class AircraftState {
    String icao;
    String callsign;
    double lat = Double.NaN;
    double lon = Double.NaN;
    double altFt = Double.NaN;
    double groundSpeed = Double.NaN;
    double heading = Double.NaN;
    double verticalRate = Double.NaN;
    String squawk;
    boolean alert;
    boolean emergency;
    boolean isOnGround;
    long lastUpdateTime;

    boolean hasPosition() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }
}
