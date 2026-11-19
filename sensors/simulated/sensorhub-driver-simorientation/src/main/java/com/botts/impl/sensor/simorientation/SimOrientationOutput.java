/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package com.botts.impl.sensor.simorientation;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.api.data.DataEvent;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;

public class SimOrientationOutput extends AbstractSensorOutput<SimOrientationSensor> {
    DataComponent dataStruct;
    DataEncoding dataEncoding;

    ScheduledFuture<?> orientationUpdateTask;

    Random rand = new Random();

    final int MAX_AZIMUTH = 360;
    final int MIN_AZIMUTH = 0;
    final double SAMPLE_RATE_S = 1.0f;

    double azimuth = 0.0;

    
    public SimOrientationOutput(SimOrientationSensor parentSensor) {
        super("simOrientation", parentSensor);
        init();
    }


    private void init() {
        SWEHelper fac = new SWEHelper();

        dataStruct = fac.createRecord()
                .name(getName())
                .definition(SWEHelper.getPropertyUri("SimulatedOrientation"))
                .description("Simulated orientation components")
                .addField("samplingTime", fac.createTime()
                        .asSamplingTimeIsoUTC())
                .addField("azimuth", fac.createQuantity()
                        .definition(GeoPosHelper.DEF_AZIMUTH_ANGLE)
                        .refFrame(SWEConstants.REF_FRAME_NED)
                        .axisId("z")
                        .label("Azimuth Angle")
                        .description("Azimuth/Heading angle of line-of-sight measured from true north")
                        .uomCode("deg"))
                // TODO: Expand to include other orientation outputs as needed
                .build();

        dataEncoding = fac.newTextEncoding(",", "\n");
    }

    
    private void sendMeasurement() {
        double time = System.currentTimeMillis() / 1000.;

        // create random azimuth bounded to max
        azimuth = rand.nextInt(MAX_AZIMUTH);

        DataBlock dataBlock = dataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, azimuth);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publish(new DataEvent(latestRecordTime, SimOrientationOutput.this, dataBlock));
    }


    protected void start() {
        if (orientationUpdateTask != null)
            return;

        orientationUpdateTask = parentSensor.executorService.scheduleAtFixedRate(
                this::sendMeasurement,
                0L,
                (long) getAverageSamplingPeriod(),
                TimeUnit.SECONDS);
    }


    protected void stop() {
        if (orientationUpdateTask != null) {
            orientationUpdateTask.cancel(true);
            orientationUpdateTask = null;
        }
    }


    @Override
    public double getAverageSamplingPeriod() {
    	// sample every 1 second
        return SAMPLE_RATE_S;
    }


    @Override
    public DataComponent getRecordDescription() {
        return dataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding() {
        return dataEncoding;
    }
}
