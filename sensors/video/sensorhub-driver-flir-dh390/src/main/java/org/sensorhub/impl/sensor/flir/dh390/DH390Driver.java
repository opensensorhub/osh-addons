/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.flir.dh390;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

/**
 * Sensor driver for the FLIR DH-390 camera.
 */
public class DH390Driver extends AbstractSensorModule<DH390Config> {

    protected DH390OrientationOutput orientationOutput;

    /**
     * Initialize the sensor driver and its outputs.
     *
     * @throws SensorHubException If there is an error initializing the sensor driver.
     */
    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        generateUniqueID("urn:osh:sensor:flir:dh-390:", config.serialNumber);
        generateXmlID("FLIR_DH-390_", config.serialNumber);

        // Initialize the orientation output
        if (config.positionConfig.orientation != null) {
            orientationOutput = new DH390OrientationOutput(this);
            orientationOutput.doInit();
            addOutput(orientationOutput, true);
        }
    }

    /**
     * Start the sensor driver and its outputs.
     *
     * @throws SensorHubException If there is an error starting the sensor driver.
     */
    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        // Set the sensor orientation
        if (orientationOutput != null && config.positionConfig.orientation != null) {
            orientationOutput.setOrientation(config.positionConfig.orientation);
        }
    }

    /**
     * Stop the sensor driver and its outputs.
     *
     * @throws SensorHubException If there is an error stopping the sensor driver.
     */
    @Override
    protected void doStop() throws SensorHubException {
        super.doStop();
    }

    /**
     * Check if the sensor driver is connected to the camera.
     *
     * @return True if the sensor driver is connected to the camera, false otherwise.
     */
    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();

            if (!sensorDescription.isSetDescription())
                sensorDescription.setDescription("FLIR DH-390 Camera");
        }
    }
}
