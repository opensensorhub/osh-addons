/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.outputs;

import java.util.concurrent.Executor;

import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.impl.sensor.ffmpeg.FFMPEGSensorBase;
import org.sensorhub.impl.sensor.ffmpeg.common.SyncTime;
import org.sensorhub.impl.sensor.ffmpeg.config.FFMPEGConfig;
import org.sensorhub.impl.sensor.videocam.VideoCamHelper;
import org.sensorhub.mpegts.DataBufferListener;
import org.sensorhub.mpegts.DataBufferRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.util.Asserts;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @param <UasConfigType> A type parameter that allows us to use this output on both types of sensors that are defined
 *   in this module.
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class Video<UasConfigType extends FFMPEGConfig> extends AbstractSensorOutput<FFMPEGSensorBase<UasConfigType>> implements DataBufferListener {

    private static final String SENSOR_OUTPUT_NAME = "video";
    private static final String SENSOR_OUTPUT_LABEL = "Video";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Video stream using ffmpeg library";

    private static final Logger logger = LoggerFactory.getLogger(Video.class);

    private final int videoFrameWidth;
    private final int videoFrameHeight;
    private String codecFormat;

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int frameCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();
    private Executor executor;


    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     * @param videoFrameDimensions The width and height of the video frame
     */
    public Video(FFMPEGSensorBase<UasConfigType> parentSensor, int[] videoFrameDimensions, String cFormat) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Video created");
        
        videoFrameWidth = videoFrameDimensions[0];
        videoFrameHeight = videoFrameDimensions[1];
        codecFormat = cFormat;

    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    public void init() {

        logger.debug("Initializing Video");


        // Get an instance of SWE Factory suitable to build components
        VideoCamHelper sweFactory = new VideoCamHelper();
        Boolean isMJPEG = (parentSensor).getIsMJPEG();
        DataStream outputDef;

        logger.debug(codecFormat);
        if (Boolean.TRUE.equals(isMJPEG)){
             outputDef = sweFactory.newVideoOutputMJPEG(getName(), videoFrameWidth, videoFrameHeight);
        } else {
             outputDef = sweFactory.newVideoOutputH264(getName(), videoFrameWidth, videoFrameHeight);
        }


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
    public void onDataBuffer(DataBufferRecord record) {

        executor.execute(() -> {
            try {
                processBuffer(record);
            } catch (Throwable e) {
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

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }

    public void processBuffer(DataBufferRecord record) {

        SyncTime syncTime = (parentSensor).getSyncTime();
        Boolean ignoreDataTimestamp = (parentSensor).getIgnoreDataTimestamp();

        // If synchronization time data is available
        if (null != syncTime || ignoreDataTimestamp) {

            byte[] dataBuffer = record.getDataBuffer();

            DataBlock dataBlock;
            if (latestRecord == null) {

                dataBlock = dataStruct.createDataBlock();

            } else {

                dataBlock = latestRecord.renew();
            }

            if (Boolean.TRUE.equals(ignoreDataTimestamp)){
                dataBlock.setDoubleValue(0, System.currentTimeMillis()/1000);
            } else {

                double sampleTime = syncTime.getPrecisionTimeStamp() + (record.getPresentationTimestamp() - syncTime.getPresentationTimeStamp());
                dataBlock.setDoubleValue(0, sampleTime);
            }
            ++frameCount;

            // Set underlying video frame data
            AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];
            frameData.setUnderlyingObject(dataBuffer);

            latestRecord = dataBlock;

            latestRecordTime = System.currentTimeMillis();

            eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));

        } else {

            logger.warn("Synchronization record not yet available from Telemetry, dropping video packet");
        }
    }
}
