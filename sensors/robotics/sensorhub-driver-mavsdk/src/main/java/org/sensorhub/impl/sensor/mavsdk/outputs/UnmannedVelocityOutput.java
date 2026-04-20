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
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.mavsdk.UnmannedSystem;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;


/**
 * UnmannedOutput specification and provider for {@link UnmannedSystem}.
 */
public class UnmannedVelocityOutput extends AbstractSensorOutput<UnmannedSystem> {
    static final String SENSOR_OUTPUT_NAME = "UnmannedVelocityOutput";
    static final String SENSOR_OUTPUT_LABEL = "Velocity";
    static final String SENSOR_OUTPUT_DESCRIPTION = "UnmannedSystem velocity output data";

    private static final int MAX_NUM_TIMING_SAMPLES = 10;

    private final ArrayList<Double> intervalHistogram = new ArrayList<>(MAX_NUM_TIMING_SAMPLES);
    private final Object histogramLock = new Object();
    private final Object processingLock = new Object();

    private DataRecord dataRecord;
    private DataEncoding dataEncoding;

    static Telemetry.VelocityNed currentVelocity = null;
    private final ReentrantLock lock = new ReentrantLock();


    /**
     * Creates a new output for the sensor driver.
     *
     * @param parentSensor UnmannedSystem driver providing this output.
     */
    public UnmannedVelocityOutput(UnmannedSystem parentSensor) {
        super(SENSOR_OUTPUT_NAME, parentSensor);
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering, and data types.
     */
    public void doInit() {
        // Get an instance of SWE Factory suitable to build components
        GeoPosHelper sweFactory = new GeoPosHelper();

        // Create the data record description
        dataRecord = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                   .asSamplingTimeIsoUTC()
                   .label("Sample Time")
                   .description("Time of data collection"))
                .addField( "Velocity", sweFactory.createVelocityVectorNED("m/s")
                    .label("Velocity")
                    .definition(SWEHelper.getPropertyUri("LinearVelocity"))
                    .description("Velocity"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");
    }

    @Override
    public DataComponent getRecordDescription() {
        return dataRecord;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {
        synchronized (histogramLock) {
            double sum = 0;
            for (double sample : intervalHistogram)
                sum += sample;

            return sum / intervalHistogram.size();
        }
    }


    public void setData( long timestamp ) {
        synchronized (processingLock) {
            DataBlock dataBlock = latestRecord == null ? dataRecord.createDataBlock() : latestRecord.renew();

            updateIntervalHistogram();

            lock.lock();
            try {
                int index = 0;
                // Populate the data block

                if ( currentVelocity != null ) {
                    dataBlock.setDoubleValue(index++, timestamp / 1000d);

                    //Velocity
                    dataBlock.setDoubleValue(index++, currentVelocity.getNorthMS());
                    dataBlock.setDoubleValue(index++, currentVelocity.getEastMS());
                    dataBlock.setDoubleValue(index++, currentVelocity.getDownMS());
                }

            } finally {
                lock.unlock();
            }

            // Publish the data block
            latestRecord = dataBlock;
            latestRecordTime = timestamp;
            eventHandler.publish(new DataEvent(latestRecordTime, UnmannedVelocityOutput.this, dataBlock));
        }
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

    public void subscribe( io.mavsdk.System drone ) {

        drone.getTelemetry().getVelocityNed()
                .subscribe(
                        vel -> {
                            //log.debug("MAVSDK: Velocity E:" + vel.getEastMS() + " M/S, N:" + vel.getNorthMS() + " M/S");

                            lock.lock();
                            try {
                                currentVelocity = vel;
                            } finally {
                                lock.unlock();
                            }

                            setData(System.currentTimeMillis());
                        },
                        err -> log.error("MAVSDK: Position error: " + err)
                );

    }
}
