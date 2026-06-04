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
                .addField("utcYear", sweFactory.createCount()
                        .label("UTC Year")
                        .description("UTC Year (1-9999, 0 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcYear")))
                .addField("utcMonth", sweFactory.createCount()
                        .label("UTC Month")
                        .addAllowedValues(IntStream.rangeClosed(0, 12).toArray())
                        .description("UTC Month (1-12, 0 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcMonth")))
                .addField("utcDay", sweFactory.createCount()
                        .label("UTC Day")
                        .addAllowedValues(IntStream.rangeClosed(0, 31).toArray())
                        .description("UTC Day (1-31, 0 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcDay")))
                .addField("utcHour", sweFactory.createCount()
                        .label("UTC Hour")
                        .addAllowedValues(IntStream.rangeClosed(0, 24).toArray())
                        .description("UTC Hour (0-23, 24 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcHour")))
                .addField("utcMinute", sweFactory.createCount()
                        .label("UTC Minute")
                        .addAllowedValues(IntStream.rangeClosed(0, 60).toArray())
                        .description("UTC Minute (0-59, 60 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcMinute")))
                .addField("utcSecond", sweFactory.createCount()
                        .label("UTC Second")
                        .addAllowedValues(IntStream.rangeClosed(0, 60).toArray())
                        .description("UTC Second (0-59, 60 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcSecond")))
                .addField("positionAccuracy", fac.createPositionAccuracy())
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("epfd", fac.createCategory()
                        .label("EPFD Type")
                        .addAllowedValues(0,1,2,3,4,5,6,7,8,15)
                        .description("Type of Electronic Position Fixing Device: 0 = undefined, 1 = GPS, 2 = GLONASS, 3 = Combined GPS/GLONASS, 4 = Loran-C, 5 = Chayka, 6 = Integrated navigation system, 7 = Surveyed, 8 = Galileo, 15 = internal GNSS")
                        .definition(SWEHelper.getPropertyUri("Epfd")))
                .addField("raim", fac.createCategory()
                        .label("RAIM Flag")
                        .addAllowedValues(0,1)
                        .description("Receiver autonomous integrity monitoring flag; 0 = RAIM not in use; 1 = RAIM in use")
                        .definition(SWEHelper.getPropertyUri("Raim")));

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
            dataBlock.setIntValue(5,  report.getUtcYear());
            dataBlock.setIntValue(6,  report.getUtcMonth());
            dataBlock.setIntValue(7,  report.getUtcDay());
            dataBlock.setIntValue(8,  report.getUtcHour());
            dataBlock.setIntValue(9,  report.getUtcMinute());
            dataBlock.setIntValue(10,  report.getUtcSecond());
            dataBlock.setStringValue(11, AisCodeHelper.PosAcc.getDescription(report.getPosAcc()));
            dataBlock.setDoubleValue(12, report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(13, report.getPos().getLongitudeDouble());
            dataBlock.setStringValue(14, String.valueOf(report.getPosType()));
            dataBlock.setStringValue(15, String.valueOf(report.getRaim()));

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
