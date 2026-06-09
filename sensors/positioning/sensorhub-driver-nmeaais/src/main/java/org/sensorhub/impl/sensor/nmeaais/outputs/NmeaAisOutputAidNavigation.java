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

import dk.dma.ais.message.AisMessage21;
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

public class NmeaAisOutputAidNavigation extends VarRateSensorOutput<NmeaAisDriver> implements NmeaAisReportInterface<AisMessage21> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "nmeaAisOutputAidNavigation";
    private static final String OUTPUT_LABEL       = "Aid-to-Navigation Report";
    private static final String OUTPUT_DESCRIPTION = "AIS Aid-to-Navigation Report (type 21) — buoys, lighthouses, beacons";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("NmeaAisOutputAidNavigation");

    private final Object processingLock = new Object();

    public NmeaAisOutputAidNavigation(NmeaAisDriver nmeaAisDriver) {
        super(OUTPUT_NAME, nmeaAisDriver, 1.0);
    }

    /**
     * Initializes the data structure for the output.
     *
     * Flat index map:
     *   0  = samplingTime         1  = messageId             2  = reportDescription
     *   3  = repeat               4  = mmsi                  5  = typeOfAidsToNav
     *   6  = name
     *   7  = positionAccuracy (boolean)
     *   8  = latitude  (lat component of location vector)
     *   9  = longitude (lon component of location vector)
     *   10 = dimBow               11 = dimStern              12 = dimPort
     *   13 = dimStarboard         14 = epfd                  15 = utcSecond
     *   16 = offPositionIndicator 17 = raim (boolean)
     *   18 = virtualAid (boolean) 19 = assignedMode
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
                .addField("typeOfAidsToNav", fac.createText()
                        .label("Type of Aid-to-Nav")
                        .description("Type of Aid to Navigation based on IALA Maritime Buyage System")
                        .definition(SWEHelper.getPropertyUri("TypeOfAidsToNav")))
                .addField("name", fac.createText()
                        .label("Name of Aid-to-Nav")
                        .description("Maximum 20 characters; padded with spaces")
                        .definition(SWEHelper.getPropertyUri("AidToNavName")))
                .addField("positionAccuracy", fac.createPositionAccuracy())
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("dimBow", fac.createQuantity()
                        .label("Dimension to Bow from AtoN Position")
                        .description("Size of the aid-to-navigation, bow to GPS antenna in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("AtoNDimBow")))
                .addField("dimStern", fac.createQuantity()
                        .label("Dimension to Ster from AtoN Positionn")
                        .description("Size of the aid-to-navigation, GPS antenna to stern in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("AtoNDimStern")))
                .addField("dimPort", fac.createQuantity()
                        .label("Dimension to Port from AtoN Position")
                        .description("Size of the aid-to-navigation, GPS antenna to port side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("AtoNDimPort")))
                .addField("dimStarboard", fac.createQuantity()
                        .label("Dimension to Starboard from AtoN Position")
                        .description("Size of the aid-to-navigation, GPS antenna to starboard side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("AtoNDimStarboard")))
                .addField("epfd", fac.createEpfd())
                .addField("utcSecond", fac.createUtcSecond())
                .addField("offPositionIndicator", fac.createOffPositionIndicator())
                .addField("raim", fac.createRAIM())
                .addField("virtualAid", fac.createVirtualAid())
                .addField("assignedMode", fac.createAssignedMode());

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisMessage21 report) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, AisCodeHelper.MessageType.getDescription(report.getMsgId()));
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            dataBlock.setStringValue(5, AisCodeHelper.AtoNType.getDescription(report.getAtonType()));
            dataBlock.setStringValue(6, AisCodeHelper.cleanVesselName(report.getName()));
            dataBlock.setBooleanValue(7, report.getPosAcc() == 1);
            dataBlock.setDoubleValue(8,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(9,  report.getPos().getLongitudeDouble());
            dataBlock.setIntValue(10,  report.getDimBow());
            dataBlock.setIntValue(11, report.getDimStern());
            dataBlock.setIntValue(12, report.getDimPort());
            dataBlock.setIntValue(13, report.getDimStarboard());
            dataBlock.setStringValue(14, AisCodeHelper.EpfdType.getDescription(report.getPosType()));
            dataBlock.setIntValue(15, report.getUtcSec());
            dataBlock.setStringValue(16, report.getUtcSec() >= 60
                    ? "N/A"
                    : AisCodeHelper.OffPositionIndicator.getDescription(report.getOffPosition()));
            dataBlock.setBooleanValue(17, report.getRaim()==1);
            dataBlock.setBooleanValue(18, report.getVirtual() == 1);
            dataBlock.setStringValue(19, AisCodeHelper.AssignedMode.getDescription(report.getAssigned()));

            String foiUID = parentSensor.addFoi(String.valueOf(report.getUserId()));

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputAidNavigation.this, foiUID, dataBlock));
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
