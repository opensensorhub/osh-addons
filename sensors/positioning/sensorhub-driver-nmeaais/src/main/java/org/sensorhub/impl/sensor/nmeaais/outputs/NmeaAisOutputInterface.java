package org.sensorhub.impl.sensor.nmeaais.outputs;

public interface NmeaAisOutputInterface<T> {
    void setData(String nmeaAisMsg, T report);
}


