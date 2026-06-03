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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.checkerframework.checker.units.qual.N;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.sensor.nmeaais.NmeaAisDriver;
import org.sensorhub.impl.sensor.nmeaais.helpers.NmeaAisHelper;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Output for AIS Type 24 — Class B CS Static Data.
 *
 * Type 24 is transmitted in two separate sentences:
 *   Part A (partNumber = 0): MMSI + vessel name
 *   Part B (partNumber = 1): MMSI + callsign + ship type + dimensions + vendor ID
 *
 * The handler caches Part A names by MMSI and combines them with Part B on arrival.
 * A record is published when Part B is received; the name field will be empty if
 * the corresponding Part A has not yet been seen.
 *
 * Flat index map:
 *   0  = messageId       1  = reportDescription   2  = repeat
 *   3  = mmsi            4  = name                5  = callSign
 *   6  = shipType        7  = dimBow              8  = dimStern
 *   9  = dimPort         10 = dimStarboard        11 = vendorId
 */
public class NmeaAisOutputStaticDataClassB extends VarRateSensorOutput<NmeaAisDriver> {
    private DataRecord aisReportRecord;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "nmeaAisOutputStaticDataClassB";
    private static final String OUTPUT_LABEL       = "Class B Static Data";
    private static final String OUTPUT_DESCRIPTION = "AIS Class B CS Static Data Report (type 24) — vessel name, callsign, ship type, and dimensions";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("NmeaAisOutputStaticDataClassB");

    private final Object processingLock = new Object();

    public NmeaAisOutputStaticDataClassB(NmeaAisDriver nmeaAisDriver) {
        super(OUTPUT_NAME, nmeaAisDriver, 1.0);
    }

    public void doInit() {
        GeoPosHelper geoFac = new GeoPosHelper();
        SWEHelper sweFactory = new SWEHelper();
        NmeaAisHelper fac = new NmeaAisHelper();

        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(OUTPUT_NAME)
                .label(OUTPUT_LABEL)
                .description(OUTPUT_DESCRIPTION)
                .definition(OUTPUT_DEFINITION)
                .addField("messageId", fac.createNmeaMessageId())
                .addField("reportDescription", fac.createReportDescription())
                .addField("repeat", fac.createRepeatIndicator())
                .addField("mmsi", fac.createMssi())
                .addField("name", sweFactory.createText()
                        .label("Vessel Name")
                        .description("Vessel name from Part A (20 characters max); empty if Part A not yet received")
                        .definition(SWEHelper.getPropertyUri("VesselName")))
                .addField("callSign", sweFactory.createText()
                        .label("Call Sign")
                        .description("Call sign (7 x 6-bit ASCII characters, padded with spaces)")
                        .definition(SWEHelper.getPropertyUri("CallSign")))
                .addField("shipType", sweFactory.createQuantity()
                        .label("Ship Type")
                        .description("Ship type per ITU-R M.1371-5 Table 53; 0 = not available or no ship = default")
                        .definition(SWEHelper.getPropertyUri("ShipType")))
                .addField("dimBow", sweFactory.createQuantity()
                        .label("Dimension to Bow")
                        .description("Distance from GPS antenna to bow in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimBow")))
                .addField("dimStern", sweFactory.createQuantity()
                        .label("Dimension to Stern")
                        .description("Distance from GPS antenna to stern in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStern")))
                .addField("dimPort", sweFactory.createQuantity()
                        .label("Dimension to Port")
                        .description("Distance from GPS antenna to port side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimPort")))
                .addField("dimStarboard", sweFactory.createQuantity()
                        .label("Dimension to Starboard")
                        .description("Distance from GPS antenna to starboard side in metres; 0 = not available = default")
                        .uom("m")
                        .definition(SWEHelper.getPropertyUri("DimStarboard")))
                .addField("vendorId", sweFactory.createText()
                        .label("Vendor ID")
                        .description("Unique identification of the Unit by the manufacturer")
                        .definition(SWEHelper.getPropertyUri("VendorId")));

        aisReportRecord = recordBuilder.build();
        dataEncoding = geoFac.newTextEncoding(",", "\n");
    }

    /**
     * Publishes a combined Part A + Part B record.
     *
     * @param mmsi      MMSI from the Part B sentence
     * @param repeat    Repeat indicator from Part B
     * @param name      Vessel name from cached Part A; empty string if Part A not yet received
     * @param callSign  Call sign from Part B
     * @param shipType  Ship type from Part B
     * @param dimBow    Dimension to bow from Part B
     * @param dimStern  Dimension to stern from Part B
     * @param dimPort   Dimension to port from Part B
     * @param dimStarboard Dimension to starboard from Part B
     * @param vendorId  Vendor ID from Part B
     * @param description Message type description
     */
    public void setData(int mmsi, int repeat, String name, String callSign, int shipType,
                        int dimBow, int dimStern, int dimPort, int dimStarboard,
                        String vendorId, String description) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? aisReportRecord.createDataBlock() : latestRecord.renew();

            dataBlock.setIntValue(0,  24);
            dataBlock.setStringValue(1, description);
            dataBlock.setIntValue(2,  repeat);
            dataBlock.setStringValue(3,  String.valueOf(mmsi));
            dataBlock.setStringValue(4, name);
            dataBlock.setStringValue(5, callSign);
            dataBlock.setIntValue(6,  shipType);
            dataBlock.setIntValue(7,  dimBow);
            dataBlock.setIntValue(8,  dimStern);
            dataBlock.setIntValue(9,  dimPort);
            dataBlock.setIntValue(10, dimStarboard);
            dataBlock.setStringValue(11, vendorId);

            String foiUID = parentSensor.addFoi(String.valueOf(mmsi));

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, NmeaAisOutputStaticDataClassB.this, foiUID, dataBlock));
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
