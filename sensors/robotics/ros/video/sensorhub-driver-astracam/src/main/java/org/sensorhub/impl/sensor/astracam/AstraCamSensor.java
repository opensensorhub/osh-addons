/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one
 * at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Copyright (C) 2023 Botts Innovative Research, Inc. All Rights Reserved.
 *
 */
package org.sensorhub.impl.sensor.astracam;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;

/**
 * Sensor driver for the video/image ROS topics providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Feb. 2, 2023
 */
public class AstraCamSensor extends AbstractSensorModule<AstraCamConfig> {

    private VideoOutput videoOutput;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("ros-image-", config.id);
        generateXmlID("ros-image-", config.id);

        // Create and initialize output
        videoOutput = new VideoOutput(this);

        addOutput(videoOutput, false);

        videoOutput.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != videoOutput) {

            // Allocate necessary resources and start outputs
            videoOutput.doStart();
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != videoOutput) {

            videoOutput.doStop();
        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return videoOutput.isAlive();
    }
}
