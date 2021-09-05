/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.uas.outputs;

import org.sensorhub.impl.sensor.uas.common.SyncTime;
import org.sensorhub.impl.sensor.uas.UasSensor;
import org.sensorhub.misb.stanag4609.comm.DataBufferListener;
import org.sensorhub.misb.stanag4609.comm.DataBufferRecord;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Boolean;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class Video extends AbstractSensorOutput<UasSensor> implements DataBufferListener, Runnable {

    private static final String SENSOR_OUTPUT_NAME = "video";
    private static final String SENSOR_OUTPUT_LABEL = "UAS Video";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Video acquired by on-board image sensor";

    private static final Logger logger = LoggerFactory.getLogger(Video.class);

    private final int videoFrameWidth;
    private final int videoFrameHeight;

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private final BlockingQueue<DataBufferRecord> dataBufferQueue = new LinkedBlockingDeque<>();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int frameCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor         Sensor driver providing this output
     * @param videoFrameDimensions The width and height of the video frame
     */
    public Video(UasSensor parentSensor, int[] videoFrameDimensions) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Video created");

        videoFrameWidth = videoFrameDimensions[0];

        videoFrameHeight = videoFrameDimensions[1];
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    public void init() {

        logger.debug("Initializing Video");

        // Get an instance of SWE Factory suitable to build components
        VideoCamHelper sweFactory = new VideoCamHelper();

        DataStream outputDef = sweFactory.newVideoOutputH264(getName(), videoFrameWidth, videoFrameHeight);
        dataStruct = outputDef.getElementType();
        dataStruct.setLabel(SENSOR_OUTPUT_LABEL);
        dataStruct.setDescription(SENSOR_OUTPUT_DESCRIPTION);

        dataEncoding = outputDef.getEncoding();

        worker = new Thread(this, this.name);

        logger.debug("Initializing Video Complete");
    }

    /**
     * Begins processing data for output
     */
    public void start() {

        logger.info("Starting worker thread: {}", worker.getName());

        worker.start();
    }

    @Override
    public void onDataBuffer(DataBufferRecord record) {

        try {

            dataBufferQueue.put(record);

        } catch (InterruptedException e) {

            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), e.toString());
        }
    }

    /**
     * Terminates processing data for output
     */
    public void stop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return worker.isAlive();
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

    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {

            while (processSets) {

                DataBufferRecord record = dataBufferQueue.take();

                SyncTime syncTime = ((UasSensor)parentSensor).getSyncTime();

                // If synchronization time data is available
                if (null != syncTime) {

                    byte[] dataBuffer = record.getDataBuffer();

                    synchronized (histogramLock) {

                        int frameIndex = frameCount % MAX_NUM_TIMING_SAMPLES;

                        // Get a sampling time for latest set based on previous set sampling time
                        timingHistogram[frameIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                        // Set latest sampling time to now
                        lastSetTimeMillis = timingHistogram[frameIndex];
                    }

                    DataBlock dataBlock;
                    if (latestRecord == null) {

                        dataBlock = dataStruct.createDataBlock();

                    } else {

                        dataBlock = latestRecord.renew();
                    }

                    double sampleTime = syncTime.getPrecisionTimeStamp() + (record.getPresentationTimestamp() - syncTime.getPresentationTimeStamp());

                    dataBlock.setDoubleValue(0, sampleTime);
                    ++frameCount;

                    // Set underlying video frame data
                    AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];
                    frameData.setUnderlyingObject(dataBuffer);

                    latestRecord = dataBlock;

                    latestRecordTime = System.currentTimeMillis();

                    eventHandler.publish(new DataEvent(latestRecordTime, this, parentSensor.getImagedFoiUID(), dataBlock));

                } else {

                    logger.warn("Synchronization record not yet available from Telemetry, dropping video packet");
                }

                synchronized (processingLock) {

                    processSets = !stopProcessing;
                }
            }

        } catch (InterruptedException e) {

            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), e.toString());

        } catch (Exception e) {

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());

        } finally {

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
