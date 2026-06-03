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

import dk.dma.ais.message.*;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.sensor.nmeaais.NmeaAisDriver;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class NmeaAisOutputPositionClassA extends VarRateSensorOutput<NmeaAisDriver> implements NmeaAisReportInterface<AisPositionMessage> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME = "nmeaAisOutputPositionClassA";
    private static final String OUTPUT_LABEL = "Position Report Class A";
    private static final String OUTPUT_DESCRIPTION = "Class A AIS Position Report";
    private static final String OUTPUT_DEFINITION = SWEHelper.getPropertyUri("NmeaAisOutputPositionClassA");

    private final Object processingLock = new Object();

    public NmeaAisOutputPositionClassA(NmeaAisDriver nmeaAisDriver) {
        super(OUTPUT_NAME, nmeaAisDriver,1.0);
    }

    /**
     * Initializes the data structure for the output.
     *
     * Flat index map:
     *   0  = messageId        1  = repeat           2  = mmsi
     *   3  = navStatus        4  = rot              5  = sog
     *   6  = positionAccuracy
     *   7  = latitude  (lat component of location vector)
     *   8  = longitude (lon component of location vector)
     *   9  = cog              10 = heading          11 = timeStamp
     *   12 = smi              13 = raim             14 = commState
     *   15 = bits
     */
    public void doInit() {
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();

        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(OUTPUT_NAME)
                .label(OUTPUT_LABEL)
                .description(OUTPUT_DESCRIPTION)
                .definition(OUTPUT_DEFINITION)
                .addField("messageId", sweFactory.createQuantity()
                        .label("Message Id")
                        .description("Identifier for this message 1, 2 or 3")
                        .definition(SWEHelper.getPropertyUri("MessageId")))
                .addField("reportDescription", sweFactory.createText()
                        .label("Report Description")
                        .description("Describes the report based on the Message Id provided")
                        .definition(SWEHelper.getPropertyUri("ReportDescription")))
                .addField("repeat", sweFactory.createQuantity()
                        .label("Repeat Indicator")
                        .description("Used by the repeater to indicate how many times a message has been repeated. See Section 4.6.1, Annex 2; 0-3; 0 = default; 3 = do not repeat any more.")
                        .definition(SWEHelper.getPropertyUri("repeat")))
                .addField("mmsi", sweFactory.createQuantity()
                        .label("MMSI Number")
                        .description("MMSI Number")
                        .definition(SWEHelper.getPropertyUri("Mmsi")))
                .addField("navStatus", sweFactory.createQuantity()
                        .label("Navigational Status")
                        .description(
                                "0 = under way using engine, 1 = at anchor, 2 = not under command, 3 = restricted maneuverability, 4 = constrained by her draught, 5 = moored, 6 = aground, 7 = engaged in fishing, 8 = under way sailing, 9 = reserved for future amendment of navigational status for ships carrying DG, HS, or MP, or IMO hazard or pollutant category C, high speed craft (HSC), 10 = reserved for future amendment of navigational status for ships carrying dangerous goods (DG), harmful substances (HS) or marine pollutants (MP), or IMO hazard or pollutant category A, wing in ground (WIG); 11 = power-driven vessel towing astern (regional use); 12 = power-driven vessel pushing ahead or towing alongside (regional use);\n" +
                                "13 = reserved for future use,\n" +
                                "14 = AIS-SART (active), MOB-AIS, EPIRB-AIS\n" +
                                "15 = undefined = default (also used by AIS-SART, MOB-AIS and EPIRB-AIS under test)"
                        )
                        .definition(SWEHelper.getPropertyUri("NavStatus")))
                .addField("rot", sweFactory.createQuantity()
                        .label("Rate of Turn")
                        .description(
                                "0 to +126 = turning right at up to 708 deg per min or higher\n" +
                                "0 to -126 = turning left at up to 708 deg per min or higher Values between 0 and 708 deg per min coded by ROTAIS = 4.733 SQRT(ROTsensor) degrees per min\n" +
                                "where ROTsensor is the Rate of Turn as input by an external Rate of Turn Indicator (TI). ROTAIS is rounded to the nearest integer value.\n" +
                                "+127 = turning right at more than 5 deg per 30 s (No TI available)\n" +
                                "-127 = turning left at more than 5 deg per 30 s (No TI available)\n" +
                                "-128 (80 hex) indicates no turn information available (default).\n" +
                                "ROT data should not be derived from COG information."
                        )
                        .definition(SWEHelper.getPropertyUri("Rot")))
                .addField("sog", sweFactory.createQuantity()
                        .label("Speed Over Ground")
                        .description("Speed over ground in knots (0–102.2 knots, 102.3 = not available, 102.3+ should not be used)")
                        .uom("[kn_i]")
                        .definition(SWEHelper.getPropertyUri("SpeedOverGround")))
                .addField("positionAccuracy", sweFactory.createQuantity()
                        .label("Position Accuracy")
                        .description(
                                "1 = high (<= 10 m)\n" +
                                "0 = low (> 10 m)\n" +
                                "0 = default"
                        )
                        .definition(SWEHelper.getPropertyUri("PositionAccuracy")))
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("cog", sweFactory.createQuantity()
                        .label("COG")
                        .description("Course over ground in 1/10 = (0-3599). 3600 (E10h) = not available = default. 3601-4095 should not be used")
                        .definition(SWEHelper.getPropertyUri("Cog")))
                .addField("heading", sweFactory.createQuantity()
                        .label("True Heading")
                        .uom("deg")
                        .description("Degrees (0-359) (511 indicates not available = default)")
                        .definition(SWEHelper.getPropertyUri("heading")))
                .addField("timeStamp", sweFactory.createTime()
                        .label("Time Stamp")
                        .description("UTC second when the report was generated by the electronic position system (EPFS) (0-59, or 60 if time stamp is not available, which should also be the default value, or 61 if positioning system is in manual input mode, or 62 if electronic position fixing system operates in estimated (dead reckoning) mode, or 63 if the positioning system is inoperative)")
                        .definition(SWEHelper.getPropertyUri("TimeStamp")))
                .addField("smi", sweFactory.createQuantity()
                        .label("Special Maneuvre Indicator")
                        .description("0 = not available = default\n" +
                                "1 = not engaged in special maneuver\n" +
                                "2 = engaged in special maneuver\n" +
                                "(i.e.: regional passing arrangement on Inland Waterway)")
                        .definition(SWEHelper.getPropertyUri("Smi")))
                .addField("raim", sweFactory.createQuantity()
                        .label("RAIM-flag")
                        .description("Receiver autonomous integrity monitoring (RAIM) flag of electronic position fixing device; 0 = RAIM not in use = default; 1 = RAIM in use. See Table")
                        .definition(SWEHelper.getPropertyUri("Raim")))
                .addField("commState", sweFactory.createQuantity()
                        .label("Communication State")
                        .description("visit https://www.navcen.uscg.gov/ais-class-a-reports#CommState")
                        .definition(SWEHelper.getPropertyUri("CommState")));

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisPositionMessage report, String description) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setIntValue(0,  report.getMsgId());
            dataBlock.setStringValue(1, description);
            dataBlock.setIntValue(2,  report.getRepeat());
            dataBlock.setIntValue(3,  report.getUserId());
            dataBlock.setIntValue(4,  report.getNavStatus());
            dataBlock.setIntValue(5,  report.getRot());
            dataBlock.setDoubleValue(6,  report.getSog() / 10.0);
            dataBlock.setIntValue(7,  report.getPosAcc());
            dataBlock.setDoubleValue(8,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(9,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(10,  report.getCog() / 10.0);
            dataBlock.setIntValue(11, report.getTrueHeading());
            dataBlock.setIntValue(12, report.getUtcSec());
            dataBlock.setIntValue(13, report.getSpecialManIndicator());
            dataBlock.setIntValue(14, report.getRaim());
            dataBlock.setIntValue(15, report.getSyncState());

            String foiUID = parentSensor.addFoi(report.getUserId());

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputPositionClassA.this, foiUID, dataBlock));
        }
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
