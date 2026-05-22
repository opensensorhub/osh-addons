package org.sensorhub.impl.sensor.nmeaais;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage18;
import dk.dma.ais.message.AisPositionMessage;
import org.sensorhub.impl.sensor.nmeaais.helpers.MessageIdDescriptions;

public class NmeaAisHandler {
    private final NmeaAisDriver nmeaAisDriver;
    private final MessageIdDescriptions reportDescription = new MessageIdDescriptions();

    public NmeaAisHandler(NmeaAisDriver driver) {
        this.nmeaAisDriver = driver;
    }

    public void handleAisMessage(AisMessage aisMessage) {
        int messageId = aisMessage.getMsgId();
        String description = getReportDescription(messageId);
        switch (messageId) {
            case 1, 2, 3:
                nmeaAisDriver.publishPositionAReport((AisPositionMessage) aisMessage, description);
                break;
            case 4, 11: // AIS Base Station Reports
                break;
            case 5: // Static/Voyage Data
                break;
            case 18: // Class B Position Reports
                nmeaAisDriver.publishPositionBReport((AisMessage18) aisMessage, description);
                break;
        }
    }

    private String getReportDescription(int messageId) {
        if (messageId < 1 || messageId > reportDescription.descriptions.length)
            return "Unknown message type " + messageId;
        return reportDescription.descriptions[messageId - 1];
    }
}
