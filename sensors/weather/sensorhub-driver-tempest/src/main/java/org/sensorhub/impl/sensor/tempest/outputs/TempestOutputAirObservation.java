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
 * Output for Tempest obs_air messages — Air module observation.
 *
 * Flat index map:
 *   0 = sampleTime              1 = stationPressure         2 = airTemperature
 *   3 = relativeHumidity        4 = lightningStrikeCount    5 = lightningStrikeAvgDistance
 *   6 = batteryVoltage          7 = reportInterval
 */
public class TempestOutputAirObservation extends VarRateSensorOutput<TempestDriver> {
    private DataRecord record;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "tempestOutputAirObservation";
    private static final String OUTPUT_LABEL       = "Air Observation";
    private static final String OUTPUT_DESCRIPTION = "Air observation from Tempest Air module (obs_air)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("TempestOutputAirObservation");

    private final Object processingLock = new Object();

    public TempestOutputAirObservation(TempestDriver parent) {
        super(OUTPUT_NAME, parent, 1.0);
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
                .addField("stationPressure", swe.createQuantity()
                        .label("Station Pressure")
                        .description("Raw barometric pressure at station elevation")
                        .uom("mbar")
                        .definition("http://sensorml.com/ont/isa/property/Atmospheric_Pressure"))
                .addField("airTemperature", swe.createQuantity()
                        .label("Air Temperature")
                        .description("Ambient air temperature")
                        .uom("Cel")
                        .definition("http://sensorml.com/ont/misb0601/property/Outside_Air_Temperature"))
                .addField("relativeHumidity", swe.createQuantity()
                        .label("Relative Humidity")
                        .description("Relative humidity")
                        .uom("%")
                        .definition(SWEHelper.getPropertyUri("RelativeHumidity")))
                .addField("lightningStrikeCount", swe.createCount()
                        .label("Lightning Strike Count")
                        .description("Number of lightning strikes over the last 3 hours")
                        .definition(SWEHelper.getPropertyUri("LightningCount")))
                .addField("lightningStrikeAvgDistance", swe.createQuantity()
                        .label("Lightning Strike Avg Distance")
                        .description("Average distance of lightning strikes over the last 3 hours")
                        .uom("km")
                        .definition(SWEHelper.getPropertyUri("LightningDistance")))
                .addField("batteryVoltage", swe.createQuantity()
                        .label("Battery Voltage")
                        .description("Device battery voltage")
                        .uom("V")
                        .definition(SWEHelper.getPropertyUri("BatteryVoltage")))
                .addField("reportInterval", swe.createCount()
                        .label("Report Interval")
                        .description("Reporting interval in minutes")
                        .definition(SWEHelper.getPropertyUri("SamplingInterval")));

        record = builder.build();
        dataEncoding = geo.newTextEncoding(",", "\n");
    }

    public void setData(JsonNode json) {
        synchronized (processingLock) {
            JsonNode obsArray = json.path("obs");
            if (!obsArray.isArray() || obsArray.isEmpty()) return;

            JsonNode obs = obsArray.get(0);
            if (!obs.isArray() || obs.size() < 8) return;

            long timestamp                = obs.get(0).asLong();
            double stationPressure        = obs.get(1).asDouble();
            double airTemperature         = obs.get(2).asDouble();
            double relativeHumidity       = obs.get(3).asDouble();
            int lightningStrikeCount      = obs.get(4).asInt();
            double lightningStrikeAvgDist = obs.get(5).asDouble();
            double batteryVoltage         = obs.get(6).asDouble();
            int reportInterval            = obs.get(7).asInt();

            String serialNumber = json.path("serial_number").asText();
            String foiUID = parentSensor.addFoi(serialNumber);

            DataBlock dataBlock = latestRecord == null ? record.createDataBlock() : latestRecord.renew();

            int i = 0;
            dataBlock.setDoubleValue(i++, timestamp);
            dataBlock.setDoubleValue(i++, stationPressure);
            dataBlock.setDoubleValue(i++, airTemperature);
            dataBlock.setDoubleValue(i++, relativeHumidity);
            dataBlock.setIntValue(i++,    lightningStrikeCount);
            dataBlock.setDoubleValue(i++, lightningStrikeAvgDist);
            dataBlock.setDoubleValue(i++, batteryVoltage);
            dataBlock.setIntValue(i,      reportInterval);

            latestRecord = dataBlock;
            latestRecordTime = timestamp * 1000L;
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, TempestOutputAirObservation.this, foiUID, dataBlock));
        }
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }
}
