/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais.outputs;

import dk.dma.ais.message.AisMessage18;
import dk.dma.ais.message.AisMessage19;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.sensor.nmeaais.NmeaAisDriver;
import org.sensorhub.impl.sensor.nmeaais.helpers.NmeaAisHelper;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Output for Class B AIS position reports.
 * Handles both type 18 (Standard CS Position) and type 19 (Extended CS Position).
 * Type 19 records additionally carry vessel name, ship type, and dimensions;
 * those fields are empty/zero for type 18 records.
 */
public class NmeaAisOutputPositionClassB extends VarRateSensorOutput<NmeaAisDriver> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private final Object processingLock = new Object();

    public NmeaAisOutputPositionClassB(NmeaAisDriver nmeaAisDriver) {
        super("nmeaAisOutputPositionClassB", nmeaAisDriver, 1.0);
    }

    /**
     * Initializes the data structure for the output.
     *
     * Flat index map (common to types 18 and 19):
     *   0  = messageId        1  = reportDescription   2  = repeat
     *   3  = mmsi             4  = sog                 5  = positionAccuracy
     *   6  = latitude  (lat component of location vector)
     *   7  = longitude (lon component of location vector)
     *   8  = cog              9  = heading             10 = timeStamp
     *   11 = unitFlag         12 = displayFlag         13 = dscFlag
     *   14 = bandFlag         15 = message22Flag       16 = modeFlag
     *   17 = raim             18 = commStateFlag       19 = commState
     *
     * Additional fields populated only for type 19 (empty/zero for type 18):
     *   20 = name             21 = shipType            22 = dimBow
     *   23 = dimStern         24 = dimPort             25 = dimStarboard
     *   26 = epfd             27 = dte                 28 = assignedMode
     */
    public void doInit() {
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();
        NmeaAisHelper fac = new NmeaAisHelper();

        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name("nmeaAisOutputPositionClassB")
                .label("Position Report Class B")
                .description("Class B AIS Position Report — types 18 (Standard) and 19 (Extended). " +
                             "Name, ship type, and dimension fields are only populated for type 19.")
                .definition(SWEHelper.getPropertyUri("NmeaAisOutputPositionClassB"))
                // --- fields common to type 18 and 19 ---
                .addField("messageId", fac.createNmeaMessageId())
                .addField("reportDescription", fac.createReportDescription())
                .addField("repeat", fac.createRepeatIndicator())
                .addField("mmsi", fac.createMssi())
                .addField("sog", sweFactory.createQuantity()
                        .label("Speed Over Ground")
                        .description("Speed over ground in knots (0–102.2 knots; 102.3 = not available)")
                        .uom("[kn_i]")
                        .definition(SWEHelper.getPropertyUri("SpeedOverGround")))
                .addField("positionAccuracy", sweFactory.createQuantity()
                        .label("Position Accuracy")
                        .description("1 = high (<= 10 m); 0 = low (> 10 m); 0 = default")
                        .definition(SWEHelper.getPropertyUri("PositionAccuracy")))
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("cog", sweFactory.createQuantity()
                        .label("COG")
                        .description("Course over ground in degrees (0–359.9); 360 = not available = default")
                        .definition(SWEHelper.getPropertyUri("Cog")))
                .addField("heading", sweFactory.createQuantity()
                        .label("True Heading")
                        .uom("deg")
                        .description("Degrees (0-359); 511 = not available = default")
                        .definition(SWEHelper.getPropertyUri("heading")))
                .addField("timeStamp", sweFactory.createTime()
                        .label("Time Stamp")
                        .description("UTC second when the report was generated (0-59); 60 = not available = default")
                        .definition(SWEHelper.getPropertyUri("TimeStamp")))
                .addField("unitFlag", sweFactory.createQuantity()
                        .label("Class B Unit Flag")
                        .description("0 = Class B SOTDMA unit; 1 = Class B CS unit")
                        .definition(SWEHelper.getPropertyUri("UnitFlag")))
                .addField("displayFlag", sweFactory.createQuantity()
                        .label("Class B Display Flag")
                        .description("0 = No display available; 1 = Equipped with display for Message 12 and 14")
                        .definition(SWEHelper.getPropertyUri("DisplayFlag")))
                .addField("dscFlag", sweFactory.createQuantity()
                        .label("Class B DSC Flag")
                        .description("0 = Not equipped with DSC function; 1 = Equipped with DSC function")
                        .definition(SWEHelper.getPropertyUri("DscFlag")))
                .addField("bandFlag", sweFactory.createQuantity()
                        .label("Class B Band Flag")
                        .description("0 = Capable of operating over upper 525 kHz band; 1 = Capable of operating over whole marine band")
                        .definition(SWEHelper.getPropertyUri("BandFlag")))
                .addField("message22Flag", sweFactory.createQuantity()
                        .label("Class B Message 22 Flag")
                        .description("0 = No frequency management via Message 22; 1 = Frequency management via Message 22")
                        .definition(SWEHelper.getPropertyUri("Message22Flag")))
                .addField("modeFlag", sweFactory.createQuantity()
                        .label("Mode Flag")
                        .description("0 = Autonomous and continuous mode = default; 1 = Assigned mode")
                        .definition(SWEHelper.getPropertyUri("ModeFlag")))
                .addField("raim", sweFactory.createQuantity()
                        .label("RAIM Flag")
                        .description("0 = RAIM not in use = default; 1 = RAIM in use")
                        .definition(SWEHelper.getPropertyUri("Raim")))
                .addField("commStateFlag", sweFactory.createQuantity()
                        .label("Communication State Selector Flag")
                        .description("0 = SOTDMA communication state follows; 1 = ITDMA (always 1 for Class B CS)")
                        .definition(SWEHelper.getPropertyUri("CommStateFlag")))
                .addField("commState", sweFactory.createQuantity()
                        .label("Communication State")
                        .description("SOTDMA or ITDMA communication state")
                        .definition(SWEHelper.getPropertyUri("CommState")))
                // --- additional fields populated only for type 19 ---
                .addField("name", sweFactory.createText()
                        .label("Vessel Name")
                        .description("Vessel name (type 19 only; empty for type 18)")
                        .definition(SWEHelper.getPropertyUri("VesselName")))
                .addField("shipType", sweFactory.createQuantity()
                        .label("Ship Type")
                        .description("Ship type per ITU-R M.1371-5 Table 53 (type 19 only; 0 for type 18)")
                        .definition(SWEHelper.getPropertyUri("ShipType")))
                .addField("dimBow", sweFactory.createQuantity()
                        .label("Dimension to Bow")
                        .description("Distance from GPS antenna to bow in metres (type 19 only)")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimBow")))
                .addField("dimStern", sweFactory.createQuantity()
                        .label("Dimension to Stern")
                        .description("Distance from GPS antenna to stern in metres (type 19 only)")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStern")))
                .addField("dimPort", sweFactory.createQuantity()
                        .label("Dimension to Port")
                        .description("Distance from GPS antenna to port side in metres (type 19 only)")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimPort")))
                .addField("dimStarboard", sweFactory.createQuantity()
                        .label("Dimension to Starboard")
                        .description("Distance from GPS antenna to starboard side in metres (type 19 only)")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStarboard")))
                .addField("epfd", sweFactory.createQuantity()
                        .label("Type of EPFD")
                        .description("Electronic position fixing device type (type 19 only)")
                        .definition(SWEHelper.getPropertyUri("Epfd")))
                .addField("dte", sweFactory.createQuantity()
                        .label("DTE")
                        .description("Data terminal equipment available; 0 = available; 1 = not available (type 19 only)")
                        .definition(SWEHelper.getPropertyUri("Dte")))
                .addField("assignedMode", sweFactory.createQuantity()
                        .label("Assigned Mode Flag")
                        .description("0 = autonomous mode = default; 1 = assigned mode (type 19 only)")
                        .definition(SWEHelper.getPropertyUri("AssignedMode")));

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    /** Publishes a type 18 (Standard CS) record. Type-19-specific fields are set to zero/empty. */
    public void setData(AisMessage18 report, String description) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setIntValue(0,  report.getMsgId());
            dataBlock.setStringValue(1, description);
            dataBlock.setIntValue(2,  report.getRepeat());
            dataBlock.setStringValue(3,  String.valueOf(report.getUserId()));
            dataBlock.setDoubleValue(4,  report.getSog() / 10.0);
            dataBlock.setIntValue(5,  report.getPosAcc());
            dataBlock.setDoubleValue(6,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(7,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(8,  report.getCog() / 10.0);
            dataBlock.setIntValue(9,  report.getTrueHeading());
            dataBlock.setIntValue(10, report.getUtcSec());
            dataBlock.setIntValue(11, report.getClassBUnitFlag());
            dataBlock.setIntValue(12, report.getClassBDisplayFlag());
            dataBlock.setIntValue(13, report.getClassBDscFlag());
            dataBlock.setIntValue(14, report.getClassBBandFlag());
            dataBlock.setIntValue(15, report.getClassBMsg22Flag());
            dataBlock.setIntValue(16, report.getModeFlag());
            dataBlock.setIntValue(17, report.getRaim());
            dataBlock.setIntValue(18, report.getCommStateSelectorFlag());
            dataBlock.setIntValue(19, report.getCommState());
            // type-19-only fields — not present in type 18
            dataBlock.setStringValue(20, "");
            dataBlock.setIntValue(21, 0);
            dataBlock.setIntValue(22, 0);
            dataBlock.setIntValue(23, 0);
            dataBlock.setIntValue(24, 0);
            dataBlock.setIntValue(25, 0);
            dataBlock.setIntValue(26, 0);
            dataBlock.setIntValue(27, 0);
            dataBlock.setIntValue(28, 0);

            publish(dataBlock, String.valueOf(report.getUserId()));
        }
    }

    /** Publishes a type 19 (Extended CS) record. All fields including name, ship type, and dimensions are populated. */
    public void setData(AisMessage19 report, String description) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setIntValue(0,  report.getMsgId());
            dataBlock.setStringValue(1, description);
            dataBlock.setIntValue(2,  report.getRepeat());
            dataBlock.setStringValue(3,  String.valueOf(report.getUserId()));
            dataBlock.setDoubleValue(4,  report.getSog() / 10.0);
            dataBlock.setIntValue(5,  report.getPosAcc());
            dataBlock.setDoubleValue(6,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(7,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(8,  report.getCog() / 10.0);
            dataBlock.setIntValue(9,  report.getTrueHeading());
            dataBlock.setIntValue(10, report.getUtcSec());
            // type 19 does not carry Class B CS radio flags — set to 0
            dataBlock.setIntValue(11, 0);
            dataBlock.setIntValue(12, 0);
            dataBlock.setIntValue(13, 0);
            dataBlock.setIntValue(14, 0);
            dataBlock.setIntValue(15, 0);
            dataBlock.setIntValue(16, report.getModeFlag());
            dataBlock.setIntValue(17, report.getRaim());
            dataBlock.setIntValue(18, 0);
            dataBlock.setIntValue(19, 0);
            // type-19-only fields
            dataBlock.setStringValue(20, report.getName());
            dataBlock.setIntValue(21, report.getShipType());
            dataBlock.setIntValue(22, report.getDimBow());
            dataBlock.setIntValue(23, report.getDimStern());
            dataBlock.setIntValue(24, report.getDimPort());
            dataBlock.setIntValue(25, report.getDimStarboard());
            dataBlock.setIntValue(26, report.getPosType());
            dataBlock.setIntValue(27, report.getDte());
            dataBlock.setIntValue(28, report.getModeFlag());

            publish(dataBlock, String.valueOf(report.getUserId()));
        }
    }

    private void publish(DataBlock dataBlock, String userId) {
        String foiUID = parentSensor.addFoi(userId);
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        updateSamplingPeriod(latestRecordTime);
        eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputPositionClassB.this, foiUID, dataBlock));
    }

    @Override
    public DataComponent getRecordDescription() {
        return aisReportRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }
}
