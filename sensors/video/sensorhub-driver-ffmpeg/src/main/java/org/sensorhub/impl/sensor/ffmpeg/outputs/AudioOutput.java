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
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.util.Asserts;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Output for video data from the FFMPEG sensor.
 */
public class AudioOutput<T extends ISensorModule<?>> extends AbstractSensorOutput<T> implements DataBufferListener {
    private static final String NUM_SAMPLES_ID = "numSamplesID";

    private final String outputLabel;
    private final String outputDescription;

    private static final Logger logger = LoggerFactory.getLogger(AudioOutput.class.getSimpleName());

    private final int sampleRate;
    private final String codecName;

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private Executor executor;

    /**
     * Creates a new video output.
     *
     * @param parentSensor Sensor driver providing this output
     */
    public AudioOutput(T parentSensor, int sampleRate, String codecName) {
        this(parentSensor, sampleRate, codecName, "audio", "Audio", "Audio stream using ffmpeg library");
    }

    public AudioOutput(T parentSensor, int sampleRate, String codecName, String outputName, String outputLabel, String outputDescription) {
        super(outputName, parentSensor);

        this.sampleRate = sampleRate;
        this.codecName = codecName;
        this.outputLabel = outputLabel;
        this.outputDescription = outputDescription;

        logger.debug("Video output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.debug("Initializing audio output.");

        SWEHelper sweHelper = new SWEHelper();
        dataStruct = sweHelper.createRecord()
                .name(getName())
                .description(outputDescription)
                .label(outputLabel)
                .definition(SWEHelper.getPropertyUri("AudioFrame"))
                .addField("sampleTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("sampleRate", sweHelper.createQuantity()
                        .label("Sample Rate")
                        .description("Number of audio samples per second")
                        .definition(SWEHelper.getQudtUri("DataRate"))
                        .uomCode("Hz")
                        .dataType(DataType.INT))
                .addField("numSamples", sweHelper.createCount()
                        .id(NUM_SAMPLES_ID)
                        .label("Num Samples")
                        .description("Number of audio samples packaged in this record")
                        .dataType(DataType.INT))
                .addField("samples", sweHelper.createArray()
                        .withVariableSize(NUM_SAMPLES_ID)
                        .withElement("sample", sweHelper.createCount()
                                .label("Audio Sample")
                                .definition(SWEConstants.DEF_DN)
                                .dataType(DataType.BYTE)))
                .build();


        BinaryEncoding dataEnc = sweHelper.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        // Sample time
        BinaryComponent comp = sweHelper.newBinaryComponent();
        comp.setRef("/" + dataStruct.getComponent(0).getName());
        comp.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(comp);

        // Sample rate
        comp = sweHelper.newBinaryComponent();
        comp.setRef("/" + dataStruct.getComponent(1).getName());
        comp.setCdmDataType(DataType.INT);
        dataEnc.addMemberAsComponent(comp);

        // Number of samples
        comp = sweHelper.newBinaryComponent();
        comp.setRef("/" + dataStruct.getComponent(2).getName());
        comp.setCdmDataType(DataType.INT);
        dataEnc.addMemberAsComponent(comp);

        // Samples
        BinaryBlock compressedBlock = sweHelper.newBinaryBlock();
        compressedBlock.setRef("/" + dataStruct.getComponent(3).getName());
        compressedBlock.setCompression(codecName);
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
                logger.error("Error while publishing data.", e);
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
     * Sets the audio data in the output.
     *
     * @param dataBufferRecord The data buffer record containing the audio data.
     */
    public void processBuffer(DataBufferRecord dataBufferRecord) {
        long timestamp = System.currentTimeMillis();
        byte[] dataBuffer = dataBufferRecord.getDataBuffer();

        DataBlock dataBlock = latestRecord == null ? dataStruct.createDataBlock() : latestRecord.renew();
        updateIntervalHistogram();

        int index = 0;
        dataBlock.setDoubleValue(index++, timestamp / 1000d);
        dataBlock.setIntValue(index++, sampleRate);
        dataBlock.setIntValue(index++, dataBuffer.length);

        // set encoded data
        AbstractDataBlock audioData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[index];
        audioData.setUnderlyingObject(dataBuffer);

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
