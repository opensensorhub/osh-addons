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
     *   0  = messageId       1  = reportDescription   2  = repeat
     *   3  = mmsi            4  = aisVersion          5  = imoNumber
     *   6  = callSign        7  = name                8  = shipType
     *   9  = dimBow          10 = dimStern            11 = dimPort
     *   12 = dimStarboard    13 = epfd               14 = etaMonth
     *   15 = etaDay          16 = etaHour            17 = etaMinute
     *   18 = draught         19 = destination         20 = dte
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
                .addField("aisVersion", sweFactory.createQuantity()
                        .label("AIS Version")
                        .description("0 = ITU1371; 1-3 = future editions")
                        .definition(SWEHelper.getPropertyUri("AisVersion")))
                .addField("imoNumber", sweFactory.createQuantity()
                        .label("IMO Number")
                        .description("1-999999999; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("ImoNumber")))
                .addField("callSign", sweFactory.createText()
                        .label("Call Sign")
                        .description("7 x 6 bit ASCII characters, padded with spaces after")
                        .definition(SWEHelper.getPropertyUri("CallSign")))
                .addField("name", sweFactory.createText()
                        .label("Vessel Name")
                        .description("Maximum 20 characters; padded with spaces after. Indicate \"Not available\" if not known")
                        .definition(SWEHelper.getPropertyUri("VesselName")))
                .addField("shipType", sweFactory.createQuantity()
                        .label("Ship Type")
                        .description("0 = not available or no ship = default; 1-99 per ITU-R M.1371-5 Table 53")
                        .definition(SWEHelper.getPropertyUri("ShipType")))
                .addField("dimBow", sweFactory.createQuantity()
                        .label("Dimension to Bow")
                        .description("Distance from GPS antenna to bow in metres; 0 = not available = default; 511 = 511 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimBow")))
                .addField("dimStern", sweFactory.createQuantity()
                        .label("Dimension to Stern")
                        .description("Distance from GPS antenna to stern in metres; 0 = not available = default; 511 = 511 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStern")))
                .addField("dimPort", sweFactory.createQuantity()
                        .label("Dimension to Port")
                        .description("Distance from GPS antenna to port side in metres; 0 = not available = default; 63 = 63 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimPort")))
                .addField("dimStarboard", sweFactory.createQuantity()
                        .label("Dimension to Starboard")
                        .description("Distance from GPS antenna to starboard side in metres; 0 = not available = default; 63 = 63 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStarboard")))
                .addField("epfd", sweFactory.createQuantity()
                        .label("Type of EPFD")
                        .description("0 = undefined, 1 = GPS, 2 = GLONASS, 3 = Combined GPS/GLONASS, 4 = Loran-C, 5 = Chayka, 6 = Integrated navigation system, 7 = Surveyed, 8 = Galileo, 15 = internal GNSS")
                        .definition(SWEHelper.getPropertyUri("Epfd")))
                .addField("etaMonth", sweFactory.createQuantity()
                        .label("ETA Month")
                        .description("Estimated time of arrival month; 1-12; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaMonth")))
                .addField("etaDay", sweFactory.createQuantity()
                        .label("ETA Day")
                        .description("Estimated time of arrival day; 1-31; 0 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaDay")))
                .addField("etaHour", sweFactory.createQuantity()
                        .label("ETA Hour")
                        .description("Estimated time of arrival hour; 0-23; 24 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaHour")))
                .addField("etaMinute", sweFactory.createQuantity()
                        .label("ETA Minute")
                        .description("Estimated time of arrival minute; 0-59; 60 = not available = default")
                        .definition(SWEHelper.getPropertyUri("EtaMinute")))
                .addField("draught", sweFactory.createQuantity()
                        .label("Maximum Present Static Draught")
                        .description("In metres; 1/10 m steps; 0 = not available = default; 25.5 m = draught is 25.5 m or greater")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("Draught")))
                .addField("destination", sweFactory.createText()
                        .label("Destination")
                        .description("Maximum 20 characters; padded with spaces after. Indicate \"Not available\" if not known")
                        .definition(SWEHelper.getPropertyUri("Destination")))
                .addField("dte", sweFactory.createQuantity()
                        .label("DTE")
                        .description("Data terminal equipment (DTE) available; 0 = available; 1 = not available = default")
                        .definition(SWEHelper.getPropertyUri("Dte")));

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisMessage5 report, String description) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, description);
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            dataBlock.setIntValue(5,  report.getVersion());
            dataBlock.setLongValue(6,  report.getImo());
            dataBlock.setStringValue(7, report.getCallsign());
            dataBlock.setStringValue(8, report.getName());
            dataBlock.setIntValue(9,  report.getShipType());
            dataBlock.setIntValue(10,  report.getDimBow());
            dataBlock.setIntValue(11, report.getDimStern());
            dataBlock.setIntValue(12, report.getDimPort());
            dataBlock.setIntValue(13, report.getDimStarboard());
            dataBlock.setIntValue(14, report.getPosType());

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
            dataBlock.setStringValue(20, report.getDest());
            dataBlock.setIntValue(21, report.getDte());

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
