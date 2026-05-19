package org.sensorhub.impl.sensor.nmeaais;

public class NmeaAisHandler {
    String nmeaAisMsg;
    String rawPayload;

    private final NmeaAisDriver nmeaAisDriver;

    public NmeaAisHandler(NmeaAisDriver driver){
        this.nmeaAisDriver = driver;
    }

    public void handleNmeaAisMessage(String nmeaAisMsg){
        this.nmeaAisMsg = nmeaAisMsg;
        String[] nmea = nmeaAisMsg.split(",");
        rawPayload  = nmea[5];
        parsePayload(rawPayload);
    }

    public void parsePayload(String payload){
        // Todo This function will take the raw payload and get its Message Id. If Message ID is 1, 2, or 3 it will ParsePositionReport()

    }

    public void parsePositionReport(String positionPayload){
        // Todo This function will create a position report array or object with the following variables:
    }



}
