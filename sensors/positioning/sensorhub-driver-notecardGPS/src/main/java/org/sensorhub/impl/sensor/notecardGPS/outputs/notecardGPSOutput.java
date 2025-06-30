/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.notecardGPS.outputs;

import org.sensorhub.impl.sensor.notecardGPS.notecardGPSSensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


/**
 * Output specification and provider for {@link notecardGPSSensor}.
 */
public class notecardGPSOutput extends AbstractSensorOutput<notecardGPSSensor> {
    static final String SENSOR_OUTPUT_NAME = "NotecardGPS";
    static final String SENSOR_OUTPUT_LABEL = "GPS";
    static final String SENSOR_OUTPUT_DESCRIPTION = "This is the output for the GPS module on the Blues Notecard";

    // myNote:
    // Added Variables because there were in other templates
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private long lastSetTimeMillis = System.currentTimeMillis();

    private static final Logger logger = LoggerFactory.getLogger(notecardGPSOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public notecardGPSOutput(notecardGPSSensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.info("Initializing Notecard GPS Output");
        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();
        GeoPosHelper geoFactory = new GeoPosHelper();


        // Create the data record description
        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("oshTimestamp", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("OSH Time Stamp")
                        .description("Timestamp of when OSH collected data"))
                .addField("status", sweFactory.createText()
                        .label("Status")
                        .description("Information of GPS Status"))
                .addField("mode", sweFactory.createText()
                        .label("Mode")
                        .description("Periodic or Continuous"))
                .addField("collectionTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("GPS Collection Time")
                        .description("Timestamp of when GPS data was actually taken"))
                .addField("Location", geoFactory.createLocationVectorLatLon());

        dataStruct = recordBuilder.build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        long accumulator = 0;
        synchronized (histogramLock) {
            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {
                accumulator += timingHistogram[idx];
            }
        }
        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    public void SetData(String status, String mode, long gpsTime, float lat, float lon) {
               DataBlock dataBlock;
        try {
            if (latestRecord == null) {
                dataBlock = dataStruct.createDataBlock();
            } else {
                dataBlock = latestRecord.renew();
            }
            synchronized (histogramLock) {
                int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;
                // Get a sampling time for latest set based on previous set sampling time
                timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;
                // Set latest sampling time to now
                lastSetTimeMillis = timingHistogram[setIndex];
            }
            ++setCount;

            // Convert Unix Epoch time provided by GPS to OffsetDateTime
            Instant gpsTimeInstant = Instant.ofEpochSecond(gpsTime);
            OffsetDateTime odt = gpsTimeInstant.atOffset(ZoneOffset.UTC);

            // Add helpful message to status
            status = (status.equals("GPS inactive {gps-inactive}")) ? status + "{If this is first time starting, may take several minutes for GPS to initialize}" : status;


            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setStringValue(1, status);
            dataBlock.setStringValue(2, mode);
            dataBlock.setDateTime(3, odt);
            dataBlock.setFloatValue(4, lat);
            dataBlock.setFloatValue(5, lon);


            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, notecardGPSOutput.this, dataBlock));

        } catch (Exception e) {
            System.err.println("Error reading from GPS: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
