/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.replay;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.impl.sensor.VarRateSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles processing of data records from a data store, including determining
 * through statistical sampling what the "data frame" rate should be.
 * This processor operates on a batching principle, in that it will retrieve
 * batches of N observations where N is configurable as
 * {@link ReplayConfig#batchSize}.
 *
 * @author Nicolas Garay
 * @since 3/19/2025
 */
public class ReplayOutput extends VarRateSensorOutput<ReplaySensor> {

    private static final Logger logger = LoggerFactory.getLogger(ReplayOutput.class);

    private final AtomicBoolean doWork = new AtomicBoolean(false);

    private final DataStreamKey dataStreamKey;

    private final IDataStreamInfo dataStreamInfo;

    private final IObsSystemDatabase systemDriverDatabase;

    private final Instant startTime;

    private final Instant endTime;

    private final int batchSize;

    private final BlockingQueue<List<IObsData>> queue = new LinkedBlockingQueue<>(8);

    private long numTotalSamples = 0;

    private long avgFrameIntervalMillis = 1000;

    private long batchIndex = 0;

    private long totalBatches = 0;

    private ObsFilter obsFilter;

    private Thread backgroundThread;

    private Thread foregroundThread;

    public ReplayOutput(ReplaySensor parentSensor, DataStreamKey dataStreamKey, IDataStreamInfo dataStreamInfo, IObsSystemDatabase systemDriverDatabase, Instant startTime, Instant endTime, int batchSize) {

        super(dataStreamInfo.getRecordStructure().getName(), parentSensor, 1.0);

        this.dataStreamKey = dataStreamKey;
        this.startTime = startTime;
        this.endTime = endTime;
        this.batchSize = batchSize;
        this.systemDriverDatabase = systemDriverDatabase;
        this.dataStreamInfo = dataStreamInfo;
    }

    @Override
    public DataComponent getRecordDescription() {

        return dataStreamInfo.getRecordStructure();
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataStreamInfo.getRecordEncoding();
    }

    public void doInit() {

        obsFilter = new ObsFilter.Builder()
                .withDataStreams(dataStreamKey.getInternalID())
                .withPhenomenonTimeDuring(startTime, endTime)
                .build();

        numTotalSamples = systemDriverDatabase.getObservationStore().select(obsFilter).count();

        totalBatches = numTotalSamples / batchSize;

        long sampleSize = 100;

        if (numTotalSamples != 0 && sampleSize > numTotalSamples) {

            sampleSize = numTotalSamples;
        }

        if (numTotalSamples > sampleSize) {

            Random r = new Random();
            long sum = 0;

            for (int s = 0; s < sampleSize; s++) {
                long i = 1 + r.nextLong(numTotalSamples - 1);  // pick index 1..numTotalSamples - 1

                List<IObsData> pair = systemDriverDatabase.getObservationStore().select(obsFilter)
                        .sequential()
                        .skip(i - 1)
                        .limit(2)
                        .toList();

                if (pair.size() == 2) {
                    long t1 = pair.get(0).getPhenomenonTime().toEpochMilli();
                    long t2 = pair.get(1).getPhenomenonTime().toEpochMilli();
                    sum += (t2 - t1);
                }
            }

            avgFrameIntervalMillis = sum / sampleSize;
        }

        updateSamplingPeriod(avgFrameIntervalMillis);

        backgroundThread = new Thread(this::selectObservations);

        foregroundThread = new Thread(this::publishObservations);
    }

    public void doStart() {

        doWork.set(true);

        if (numTotalSamples > 0) {

            backgroundThread.start();

            foregroundThread.start();
        }
    }

    public void doStop() {

        doWork.set(false);

        try {

            foregroundThread.join();

            backgroundThread.join();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    void publishOutput(final IObsData observation) {

        latestRecord = observation.getResult();

        for (int index = 0; index < latestRecord.getAtomCount(); ++index) {

            if (DataType.ASCII_STRING == latestRecord.getDataType(index) || DataType.UTF_STRING == latestRecord.getDataType(index)) {

                if (null == latestRecord.getStringValue(index)) {

                    latestRecord.setStringValue(index, "");
                }
            }
        }

        latestRecordTime = Instant.now().toEpochMilli();

        eventHandler.publish(new DataEvent(latestRecordTime, this, latestRecord));
    }

    private void selectObservations() {

        while (doWork.get()) {

            List<IObsData> batch = systemDriverDatabase.getObservationStore()
                    .select(obsFilter)
                    .sequential()
                    .skip(batchIndex * batchSize)
                    .limit(batchSize)
                    .toList();

            batchIndex = (batchIndex >= totalBatches) ? 0 : batchIndex + 1;

            try {

                queue.put(batch);   // blocks if consumer is behind

                if (!doWork.get()) {

                    queue.put(List.of());
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                break;
            }
        }
    }

    public void publishObservations() {

        while (doWork.get()) {

            try {

                List<IObsData> observations = queue.take(); // blocks until producer puts a batch

                for (IObsData observation : observations) {

                    if (!doWork.get()) {

                        break;
                    }

                    observation.getResult().setDoubleValue(0, Instant.now().toEpochMilli() / 1000.0);

                    publishOutput(observation);

                    Thread.sleep(avgFrameIntervalMillis);
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }
        }

        queue.clear();
    }
}