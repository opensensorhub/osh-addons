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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.IntStream;

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

public class NmeaAisOutputStaticVoyage extends VarRateSensorOutput<NmeaAisDriver> implements NmeaAisReportInterface<AisMessage5> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "nmeaAisOutputStaticVoyage";
    private static final String OUTPUT_LABEL       = "Static and Voyage Related Data";
    private static final String OUTPUT_DESCRIPTION = "AIS Static and Voyage Related Data Report (type 5)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("NmeaAisOutputStaticVoyage");

    private final Object processingLock = new Object();

    public NmeaAisOutputStaticVoyage(NmeaAisDriver nmeaAisDriver) {
        super(OUTPUT_NAME, nmeaAisDriver, 1.0);
    }

    /**
     * Initializes the data structure for the output.
     *
     * Flat index map:
     *   0  = samplingTime        1  = messageId               2  = reportDescription
     *   3  = repeat              4  = mmsi                    5  = aisVersion
     *   6  = imoNumber           7  = callSign                8  = name
     *   9  = shipType            10 = dimBow                  11 = dimStern
     *   12 = dimPort             13 = dimStarboard            14 = epfd
     *   15 = etaMonth            16 = etaDay                  17 = etaHour
     *   18 = etaMinute           19 = draught                 20 = destination
     *   21 = dte
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
                .addField("aisVersion", fac.createCategory()
                        .label("AIS Version")
                        .addAllowedValues(0,1,2,3)
                        .description("0 = ITU1371; 1-3 = future editions")
                        .definition(SWEHelper.getPropertyUri("AisVersion")))
                .addField("imoNumber", fac.createQuantity()
                        .label("IMO Number")
                        .description("1-999999999; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("ImoNumber")))
                .addField("callSign", fac.createText()
                        .label("Call Sign")
                        .description("7 x 6 bit ASCII characters, padded with spaces after")
                        .definition(SWEHelper.getPropertyUri("CallSign")))
                .addField("name", fac.createVesselName())
                .addField("shipType", fac.createText()
                        .label("Ship Type")
                        .description("0 = not available or no ship = default; 1-99 per ITU-R M.1371-5 Table 53")
                        .definition(SWEHelper.getPropertyUri("ShipType")))
                .addField("dimBow", fac.createQuantity()
                        .label("Dimension to Bow")
                        .description("Distance from GPS antenna to bow in metres; 0 = not available = default; 511 = 511 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimBow")))
                .addField("dimStern", fac.createQuantity()
                        .label("Dimension to Stern")
                        .description("Distance from GPS antenna to stern in metres; 0 = not available = default; 511 = 511 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStern")))
                .addField("dimPort", fac.createQuantity()
                        .label("Dimension to Port")
                        .description("Distance from GPS antenna to port side in metres; 0 = not available = default; 63 = 63 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimPort")))
                .addField("dimStarboard", fac.createQuantity()
                        .label("Dimension to Starboard")
                        .description("Distance from GPS antenna to starboard side in metres; 0 = not available = default; 63 = 63 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStarboard")))
                .addField("epfd", fac.createEpfd())
                .addField("etaMonth", fac.createCount()
                        .label("ETA Month")
                        .addAllowedValues(IntStream.rangeClosed(0, 12).toArray())
                        .description("Estimated time of arrival month; 1-12; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaMonth")))
                .addField("etaDay", fac.createCount()
                        .label("ETA Day")
                        .addAllowedValues(IntStream.rangeClosed(0, 31).toArray())
                        .description("Estimated time of arrival day; 1-31; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaDay")))
                .addField("etaHour", fac.createCount()
                        .label("ETA Hour")
                        .addAllowedValues(IntStream.rangeClosed(0, 24).toArray())
                        .description("Estimated time of arrival hour; 0-23; 24 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaHour")))
                .addField("etaMinute", fac.createCount()
                        .label("ETA Minute")
                        .addAllowedValues(IntStream.rangeClosed(0, 60).toArray())
                        .description("Estimated time of arrival minute; 0-59; 60 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaMinute")))
                .addField("draught", fac.createQuantity()
                        .label("Maximum Present Static Draught")
                        .description("In metres; 1/10 m steps; 0 = not available = default; 25.5 m = draught is 25.5 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("Draught")))
                .addField("destination", fac.createText()
                        .label("Destination")
                        .description("Maximum 20 characters; padded with spaces after. Indicate \"Not available\" if not known")
                        .definition(SWEHelper.getPropertyUri("Destination")))
                .addField("dte", fac.createDte());

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisMessage5 report) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            dataBlock.setStringValue(5, String.valueOf(report.getVersion()));
            dataBlock.setLongValue(6,  report.getImo());
            dataBlock.setStringValue(7, report.getCallsign());
            dataBlock.setStringValue(8, AisCodeHelper.cleanVesselName(report.getName()));
            dataBlock.setStringValue(9, AisCodeHelper.ShipType.getDescription(report.getShipType()));
            dataBlock.setIntValue(10,  report.getDimBow());
            dataBlock.setIntValue(11, report.getDimStern());
            dataBlock.setIntValue(12, report.getDimPort());
            dataBlock.setIntValue(13, report.getDimStarboard());
            dataBlock.setStringValue(14, AisCodeHelper.EpfdType.getDescription(report.getPosType()));

            // ETA is stored as a packed long; getEtaDate() converts to java.util.Date
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
            dataBlock.setIntValue(15, etaMonth);
            dataBlock.setIntValue(16, etaDay);
            dataBlock.setIntValue(17, etaHour);
            dataBlock.setIntValue(18, etaMinute);
            dataBlock.setDoubleValue(19, report.getDraught() / 10.0);
            dataBlock.setStringValue(20, AisCodeHelper.cleanVesselName(report.getDest()));
            dataBlock.setStringValue(21, AisCodeHelper.Dte.getDescription(report.getDte()));

            String foiUID = parentSensor.addFoi(String.valueOf(report.getUserId()));

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputStaticVoyage.this, foiUID, dataBlock));
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
