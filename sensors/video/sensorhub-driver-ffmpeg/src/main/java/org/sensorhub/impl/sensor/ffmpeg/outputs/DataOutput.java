/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research, LLC. All Rights Reserved.
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
 * Output for the raw, undecoded bytes of a stream read by the FFMPEG sensor.
 */
public class DataOutput<T extends ISensorModule<?>> extends AbstractSensorOutput<T> implements DataBufferListener {
    private static final String NUM_BYTES_ID = "numBytesID";
    private static final Logger logger = LoggerFactory.getLogger(DataOutput.class.getSimpleName());
    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final String outputLabel;
    private final String outputDescription;
    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();

    private DataComponent dataStruct;
    private DataEncoding dataEncoding;
    private Executor executor;

    /**
     * Creates a new raw data output.
     *
     * @param parentSensor Sensor driver providing this output.
     */
    public DataOutput(T parentSensor) {
        this(parentSensor, "data", "Data", "Raw data stream using ffmpeg library");
    }

    /**
     * Creates a new raw data output.
     *
     * @param parentSensor      Sensor driver providing this output.
     * @param name              The name of the output.
     * @param outputLabel       The label of the output.
     * @param outputDescription The description of the output.
     */
    public DataOutput(T parentSensor, String name, String outputLabel, String outputDescription) {
        super(name, parentSensor);

        this.outputLabel = outputLabel;
        this.outputDescription = outputDescription;

        logger.debug("Data output created.");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        logger.debug("Initializing data output.");

        SWEHelper sweHelper = new SWEHelper();
        dataStruct = sweHelper.createRecord()
                .name(getName())
                .label(outputLabel)
                .description(outputDescription)
                .definition(SWEHelper.getPropertyUri("DataFrame"))
                .addField("sampleTime", sweHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("numBytes", sweHelper.createCount()
                        .id(NUM_BYTES_ID)
                        .label("Num Bytes")
                        .description("Number of bytes packaged in this record")
                        .dataType(DataType.INT))
                .addField("data", sweHelper.createArray()
                        .withVariableSize(NUM_BYTES_ID)
                        .label("Data")
                        .description("Raw bytes of the stream packet, exactly as they were demuxed")
                        .withElement("byte", sweHelper.createCount()
                                .label("Byte")
                                .definition(SWEConstants.DEF_DN)
                                .dataType(DataType.BYTE)))
                .build();

        BinaryEncoding dataEnc = sweHelper.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        // Sample time
        BinaryComponent comp = sweHelper.newBinaryComponent();
        comp.setRef("/" + dataStruct.getComponent(0).getName());
        comp.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(comp);

        // Number of bytes
        comp = sweHelper.newBinaryComponent();
        comp.setRef("/" + dataStruct.getComponent(1).getName());
        comp.setCdmDataType(DataType.INT);
        dataEnc.addMemberAsComponent(comp);

        // Bytes. The array element is encoded rather than the array itself, since the payload is not
        // compressed and so has no codec to name in a binary block.
        DataComponent dataArray = dataStruct.getComponent(2);
        comp = sweHelper.newBinaryComponent();
        comp.setRef("/" + dataArray.getName() + "/" + ((DataArray) dataArray).getElementType().getName());
        comp.setCdmDataType(DataType.BYTE);
        dataEnc.addMemberAsComponent(comp);

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
     * Sets the raw data in the output.
     *
     * @param dataBufferRecord The data buffer record containing the raw data.
     */
    public void processBuffer(DataBufferRecord dataBufferRecord) {
        long timestamp = System.currentTimeMillis();
        byte[] dataBuffer = dataBufferRecord.getDataBuffer();

        DataBlock dataBlock = latestRecord == null ? dataStruct.createDataBlock() : latestRecord.renew();
        updateIntervalHistogram();

        int index = 0;
        dataBlock.setDoubleValue(index++, timestamp / 1000d);
        dataBlock.setIntValue(index++, dataBuffer.length);

        // Set the raw bytes, which also sizes the variable-size array to match the buffer
        AbstractDataBlock rawData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[index];
        rawData.setUnderlyingObject(dataBuffer);

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