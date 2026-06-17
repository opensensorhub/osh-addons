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

import dk.dma.ais.message.AisMessage5;
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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.IntStream;

/**
 * Voyage-specific data output for AIS type 5 (Class A Static and Voyage Related Data).
 *
 * Voyage fields change per voyage (destination, ETA, draught) and are streamed here.
 * Static vessel identity fields from the same type 5 message (name, callsign, ship type,
 * dimensions, IMO, AIS version, EPFD) are written to the FOI as properties instead.
 *
 * Flat DataBlock index map:
 *   0  samplingTime     1  messageId        2  reportDescription
 *   3  repeat           4  mmsi
 *   5  destination      6  etaMonth         7  etaDay
 *   8  etaHour          9  etaMinute        10 draught
 *   11 dte
 */
public class NmeaAisOutputVoyageInfo extends VarRateSensorOutput<NmeaAisDriver> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "voyageInfo";
    private static final String OUTPUT_LABEL       = "Voyage Info";
    private static final String OUTPUT_DESCRIPTION = "AIS Class A voyage-related data (type 5): destination, ETA, and draught. " +
                                                     "Static vessel identity from the same message is written to the FOI.";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("VoyageInfo");

    private final Object processingLock = new Object();

    public NmeaAisOutputVoyageInfo(NmeaAisDriver nmeaAisDriver) {
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
                .addField("destination", fac.createText()
                        .label("Destination")
                        .description("Maximum 20 characters; '@' padding stripped. \"Not available\" if not known")
                        .definition(SWEHelper.getPropertyUri("Destination")))
                .addField("etaMonth", fac.createCount()
                        .label("ETA Month")
                        .addAllowedValues(IntStream.rangeClosed(0, 12).toArray())
                        .description("Estimated time of arrival month; 1–12; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaMonth")))
                .addField("etaDay", fac.createCount()
                        .label("ETA Day")
                        .addAllowedValues(IntStream.rangeClosed(0, 31).toArray())
                        .description("Estimated time of arrival day; 1–31; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaDay")))
                .addField("etaHour", fac.createCount()
                        .label("ETA Hour")
                        .addAllowedValues(IntStream.rangeClosed(0, 24).toArray())
                        .description("Estimated time of arrival hour; 0–23; 24 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaHour")))
                .addField("etaMinute", fac.createCount()
                        .label("ETA Minute")
                        .addAllowedValues(IntStream.rangeClosed(0, 60).toArray())
                        .description("Estimated time of arrival minute; 0–59; 60 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaMinute")))
                .addField("draught", fac.createQuantity()
                        .label("Maximum Present Static Draught")
                        .description("In metres; 1/10 m steps; 0 = not available = default; 25.5 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("Draught")))
                .addField("dte", fac.createDte());

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    /**
     * Publishes voyage data and updates the FOI with static vessel identity.
     */
    public void setData(AisMessage5 report) {
        synchronized (processingLock) {
            String mmsi = String.valueOf(report.getUserId());

            // Static identity → FOI properties (not streamed)
            String foiUID = parentSensor.updateFoiStaticData(
                    mmsi,
                    AisCodeHelper.cleanVesselName(report.getName()),
                    report.getCallsign(),
                    report.getShipType(),
                    report.getImo(),
                    report.getVersion(),
                    null,   // vendorId not in type 5
                    AisCodeHelper.EpfdType.getDescription(report.getPosType()),
                    report.getDte() != 1,
                    report.getDimBow(),
                    report.getDimStern(),
                    report.getDimPort(),
                    report.getDimStarboard()
            );

            // Voyage data → datastream
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,   report.getMsgId());
            dataBlock.setStringValue(2, AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,   report.getRepeat());
            dataBlock.setStringValue(4, mmsi);
            dataBlock.setStringValue(5, AisCodeHelper.cleanVesselName(report.getDest()));

            int etaMonth = 0, etaDay = 0, etaHour = 24, etaMinute = 60;
            Date etaDate = report.getEtaDate();
            if (etaDate != null) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTime(etaDate);
                etaMonth  = cal.get(Calendar.MONTH) + 1;
                etaDay    = cal.get(Calendar.DAY_OF_MONTH);
                etaHour   = cal.get(Calendar.HOUR_OF_DAY);
                etaMinute = cal.get(Calendar.MINUTE);
            }
            dataBlock.setIntValue(6,    etaMonth);
            dataBlock.setIntValue(7,    etaDay);
            dataBlock.setIntValue(8,    etaHour);
            dataBlock.setIntValue(9,    etaMinute);
            dataBlock.setDoubleValue(10, report.getDraught() / 10.0);
            dataBlock.setBooleanValue(11, report.getDte() != 1);

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputVoyageInfo.this, foiUID, dataBlock));
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
