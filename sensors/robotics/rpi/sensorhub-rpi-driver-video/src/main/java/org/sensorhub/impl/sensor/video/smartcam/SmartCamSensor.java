/*
 * The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain
 * one at http://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) 2021-2024 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.impl.sensor.video.smartcam;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sensor module for the OpenSensorHub driver
 *
 * @author Nick Garay
 * @since 1.0.0
 */
public class SmartCamSensor extends AbstractSensorModule<SmartCamConfig> {

    private AtomicBoolean isConnected = new AtomicBoolean(false);

    private SmartCamImageOutput imageOutput;

    private SmartCamLocationOutput smartCamLocationOutput;

    @Override
    public void doInit() throws SensorHubException {

        logger = LoggerFactory.getLogger(SmartCamSensor.class);

        super.doInit();

        logger.debug("Initializing");

        generateUniqueID("urn:sentinel:sensor:smartcam:", config.serialNumber);

        generateXmlID("SENTINEL_SMARTCAM_", config.serialNumber);

        imageOutput = new SmartCamImageOutput(this);

        try {

            imageOutput.init();

            addOutput(imageOutput, false);

        } catch (SensorException e) {

            logger.error("Failed to initialize {}", imageOutput.getName());

            throw new SensorHubException("Failed to initialize " + imageOutput.getName());
        }

        smartCamLocationOutput = new SmartCamLocationOutput(this);

        try {

            smartCamLocationOutput.init();

            addOutput(smartCamLocationOutput, false);

        } catch (SensorException e) {

            logger.error("Failed to initialize {}", smartCamLocationOutput.getName());

            throw new SensorHubException("Failed to initialize " + smartCamLocationOutput.getName());
        }
    }

    @Override
    public boolean isConnected() {

        return isConnected.get();
    }

    @Override
    public void doStart() throws SensorHubException {

        logger.debug("Starting");

        super.doStart();

        if(null != imageOutput) {

            try {

                imageOutput.start();

            } catch (SensorException e) {

                logger.error("Failed to start {} due to {}", imageOutput.getName(), e);
            }
        }

        if(null != smartCamLocationOutput) {

            try {

                smartCamLocationOutput.start();

            } catch (SensorException e) {

                logger.error("Failed to start {} due to {}", smartCamLocationOutput.getName(), e);
            }
        }

        isConnected.set(true);

        logger.debug("Started");
    }

    @Override
    public void doStop() throws SensorHubException {

        logger.debug("Stopping");

        super.doStop();

        if(null != imageOutput) {

            imageOutput.stop();
        }

        if(null != smartCamLocationOutput) {

            smartCamLocationOutput.stop();
        }

        isConnected.set(false);

        logger.debug("Stopped");
    }
}
