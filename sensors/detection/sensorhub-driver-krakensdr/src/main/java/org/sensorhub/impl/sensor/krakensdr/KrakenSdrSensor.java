/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.krakensdr;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
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

    // GLOBAL VARIABLES FOR SENSOR OPERATION
    KrakenUTILITY util;
    KrakenSdrOutputSettings krakenSdrOutputSettings;
    KrakenSdrOutputDOA krakenSdrOutputDOA;
    KrakenSdrControlReceiver krakenSdrControlReceiver;
    KrakenSdrControlDoA krakenSdrControlDoA;
    KrakenSdrControlStation krakenSdrControlStation;

    String OUTPUT_URL;
    String SETTINGS_URL;
    String DOA_CSV_URL;
    String DOA_XML_URL;


    private volatile boolean keepRunning = false;

    //  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // THE KRAKEN GUI APPLICATION SERVES IT'S _SHARE DIRECTORY TO A SPECIFIC PORT. DEFINE STRUCTURE IN CONFIG TO USE IN APP
        OUTPUT_URL  = "http://" + config.krakenIPaddress + ":" + config.krakenPort;
        SETTINGS_URL = OUTPUT_URL + "/settings.json";
        DOA_CSV_URL = OUTPUT_URL + "/DOA_value.html";
        DOA_XML_URL = OUTPUT_URL + "/doa.xml";

        // INITIALIZE UTILITY
        util = new KrakenUTILITY(this);

        // VALIDATE KRAKEN CONNECTIVITY
        util.getSettings();

        // INITIALIZE CONTROLS
        krakenSdrControlReceiver = new KrakenSdrControlReceiver(this);
        addControlInput(krakenSdrControlReceiver);
        krakenSdrControlReceiver.doInit();

        krakenSdrControlDoA = new KrakenSdrControlDoA(this);
        addControlInput(krakenSdrControlDoA);
        krakenSdrControlDoA.doInit();

        krakenSdrControlStation = new KrakenSdrControlStation(this);
        addControlInput(krakenSdrControlStation);
        krakenSdrControlStation.doInit();


        // INITIALIZE OUTPUTS
        // CURRENT SETTINGS OUTPUT
        krakenSdrOutputSettings = new KrakenSdrOutputSettings(this);
        addOutput(krakenSdrOutputSettings, false);
        krakenSdrOutputSettings.doInit();

        // DOA INFO SETTINGS
        krakenSdrOutputDOA = new KrakenSdrOutputDOA(this);
        addOutput(krakenSdrOutputDOA, false);
        krakenSdrOutputDOA.doInit();

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
            // SET ALL OUTPUTS AT DESIRED TIME INERVAL FROM ADMIN PANEL
            krakenSdrOutputSettings.setData();      //settings.json data
            krakenSdrOutputDOA.setData();           //DoA data
            try {
                // Sleep per the sample rate provided by admin panel
                Thread.sleep(TimeUnit.SECONDS.toMillis(config.sampleRate));

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt flag
                getLogger().debug("KrakenSDR worker thread interrupted, shutting down", e);
                break;
            }
        }

    }
}
