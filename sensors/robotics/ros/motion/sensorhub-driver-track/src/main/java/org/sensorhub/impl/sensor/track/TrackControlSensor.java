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
package org.sensorhub.impl.sensor.track;

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
public class TrackControlSensor extends AbstractSensorModule<TrackControlConfig> {

    private TrackControlOutput trackControlOutput;

    private TrackActuatorsControl trackActuatorsControl;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(TrackControlSensor.class);

        // Generate identifiers
        generateUniqueID("ros-track-", config.id);
        generateXmlID("ros-track-", config.id);

        // Create and initialize output
        trackControlOutput = new TrackControlOutput(this);

        addOutput(trackControlOutput, false);

        trackControlOutput.doInit();

        trackActuatorsControl = new TrackActuatorsControl(this);

        addControlInput(trackActuatorsControl);

        trackActuatorsControl.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != trackControlOutput) {

            // Allocate necessary resources and start outputs
            trackControlOutput.doStart();
        }

        if (null != trackActuatorsControl) {

            trackActuatorsControl.doStart();
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != trackControlOutput) {

            trackControlOutput.doStop();
        }

        if (null != trackActuatorsControl) {

            trackActuatorsControl.doStop();
        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return trackControlOutput.isAlive();
    }
}
