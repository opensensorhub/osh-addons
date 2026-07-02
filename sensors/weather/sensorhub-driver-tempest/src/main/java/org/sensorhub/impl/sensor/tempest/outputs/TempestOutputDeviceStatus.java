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
 * Output for Tempest device_status messages — periodic device health report.
 *
 * All values are top-level JSON fields (no obs array).
 *
 * Flat index map:
 *   0  = sampleTime         1  = serialNumber      2  = hubSerialNumber
 *   3  = uptime             4  = voltage            5  = firmwareRevision
 *   6  = rssi               7  = hubRssi            8  = sensorStatus
 *   9  = debug
 */
public class TempestOutputDeviceStatus extends VarRateSensorOutput<TempestDriver> {
    private DataRecord record;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "tempestOutputDeviceStatus";
    private static final String OUTPUT_LABEL       = "Device Status";
    private static final String OUTPUT_DESCRIPTION = "Device health status from Tempest Weather Station (device_status)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("TempestOutputDeviceStatus");

    private final Object processingLock = new Object();

    public TempestOutputDeviceStatus(TempestDriver tempestDriver) {
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
                        .description("UTC epoch timestamp of the status report")
                        .definition(SWEHelper.getPropertyUri("SampleTime")))
                .addField("serialNumber", swe.createText()
                        .label("Serial Number")
                        .description("Device serial number (e.g. ST-00000000)")
                        .definition(SWEHelper.getPropertyUri("SerialNumber")))
                .addField("hubSerialNumber", swe.createText()
                        .label("Hub Serial Number")
                        .description("Hub serial number (e.g. HB-00000000)")
                        .definition(SWEHelper.getPropertyUri("SerialNumber")))
                .addField("uptime", swe.createCount()
                        .label("Uptime")
                        .description("Device uptime in seconds since last restart")
                        .definition(SWEHelper.getPropertyUri("Uptime")))
                .addField("voltage", swe.createQuantity()
                        .label("Voltage")
                        .description("Device battery or supply voltage")
                        .uom("V")
                        .definition(SWEHelper.getPropertyUri("BatteryVoltage")))
                .addField("firmwareRevision", swe.createCount()
                        .label("Firmware Revision")
                        .description("Device firmware revision number")
                        .definition(SWEHelper.getPropertyUri("FirmwareVersion")))
                .addField("rssi", swe.createQuantity()
                        .label("RSSI")
                        .description("Device radio signal strength indicator")
                        .uom("dB")
                        .definition(SWEHelper.getPropertyUri("RSSI")))
                .addField("hubRssi", swe.createQuantity()
                        .label("Hub RSSI")
                        .description("Hub radio signal strength indicator as seen by the device")
                        .uom("dB")
                        .definition(SWEHelper.getPropertyUri("RSSI")))
                .addField("sensorStatus", swe.createCount()
                        .label("Sensor Status")
                        .description("Bitmask of individual sensor health flags")
                        .definition(SWEHelper.getPropertyUri("SensorStatus")))
                .addField("debug", swe.createCount()
                        .label("Debug")
                        .description("Debug mode flag (0 = off, 1 = on)")
                        .definition(SWEHelper.getPropertyUri("DebugMode")));

        record = builder.build();
        dataEncoding = geo.newTextEncoding(",", "\n");
    }

    public void setData(JsonNode json) {
        synchronized (processingLock) {
            long timestamp        = json.path("timestamp").asLong();
            String serialNumber   = json.path("serial_number").asText();
            String hubSn          = json.path("hub_sn").asText();
            int uptime            = json.path("uptime").asInt();
            double voltage        = json.path("voltage").asDouble();
            int firmwareRevision  = json.path("firmware_revision").asInt();
            double rssi           = json.path("rssi").asDouble();
            double hubRssi        = json.path("hub_rssi").asDouble();
            int sensorStatus      = json.path("sensor_status").asInt();
            int debug             = json.path("debug").asInt();

            String foiUID = parentSensor.addFoi(serialNumber);

            DataBlock dataBlock = latestRecord == null ? record.createDataBlock() : latestRecord.renew();

            int i = 0;
            dataBlock.setDoubleValue(i++, timestamp);
            dataBlock.setStringValue(i++, serialNumber);
            dataBlock.setStringValue(i++, hubSn);
            dataBlock.setIntValue(i++,    uptime);
            dataBlock.setDoubleValue(i++, voltage);
            dataBlock.setIntValue(i++,    firmwareRevision);
            dataBlock.setDoubleValue(i++, rssi);
            dataBlock.setDoubleValue(i++, hubRssi);
            dataBlock.setIntValue(i++,    sensorStatus);
            dataBlock.setIntValue(i,      debug);

            latestRecord = dataBlock;
            latestRecordTime = timestamp * 1000L;
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, TempestOutputDeviceStatus.this, foiUID, dataBlock));
        }
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }
}
