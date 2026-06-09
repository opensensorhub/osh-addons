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
import org.sensorhub.impl.sensor.nmeaais.helpers.AisCodeHelper;
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
     *   0  = samplingTime          1  = messageId              2  = reportDescription
     *   3  = repeat                4  = mmsi                   5  = sog
     *   6  = positionAccuracy
     *   7  = latitude  (lat component of location vector)
     *   8  = longitude (lon component of location vector)
     *   9  = cog                   10 = heading                11 = utcSecond
     *   12 = unitFlag              13 = displayFlag            14 = dscFlag
     *   15 = bandFlag              16 = message22Flag          17 = modeFlag
     *   18 = raim                  19 = commStateFlag          20 = commState
     *
     * Additional fields populated only for type 19 (empty/zero for type 18):
     *   21 = name             22 = shipType            23 = dimBow
     *   24 = dimStern         25 = dimPort             26 = dimStarboard
     *   27 = epfd             28 = dte                 29 = assignedMode
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
                .addField("samplingTime",fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time data was received")
                        .definition("SampleTime"))
                .addField("messageId", fac.createNmeaMessageId())
                .addField("reportDescription", fac.createReportDescription())
                .addField("repeat", fac.createRepeatIndicator())
                .addField("mmsi", fac.createMssi())
                .addField("sog", sweFactory.createQuantity()
                        .label("Speed Over Ground")
                        .description("Speed over ground in knots (0–102.2 knots; 102.3 = not available)")
                        .uom("[kn_i]")
                        .definition(SWEHelper.getPropertyUri("SpeedOverGround")))
                .addField("positionAccuracy", fac.createPositionAccuracy())
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
                .addField("utcSecond", sweFactory.createCount()
                        .label("UTC Second")
                        .description("UTC second when the report was generated (0-59); 60 = not available = default")
                        .definition(SWEHelper.getPropertyUri("UtcSecond")))
                .addField("unitFlag", fac.createText()
                        .label("Class B Unit Flag")
                        .description("0 = Class B SOTDMA unit; 1 = Class B CS unit")
                        .definition(SWEHelper.getPropertyUri("UnitFlag")))
                .addField("displayFlag", fac.createText()
                        .label("Class B Display Flag")
                        .description("0 = No display available; 1 = Equipped with display for Message 12 and 14")
                        .definition(SWEHelper.getPropertyUri("DisplayFlag")))
                .addField("dscFlag", fac.createText()
                        .label("Class B DSC Flag")
                        .description("0 = Not equipped with DSC function; 1 = Equipped with DSC function")
                        .definition(SWEHelper.getPropertyUri("DscFlag")))
                .addField("bandFlag", fac.createText()
                        .label("Class B Band Flag")
                        .description("0 = Capable of operating over upper 525 kHz band; 1 = Capable of operating over whole marine band")
                        .definition(SWEHelper.getPropertyUri("BandFlag")))
                .addField("message22Flag", fac.createText()
                        .label("Class B Message 22 Flag")
                        .description("0 = No frequency management via Message 22; 1 = Frequency management via Message 22")
                        .definition(SWEHelper.getPropertyUri("Message22Flag")))
                .addField("modeFlag", fac.createText()
                        .label("Mode Flag")
                        .description("0 = Autonomous and continuous mode = default; 1 = Assigned mode")
                        .definition(SWEHelper.getPropertyUri("ModeFlag")))
                .addField("raim", fac.createRAIM())
                .addField("commStateFlag", fac.createText()
                        .label("Communication State Selector Flag")
                        .description("0 = SOTDMA communication state follows; 1 = ITDMA (always 1 for Class B CS)")
                        .definition(SWEHelper.getPropertyUri("CommStateFlag")))
                .addField("commState", sweFactory.createCount()
                        .label("Communication State")
                        .description("SOTDMA or ITDMA communication state")
                        .definition(SWEHelper.getPropertyUri("CommState")))
                // --- additional fields populated only for type 19 ---
                .addField("name", fac.createVesselName()
                        .description("Vessel name (type 19 only; empty for type 18)"))
                .addField("shipType", sweFactory.createCount()
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
                .addField("epfd", fac.createEpfd())
                .addField("dte", fac.createDte())
                .addField("assignedMode", fac.createAssignedMode());

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    /** Publishes a type 18 (Standard CS) record. Type-19-specific fields are set to zero/empty. */
    public void setData(AisMessage18 report) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            dataBlock.setDoubleValue(5,  report.getSog() / 10.0);
            dataBlock.setStringValue(6, AisCodeHelper.PosAcc.getDescription(report.getPosAcc()));
            dataBlock.setDoubleValue(7,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(8,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(9,  report.getCog() / 10.0);
            dataBlock.setIntValue(10,  report.getTrueHeading());
            dataBlock.setIntValue(11, report.getUtcSec());
            dataBlock.setStringValue(12, AisCodeHelper.ClassBUnitFlag.getDescription(report.getClassBUnitFlag()));
            dataBlock.setStringValue(13, AisCodeHelper.DisplayFlag.getDescription(report.getClassBDisplayFlag()));
            dataBlock.setStringValue(14, AisCodeHelper.DscFlag.getDescription(report.getClassBDscFlag()));
            dataBlock.setStringValue(15, AisCodeHelper.BandFlag.getDescription(report.getClassBBandFlag()));
            dataBlock.setStringValue(16, AisCodeHelper.Message22Flag.getDescription(report.getClassBMsg22Flag()));
            dataBlock.setStringValue(17, AisCodeHelper.AssignedMode.getDescription(report.getModeFlag()));
            dataBlock.setBooleanValue(18, report.getRaim() == 1);
            dataBlock.setStringValue(19, AisCodeHelper.CommStateSelectorFlag.getDescription(report.getCommStateSelectorFlag()));
            dataBlock.setIntValue(20, report.getCommState());
            // type-19-only fields — not present in type 18
            dataBlock.setStringValue(21, "");
            dataBlock.setIntValue(22, 0);
            dataBlock.setIntValue(23, 0);
            dataBlock.setIntValue(24, 0);
            dataBlock.setIntValue(25, 0);
            dataBlock.setIntValue(26, 0);
            dataBlock.setStringValue(27, "0");
            dataBlock.setStringValue(28, "0");
            dataBlock.setStringValue(29, "0");

            publish(dataBlock, String.valueOf(report.getUserId()));
        }
    }

    /** Publishes a type 19 (Extended CS) record. All fields including name, ship type, and dimensions are populated. */
    public void setData(AisMessage19 report) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            dataBlock.setDoubleValue(5,  report.getSog() / 10.0);
            dataBlock.setStringValue(6, AisCodeHelper.PosAcc.getDescription(report.getPosAcc()));
            dataBlock.setDoubleValue(7,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(8,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(9,  report.getCog() / 10.0);
            dataBlock.setIntValue(10,  report.getTrueHeading());
            dataBlock.setIntValue(11, report.getUtcSec());
            // type 19 does not carry Class B CS radio flags — set to 0
            dataBlock.setStringValue(12, "0");
            dataBlock.setStringValue(13, "0");
            dataBlock.setStringValue(14, "0");
            dataBlock.setStringValue(15, "0");
            dataBlock.setStringValue(16, "0");
            dataBlock.setStringValue(17, AisCodeHelper.AssignedMode.getDescription(report.getModeFlag()));
            dataBlock.setBooleanValue(18, report.getRaim() == 1);
            dataBlock.setStringValue(19, "0");
            dataBlock.setIntValue(20, 0);
            // type-19-only fields
            dataBlock.setStringValue(21,  AisCodeHelper.cleanVesselName(report.getName()));
            dataBlock.setIntValue(22, report.getShipType());
            dataBlock.setIntValue(23, report.getDimBow());
            dataBlock.setIntValue(24, report.getDimStern());
            dataBlock.setIntValue(25, report.getDimPort());
            dataBlock.setIntValue(26, report.getDimStarboard());
            dataBlock.setStringValue(27, AisCodeHelper.EpfdType.getDescription(report.getPosType()));
            dataBlock.setBooleanValue(28, report.getDte() != 1);
            dataBlock.setStringValue(29, AisCodeHelper.AssignedMode.getDescription(report.getModeFlag()));

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
