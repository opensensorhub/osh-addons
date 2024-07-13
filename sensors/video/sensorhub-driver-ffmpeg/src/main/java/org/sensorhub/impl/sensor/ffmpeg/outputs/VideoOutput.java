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

import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.cdm.common.CDMException;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;
import org.vast.util.Asserts;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Output for video data from the FFMPEG sensor.
 */
public class VideoOutput<T extends ISensorModule<?>> extends AbstractSensorOutput<T> implements DataBufferListener {
    private final String outputLabel;
    private final String outputDescription;

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
     * @param parentSensor         Sensor driver providing this output.
     * @param videoFrameDimensions The width and height of the video frame.
     */
    public VideoOutput(T parentSensor, int[] videoFrameDimensions) {
        this(parentSensor, videoFrameDimensions, "video", "Video", "Video stream using ffmpeg library");
    }

    /**
     * Creates a new video output.
     *
     * @param parentSensor         Sensor driver providing this output.
     * @param videoFrameDimensions The width and height of the video frame.
     * @param name                 The name of the output.
     * @param outputLabel          The label of the output.
     * @param outputDescription    The description of the output.
     */
    public VideoOutput(T parentSensor, int[] videoFrameDimensions, String name, String outputLabel, String outputDescription) {
        super(name, parentSensor);
        this.outputLabel = outputLabel;
        this.outputDescription = outputDescription;

        videoFrameWidth = videoFrameDimensions[0];
        videoFrameHeight = videoFrameDimensions[1];

        logger.debug("Video output created.");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.debug("Initializing video output.");

        RasterHelper sweHelper = new RasterHelper();
        dataStruct = sweHelper.createRecord()
                .name(getName())
                .label(outputLabel)
                .description(outputDescription)
                .definition(SWEHelper.getPropertyUri("VideoFrame"))
                .addField("sampleTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("img", sweHelper.newRgbImage(videoFrameWidth, videoFrameHeight, DataType.BYTE))
                .build();

        BinaryEncoding dataEnc = sweHelper.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        BinaryComponent timeEnc = sweHelper.newBinaryComponent();
        timeEnc.setRef("/" + dataStruct.getComponent(0).getName());
        timeEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(timeEnc);

        BinaryBlock compressedBlock = sweHelper.newBinaryBlock();
        compressedBlock.setRef("/" + dataStruct.getComponent(1).getName());
        compressedBlock.setCompression("H264");
        dataEnc.addMemberAsBlock(compressedBlock);

        try {
            SWEHelper.assignBinaryEncoding(dataStruct, dataEnc);
        } catch (CDMException e) {
            throw new RuntimeException("Invalid binary encoding configuration", e);
        }

        this.dataEncoding = dataEnc;
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
                logger.error("Error while decoding.", e);
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
        long timestamp = System.currentTimeMillis();
        byte[] dataBuffer = dataBufferRecord.getDataBuffer();

        DataBlock dataBlock = latestRecord == null ? dataStruct.createDataBlock() : latestRecord.renew();
        updateIntervalHistogram();

        dataBlock.setDoubleValue(0, timestamp / 1000d);

        // Set underlying video frame data
        AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];
        frameData.setUnderlyingObject(dataBuffer);

        latestRecord = dataBlock;
        latestRecordTime = timestamp;

        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

    /**
     * Updates the interval histogram with the time between the latest record and the current time
     * for calculating the average sampling period.
     */
    private void updateIntervalHistogram() {
        synchronized (histogramLock) {
            if (latestRecord != null && latestRecordTime != Long.MIN_VALUE) {
                long interval = System.currentTimeMillis() - latestRecordTime;
                intervalHistogram.add(interval / 1000d);

                if (intervalHistogram.size() > MAX_NUM_TIMING_SAMPLES) {
                    intervalHistogram.remove(0);
                }
            }
        }
    }
}
