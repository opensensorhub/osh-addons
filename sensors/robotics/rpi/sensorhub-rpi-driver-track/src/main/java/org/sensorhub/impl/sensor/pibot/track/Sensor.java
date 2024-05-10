/***************************** BEGIN LICENSE BLOCK ***************************

 Copyright (C) 2020-2024 Nicolas Garay. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.pibot.track;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sensor driver for the UAS providing sensor description, output registration,
 * initialization and shutdown of driver and outputs.
 *
 * @author Nick Garay
 * @since Jan. 24, 2021
 */
public class Sensor extends AbstractSensorModule<Config> {

    Output output;

    Object syncTimeLock = new Object();

    @Override
    public void init() throws SensorHubException {

        logger = LoggerFactory.getLogger(Sensor.class);

        super.init();

        // Generate identifiers
        generateUniqueID("[URN]", config.serialNumber);
        generateXmlID("[XML-PREFIX]", config.serialNumber);

        // Create and initialize output
        output = new Output(this);

        addOutput(output, false);

        output.init();

        // TODO: Perform other initialization
    }

    @Override
    public void start() throws SensorHubException {

        if (null != output) {

            // Allocate necessary resources and start outputs
            output.start();
        }

        // TODO: Perform other startup procedures
    }

    @Override
    public void stop() throws SensorHubException {

        if (null != output) {

            output.stop();
        }

        // TODO: Perform other shutdown procedures
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return output.isAlive();
    }
}
