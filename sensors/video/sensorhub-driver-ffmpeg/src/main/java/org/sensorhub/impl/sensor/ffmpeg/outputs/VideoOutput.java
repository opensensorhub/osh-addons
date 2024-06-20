/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.outputs;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensor;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.util.Asserts;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Output for video data from the FFMPEG sensor.
 */
public class VideoOutput extends AbstractSensorOutput<FFMPEGSensor> implements DataBufferListener {
    private static final String SENSOR_OUTPUT_NAME = "video";
    private static final String SENSOR_OUTPUT_LABEL = "Video";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Video stream using ffmpeg library";

    private static final Logger logger = LoggerFactory.getLogger(VideoOutput.class.getSimpleName());

    private final int videoFrameWidth;
    private final int videoFrameHeight;

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private Executor executor;

    /**
     * Creates a new video output.
     *
     * @param parentSensor         Sensor driver providing this output
     * @param videoFrameDimensions The width and height of the video frame
     */
    public VideoOutput(FFMPEGSensor parentSensor, int[] videoFrameDimensions) {
        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Video output created");

        videoFrameWidth = videoFrameDimensions[0];
        videoFrameHeight = videoFrameDimensions[1];
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.debug("Initializing Video");

        // Get an instance of SWE Factory suitable to build components
        VideoCamHelper sweFactory = new VideoCamHelper();

        DataStream outputDef = sweFactory.newVideoOutputH264(getName(), videoFrameWidth, videoFrameHeight);
        dataStruct = outputDef.getElementType();
        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);
        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        dataEncoding = outputDef.getEncoding();

        logger.debug("Initializing Video Complete");
    }

    public void setExecutor(Executor executor) {
        this.executor = Asserts.checkNotNull(executor, Executor.class);
    }

    @Override
    public void onDataBuffer(DataBufferRecord dataBufferRecord) {
        executor.execute(() -> {
            try {
                processBuffer(dataBufferRecord);
            } catch (Exception e) {
                logger.error("Error while decoding", e);
            }
        });
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
        double sum = 0;

        synchronized (histogramLock) {
            for (double sample : intervalHistogram) {
                sum += sample;
            }
        }

        return sum / intervalHistogram.size();
    }

    /**
     * Sets the video frame data in the output.
     *
     * @param dataBufferRecord The data buffer record containing the video frame data.
     */
    public void processBuffer(DataBufferRecord dataBufferRecord) {
        byte[] dataBuffer = dataBufferRecord.getDataBuffer();

        DataBlock dataBlock = createDataBlock();
        updateTimingHistogram();

        dataBlock.setDoubleValue(0, System.currentTimeMillis() / 1000d);

        // Set underlying video frame data
        AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];
        frameData.setUnderlyingObject(dataBuffer);

        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();

        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

    /**
     * Creates a new data block for the output.
     *
     * @return A new data block
     */
    private DataBlock createDataBlock() {
        if (latestRecord == null) {
            return dataStruct.createDataBlock();
        } else {
            return latestRecord.renew();
        }
    }

    /**
     * Updates the interval histogram with the latest set time.
     */
    private void updateTimingHistogram() {
        synchronized (histogramLock) {
            if (latestRecord != null) {
                long interval = System.currentTimeMillis() - latestRecordTime;
                intervalHistogram.add(interval / 1000d);

                if (intervalHistogram.size() > MAX_NUM_TIMING_SAMPLES) {
                    intervalHistogram.remove(0);
                }
            }
        }
    }
}
