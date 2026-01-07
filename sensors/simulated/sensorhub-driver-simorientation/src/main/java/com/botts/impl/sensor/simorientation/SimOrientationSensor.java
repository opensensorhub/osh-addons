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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

import java.util.concurrent.*;

public class SimOrientationSensor extends AbstractSensorModule<SimOrientationConfig> {
    ScheduledFuture<?> locationUpdateTask;
    SimOrientationOutput orientationOutput;
    ScheduledExecutorService executorService;
    
    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();
        
        // generate identifiers
        generateUniqueID("urn:osh:sensor:simorientation:", config.serialNumber);
        generateXmlID("SIMULATED_ORIENTATION_SENSOR_", config.serialNumber);
        
        // init main data interface
        orientationOutput = new SimOrientationOutput(this);
        addOutput(orientationOutput, false);

        executorService = Executors.newSingleThreadScheduledExecutor();
    }


    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            
            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("Simulated orientation sensor generating random measurements");
        }
    }


    @Override
    protected void doStart() throws SensorHubException {
        if (orientationOutput != null)
            orientationOutput.start();

        locationUpdateTask = executorService.scheduleAtFixedRate(() ->
                        locationOutput.updateLocation(
                                System.currentTimeMillis()/1000d,
                                config.location.lon,
                                config.location.lat,
                                config.location.alt,
                                true)
        , 0, 1, TimeUnit.SECONDS);
    }
    

    @Override
    protected void doStop() throws SensorHubException {
        if (orientationOutput != null)
            orientationOutput.stop();

        if (locationUpdateTask != null)
            locationUpdateTask.cancel(true);
    }

    @Override
    public void cleanup() throws SensorHubException {
        super.cleanup();

        doStop();

        executorService.shutdown();
    }

    @Override
    public boolean isConnected() {
        return locationUpdateTask.isCancelled();
    }
}

