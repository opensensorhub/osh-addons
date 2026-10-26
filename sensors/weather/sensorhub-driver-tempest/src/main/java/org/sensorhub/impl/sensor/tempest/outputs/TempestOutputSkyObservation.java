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
 * Output for Tempest obs_sky messages — sky and wind observation from Sky devices.
 *
 * Flat index map:
 *   0  = sampleTime                1  = illuminance             2  = uvIndex
 *   3  = rainAccumulated           4  = windLull                5  = windAvg
 *   6  = windGust                  7  = windDirection           8  = batteryVoltage
 *   9  = reportInterval            10 = solarRadiation          11 = localDayRainAccumulation
 *   12 = precipitationType         13 = windSampleInterval
 */
public class TempestOutputSkyObservation extends VarRateSensorOutput<TempestDriver> {
    private DataRecord record;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "tempestOutputSkyObservation";
    private static final String OUTPUT_LABEL       = "Sky Observation";
    private static final String OUTPUT_DESCRIPTION = "Sky and wind observation from Tempest Sky device (obs_sky)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("TempestOutputSkyObservation");

    private final Object processingLock = new Object();

    public TempestOutputSkyObservation(TempestDriver tempestDriver) {
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
                .addField("rainAccumulated", swe.createQuantity()
                        .label("Rain Accumulated")
                        .description("Rain accumulated over the report interval")
                        .uom("mm")
                        .definition(SWEHelper.getPropertyUri("RainfallAccumulation")))
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
                .addField("batteryVoltage", swe.createQuantity()
                        .label("Battery Voltage")
                        .description("Device battery voltage")
                        .uom("V")
                        .definition(SWEHelper.getPropertyUri("BatteryVoltage")))
                .addField("reportInterval", swe.createCount()
                        .label("Report Interval")
                        .description("Reporting interval in minutes")
                        .definition(SWEHelper.getPropertyUri("SamplingInterval")))
                .addField("solarRadiation", swe.createQuantity()
                        .label("Solar Radiation")
                        .description("Solar radiation in watts per square metre")
                        .uom("W/m2")
                        .definition(SWEHelper.getPropertyUri("SolarRadiation")))
                .addField("localDayRainAccumulation", swe.createQuantity()
                        .label("Local Day Rain Accumulation")
                        .description("Rain accumulated over the local day")
                        .uom("mm")
                        .definition(SWEHelper.getPropertyUri("RainfallAccumulation")))
                .addField("precipitationType", swe.createText()
                        .label("Precipitation Type")
                        .description("Type of precipitation: None, Rain, or Hail")
                        .definition(SWEHelper.getPropertyUri("PrecipitationType")))
                .addField("windSampleInterval", swe.createCount()
                        .label("Wind Sample Interval")
                        .description("Number of seconds of wind samples used to generate the avg/lull/gust values")
                        .definition(SWEHelper.getPropertyUri("SamplingInterval")));

        record = builder.build();
        dataEncoding = geo.newTextEncoding(",", "\n");
    }

    public void setData(JsonNode json) {
        synchronized (processingLock) {
            JsonNode obsArray = json.path("obs");
            if (!obsArray.isArray() || obsArray.isEmpty()) return;
            JsonNode obs = obsArray.get(0);
            if (!obs.isArray() || obs.size() < 14) return;

            long timestamp                  = obs.get(0).asLong();
            double illuminance              = obs.get(1).asDouble();
            double uvIndex                  = obs.get(2).asDouble();
            double rainAccumulated          = obs.get(3).asDouble();
            double windLull                 = obs.get(4).asDouble();
            double windAvg                  = obs.get(5).asDouble();
            double windGust                 = obs.get(6).asDouble();
            double windDirection            = obs.get(7).asDouble();
            double batteryVoltage           = obs.get(8).asDouble();
            int reportInterval              = obs.get(9).asInt();
            double solarRadiation           = obs.get(10).asDouble();
            double localDayRainAccumulation = obs.get(11).asDouble();
            String precipType               = decodePrecipType(obs.get(12).asInt());
            int windSampleInterval          = obs.get(13).asInt();

            String serialNumber = json.path("serial_number").asText();
            String foiUID = parentSensor.addFoi(serialNumber);

            DataBlock dataBlock = latestRecord == null ? record.createDataBlock() : latestRecord.renew();

            int i = 0;
            dataBlock.setDoubleValue(i++, timestamp);
            dataBlock.setDoubleValue(i++, illuminance);
            dataBlock.setDoubleValue(i++, uvIndex);
            dataBlock.setDoubleValue(i++, rainAccumulated);
            dataBlock.setDoubleValue(i++, windLull);
            dataBlock.setDoubleValue(i++, windAvg);
            dataBlock.setDoubleValue(i++, windGust);
            dataBlock.setDoubleValue(i++, windDirection);
            dataBlock.setDoubleValue(i++, batteryVoltage);
            dataBlock.setIntValue(i++,    reportInterval);
            dataBlock.setDoubleValue(i++, solarRadiation);
            dataBlock.setDoubleValue(i++, localDayRainAccumulation);
            dataBlock.setStringValue(i++, precipType);
            dataBlock.setIntValue(i,      windSampleInterval);

            latestRecord = dataBlock;
            latestRecordTime = timestamp * 1000L;
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, TempestOutputSkyObservation.this, foiUID, dataBlock));
        }
    }

    private static String decodePrecipType(int code) {
        return switch (code) {
            case 0 -> "None";
            case 1 -> "Rain";
            case 2 -> "Hail";
            default -> "Unknown (" + code + ")";
        };
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }
}
