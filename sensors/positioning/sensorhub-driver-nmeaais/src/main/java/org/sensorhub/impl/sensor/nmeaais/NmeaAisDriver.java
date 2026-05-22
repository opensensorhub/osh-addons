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

import dk.dma.ais.message.AisMessage18;
import dk.dma.ais.message.AisPositionMessage;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputRawMessages;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputPositionClassA;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputPositionClassB;
import org.vast.ogc.om.MovingFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

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
    AisReader aisReader;
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

        // Initialize Handler
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

        final InputStream inputStream;
        try {
            inputStream = commProvider.getInputStream();
            getLogger().info("Connected to AIS data stream");
        } catch (IOException e) {
            throw new SensorHubException("Error opening AIS input stream", e);
        }

        // DatagramInputStream only overrides the single-byte read(), so BufferedReader
        // (used internally by AisReader) would buffer ~8192 bytes before returning any
        // lines (~100 UDP packets). Work around this by reading byte-by-byte and piping
        // complete lines to AisReader so it still handles fragment reassembly.
        PipedInputStream pipedIn;
        final PipedOutputStream pipedOut;
        try {
            pipedIn = new PipedInputStream(65536);
            pipedOut = new PipedOutputStream(pipedIn);
        } catch (IOException e) {
            throw new SensorHubException("Error creating AIS pipe", e);
        }

        aisReader = AisReaders.createReaderFromInputStream(pipedIn);
        aisReader.registerHandler(aisMessage ->
            nmeaAisHandler.handleAisMessage(aisMessage)
        );

        Thread readerThread = new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                int b;
                while (started && (b = inputStream.read()) != -1) {
                    if (b == '\n') {
                        String line = sb.toString().trim();
                        sb.setLength(0);
                        if (!line.isEmpty()) {
                            publishRawMessage(line);
                            byte[] bytes = (line + "\n").getBytes();
                            pipedOut.write(bytes);
                            pipedOut.flush();
                        }
                    } else if (b != '\r') {
                        sb.append((char) b);
                    }
                }
            } catch (IOException e) {
                if (started)
                    getLogger().error("Error reading AIS data stream", e);
            } finally {
                try { pipedOut.close(); } catch (IOException ignored) {}
            }
        });

        started = true;
        readerThread.setName("ais-reader");
        readerThread.setDaemon(true);
        readerThread.start();
        aisReader.start();
    }

    @Override
    public void doStop() throws SensorHubException {
        started = false;

        if (aisReader != null) {
            aisReader.stop();
            aisReader = null;
        }

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
    public String addFoi(int mmsi) {
        String foiUID = UID_PREFIX + "foi:" + mmsi;

        if (!foiMap.containsKey(foiUID)) {
            MovingFeature foi = new MovingFeature();
            foi.setId(Integer.toString(mmsi));
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
     */
    void publishRawMessage(String nmeaAisMsg) {
        nmeaAisOutputRawMessages.setData(nmeaAisMsg);
    }

    /**
     * Forwards a decoded Class A position report to the position output for publishing.
     */
    void publishPositionAReport(AisPositionMessage report, String description) {
        nmeaAisOutputPositionClassA.setData(report, description);
    }

    /**
     * Forwards a decoded Class B position report to the position output for publishing.
     */
    void publishPositionBReport(AisMessage18 report, String description) {
        nmeaAisOutputPositionClassB.setData(report, description);
    }
}
