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
package org.sensorhub.impl.sensor.movebase;

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
public class MoveBaseSensor extends AbstractSensorModule<MoveBaseConfig> {

    private MoveBaseControl moveBaseControl;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        logger = LoggerFactory.getLogger(MoveBaseSensor.class);

        // Generate identifiers
        generateUniqueID("ros-movebase-", config.id);
        generateXmlID("ros-movebase-", config.id);

        moveBaseControl = new MoveBaseControl(this);

        addControlInput(moveBaseControl);

        moveBaseControl.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != moveBaseControl) {

            moveBaseControl.doStart();
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != moveBaseControl) {

            moveBaseControl.doStop();
        }
    }

    @Override
    public boolean isConnected() {

        return true;
    }
}
