/***************************** BEGIN LICENSE BLOCK ***************************
 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2025 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.nmeaais;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputRawMessages;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputPositionClassA;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputPositionClassB;
import org.sensorhub.impl.sensor.nmeaais.reportschemas.PositionReportClassA;
import org.sensorhub.impl.sensor.nmeaais.reportschemas.PositionReportClassB;
import org.vast.ogc.om.MovingFeature;

import java.io.IOException;
import java.io.InputStream;

/**
 * Driver implementation for the sensor.
 * <p>
 * This class is responsible for providing sensor information, managing output registration,
 * and performing initialization and shutdown for the driver and its outputs.
 */
public class NmeaAisDriver extends AbstractSensorModule<NmeaAisConfig> {
    static final String UID_PREFIX = "urn:osh:sensor:nmea:ais:";
    static final String XML_PREFIX = "nmea:ais:";

    // GLOBAL VARIABLES FOR SENSOR OPERATION
    NmeaAisHandler nmeaAisHandler;
    NmeaAisOutputRawMessages nmeaAisOutputRawMessages;
    NmeaAisOutputPositionClassA nmeaAisOutputPositionClassA;
    NmeaAisOutputPositionClassB nmeaAisOutputPositionClassB;

    ICommProvider<?> commProvider;
    volatile boolean started;

    //  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // INITIALIZE OUTPUT
        nmeaAisOutputRawMessages = new NmeaAisOutputRawMessages(this);
        addOutput(nmeaAisOutputRawMessages, false);
        nmeaAisOutputRawMessages.doInit();

        nmeaAisOutputPositionClassA = new NmeaAisOutputPositionClassA(this);
        addOutput(nmeaAisOutputPositionClassA, false);
        nmeaAisOutputPositionClassA.doInit();

        nmeaAisOutputPositionClassB = new NmeaAisOutputPositionClassB(this);
        addOutput(nmeaAisOutputPositionClassB, false);
        nmeaAisOutputPositionClassB.doInit();

        // Initialize Parser
        nmeaAisHandler = new NmeaAisHandler(this);
    }

    @Override
    public void doStart() throws SensorHubException {
        super.doStart();

        // Init comm provider — recreated here so it picks up any config changes made via the UI
        if (commProvider == null) {
            if (config.commSettings == null)
                throw new SensorHubException("No communication settings specified");
            try {
                var moduleReg = getParentHub().getModuleRegistry();
                commProvider = (ICommProvider<?>) moduleReg.loadSubModule(config.commSettings, true);
                commProvider.start();
            } catch (Exception e) {
                commProvider = null;
                throw e;
            }
        }

        // Get input stream — read byte-by-byte to avoid BufferedReader's large internal buffer,
        // which would delay output until ~100 UDP packets accumulate before returning any lines.
        final InputStream inputStream;
        try {
            inputStream = commProvider.getInputStream();
            getLogger().info("Connected to AIS data stream");
        } catch (IOException e) {
            throw new SensorHubException("Error opening AIS input stream", e);
        }

        Thread t = new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                int b;
                while (started && (b = inputStream.read()) != -1) {
                    if (b == '\n') {
                        String line = sb.toString().trim();
                        sb.setLength(0);
                        if (!line.isEmpty())
                            nmeaAisHandler.handleNmeaAisMessage(line);
                    } else if (b != '\r') {
                        sb.append((char) b);
                    }
                }
            } catch (IOException e) {
                if (started)
                    getLogger().error("Error reading AIS data stream", e);
            }
        });

        started = true;
        t.setName("ais-reader");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void doStop() throws SensorHubException {
        started = false;

        if (commProvider != null) {
            commProvider.stop();
            commProvider = null;
        }

        super.doStop();
    }

    @Override
    public boolean isConnected() {
        return commProvider != null;
    }

    /**
     * Registers an AIS vessel as a Feature of Interest (FOI) keyed by its MMSI.
     * Subsequent calls with the same MMSI are no-ops; the existing FOI UID is returned.
     */
    public String addFoi(String mmsi) {
        String foiUID = UID_PREFIX + "foi:" + mmsi;

        if (!foiMap.containsKey(foiUID)) {
            MovingFeature foi = new MovingFeature();
            foi.setId(mmsi);
            foi.setUniqueIdentifier(foiUID);
            foi.setName("Vessel " + mmsi);
            foi.setDescription("AIS vessel with MMSI " + mmsi);
            addFoi(foi);
            getLogger().debug("New AIS vessel added as FOI: {}", foiUID);
        }

        return foiUID;
    }

    /**
     * Publishes a raw NMEA AIS sentence to the messages output.
     * Called by {@link NmeaAisHandler} for every parsed message regardless of type.
     */
    void publishRawMessage(String nmeaAisMsg) {
        nmeaAisOutputRawMessages.setData(nmeaAisMsg);
    }

    /**
     * Forwards a decoded position report to the position output for publishing.
     * Called by {@link NmeaAisHandler} — keeps the handler decoupled from the Outputs package.
     */
    void publishPositionAReport(PositionReportClassA report) {
        nmeaAisOutputPositionClassA.setData(report);
    }

    void publishPositionBReport(PositionReportClassB report) {
        nmeaAisOutputPositionClassB.setData(report);
    }
}
