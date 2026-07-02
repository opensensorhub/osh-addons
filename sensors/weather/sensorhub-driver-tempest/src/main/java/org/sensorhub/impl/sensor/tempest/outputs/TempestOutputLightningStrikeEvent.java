/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.tempest.outputs;

import com.fasterxml.jackson.databind.JsonNode;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.sensorhub.impl.sensor.tempest.TempestDriver;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

/**
 * Output for Tempest evt_strike messages — lightning strike events.
 *
 * Flat index map:
 *   0 = sampleTime   1 = distance   2 = energy
 */
public class TempestOutputLightningStrikeEvent extends VarRateSensorOutput<TempestDriver> {
    private DataRecord record;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "tempestOutputLightningStrikeEvent";
    private static final String OUTPUT_LABEL       = "Lightning Strike Event";
    private static final String OUTPUT_DESCRIPTION = "Lightning strike event from Tempest Weather Station (evt_strike)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("TempestOutputLightningStrikeEvent");

    private final Object processingLock = new Object();

    public TempestOutputLightningStrikeEvent(TempestDriver tempestDriver) {
        super(OUTPUT_NAME, tempestDriver, 1.);
    }

    public void doInit() {
        SWEHelper swe = new SWEHelper();
        GeoPosHelper geo = new GeoPosHelper();

        SWEBuilders.DataRecordBuilder builder = swe.createRecord()
                .name(OUTPUT_NAME)
                .label(OUTPUT_LABEL)
                .description(OUTPUT_DESCRIPTION)
                .definition(OUTPUT_DEFINITION)
                .addField("sampleTime", swe.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("UTC epoch timestamp of the lightning strike")
                        .definition(SWEHelper.getPropertyUri("SampleTime")))
                .addField("distance", swe.createQuantity()
                        .label("Distance")
                        .description("Estimated distance to the lightning strike")
                        .uom("km")
                        .definition(SWEHelper.getPropertyUri("LightningDistance")))
                .addField("energy", swe.createQuantity()
                        .label("Energy")
                        .description("Relative energy of the lightning strike (dimensionless)")
                        .uom("1")
                        .definition(SWEHelper.getPropertyUri("LightningEnergy")));

        record = builder.build();
        dataEncoding = geo.newTextEncoding(",", "\n");
    }

    public void setData(JsonNode json) {
        synchronized (processingLock) {
            JsonNode evt = json.path("evt");
            if (!evt.isArray() || evt.size() < 3) return;

            long timestamp   = evt.get(0).asLong();
            double distance  = evt.get(1).asDouble();
            double energy    = evt.get(2).asDouble();

            String serialNumber = json.path("serial_number").asText();
            String foiUID = parentSensor.addFoi(serialNumber);

            DataBlock dataBlock = latestRecord == null ? record.createDataBlock() : latestRecord.renew();

            int i = 0;
            dataBlock.setDoubleValue(i++, timestamp);
            dataBlock.setDoubleValue(i++, distance);
            dataBlock.setDoubleValue(i,   energy);

            latestRecord = dataBlock;
            latestRecordTime = timestamp * 1000L;
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, TempestOutputLightningStrikeEvent.this, foiUID, dataBlock));
        }
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }
}
