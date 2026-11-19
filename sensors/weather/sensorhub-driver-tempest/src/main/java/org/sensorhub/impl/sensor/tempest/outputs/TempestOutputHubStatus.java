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
 * Output for Tempest hub_status messages — periodic hub health report.
 *
 * All values are top-level JSON fields (no obs array).
 * FOI is keyed by the hub serial number (e.g. HB-00000000).
 *
 * Flat index map:
 *   0 = sampleTime   1 = serialNumber   2 = firmwareRevision
 *   3 = uptime       4 = rssi           5 = resetFlags
 *   6 = seq
 */
public class TempestOutputHubStatus extends VarRateSensorOutput<TempestDriver> {
    private DataRecord record;
    private DataEncoding dataEncoding;

    private static final String OUTPUT_NAME        = "tempestOutputHubStatus";
    private static final String OUTPUT_LABEL       = "Hub Status";
    private static final String OUTPUT_DESCRIPTION = "Hub health status from Tempest hub device (hub_status)";
    private static final String OUTPUT_DEFINITION  = SWEHelper.getPropertyUri("TempestOutputHubStatus");

    private final Object processingLock = new Object();

    public TempestOutputHubStatus(TempestDriver tempestDriver) {
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
                        .description("UTC epoch timestamp of the hub status report")
                        .definition(SWEHelper.getPropertyUri("SampleTime")))
                .addField("serialNumber", swe.createText()
                        .label("Serial Number")
                        .description("Hub serial number (e.g. HB-00000000)")
                        .definition(SWEHelper.getPropertyUri("SerialNumber")))
                .addField("firmwareRevision", swe.createCount()
                        .label("Firmware Revision")
                        .description("Hub firmware revision number")
                        .definition(SWEHelper.getPropertyUri("FirmwareVersion")))
                .addField("uptime", swe.createCount()
                        .label("Uptime")
                        .description("Hub uptime in seconds since last restart")
                        .definition(SWEHelper.getPropertyUri("Uptime")))
                .addField("rssi", swe.createQuantity()
                        .label("RSSI")
                        .description("Hub Wi-Fi signal strength indicator")
                        .uom("dB")
                        .definition(SWEHelper.getPropertyUri("RSSI")))
                .addField("resetFlags", swe.createText()
                        .label("Reset Flags")
                        .description("Comma-separated list of reset reason flags since last boot")
                        .definition(SWEHelper.getPropertyUri("ResetFlags")))
                .addField("seq", swe.createCount()
                        .label("Sequence")
                        .description("Hub status message sequence number")
                        .definition(SWEHelper.getPropertyUri("SequenceNumber")));

        record = builder.build();
        dataEncoding = geo.newTextEncoding(",", "\n");
    }

    public void setData(JsonNode json) {
        synchronized (processingLock) {
            long timestamp       = json.path("timestamp").asLong();
            String serialNumber  = json.path("serial_number").asText();
            int firmwareRevision = json.path("firmware_revision").asInt();
            int uptime           = json.path("uptime").asInt();
            double rssi          = json.path("rssi").asDouble();
            String resetFlags    = json.path("reset_flags").asText();
            int seq              = json.path("seq").asInt();

            String foiUID = parentSensor.addFoi(serialNumber);

            DataBlock dataBlock = latestRecord == null ? record.createDataBlock() : latestRecord.renew();

            int i = 0;
            dataBlock.setDoubleValue(i++, timestamp);
            dataBlock.setStringValue(i++, serialNumber);
            dataBlock.setIntValue(i++,    firmwareRevision);
            dataBlock.setIntValue(i++,    uptime);
            dataBlock.setDoubleValue(i++, rssi);
            dataBlock.setStringValue(i++, resetFlags);
            dataBlock.setIntValue(i,      seq);

            latestRecord = dataBlock;
            latestRecordTime = timestamp * 1000L;
            updateSamplingPeriod(latestRecordTime);
            eventHandler.publish(new DataEvent(latestRecordTime, TempestOutputHubStatus.this, foiUID, dataBlock));
        }
    }

    @Override
    public DataComponent getRecordDescription() { return record; }

    @Override
    public DataEncoding getRecommendedEncoding() { return dataEncoding; }
}
