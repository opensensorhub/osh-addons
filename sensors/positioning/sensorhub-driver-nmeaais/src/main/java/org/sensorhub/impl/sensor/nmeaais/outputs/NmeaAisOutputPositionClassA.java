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
import org.sensorhub.impl.sensor.nmeaais.helpers.AisCodeHelper;
import org.sensorhub.impl.sensor.nmeaais.helpers.NmeaAisHelper;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.stream.IntStream;

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
     *   0  = samplingTime          1  = messageId              2  = reportDescription
     *   3  = repeat                4  = mmsi                   5  = navStatus (Category)
     *   6  = rot                   7  = sog
     *   8  = positionAccuracy (Category)
     *   9  = latitude  (lat component of location vector)
     *   10 = longitude (lon component of location vector)
     *   11 = cog                   12 = heading                13 = utcSecond (Count)
     *   14 = smi (Category)        15 = raim (Category)        16 = commState
     */
    public void doInit() {
        GeoPosHelper geoFac = new GeoPosHelper();
        NmeaAisHelper fac = new NmeaAisHelper();

        SWEBuilders.DataRecordBuilder recordBuilder = fac.createRecord()
                .name(OUTPUT_NAME)
                .label(OUTPUT_LABEL)
                .description(OUTPUT_DESCRIPTION)
                .definition(OUTPUT_DEFINITION)
                .addField("samplingTime",fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time data was received")
                        .definition("SampleTime"))
                .addField("messageId", fac.createNmeaMessageId())
                .addField("reportDescription", fac.createReportDescription())
                .addField("repeat", fac.createRepeatIndicator())
                .addField("mmsi", fac.createMssi())
                .addField("navStatus", fac.createTime()
                        .label("Navigational Status")
                        .description(
                                "0 = under way using engine, 1 = at anchor, 2 = not under command, 3 = restricted maneuverability, 4 = constrained by her draught, 5 = moored, 6 = aground, 7 = engaged in fishing, 8 = under way sailing, 9 = reserved for future amendment of navigational status for ships carrying DG, HS, or MP, or IMO hazard or pollutant category C, high speed craft (HSC), 10 = reserved for future amendment of navigational status for ships carrying dangerous goods (DG), harmful substances (HS) or marine pollutants (MP), or IMO hazard or pollutant category A, wing in ground (WIG); 11 = power-driven vessel towing astern (regional use); 12 = power-driven vessel pushing ahead or towing alongside (regional use);\n" +
                                "13 = reserved for future use,\n" +
                                "14 = AIS-SART (active), MOB-AIS, EPIRB-AIS\n" +
                                "15 = undefined = default (also used by AIS-SART, MOB-AIS and EPIRB-AIS under test)"
                        )
                        .definition(SWEHelper.getPropertyUri("NavStatus")))
                .addField("rot", fac.createQuantity()
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
                .addField("sog", fac.createQuantity()
                        .label("Speed Over Ground")
                        .description("Speed over ground in knots (0–102.2 knots, 102.3 = not available, 102.3+ should not be used)")
                        .uom("[kn_i]")
                        .definition(SWEHelper.getPropertyUri("SpeedOverGround")))
                .addField("positionAccuracy", fac.createPositionAccuracy())
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("cog", fac.createQuantity()
                        .label("COG")
                        .description("Course over ground in 1/10 = (0-3599). 3600 (E10h) = not available = default. 3601-4095 should not be used")
                        .definition(SWEHelper.getPropertyUri("Cog")))
                .addField("heading", fac.createQuantity()
                        .label("True Heading")
                        .uom("deg")
                        .description("Degrees (0-359) (511 indicates not available = default)")
                        .definition(SWEHelper.getPropertyUri("heading")))
                .addField("utcSecond", fac.createQuantity()
                        .label("UTC Second")
                        .uom("s")
                        .description("UTC second when the report was generated by the electronic position system (EPFS) (0-59, or 60 if time stamp is not available, which should also be the default value, or 61 if positioning system is in manual input mode, or 62 if electronic position fixing system operates in estimated (dead reckoning) mode, or 63 if the positioning system is inoperative)")
                        .definition(SWEHelper.getPropertyUri("UtcSecond")))
                .addField("smi", fac.createCategory()
                        .label("Special Maneuvre Indicator")
                        .addAllowedValues(0,1,2)
                        .description("0 = not available = default\n" +
                                "1 = not engaged in special maneuver\n" +
                                "2 = engaged in special maneuver\n" +
                                "(i.e.: regional passing arrangement on Inland Waterway)")
                        .definition(SWEHelper.getPropertyUri("Smi")))
                .addField("raim", fac.createCategory()
                        .label("RAIM-flag")
                        .addAllowedValues(0,1)
                        .description("Receiver autonomous integrity monitoring (RAIM) flag of electronic position fixing device; 0 = RAIM not in use = default; 1 = RAIM in use. See Table")
                        .definition(SWEHelper.getPropertyUri("Raim")))
                .addField("commState", fac.createCount()
                        .label("Communication State")
                        .description("visit https://www.navcen.uscg.gov/ais-class-a-reports#CommState")
                        .definition(SWEHelper.getPropertyUri("CommState")))
                ;

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisPositionMessage report) {

        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            dataBlock.setStringValue(5, AisCodeHelper.NavigationalStatus.getDescription(report.getNavStatus()));
            dataBlock.setIntValue(6,  report.getRot());
            dataBlock.setDoubleValue(7,  report.getSog() / 10.0);
            dataBlock.setStringValue(8, String.valueOf(report.getPosAcc()));
            dataBlock.setDoubleValue(9,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(10,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(11,  report.getCog() / 10.0);
            dataBlock.setIntValue(12, report.getTrueHeading());
            dataBlock.setIntValue(13, report.getUtcSec());
            dataBlock.setStringValue(14, String.valueOf(report.getSpecialManIndicator()));
            dataBlock.setStringValue(15, String.valueOf(report.getRaim()));
            dataBlock.setIntValue(16, report.getSyncState());

            String foiUID = parentSensor.addFoi(String.valueOf(report.getUserId()));

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
