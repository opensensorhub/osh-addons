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
package org.sensorhub.impl.sensor.remotecontrol;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Feb. 2, 2023
 */
public class RemoteControlSensor extends AbstractSensorModule<RemoteControlConfig> {

    private RemoteControlOutput remoteControlOutput;

    private RemoteSensorControl remoteSensorControl;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(RemoteControlSensor.class);

        // Generate identifiers
        generateUniqueID("ros-teleop-", config.id);
        generateXmlID("ros-teleop-", config.id);

        // Create and initialize output
        remoteControlOutput = new RemoteControlOutput(this);

        addOutput(remoteControlOutput, false);

        remoteControlOutput.doInit();

        remoteSensorControl = new RemoteSensorControl(this);

        addControlInput(remoteSensorControl);

        remoteSensorControl.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != remoteControlOutput) {

            // Allocate necessary resources and start outputs
            remoteControlOutput.doStart();
        }

        if (null != remoteSensorControl) {

            remoteSensorControl.doStart();
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != remoteControlOutput) {

            remoteControlOutput.doStop();
        }

        if (null != remoteSensorControl) {

            remoteSensorControl.doStop();
        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return remoteControlOutput.isAlive();
    }
}
