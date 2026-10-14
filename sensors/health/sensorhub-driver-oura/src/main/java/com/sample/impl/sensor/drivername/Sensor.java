/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.drivername;

import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;

/**
 * Sensor driver providing sensor description, output registration, initialization and shutdown of driver and outputs.
 *
 * @author your_name
 * @since date
 */
public class Sensor extends AbstractSensorModule<Config> {

    private static final Logger logger = LoggerFactory.getLogger(Sensor.class);

    SleepOutput sleepOutput;
    SpO2Output spO2Output;
    HeartOutput heartOutput; // Added 08/19
    ReadinessOutput readinessOutput; // Added 09/10

    @Override
    protected void updateSensorDescription() {
        synchronized (sensorDescLock) {
            super.updateSensorDescription();
            if (!sensorDescription.isSetDescription()) {
                sensorDescription.setDescription("Oura Ring Data");
                SMLHelper smlHelper = new SMLHelper();
                smlHelper.edit((PhysicalSystem) sensorDescription)
                        .addIdentifier(smlHelper.identifiers.serialNumber("00001"))
                        .addClassifier(smlHelper.classifiers.sensorType("Wearable Health Monitor"));
            }
        }
    }

    @Override
    public void doInit() throws SensorHubException {

        super.doInit();

        // Generate identifiers
        generateUniqueID("urn:osh:sensor:oura", null);
        generateXmlID("Oura Ring", null);

        // Create and initialize output
        sleepOutput = new SleepOutput(this);
        addOutput(sleepOutput, false);
        sleepOutput.doInit(config.startTime, config.endTime);

        spO2Output = new SpO2Output(this);
        addOutput(spO2Output, false);
        spO2Output.doInit(config.startTime, config.endTime);

        heartOutput = new HeartOutput(this);
        addOutput(heartOutput, false);
        heartOutput.doInit(config.startTime, config.endTime);

        readinessOutput = new ReadinessOutput(this);
        addOutput(readinessOutput, false);
        readinessOutput.doInit(config.startTime, config.endTime);

        // TODO: Perform other initialization
    }

    @Override
    public void doStart() throws SensorHubException {

        if (null != sleepOutput) {
            sleepOutput.doStart();
        }

        if (null != spO2Output) {
            spO2Output.doStart();
        }

        if (null != heartOutput) {
            heartOutput.doStart();
        }

        if (null != readinessOutput) {
            readinessOutput.doStart();
        }

        // TODO: Perform other startup procedures
    }

    @Override
    public void doStop() throws SensorHubException {

        if (null != sleepOutput) {
            sleepOutput.doStop();
        }

        if (null != spO2Output) {
            spO2Output.doStop();
        }

        if (null != heartOutput) {
            heartOutput.doStop();
        }

        if (null != readinessOutput) {
            readinessOutput.doStop();
        }

        // TODO: Perform other shutdown procedures
    }

    @Override
    public boolean isConnected() {

        // Determine if sensor is connected
        return (sleepOutput.isAlive() && spO2Output.isAlive() && heartOutput.isAlive() && readinessOutput.isAlive());
    }
}
