/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.BNO085.outputs;

import org.sensorhub.impl.sensor.BNO085.Bno085Sensor;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;


/**
 * Output specification and provider for {@link Bno085Sensor}.
 */
public class GyroCalibratedOutput extends AbstractSensorOutput<Bno085Sensor> {
    static final String SENSOR_OUTPUT_NAME = "Gyroscope Calibrated";
    static final String SENSOR_OUTPUT_LABEL = "Gyroscope Calibrated";
    static final String SENSOR_OUTPUT_DESCRIPTION = "This is the output for the rotational velocity Data in rad/s";

    // myNote:
    // Added Variables because there were in other templates
    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private long lastSetTimeMillis = System.currentTimeMillis();

    private static final Logger logger = LoggerFactory.getLogger(GyroCalibratedOutput.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public GyroCalibratedOutput(Bno085Sensor parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.info("Initializing Calibrated Gyroscope Output");
        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // Create the data record description
        SWEBuilders.DataRecordBuilder recordBuilder = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("timestamp", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Time Stamp")
                        .description("Time of data collection"))
                .addField("gyroscope_calibrated_X", sweFactory.createQuantity()
                        .uom("rad/s")
                        .label("X")
                        .description("X-axis gyro")
                        .definition(SWEHelper.getPropertyUri("gyroscope_calibrated_x")))
                .addField("gyroscope_calibrated_Y", sweFactory.createQuantity()
                        .uom("rad/s")
                        .label("Y")
                        .description("Y-axis gyro")
                        .definition(SWEHelper.getPropertyUri("gyroscope_calibrated_y")))
                .addField("gyroscope_calibrated_Z", sweFactory.createQuantity()
                        .uom("rad/s")
                        .label("Z")
                        .description("Z-axis gyro")
                        .definition(SWEHelper.getPropertyUri("gyroscope_calibrated_z")));

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

    public void SetData(float x, float y, float z) {
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

            dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);
            dataBlock.setDoubleValue(1, x);
            dataBlock.setDoubleValue(2, y);
            dataBlock.setDoubleValue(3, z);

            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, GyroCalibratedOutput.this, dataBlock));

        } catch (Exception e) {
            System.err.println("Error reading from BNO085: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
