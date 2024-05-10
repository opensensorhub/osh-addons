/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.pibot.common.output;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base Sensor Output for Smart Sensors
 *
 * @author Nick Garay
 * @since 1.0.0
 */
public abstract class BaseSensorOutput<T extends ISensorModule<?>> extends AbstractSensorOutput<T> implements Runnable {

    protected Thread workerThread;

    protected AtomicBoolean doWork = new AtomicBoolean(false);

    protected static final int MAX_NUM_TIMING_SAMPLES = 10;

    protected int dataFrameCount = 0;

    protected final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];

    protected final Object histogramLock = new Object();

    protected long lastDataFrameTimeMillis;

    protected DataComponent dataStruct;

    protected DataEncoding dataEncoding;

    protected BaseSensorOutput(String name, T parentSensor) {
        super(name, parentSensor);
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

    /**
     * Terminates processing data for output
     */
    protected abstract void stop();

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    protected abstract void init() throws SensorException;

    /**
     * Begins processing data for output
     */
    protected abstract void start() throws SensorException;
}
