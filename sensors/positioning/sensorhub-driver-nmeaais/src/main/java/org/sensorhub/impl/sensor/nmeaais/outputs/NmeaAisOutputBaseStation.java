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

import dk.dma.ais.message.AisMessage4;
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
import org.vast.util.DateTime;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

public class NmeaAisOutputBaseStation extends VarRateSensorOutput<NmeaAisDriver> implements NmeaAisReportInterface<AisMessage4> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "nmeaAisOutputBaseStation";
    private static final String OUTPUT_LABEL       = "Base Station Report";
    private static final String OUTPUT_DESCRIPTION = "AIS Base Station / UTC-Date Response Report (types 4 and 11)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("NmeaAisOutputBaseStation");

    private final Object processingLock = new Object();

    public NmeaAisOutputBaseStation(NmeaAisDriver nmeaAisDriver) {
        super(OUTPUT_NAME, nmeaAisDriver, 1.0);
    }

    /**
     * Initializes the data structure for the output.
     *
     * Flat index map:
     *   0  = samplingTime       1  = messageId            2  = reportDescription
     *   3  = repeat             4  = mmsi                 5  = utcYear
     *   6  = utcMonth           7  = utcDay               8  = utcHour
     *   9  = utcMinute          10 = utcSecond
     *   11 = positionAccuracy (Category)
     *   12 = latitude  (lat component of location vector)
     *   13 = longitude (lon component of location vector)
     *   14 = epfd (Category)    15 = raim (Category)
     */
    public void doInit() {
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();
        NmeaAisHelper fac = new NmeaAisHelper();

        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
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
                .addField("utcDateTime", sweFactory.createTime()
                        .label("UTC Date Time")
                        .description("UTC Date Time")
                        .definition(SWEHelper.getPropertyUri("UtcYear")))
                .addField("positionAccuracy", fac.createPositionAccuracy())
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("epfd", fac.createEpfd())
                .addField("raim", fac.createRAIM());

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisMessage4 report) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            // Create DateTime for UtcTime Values
            ZonedDateTime utcDateTime = ZonedDateTime.of(
                    report.getUtcYear(),
                    report.getUtcMonth(),
                    report.getUtcDay(),
                    report.getUtcHour(),
                    report.getUtcMinute(),
                    report.getUtcSecond(),
                    0,
                    ZoneOffset.UTC
            );
            dataBlock.setDateTime(5,  utcDateTime.toOffsetDateTime());
            dataBlock.setStringValue(6, AisCodeHelper.PosAcc.getDescription(report.getPosAcc()));
            dataBlock.setDoubleValue(7, report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(8, report.getPos().getLongitudeDouble());
            dataBlock.setStringValue(9, AisCodeHelper.EpfdType.getDescription(report.getPosType()));
            dataBlock.setStringValue(10, AisCodeHelper.RaimFlag.getDescription(report.getRaim()));

            String foiUID = parentSensor.addFoi(String.valueOf(report.getUserId()));

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputBaseStation.this, foiUID, dataBlock));
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
