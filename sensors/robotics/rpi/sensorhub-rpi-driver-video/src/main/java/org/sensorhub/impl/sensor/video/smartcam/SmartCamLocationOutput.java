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
package org.sensorhub.impl.sensor.video.smartcam;

import org.sensorhub.impl.pibot.common.config.SensorPlacement;
import org.sensorhub.impl.pibot.common.output.BaseSensorOutput;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;

/**
 * Basic Location Output for SmartCamSensor
 *
 * @author Nick Garay
 * @since 1.0.0
 */
public class SmartCamLocationOutput extends BaseSensorOutput<SmartCamSensor> {

    private static final String SENSOR_OUTPUT_NAME = "SmartCamLocationOutput";

    private static final String SENSOR_OUTPUT_LABEL = "Smart Cam Location";

    private static final String SENSOR_OUTPUT_DESCRIPTION = "Logical Placement of Smart Cam";

    private final Logger logger = LoggerFactory.getLogger(SmartCamLocationOutput.class);

    private static final long SLEEP_TIME = 1000;

    public SmartCamLocationOutput(SmartCamSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        workerThread = new Thread(this,
                this.getClass().getSimpleName() + "-Worker-" +
                        parentSensor.getConfiguration().serialNumber);

        logger.debug("{} worker thread created...", workerThread.getName());
    }

    @Override
    protected void init() throws SensorException {

        logger.debug("Initializing");

        lastDataFrameTimeMillis = System.currentTimeMillis();

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        Text location = sweFactory.createText()
                .name(getName())
                .label("Location")
                .description("Location of sensor placement")
                .definition(SWEConstants.DEF_SENSOR_LOC)
                .build();

        Time timeStamp = sweFactory.createTime().asSamplingTimeIsoUTC()
                .name("SampleTime")
                .build();

        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .definition(SWEHelper.getPropertyUri(SENSOR_OUTPUT_NAME))
                .addField(timeStamp.getName(), timeStamp)
                .addField(location.getName(), location)
                .build();

        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);

        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        logger.debug("Initialized");
    }

    @Override
    protected void start() throws SensorException {

        logger.debug("Starting");

        doWork.set(true);

        workerThread.start();

        logger.debug("Started");
    }

    @Override
    protected void stop() {

        logger.debug("Stopping");

        try {

            doWork.set(false);

            workerThread.join();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.error("Failed to stop {} thread due to exception {}", workerThread.getName(), e.getMessage());
        }

        logger.debug("Stopped");
    }

    @Override
    public void run() {

        logger.debug("Starting worker {}", Thread.currentThread().getName());

        while(doWork.get()) {

            // Update the timing histogram, used to compute average sampling period
            synchronized (histogramLock) {

                int dataFrameIndex = dataFrameCount % MAX_NUM_TIMING_SAMPLES;

                // Get a sampling time for latest set based on previous set sampling time
                timingHistogram[dataFrameIndex] = System.currentTimeMillis() - lastDataFrameTimeMillis;

                // Set latest sampling time to now
                lastDataFrameTimeMillis = timingHistogram[dataFrameIndex];
            }

            DataBlock dataBlock;

            if (latestRecord == null) {

                dataBlock = dataStruct.createDataBlock();

            } else {

                dataBlock = latestRecord.renew();
            }

            double sampleTime = System.currentTimeMillis() / 1000.0;

            dataBlock.setDoubleValue(0, sampleTime);

            if (parentSensor.getConfiguration().sensorPlacement.location == SensorPlacement.Locations.OTHER) {

                dataBlock.setStringValue(1, parentSensor.getConfiguration().sensorPlacement.customLocation);

            } else {

                dataBlock.setStringValue(1, parentSensor.getConfiguration().sensorPlacement.location.name());
            }

            latestRecord = dataBlock;

            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, SmartCamLocationOutput.this, dataBlock));

            try {

                Thread.sleep(SLEEP_TIME);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                logger.error("{} sleep interrupted", Thread.currentThread().getName());
            }
        }

        logger.debug("Terminating worker {}", Thread.currentThread().getName());
    }
}
