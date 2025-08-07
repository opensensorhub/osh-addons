/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakenSDR;

import com.google.gson.JsonObject;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class KrakenSdrSensor extends AbstractSensorModule<KrakenSdrConfig> implements Runnable {
    static final String UID_PREFIX = "osh:krakenSDR:";
    static final String XML_PREFIX = "krakenSDR";

    private static final Logger logger = LoggerFactory.getLogger(KrakenSdrSensor.class);

    /// GLOBAL VARIABLES FOR SENSOR OPERATION
    KrakenUTILITY util = new KrakenUTILITY();
    KrakenSdrSettingsOutput krakenSdrSettingsOutput;
    KrakenSdrDOAOutput krakenSdrDOAOutput;

    KrakenSdrRecieverControls krakenSdrRecieverControls;
    KrakenSdrAntennaControls krakenSdrAntennaControls;

    String OUTPUT_URL;
    String settings_URL;
    HttpURLConnection settings_conn;
    String DoA_URL;


    private volatile boolean keepRunning = false;

    ///  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        OUTPUT_URL  = "http://" + config.krakenIPaddress + ":" + config.krakenPort;
        settings_URL = OUTPUT_URL + "/settings.json";
        DoA_URL = OUTPUT_URL + "/DOA_value.html";

        /// INITIALIZE CONTROLS
        // Test Connection
        // Get Connection to Kraken Settings:
        try {
            settings_conn = util.createKrakenConnection(settings_URL);
            JsonObject initialSettings = util.retrieveJSONFromAddr(settings_URL);

            krakenSdrRecieverControls = new KrakenSdrRecieverControls(this);
            addControlInput(krakenSdrRecieverControls);
            krakenSdrRecieverControls.doInit(initialSettings);

            krakenSdrAntennaControls = new KrakenSdrAntennaControls(this);
            addControlInput(krakenSdrAntennaControls);
            krakenSdrAntennaControls.doInit(initialSettings);

        } catch (SensorHubException e) {
            throw new RuntimeException("Failed to connect to: " + settings_URL);
        }


        /// INITIALIZE OUTPUTS
        // CURRENT SETTINGS OUTPUT
        krakenSdrSettingsOutput = new KrakenSdrSettingsOutput(this);
        addOutput(krakenSdrSettingsOutput, false);
        krakenSdrSettingsOutput.doInit();

        // DOA INFO SETTINGS
        krakenSdrDOAOutput = new KrakenSdrDOAOutput(this);
        addOutput(krakenSdrDOAOutput, false);
        krakenSdrDOAOutput.doInit();

    }


    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        // Set variable to continue readings
        keepRunning = true;

        // CREATE THREAD THAT CONTINUALLY READS SENSOR REPORT
        Thread readkrakenSDR = new Thread(this, "krakenSDR Worker");
        readkrakenSDR.start();    // This starts the the run() method

    }

    @Override
    public void doStop() throws SensorHubException {
        keepRunning = false;
        super.doStop();
    }

    @Override
    public boolean isConnected() {
        return true;
    }


    @Override
    public void run() {
        while (keepRunning) {
            // Send a GET request to the DoA URL
            try {

                // SET ALL OUTPUTS AT DESIRED TIME INERVAL FROM ADMIN PANEL
                krakenSdrSettingsOutput.SetData();      //settings.json data
                krakenSdrDOAOutput.SetData();           //DoA data

                // Sleep per the sample rate provided by admin panel
                Thread.sleep(TimeUnit.SECONDS.toMillis(config.sampelRate));

            } catch (InterruptedException e) {
                throw new RuntimeException(e);

            }
        }

    }
}
