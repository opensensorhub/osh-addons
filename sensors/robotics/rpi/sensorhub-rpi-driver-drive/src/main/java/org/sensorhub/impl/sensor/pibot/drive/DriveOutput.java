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
package org.sensorhub.impl.sensor.pibot.drive;

import org.sensorhub.impl.pibot.common.output.BaseSensorOutput;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.DataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.helper.GeoPosHelper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * DriveOutput specification and provider for PiBot DriveSensor Module
 *
 * @author Nick Garay
 * @since Feb. 15, 2021
 */
public class DriveOutput extends BaseSensorOutput<DriveSensor> implements Runnable {

    private static final String SENSOR_OUTPUT_NAME = "[NAME]";
    private static final String SENSOR_OUTPUT_LABEL = "[LABEL]";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "[DESCRIPTION]";

    private static final Logger logger = LoggerFactory.getLogger(DriveOutput.class);

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    DriveOutput(DriveSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    @Override
    protected void init() {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // TODO: Create data record description

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        workerThread = new Thread(this, this.name);

        logger.debug("Initializing Output Complete");
    }

    @Override
    protected void start() {

        doWork.set(true);

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

                // TODO: Populate data block

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, DriveOutput.this, dataBlock));
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
