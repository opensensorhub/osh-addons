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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class KrakenSDRSensor extends AbstractSensorModule<KrakenSDRConfig> implements Runnable {
    static final String UID_PREFIX = "osh:krakenSDR:";
    static final String XML_PREFIX = "krakenSDR";

    private static final Logger logger = LoggerFactory.getLogger(KrakenSDRSensor.class);

    /// GLOBAL VARIABLES FOR SENSOR OPERATION
    KrakenSDROutput krakenSDROutput;
    String DOA_URL;
    private volatile boolean keepRunning = false;

    // Cosmetic debugging Variables Used to make font bold
    String BoldOn = "\033[1m";  // ANSI code to turn on bold
    String BoldOff = "\033[0m"; // ANSI code to turn on bold

    ///  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        DOA_URL = "http://" + config.krakenIPaddress + ":" + config.krakenPort + "/DOA_value.html";

        // INITIALIZE OUTPUT INSTANCE
        krakenSDROutput = new KrakenSDROutput(this);
        addOutput(krakenSDROutput, false);
        krakenSDROutput.doInit();

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
                URL url = new URL(DOA_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // READ DoA CSV provided by KrakenSDR Software
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String DoAcsv;
                while ((DoAcsv = in.readLine()) != null) {
                    // Send to Output for setting to OSH
                    krakenSDROutput.SetData(DoAcsv);
                }
                // Sleep per the sample rate provided by admin panel
                Thread.sleep(TimeUnit.SECONDS.toMillis(config.sampelRate));

            } catch (MalformedURLException e) {
                logger.error("The information provided could not create a properly formed URL: {}", DOA_URL);
                throw new RuntimeException(e);
            } catch (IOException e) {
                logger.error("Failed to reach host ({}), currently, only http is provided.", DOA_URL);
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


        }

    }
}
