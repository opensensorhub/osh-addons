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
 * Output for Tempest obs_st messages — the primary unified weather observation.
 *
 * Flat index map:
 *   0  = sampleTime           1  = windLull              2  = windAvg
 *   3  = windGust             4  = windDirection         5  = windSampleInterval
 *   6  = stationPressure      7  = airTemperature        8  = relativeHumidity
 *   9  = illuminance          10 = uvIndex               11 = solarRadiation
 *   12 = rainAccumulated      13 = precipitationType     14 = lightningStrikeAvgDistance
 *   15 = lightningStrikeCount 16 = batteryVoltage        17 = reportInterval
 */
public class TempestOutputObservation extends VarRateSensorOutput<TempestDriver> {
    private DataRecord record;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "tempestOutputObservation";
    private static final String OUTPUT_LABEL       = "Tempest Observation";
    private static final String OUTPUT_DESCRIPTION = "Unified weather observation from the Tempest Weather Station (obs_st)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("TempestOutputObservation");

    private final Object processingLock = new Object();

    public TempestOutputObservation(TempestDriver tempestDriver) {
        super(OUTPUT_NAME, tempestDriver, 1.0);
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
                .addField("windLull", swe.createQuantity()
                        .label("Wind Lull")
                        .description("Minimum 3-second wind speed over the report interval")
                        .uom("m/s")
                        .definition(SWEHelper.getPropertyUri("WindSpeed")))
                .addField("windAvg", swe.createQuantity()
                        .label("Wind Avg")
                        .description("Average wind speed over the report interval")
                        .uom("m/s")
                        .definition(SWEHelper.getPropertyUri("WindSpeed")))
                .addField("windGust", swe.createQuantity()
                        .label("Wind Gust")
                        .description("Maximum 3-second wind speed over the report interval")
                        .uom("m/s")
                        .definition(SWEHelper.getPropertyUri("WindSpeed")))
                .addField("windDirection", swe.createQuantity()
                        .label("Wind Direction")
                        .description("Wind direction in degrees (0–360)")
                        .uom("deg")
                        .definition(SWEHelper.getPropertyUri("WindDirectionAngle")))
                .addField("windSampleInterval", swe.createCount()
                        .label("Wind Sample Interval")
                        .description("Number of seconds of wind samples used to generate the avg/lull/gust values")
                        .definition(SWEHelper.getPropertyUri("SamplingInterval")))
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
                .addField("illuminance", swe.createQuantity()
                        .label("Illuminance")
                        .description("Brightness in lux")
                        .uom("lx")
                        .definition(SWEHelper.getPropertyUri("Illuminance")))
                .addField("uvIndex", swe.createQuantity()
                        .label("UV Index")
                        .description("UV index (0–11+)")
                        .uom("1")
                        .definition(SWEHelper.getPropertyUri("UVIndex")))
                .addField("solarRadiation", swe.createQuantity()
                        .label("Solar Radiation")
                        .description("Solar radiation in watts per square metre")
                        .uom("W/m2")
                        .definition(SWEHelper.getPropertyUri("SolarRadiation")))
                .addField("rainAccumulated", swe.createQuantity()
                        .label("Rain Accumulated")
                        .description("Rain accumulated over the local day")
                        .uom("mm")
                        .definition(SWEHelper.getPropertyUri("RainfallAccumulation")))
                .addField("precipitationType", swe.createText()
                        .label("Precipitation Type")
                        .description("Type of precipitation: None, Rain, Hail, or Rain + Hail")
                        .definition(SWEHelper.getPropertyUri("PrecipitationType")))
                .addField("lightningStrikeAvgDistance", swe.createQuantity()
                        .label("Lightning Strike Avg Distance")
                        .description("Average distance of lightning strikes over the last 3 hours")
                        .uom("km")
                        .definition(SWEHelper.getPropertyUri("LightningDistance")))
                .addField("lightningStrikeCount", swe.createCount()
                        .label("Lightning Strike Count")
                        .description("Number of lightning strikes over the last 3 hours")
                        .definition(SWEHelper.getPropertyUri("LightningCount")))
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
            if (!obs.isArray() || obs.size() < 18) return;

            long timestamp              = obs.get(0).asLong();
            double windLull             = obs.get(1).asDouble();
            double windAvg              = obs.get(2).asDouble();
            double windGust             = obs.get(3).asDouble();
            double windDirection        = obs.get(4).asDouble();
            int windSampleInterval      = obs.get(5).asInt();
            double stationPressure      = obs.get(6).asDouble();
            double airTemperature       = obs.get(7).asDouble();
            double relativeHumidity     = obs.get(8).asDouble();
            double illuminance          = obs.get(9).asDouble();
            double uvIndex              = obs.get(10).asDouble();
            double solarRadiation       = obs.get(11).asDouble();
            double rainAccumulated      = obs.get(12).asDouble();
            String precipType           = decodePrecipType(obs.get(13).asInt());
            double lightningAvgDist     = obs.get(14).asDouble();
            int lightningCount          = obs.get(15).asInt();
            double batteryVoltage       = obs.get(16).asDouble();
            int reportInterval          = obs.get(17).asInt();

            String serialNumber = json.path("serial_number").asText();
            String foiUID = parentSensor.addFoi(serialNumber);

            DataBlock dataBlock = latestRecord == null ? record.createDataBlock() : latestRecord.renew();

            int i = 0;
            dataBlock.setDoubleValue(i++, timestamp);
            dataBlock.setDoubleValue(i++, windLull);
            dataBlock.setDoubleValue(i++, windAvg);
            dataBlock.setDoubleValue(i++, windGust);
            dataBlock.setDoubleValue(i++, windDirection);
            dataBlock.setIntValue(i++,    windSampleInterval);
            dataBlock.setDoubleValue(i++, stationPressure);
            dataBlock.setDoubleValue(i++, airTemperature);
            dataBlock.setDoubleValue(i++, relativeHumidity);
            dataBlock.setDoubleValue(i++, illuminance);
            dataBlock.setDoubleValue(i++, uvIndex);
            dataBlock.setDoubleValue(i++, solarRadiation);
            dataBlock.setDoubleValue(i++, rainAccumulated);
            dataBlock.setStringValue(i++, precipType);
            dataBlock.setDoubleValue(i++, lightningAvgDist);
            dataBlock.setIntValue(i++,    lightningCount);
            dataBlock.setDoubleValue(i++, batteryVoltage);
            dataBlock.setIntValue(i,      reportInterval);

            latestRecord = dataBlock;
            latestRecordTime = timestamp * 1000L;
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, TempestOutputObservation.this, foiUID, dataBlock));
        }
    }

    private static String decodePrecipType(int code) {
        return switch (code) {
            case 0 -> "None";
            case 1 -> "Rain";
            case 2 -> "Hail";
            case 3 -> "Rain + Hail";
            default -> "Unknown (" + code + ")";
        };
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }
}
