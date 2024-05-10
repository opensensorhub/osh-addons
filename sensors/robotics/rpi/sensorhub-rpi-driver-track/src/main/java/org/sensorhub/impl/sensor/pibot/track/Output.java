/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020-2024 Nicolas Garay. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.pibot.track;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.helper.GeoPosHelper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Output specification and provider for MISB-TS STANAG 4609 ST0601.16 UAS Metadata
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class Output extends AbstractSensorOutput<Sensor> implements Runnable {
    private static final String SENSOR_OUTPUT_NAME = "[NAME]";
    private static final String SENSOR_OUTPUT_LABEL = "[LABEL]";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "[DESCRIPTION]";

    private static final Logger logger = LoggerFactory.getLogger(Output.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    Thread worker;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    Output(Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void init() {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // TODO: Create data record description

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        worker = new Thread(this, this.name);

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void start() {

        logger.info("Starting worker thread: {}", worker.getName());

        // TODO: Perform other startup

        worker.start();
    }

    /**
     * Terminates processing data for output
     */
    public void stop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        // TODO: Perform other shutdown procedures
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

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastSetTimeMillis = timingHistogram[setIndex];
                }

                ++setCount;

                // TODO: Populate data block

                latestRecord = dataBlock;

                latestRecordTime = System.currentTimeMillis();

                eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, Output.this, dataBlock));

                synchronized (processingLock) {

                    processSets = !stopProcessing;
                }
            }

        } catch (Exception e) {

            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.error("Error in worker thread: {} due to exception: {}", Thread.currentThread().getName(), stringWriter.toString());

        } finally {

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }
}
