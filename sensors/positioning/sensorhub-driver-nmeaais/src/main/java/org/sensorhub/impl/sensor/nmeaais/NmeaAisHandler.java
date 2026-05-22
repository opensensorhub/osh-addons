package org.sensorhub.impl.sensor.nmeaais;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage18;
import dk.dma.ais.message.AisMessage19;
import dk.dma.ais.message.AisMessage21;
import dk.dma.ais.message.AisMessage24;
import dk.dma.ais.message.AisMessage4;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPositionMessage;
import org.sensorhub.impl.sensor.nmeaais.helpers.MessageIdDescriptions;

import java.util.HashMap;
import java.util.Map;

public class NmeaAisHandler {
    private final NmeaAisDriver nmeaAisDriver;
    private final MessageIdDescriptions reportDescription = new MessageIdDescriptions();

    // Cache of type 24 Part A vessel names keyed by MMSI.
    // Part A only carries the name; Part B carries everything else.
    // When Part B arrives we combine them into a single published record.
    private final Map<Integer, String> type24PartANames = new HashMap<>();

    public NmeaAisHandler(NmeaAisDriver driver) {
        this.nmeaAisDriver = driver;
    }

    public void handleAisMessage(AisMessage aisMessage) {
        int messageId = aisMessage.getMsgId();
        String description = getReportDescription(messageId);
        switch (messageId) {
            case 1, 2, 3: // Class A Position Reports
                nmeaAisDriver.publishPositionAReport((AisPositionMessage) aisMessage, description);
                break;
            case 4, 11: // Base Station / UTC-Date Response
                nmeaAisDriver.publishBaseStationReport((AisMessage4) aisMessage, description);
                break;
            case 5: // Static and Voyage Related Data
                nmeaAisDriver.publishStaticVoyageReport((AisMessage5) aisMessage, description);
                break;
            case 18: // Class B Standard Position Report
                nmeaAisDriver.publishPositionBReport((AisMessage18) aisMessage, description);
                break;
            case 19: // Class B Extended Position Report
                nmeaAisDriver.publishPositionBExtReport((AisMessage19) aisMessage, description);
                break;
            case 21: // Aid-to-Navigation Report
                nmeaAisDriver.publishAidNavReport((AisMessage21) aisMessage, description);
                break;
            case 24: // Class B CS Static Data (two-part)
                handleType24((AisMessage24) aisMessage, description);
                break;
        }
    }

    private void handleType24(AisMessage24 msg, String description) {
        if (msg.getPartNumber() == 0) {
            // Part A — store the vessel name and wait for Part B
            type24PartANames.put(msg.getUserId(), msg.getName());
        } else {
            // Part B — combine with cached Part A name (if available) and publish
            String name = type24PartANames.getOrDefault(msg.getUserId(), "");
            nmeaAisDriver.publishStaticDataClassBReport(
                    msg.getUserId(),
                    msg.getRepeat(),
                    name,
                    msg.getCallsign(),
                    msg.getShipType(),
                    msg.getDimBow(),
                    msg.getDimStern(),
                    msg.getDimPort(),
                    msg.getDimStarboard(),
                    msg.getVendorId(),
                    description
            );
        }
    }

    private String getReportDescription(int messageId) {
        if (messageId < 1 || messageId > reportDescription.descriptions.length)
            return "Unknown message type " + messageId;
        return reportDescription.descriptions[messageId - 1];
    }
}
