package org.sensorhub.impl.sensor.nmeaais.Outputs;

import org.sensorhub.impl.sensor.nmeaais.ReportTypes.PositionReport;

public interface NmeaAisOutputInterface {
    void setData(String nmeaAisMsg, PositionReport report);
}


