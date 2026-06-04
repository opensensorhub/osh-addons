/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage18;
import dk.dma.ais.message.AisMessage19;
import dk.dma.ais.message.AisMessage21;
import dk.dma.ais.message.AisMessage24;
import dk.dma.ais.message.AisMessage4;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPositionMessage;
import org.sensorhub.impl.sensor.nmeaais.helpers.AisCodeHelper;

import java.util.HashMap;
import java.util.Map;

public class NmeaAisHandler {
    private final NmeaAisDriver nmeaAisDriver;

    // Cache of type 24 Part A vessel names keyed by MMSI.
    // Part A only carries the name; Part B carries everything else.
    // When Part B arrives we combine them into a single published record.
    private final Map<Integer, String> type24PartANames = new HashMap<>();

    public NmeaAisHandler(NmeaAisDriver driver) {
        this.nmeaAisDriver = driver;
    }

    public void handleAisMessage(AisMessage aisMessage) {
        int messageId = aisMessage.getMsgId();
        switch (messageId) {
            case 1, 2, 3: // Class A Position Reports
                nmeaAisDriver.nmeaAisOutputPositionClassA.setData((AisPositionMessage) aisMessage);
                break;
            case 4, 11: // Base Station / UTC-Date Response
                nmeaAisDriver.nmeaAisOutputBaseStation.setData((AisMessage4) aisMessage);
                break;
            case 5: // Static and Voyage Related Data
                nmeaAisDriver.nmeaAisOutputStaticVoyage.setData((AisMessage5) aisMessage);
                break;
            case 18: // Class B Standard Position Report
                nmeaAisDriver.nmeaAisOutputPositionClassB.setData((AisMessage18) aisMessage);
                break;
            case 19: // Class B Extended Position Report
                nmeaAisDriver.nmeaAisOutputPositionClassB.setData((AisMessage19) aisMessage);
                break;
            case 21: // Aid-to-Navigation Report
                nmeaAisDriver.nmeaAisOutputAidNavigation.setData((AisMessage21) aisMessage);
                break;
            case 24: // Class B CS Static Data (two-part)
                handleType24((AisMessage24) aisMessage);
                break;
            default:
                nmeaAisDriver.getLogger().debug("No Output has been created to capture AIS reports with a Message ID of {}",messageId);
        }
    }

    private void handleType24(AisMessage24 msg) {
        if (msg.getPartNumber() == 0) {
            // Part A — store the vessel name and wait for Part B
            type24PartANames.put(msg.getUserId(), msg.getName());
        } else {
            // Part B — combine with cached Part A name (if available) and publish
            String name = type24PartANames.getOrDefault(msg.getUserId(), "");
            nmeaAisDriver.nmeaAisOutputStaticDataClassB.setData(
                    msg.getUserId(),
                    msg.getRepeat(),
                    name,
                    msg.getCallsign(),
                    msg.getShipType(),
                    msg.getDimBow(),
                    msg.getDimStern(),
                    msg.getDimPort(),
                    msg.getDimStarboard(),
                    msg.getVendorId()
            );
        }
    }

    private String getReportDescription(int messageId) {
        return AisCodeHelper.MessageType.getDescription(messageId);
    }
}
