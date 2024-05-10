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

import org.sensorhub.impl.pibot.common.output.BaseSensorOutput;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataStream;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Basic Image Output for SmartCamSensor
 *
 * @author Nick Garay
 * @since 1.0.0
 */
public class SmartCamImageOutput extends BaseSensorOutput<SmartCamSensor> {

    private static final String SENSOR_OUTPUT_NAME = "SmartCamImageOutput";

    private static final String SENSOR_OUTPUT_LABEL = "Smart Cam Video";

    private static final String SENSOR_OUTPUT_DESCRIPTION = "Video Feed from Smart Cam";

    private static final String VIDEO_FORMAT = "h264";

    private final Logger logger = LoggerFactory.getLogger(SmartCamImageOutput.class);

    private FrameGrabber frameGrabber;

    public SmartCamImageOutput(SmartCamSensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        workerThread = new Thread(this,
                this.getClass().getSimpleName() + "-Worker-" +
                        parentSensor.getConfiguration().serialNumber);

        logger.debug("{} thread created...", workerThread.getName());
    }

    @Override
    protected void init() throws SensorException {

        logger.debug("Initializing");

        lastDataFrameTimeMillis = System.currentTimeMillis();

        try {

            frameGrabber = FrameGrabber.createDefault(0);

        } catch (FrameGrabber.Exception e) {

            logger.debug("Failed to establish connection with camera");

            throw new SensorException("Failed to establish connection with camera", e);
        }

        frameGrabber.setFormat(VIDEO_FORMAT);

        frameGrabber.setImageHeight(parentSensor.getConfiguration().videoParameters.videoFrameHeight);

        int videoFrameHeight = frameGrabber.getImageHeight();

        frameGrabber.setImageWidth(parentSensor.getConfiguration().videoParameters.videoFrameWidth);

        int videoFrameWidth = frameGrabber.getImageWidth();

        // Get an instance of SWE Factory suitable to build components
        VideoCamHelper sweFactory = new VideoCamHelper();

        DataStream outputDef = sweFactory.newVideoOutputMJPEG(getName(), videoFrameWidth, videoFrameHeight);

        dataStruct = outputDef.getElementType();

        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);

        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        dataEncoding = outputDef.getEncoding();

        logger.debug("Initialized");
    }

    @Override
    protected void start() throws SensorException {

        logger.debug("Starting");

        if (null != frameGrabber) {

            try {

                frameGrabber.start();

                doWork.set(true);

                workerThread.start();

            } catch(FrameGrabber.Exception e) {

                e.printStackTrace();

                logger.error("Failed to start FFmpegFrameGrabber");

                throw new SensorException("Failed to start FFmpegFrameGrabber");
            }

        } else {

            logger.error("Failed to create FFmpegFrameGrabber");

            throw new SensorException("Failed to create FFmpegFrameGrabber");
        }

        logger.debug("Started");
    }

    @Override
    protected void stop() {

        logger.debug("Stopping");

        if (null != frameGrabber) {

            try {

                doWork.set(false);

                workerThread.join();

                frameGrabber.stop();

            } catch(FrameGrabber.Exception e) {

                logger.error("Failed to stop FFmpegFrameGrabber");

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                logger.error("Failed to stop {} thread due to exception {}", workerThread.getName(), e.getMessage());
            }

        } else {

            logger.error("Failed to stop FFmpegFrameGrabber");
        }

        logger.debug("Stopped");
    }

    @Override
    public void run() {

        logger.debug("Starting worker {}", Thread.currentThread().getName());

        try {

            while(doWork.get()) {

                Frame frame = frameGrabber.grab();

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

                // Set underlying video frame data
                AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];

                BufferedImage image = new Java2DFrameConverter().convert(frame);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                byte[] imageData;

                ImageIO.write(image,"jpg",byteArrayOutputStream);

                byteArrayOutputStream.flush();

                imageData = byteArrayOutputStream.toByteArray();

                byteArrayOutputStream.close();

                frameData.setUnderlyingObject(imageData);

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publish(new DataEvent(latestRecordTime, SmartCamImageOutput.this, dataBlock));
            }

        } catch(IOException e) {

            logger.error("Exception in {}", Thread.currentThread().getName());
        }

        logger.debug("Terminating worker {}", Thread.currentThread().getName());
    }
}
