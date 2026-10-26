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
 * Output for Tempest rapid_wind messages — high-frequency wind observations (every 3 seconds).
 *
 * Flat index map:
 *   0 = sampleTime   1 = windSpeed   2 = windDirection
 *
 * Note: rapid_wind uses a top-level "ob" array, not the nested "obs" array used by obs_st/obs_sky.
 */
public class TempestOutputRapidWind extends VarRateSensorOutput<TempestDriver> {
    private DataRecord record;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "tempestOutputRapidWind";
    private static final String OUTPUT_LABEL       = "Rapid Wind";
    private static final String OUTPUT_DESCRIPTION = "High-frequency wind observation from Tempest Weather Station (rapid_wind)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("TempestOutputRapidWind");

    private final Object processingLock = new Object();

    public TempestOutputRapidWind(TempestDriver tempestDriver) {
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
                        .description("UTC epoch timestamp of the observation")
                        .definition(SWEHelper.getPropertyUri("SampleTime")))
                .addField("windSpeed", swe.createQuantity()
                        .label("Wind Speed")
                        .description("Instantaneous wind speed")
                        .uom("m/s")
                        .definition(SWEHelper.getPropertyUri("WindSpeed")))
                .addField("windDirection", swe.createQuantity()
                        .label("Wind Direction")
                        .description("Instantaneous wind direction in degrees (0–360)")
                        .uom("deg")
                        .definition(SWEHelper.getPropertyUri("WindDirectionAngle")));

        record = builder.build();
        dataEncoding = geo.newTextEncoding(",", "\n");
    }

    public void setData(JsonNode json) {
        synchronized (processingLock) {
            // rapid_wind uses "ob" (singular), not "obs"
            JsonNode ob = json.path("ob");
            if (!ob.isArray() || ob.size() < 3) return;

            long timestamp     = ob.get(0).asLong();
            double windSpeed   = ob.get(1).asDouble();
            double windDir     = ob.get(2).asDouble();

            String serialNumber = json.path("serial_number").asText();
            String foiUID = parentSensor.addFoi(serialNumber);

            DataBlock dataBlock = latestRecord == null ? record.createDataBlock() : latestRecord.renew();

            int i = 0;
            dataBlock.setDoubleValue(i++, timestamp);
            dataBlock.setDoubleValue(i++, windSpeed);
            dataBlock.setDoubleValue(i,   windDir);

            latestRecord = dataBlock;
            latestRecordTime = timestamp * 1000L;
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, TempestOutputRapidWind.this, foiUID, dataBlock));
        }
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }
}
