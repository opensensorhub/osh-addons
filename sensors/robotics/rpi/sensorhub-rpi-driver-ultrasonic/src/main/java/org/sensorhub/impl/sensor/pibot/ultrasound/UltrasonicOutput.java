/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.pibot.ultrasound;

import org.sensorhub.impl.pibot.common.output.BaseSensorOutput;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * UltrasonicOutput specification and provider for PiBot UltrasonicSensor Module
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class UltrasonicOutput extends BaseSensorOutput<UltrasonicSensor> {

    private static final String SENSOR_OUTPUT_NAME = "UltrasonicSensorOutput";
    private static final String SENSOR_OUTPUT_LABEL = "UltrasonicSensor";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Distance measured via ultrasonic sensor";

    private static final Logger logger = LoggerFactory.getLogger(UltrasonicOutput.class);

    /**
     * Constructor
     *
     * @param parentUltrasonicSensor UltrasonicSensor driver providing this output
     */
    UltrasonicOutput(UltrasonicSensor parentUltrasonicSensor) {

        super(SENSOR_OUTPUT_NAME, parentUltrasonicSensor);

        logger.debug("UltrasonicOutput created");
    }

    @Override
    protected void init() {

        logger.debug("Initializing UltrasonicOutput");

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();
        dataStruct = sweFactory.createRecord()
                .name(getName())
                .updatable(true)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_LABEL))
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("SampleTime",
                        sweFactory.createTime()
                                .asSamplingTimeIsoUTC()
                                .build())
                .addField("Range",
                        sweFactory.createQuantity()
                                .name("Distance")
                                .label("Distance")
                                .uomCode("cm")
                                .definition(SWEHelper.getPropertyUri("Distance"))
                                .description("The distance of objects detected")
                                .value(Double.NaN)
                                .build())
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initializing UltrasonicOutput Complete");
    }

    @Override
    public void start() {

        doWork.set(true);

        workerThread = new Thread(this, this.name);

        logger.info("Starting worker thread: {}", workerThread.getName());

        workerThread.start();
    }

    @Override
    protected void stop() {

        doWork.set(false);
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return workerThread.isAlive();
    }

    @Override
    public void run() {

        try {

            while (doWork.get()) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {

                    int setIndex = dataFrameCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastDataFrameTimeMillis;

                    // Set latest sampling time to now
                    lastDataFrameTimeMillis = timingHistogram[setIndex];
                }

                ++dataFrameCount;

                double sampleTime = System.currentTimeMillis() / 1000.0;

                dataBlock.setDoubleValue(0, sampleTime);

                dataBlock.setDoubleValue(1, parentSensor.getDetectedRange());

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, UltrasonicOutput.this, dataBlock));
            }

        } catch (Exception e) {

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter);

        } finally {

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
