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
package org.sensorhub.impl.sensor.nmea_gps;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver for the ... providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Mar. 7, 2023
 */
public class NmeaGpsSensor extends AbstractSensorModule<NmeaGpsConfig> {

    private static final Logger logger = LoggerFactory.getLogger(NmeaGpsSensor.class);

    private NmeaGpsOutput nmeaGpsOutput;

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("ros-nmea-gps-", config.id);
        generateXmlID("ros-nmea-gps-", config.id);

        // Create and initialize output
        nmeaGpsOutput = new NmeaGpsOutput(this);

        addOutput(nmeaGpsOutput, false);

        nmeaGpsOutput.doInit();
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != nmeaGpsOutput) {

            // Allocate necessary resources and start outputs
            nmeaGpsOutput.doStart();
        }
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != nmeaGpsOutput) {

            nmeaGpsOutput.doStop();
        }
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return nmeaGpsOutput.isAlive();
    }
}
