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
     *   0  = messageId            1  = reportDescription     2  = repeat
     *   3  = mmsi                 4  = typeOfAidsToNav       5  = name
     *   6  = positionAccuracy
     *   7  = latitude  (lat component of location vector)
     *   8  = longitude (lon component of location vector)
     *   9  = dimBow               10 = dimStern              11 = dimPort
     *   12 = dimStarboard         13 = epfd                  14 = utcSecond
     *   15 = offPositionIndicator 16 = raim                  17 = virtualAid
     *   18 = assignedMode
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
                .addField("typeOfAidsToNav", sweFactory.createQuantity()
                        .label("Type of Aid-to-Nav")
                        .description("1 = Default/unspecified; 2 = Reference point; 3 = RACON; 4 = Fixed structure; 5-20 = Fixed; 21-29 = Floating; 30-31 = Landfall/Coast/Inland")
                        .definition(SWEHelper.getPropertyUri("TypeOfAidsToNav")))
                .addField("name", sweFactory.createText()
                        .label("Name of Aid-to-Nav")
                        .description("Maximum 20 characters; padded with spaces")
                        .definition(SWEHelper.getPropertyUri("AidToNavName")))
                .addField("positionAccuracy", sweFactory.createQuantity()
                        .label("Position Accuracy")
                        .description("1 = high (<= 10 m); 0 = low (> 10 m); 0 = default")
                        .definition(SWEHelper.getPropertyUri("PositionAccuracy")))
                .addField("location", geoFac.createLocationVectorLatLon()
                        .label("Location"))
                .addField("dimBow", sweFactory.createQuantity()
                        .label("Dimension to Bow")
                        .description("Size of the aid-to-navigation, bow to GPS antenna in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimBow")))
                .addField("dimStern", sweFactory.createQuantity()
                        .label("Dimension to Stern")
                        .description("Size of the aid-to-navigation, GPS antenna to stern in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStern")))
                .addField("dimPort", sweFactory.createQuantity()
                        .label("Dimension to Port")
                        .description("Size of the aid-to-navigation, GPS antenna to port side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimPort")))
                .addField("dimStarboard", sweFactory.createQuantity()
                        .label("Dimension to Starboard")
                        .description("Size of the aid-to-navigation, GPS antenna to starboard side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStarboard")))
                .addField("epfd", sweFactory.createQuantity()
                        .label("Type of EPFD")
                        .description("0 = undefined, 1 = GPS, 2 = GLONASS, 3 = Combined GPS/GLONASS, 4 = Loran-C, 5 = Chayka, 6 = Integrated navigation system, 7 = Surveyed, 8 = Galileo, 15 = internal GNSS")
                        .definition(SWEHelper.getPropertyUri("Epfd")))
                .addField("utcSecond", sweFactory.createQuantity()
                        .label("UTC Second")
                        .description("UTC second when report was generated (0-59); 60 = not available = default")
                        .definition(SWEHelper.getPropertyUri("UtcSecond")))
                .addField("offPositionIndicator", sweFactory.createQuantity()
                        .label("Off Position Indicator")
                        .description("For floating aids-to-navigation: 0 = on position; 1 = off position. Only valid if UTC second is 0-59")
                        .definition(SWEHelper.getPropertyUri("OffPositionIndicator")))
                .addField("raim", sweFactory.createQuantity()
                        .label("RAIM Flag")
                        .description("Receiver autonomous integrity monitoring flag; 0 = RAIM not in use; 1 = RAIM in use")
                        .definition(SWEHelper.getPropertyUri("Raim")))
                .addField("virtualAid", sweFactory.createQuantity()
                        .label("Virtual Aid Flag")
                        .description("0 = default; 1 = virtual aid to navigation simulated by nearby AIS station")
                        .definition(SWEHelper.getPropertyUri("VirtualAid")))
                .addField("assignedMode", sweFactory.createQuantity()
                        .label("Assigned Mode Flag")
                        .description("0 = station operating in autonomous and continuous mode = default; 1 = station operating in assigned mode")
                        .definition(SWEHelper.getPropertyUri("AssignedMode")));

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    @Override
    public void setData(AisMessage21 report, String description) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setIntValue(1,  report.getMsgId());
            dataBlock.setStringValue(2, description);
            dataBlock.setIntValue(3,  report.getRepeat());
            dataBlock.setStringValue(4,  String.valueOf(report.getUserId()));
            dataBlock.setIntValue(5,  report.getAtonType());
            dataBlock.setStringValue(6, report.getName());
            dataBlock.setIntValue(7,  report.getPosAcc());
            dataBlock.setDoubleValue(8,  report.getPos().getLatitudeDouble());
            dataBlock.setDoubleValue(9,  report.getPos().getLongitudeDouble());
            dataBlock.setIntValue(10,  report.getDimBow());
            dataBlock.setIntValue(11, report.getDimStern());
            dataBlock.setIntValue(12, report.getDimPort());
            dataBlock.setIntValue(13, report.getDimStarboard());
            dataBlock.setIntValue(14, report.getPosType());
            dataBlock.setIntValue(15, report.getUtcSec());
            dataBlock.setIntValue(16, report.getOffPosition());
            dataBlock.setIntValue(17, report.getRaim());
            dataBlock.setIntValue(18, report.getVirtual());
            dataBlock.setIntValue(19, report.getAssigned());

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
