package org.sensorhub.impl.sensor.nmeaais.outputs;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisPositionMessage;

public interface NmeaAisReportInterface<T> {
    void setData(T msgTyp, String description);
}
