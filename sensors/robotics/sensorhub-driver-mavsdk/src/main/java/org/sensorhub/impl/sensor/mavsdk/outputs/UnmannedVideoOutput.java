/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.mavsdk.outputs;

import io.mavsdk.telemetry.Telemetry;
import net.opengis.swe.v20.*;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.mpegts.DataBufferRecord;
import org.sensorhub.impl.sensor.ffmpeg.outputs.VideoOutput;
import org.vast.cdm.common.CDMException;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;
import org.vast.swe.helper.RasterHelper;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Custom Video Output that combines FFmpeg H.264 frames with MAVSDK Telemetry.
 */
public class UnmannedVideoOutput extends VideoOutput<UnmannedSystem> {

    private Telemetry.Position currentPosition = null;
    private final ReentrantLock posLock = new ReentrantLock();

    private DataRecord customDataStruct;
    private DataEncoding customDataEncoding;

    private final int width;
    private final int height;
    private final String codec;

    private static final int MAX_TIMING_SAMPLES = 10;
    private final ArrayList<Double> intervalHist = new ArrayList<>(MAX_TIMING_SAMPLES);
    private final Object histLock = new Object();

    public UnmannedVideoOutput(UnmannedSystem parentSensor, int[] videoFrameDimensions, String codecName) {
        super(parentSensor, videoFrameDimensions, codecName, "Video", "Video & Telemetry", "Video combined with drone location");
        this.width = videoFrameDimensions[0];
        this.height = videoFrameDimensions[1];
        this.codec = codecName;
    }

    @Override
    public void doInit() {
        RasterHelper rasterHelper = new RasterHelper();
        GeoPosHelper geoHelper = new GeoPosHelper();

        // 1. Build DataRecord with 3 components: Time, Image, and Location
        customDataStruct = rasterHelper.createRecord()
                .name(getName())
                .label("Unmanned Video & Telemetry")
                .description("Video stream combined with drone location")
                .definition(SWEHelper.getPropertyUri("VideoFrame"))
                .addField("sampleTime", rasterHelper.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("img", rasterHelper.newRgbImage(width, height, DataType.BYTE))
                .addField("Location", geoHelper.createLocationVectorLLA()
                        .label("Location")
                        .description("Drone Location Latitude Longitude Altitude"))
                .build();

        BinaryEncoding dataEnc = rasterHelper.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);

        // 2. Binary Mapping for Time
        BinaryComponent timeEnc = rasterHelper.newBinaryComponent();
        timeEnc.setRef("/" + customDataStruct.getComponent(0).getName());
        timeEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(timeEnc);

        // 3. Binary Mapping for Video Block
        BinaryBlock compressedBlock = rasterHelper.newBinaryBlock();
        compressedBlock.setRef("/" + customDataStruct.getComponent(1).getName());
        String finalCodec = codec.equalsIgnoreCase("h264") ? "H264" : (codec.equalsIgnoreCase("mjpeg") || codec.equalsIgnoreCase("jpeg") ? "JPEG" : codec);
        compressedBlock.setCompression(finalCodec);
        dataEnc.addMemberAsBlock(compressedBlock);

        // 4. Binary Mapping for Location (lat, lon, alt)
        String locName = customDataStruct.getComponent(2).getName();

        BinaryComponent latEnc = rasterHelper.newBinaryComponent();
        latEnc.setRef("/" + locName + "/lat");
        latEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(latEnc);

        BinaryComponent lonEnc = rasterHelper.newBinaryComponent();
        lonEnc.setRef("/" + locName + "/lon");
        lonEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(lonEnc);

        BinaryComponent altEnc = rasterHelper.newBinaryComponent();
        altEnc.setRef("/" + locName + "/alt");
        altEnc.setCdmDataType(DataType.DOUBLE);
        dataEnc.addMemberAsComponent(altEnc);

        try {
            SWEHelper.assignBinaryEncoding(customDataStruct, dataEnc);
        } catch (CDMException e) {
            throw new RuntimeException("Invalid binary encoding configuration", e);
        }

        this.customDataEncoding = dataEnc;
    }

    @Override
    public DataComponent getRecordDescription() {
        return customDataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return customDataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        synchronized (histLock) {
            if (intervalHist.isEmpty()) return 0;
            double sum = 0;
            for (double sample : intervalHist) sum += sample;
            return sum / intervalHist.size();
        }
    }

    private void updateHist() {
        synchronized (histLock) {
            if (latestRecord != null && latestRecordTime != Long.MIN_VALUE) {
                long interval = System.currentTimeMillis() - latestRecordTime;
                intervalHist.add(interval / 1000d);
                if (intervalHist.size() > MAX_TIMING_SAMPLES) {
                    intervalHist.remove(0);
                }
            }
        }
    }

    @Override
    public void processBuffer(DataBufferRecord dataBufferRecord) {
        long timestamp = System.currentTimeMillis();
        byte[] dataBuffer = dataBufferRecord.getDataBuffer();

        DataBlock dataBlock = latestRecord == null ? customDataStruct.createDataBlock() : latestRecord.renew();
        updateHist();

        // Index 0: Set Time
        dataBlock.setDoubleValue(0, timestamp / 1000d);

        // Index 1: Set Video Frame Array via DataBlockMixed
        AbstractDataBlock frameData = ((DataBlockMixed) dataBlock).getUnderlyingObject()[1];
        frameData.setUnderlyingObject(dataBuffer);

        // Index 2: Set Telemetry
        DataBlock locBlock = (DataBlock) ((DataBlockMixed) dataBlock).getUnderlyingObject()[2];

        posLock.lock();
        try {
            if (currentPosition != null) {
                locBlock.setDoubleValue(0, currentPosition.getLatitudeDeg());
                locBlock.setDoubleValue(1, currentPosition.getLongitudeDeg());
                locBlock.setDoubleValue(2, currentPosition.getRelativeAltitudeM());
            } else {
                locBlock.setDoubleValue(0, Double.NaN);
                locBlock.setDoubleValue(1, Double.NaN);
                locBlock.setDoubleValue(2, Double.NaN);
            }
        } finally {
            posLock.unlock();
        }

        latestRecord = dataBlock;
        latestRecordTime = timestamp;

        eventHandler.publish(new DataEvent(latestRecordTime, this, dataBlock));
    }

    public void subscribe(io.mavsdk.System drone) {
        drone.getTelemetry().getPosition().subscribe(
                pos -> {
                    posLock.lock();
                    try {
                        this.currentPosition = pos;
                    } finally {
                        posLock.unlock();
                    }
                },
                err -> System.err.println("MAVSDK: Position error: " + err)
        );
    }
}
