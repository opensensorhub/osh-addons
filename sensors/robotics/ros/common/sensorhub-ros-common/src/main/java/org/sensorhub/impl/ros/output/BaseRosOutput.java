/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.ros.output;

import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;

/**
 * Abstract base output for ROS Integration with OpenSensorHub modules
 *
 * @author Nicolas Garay
 * @since Mar 20, 2023
 */
public abstract class BaseRosOutput<SensorType extends ISensorModule<?>> extends AbstractSensorOutput<SensorType> {

    /**
     * Watermark for computing sampling time
     */
    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    /**
     * Current sample count
     */
    private int sampleCount = 0;

    /**
     * Last sample's timestamp
     */
    private long lastSampleTimeMillis;

    /**
     * Histogram of sample times to be used in computing average sample time
     */
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];

    /**
     * Synchronization object
     */
    private final Object histogramLock = new Object();

    /**
     * Constructor
     *
     * @param name         the output's name
     * @param parentSensor the sensor driver owning the output
     */
    protected BaseRosOutput(String name, SensorType parentSensor) {
        super(name, parentSensor);
    }

    /**
     * Constructor
     *
     * @param name         Name of the output
     * @param parentSensor Parent sensor owning this output definition
     * @param log          Instance of the logger to use
     */
    protected BaseRosOutput(String name, SensorType parentSensor, Logger log) {
        super(name, parentSensor, log);
    }

    /**
     * Computes the average sampling period for this output
     *
     * @return The average sampling period
     */
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

    /**
     * Initializes the sampling time.
     */
    protected void initSamplingTime() {

        lastSampleTimeMillis = System.currentTimeMillis();
    }

    /**
     * Updates the histogram used to compute the avg. sampling period.
     */
    protected void updateSamplingPeriodHistogram() {

        synchronized (histogramLock) {

            int setIndex = sampleCount % MAX_NUM_TIMING_SAMPLES;

            // Get a sampling time for latest set based on previous set sampling time
            timingHistogram[setIndex] = System.currentTimeMillis() - lastSampleTimeMillis;

            // Set latest sampling time to now
            lastSampleTimeMillis = timingHistogram[setIndex];
        }

        ++sampleCount;
    }

    /**
     * Defines the data structure and data encoding fields of this instance of SensorOutput
     * in order for {@link #onNewMessage} to populate the data record.
     */
    protected abstract void defineRecordStructure();

    /**
     * Processes new messages.  Derived classes must override this method in order to translate
     * the message data into SWE Common data and publish it on the Event Bus.  In addition, the
     * overriding implementation must call {@link #updateSamplingPeriodHistogram} in order for the average
     * sampling period to be calculated properly.
     *
     * @param object The message received
     */
    public abstract void onNewMessage(Object object);

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    public abstract void doInit();

    /**
     * Begins processing data for output
     */
    public abstract void doStart();

    /**
     * Terminates processing data for output
     */
    public abstract void doStop();

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public abstract boolean isAlive();
}
