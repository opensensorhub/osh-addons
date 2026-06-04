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

import java.util.stream.IntStream;

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
     *   3  = repeat               4  = mmsi                  5  = typeOfAidsToNav (Category)
     *   6  = name
     *   7  = positionAccuracy (Category)
     *   8  = latitude  (lat component of location vector)
     *   9  = longitude (lon component of location vector)
     *   10 = dimBow               11 = dimStern              12 = dimPort
     *   13 = dimStarboard         14 = epfd (Category)       15 = utcSecond
     *   16 = offPositionIndicator (Category)                 17 = raim (Category)
     *   18 = virtualAid (Category)                           19 = assignedMode (Category)
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
                        .label("Dimension to Bow")
                        .description("Size of the aid-to-navigation, bow to GPS antenna in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimBow")))
                .addField("dimStern", fac.createQuantity()
                        .label("Dimension to Stern")
                        .description("Size of the aid-to-navigation, GPS antenna to stern in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStern")))
                .addField("dimPort", fac.createQuantity()
                        .label("Dimension to Port")
                        .description("Size of the aid-to-navigation, GPS antenna to port side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimPort")))
                .addField("dimStarboard", fac.createQuantity()
                        .label("Dimension to Starboard")
                        .description("Size of the aid-to-navigation, GPS antenna to starboard side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStarboard")))
                .addField("epfd", fac.createEpfd())
                .addField("utcSecond", fac.createQuantity()
                        .label("UTC Second")
                        .uom("s")
                        .description("UTC second when report was generated (0-59); 60 = not available = default")
                        .definition(SWEHelper.getPropertyUri("UtcSecond")))
                .addField("offPositionIndicator", fac.createCategory()
                        .label("Off Position Indicator")
                        .addAllowedValues(0,1)
                        .description("For floating aids-to-navigation: 0 = on position; 1 = off position. Only valid if UTC second is 0-59")
                        .definition(SWEHelper.getPropertyUri("OffPositionIndicator")))
                .addField("raim", fac.createRAIM())
                .addField("virtualAid", fac.createCategory()
                        .label("Virtual Aid Flag")
                        .addAllowedValues(0,1)
                        .description("0 = default; 1 = virtual aid to navigation simulated by nearby AIS station")
                        .definition(SWEHelper.getPropertyUri("VirtualAid")))
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
            dataBlock.setStringValue(6, report.getName());
            dataBlock.setStringValue(7, AisCodeHelper.PosAcc.getDescription(report.getPosAcc()));
            dataBlock.setDoubleValue(8,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(9,  report.getPos().getLongitudeDouble());
            dataBlock.setIntValue(10,  report.getDimBow());
            dataBlock.setIntValue(11, report.getDimStern());
            dataBlock.setIntValue(12, report.getDimPort());
            dataBlock.setIntValue(13, report.getDimStarboard());
            dataBlock.setStringValue(14, AisCodeHelper.EpfdType.getDescription(report.getPosType()));
            dataBlock.setIntValue(15, report.getUtcSec());
            dataBlock.setStringValue(16, AisCodeHelper.OffPositionIndicator.getDescription(report.getOffPosition()));
            dataBlock.setStringValue(17, AisCodeHelper.RaimFlag.getDescription(report.getRaim()));
            dataBlock.setStringValue(18, AisCodeHelper.VirtualAtoN.getDescription(report.getVirtual()));
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
