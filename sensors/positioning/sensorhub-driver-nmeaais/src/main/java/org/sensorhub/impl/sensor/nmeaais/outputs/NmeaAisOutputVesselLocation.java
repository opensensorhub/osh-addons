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
import dk.dma.ais.message.AisPositionMessage;
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
 * Unified vessel location output for AIS types 1, 2, 3 (Class A) and 18, 19 (Class B).
 *
 * Flat DataBlock index map:
 *   0  samplingTime          1  messageId             2  reportDescription
 *   3  repeat                4  mmsi
 *   5  sog                   6  positionAccuracy (boolean)
 *   7  latitude  (lat slot of location vector)
 *   8  longitude (lon slot of location vector)
 *   9  cog                   10 heading               11 utcSecond
 *   12 raim (boolean)
 *   13 rot      (Class A only; 0.0 for Class B)
 *   14 roti     (Class A only; "" for Class B)
 *   15 smi      (Class A only; "" for Class B)
 *   16 navStatus (Class A only; "" for Class B)
 */
public class NmeaAisOutputVesselLocation extends VarRateSensorOutput<NmeaAisDriver> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "vesselLocation";
    private static final String OUTPUT_LABEL       = "Vessel Location";
    private static final String OUTPUT_DESCRIPTION = "Unified AIS vessel location — types 1/2/3 (Class A) and 18/19 (Class B). " +
                                                     "rot, roti, and smi are Class A only; Class B records carry 0/empty values for those fields.";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("VesselLocation");

    private final Object processingLock = new Object();

    public NmeaAisOutputVesselLocation(NmeaAisDriver nmeaAisDriver) {
        super(OUTPUT_NAME, nmeaAisDriver, 1.0);
    }

    public void doInit() {
        GeoPosHelper geoFac = new GeoPosHelper();
        NmeaAisHelper fac = new NmeaAisHelper();

        SWEBuilders.DataRecordBuilder recordBuilder = fac.createRecord()
                .name(OUTPUT_NAME)
                .label(OUTPUT_LABEL)
                .description(OUTPUT_DESCRIPTION)
                .definition(OUTPUT_DEFINITION)
                .addField("samplingTime", fac.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time data was received")
                        .definition("SampleTime"))
                .addField("messageId", fac.createNmeaMessageId())
                .addField("reportDescription", fac.createReportDescription())
                .addField("repeat", fac.createRepeatIndicator())
                .addField("mmsi", fac.createMssi())
                .addField("sog", fac.createSog())
                .addField("positionAccuracy", fac.createPositionAccuracy())
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("cog", fac.createCog())
                .addField("heading", fac.createHeading())
                .addField("utcSecond", fac.createUtcSecond())
                .addField("raim", fac.createRAIM())
                .addField("rot", fac.createRot())
                .addField("roti", fac.createRoti())
                .addField("smi", fac.createSmi())
                .addField("navStatus", fac.createNavStatus());

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    /** Types 1, 2, 3 — Class A position report. Also updates FOI navStatus property. */
    public void setData(AisPositionMessage report) {
        synchronized (processingLock) {
            String mmsi = String.valueOf(report.getUserId());
            String foiUID = parentSensor.addVesselFoi(mmsi);

            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            AisCodeHelper.RotRecord rotData = AisCodeHelper.getRotData(report.getRot());

            dataBlock.setDoubleValue(0,  System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,     report.getMsgId());
            dataBlock.setStringValue(2,  AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,     report.getRepeat());
            dataBlock.setStringValue(4,  mmsi);
            dataBlock.setDoubleValue(5,  report.getSog() / 10.0);
            dataBlock.setBooleanValue(6, report.getPosAcc() == 1);
            dataBlock.setDoubleValue(7,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(8,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(9,  report.getCog() / 10.0);
            dataBlock.setIntValue(10,    report.getTrueHeading());
            dataBlock.setIntValue(11,    report.getUtcSec());
            dataBlock.setBooleanValue(12, report.getRaim() == 1);
            dataBlock.setDoubleValue(13, rotData.rot() != null ? rotData.rot() : 0.0);
            dataBlock.setStringValue(14, rotData.roti());
            dataBlock.setStringValue(15, AisCodeHelper.Spi.getDescription(report.getSpecialManIndicator()));
            dataBlock.setStringValue(16, AisCodeHelper.NavigationalStatus.getDescription(report.getNavStatus()));

            publish(dataBlock, foiUID);
        }
    }

    /** Type 18 — Class B standard CS position report. rot/roti/smi set to 0/"". */
    public void setData(AisMessage18 report) {
        synchronized (processingLock) {
            String mmsi = String.valueOf(report.getUserId());
            String foiUID = parentSensor.addVesselFoi(mmsi);

            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0,  System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,     report.getMsgId());
            dataBlock.setStringValue(2,  AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,     report.getRepeat());
            dataBlock.setStringValue(4,  mmsi);
            dataBlock.setDoubleValue(5,  report.getSog() / 10.0);
            dataBlock.setBooleanValue(6, report.getPosAcc() == 1);
            dataBlock.setDoubleValue(7,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(8,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(9,  report.getCog() / 10.0);
            dataBlock.setIntValue(10,    report.getTrueHeading());
            dataBlock.setIntValue(11,    report.getUtcSec());
            dataBlock.setBooleanValue(12, report.getRaim() == 1);
            // Class B — no ROT/SMI/navStatus
            dataBlock.setDoubleValue(13, 0.0);
            dataBlock.setStringValue(14, "");
            dataBlock.setStringValue(15, "");
            dataBlock.setStringValue(16, "");

            publish(dataBlock, foiUID);
        }
    }

    /**
     * Type 19 — Class B extended CS position report.
     * Also updates FOI with static identity data carried in this message.
     */
    public void setData(AisMessage19 report) {
        synchronized (processingLock) {
            String mmsi = String.valueOf(report.getUserId());
            String foiUID = parentSensor.updateFoiStaticData(
                    mmsi,
                    AisCodeHelper.cleanVesselName(report.getName()),
                    null,   // type 19 has no callsign
                    report.getShipType(),
                    0L,     // no IMO
                    0,      // no AIS version
                    null,   // no vendor ID
                    AisCodeHelper.EpfdType.getDescription(report.getPosType()),
                    report.getDte() != 1,
                    report.getDimBow(),
                    report.getDimStern(),
                    report.getDimPort(),
                    report.getDimStarboard()
            );

            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0,  System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,     report.getMsgId());
            dataBlock.setStringValue(2,  AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,     report.getRepeat());
            dataBlock.setStringValue(4,  mmsi);
            dataBlock.setDoubleValue(5,  report.getSog() / 10.0);
            dataBlock.setBooleanValue(6, report.getPosAcc() == 1);
            dataBlock.setDoubleValue(7,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(8,  report.getPos().getLongitudeDouble());
            dataBlock.setDoubleValue(9,  report.getCog() / 10.0);
            dataBlock.setIntValue(10,    report.getTrueHeading());
            dataBlock.setIntValue(11,    report.getUtcSec());
            dataBlock.setBooleanValue(12, report.getRaim() == 1);
            // Class B extended — no ROT/SMI/navStatus
            dataBlock.setDoubleValue(13, 0.0);
            dataBlock.setStringValue(14, "");
            dataBlock.setStringValue(15, "");
            dataBlock.setStringValue(16, "");

            publish(dataBlock, foiUID);
        }
    }

    private void publish(DataBlock dataBlock, String foiUID) {
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        updateSamplingPeriod(latestRecordTime);
        eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputVesselLocation.this, foiUID, dataBlock));
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
