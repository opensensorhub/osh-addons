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
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

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
     *   0  = messageId          1  = reportDescription   2  = repeat
     *   3  = mmsi               4  = utcYear             5  = utcMonth
     *   6  = utcDay             7  = utcHour             8  = utcMinute
     *   9  = utcSecond          10 = positionAccuracy
     *   11 = latitude  (lat component of location vector)
     *   12 = longitude (lon component of location vector)
     *   13 = epfd               14 = raim
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
                        .description("Identifier for this message: 4 (UTC/Date Report) or 11 (UTC/Date Response)")
                        .definition(SWEHelper.getPropertyUri("MessageId")))
                .addField("reportDescription", sweFactory.createText()
                        .label("Report Description")
                        .description("Describes the report based on the Message Id provided")
                        .definition(SWEHelper.getPropertyUri("ReportDescription")))
                .addField("repeat", sweFactory.createQuantity()
                        .label("Repeat Indicator")
                        .description("Used by the repeater to indicate how many times a message has been repeated; 0-3; 0 = default")
                        .definition(SWEHelper.getPropertyUri("repeat")))
                .addField("mmsi", sweFactory.createQuantity()
                        .label("MMSI Number")
                        .description("MMSI Number of the base station")
                        .definition(SWEHelper.getPropertyUri("Mmsi")))
                .addField("utcYear", sweFactory.createQuantity()
                        .label("UTC Year")
                        .description("UTC Year (1-9999, 0 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcYear")))
                .addField("utcMonth", sweFactory.createQuantity()
                        .label("UTC Month")
                        .description("UTC Month (1-12, 0 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcMonth")))
                .addField("utcDay", sweFactory.createQuantity()
                        .label("UTC Day")
                        .description("UTC Day (1-31, 0 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcDay")))
                .addField("utcHour", sweFactory.createQuantity()
                        .label("UTC Hour")
                        .description("UTC Hour (0-23, 24 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcHour")))
                .addField("utcMinute", sweFactory.createQuantity()
                        .label("UTC Minute")
                        .description("UTC Minute (0-59, 60 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcMinute")))
                .addField("utcSecond", sweFactory.createQuantity()
                        .label("UTC Second")
                        .description("UTC Second (0-59, 60 = not available = default)")
                        .definition(SWEHelper.getPropertyUri("UtcSecond")))
                .addField("positionAccuracy", sweFactory.createQuantity()
                        .label("Position Accuracy")
                        .description("1 = high (<= 10 m); 0 = low (> 10 m); 0 = default")
                        .definition(SWEHelper.getPropertyUri("PositionAccuracy")))
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("epfd", sweFactory.createQuantity()
                        .label("Type of EPFD")
                        .description("0 = undefined, 1 = GPS, 2 = GLONASS, 3 = Combined GPS/GLONASS, 4 = Loran-C, 5 = Chayka, 6 = Integrated navigation system, 7 = Surveyed, 8 = Galileo, 15 = internal GNSS")
                        .definition(SWEHelper.getPropertyUri("Epfd")))
                .addField("raim", sweFactory.createQuantity()
                        .label("RAIM Flag")
                        .description("Receiver autonomous integrity monitoring flag; 0 = RAIM not in use; 1 = RAIM in use")
                        .definition(SWEHelper.getPropertyUri("Raim")));

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisMessage4 report, String description) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setIntValue(0,  report.getMsgId());
            dataBlock.setStringValue(1, description);
            dataBlock.setIntValue(2,  report.getRepeat());
            dataBlock.setIntValue(3,  report.getUserId());
            dataBlock.setIntValue(4,  report.getUtcYear());
            dataBlock.setIntValue(5,  report.getUtcMonth());
            dataBlock.setIntValue(6,  report.getUtcDay());
            dataBlock.setIntValue(7,  report.getUtcHour());
            dataBlock.setIntValue(8,  report.getUtcMinute());
            dataBlock.setIntValue(9,  report.getUtcSecond());
            dataBlock.setIntValue(10, report.getPosAcc());
            dataBlock.setDoubleValue(11, report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(12, report.getPos().getLongitudeDouble());
            dataBlock.setIntValue(13, report.getPosType());
            dataBlock.setIntValue(14, report.getRaim());

            String foiUID = parentSensor.addFoi(report.getUserId());

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
