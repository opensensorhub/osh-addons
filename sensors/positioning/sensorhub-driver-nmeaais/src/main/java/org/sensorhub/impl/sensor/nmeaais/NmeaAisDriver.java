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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputPositionClassA;
import org.sensorhub.impl.sensor.nmeaais.outputs.NmeaAisOutputPositionClassB;
import org.sensorhub.impl.sensor.nmeaais.reportschemas.PositionReportClassA;
import org.sensorhub.impl.sensor.nmeaais.reportschemas.PositionReportClassB;
import org.vast.ogc.om.MovingFeature;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

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
    NmeaAisOutputPositionClassA nmeaAisOutputPositionClassA;
    NmeaAisOutputPositionClassB nmeaAisOutputPositionClassB;

    static final int MAX_PACKET_SIZE = 4096;

//    static final String test1 = "!AIVDM,1,1,,A,15NfK=PP00qm21jCarCv4?wf20S4,0*13";
//    static final String test2 = "!AIVDM,1,1,,B,35NNm0dP@Vqm19HCbhs<HqaN01bP,0*23";
//    static final String test3 = "!AIVDM,1,1,,A,B52gRs@0anMQ6o4tQ@HUcwk1hD04,0*35";

    DatagramSocket socket;
    volatile boolean started;

    //  INITIALIZE
    @Override
    public void doInit() throws SensorHubException {
        super.doInit();

        // Create SensorHub Identifiers using designated prefix and serial number from Admin Panel
        generateUniqueID(UID_PREFIX, config.serialNumber);
        generateXmlID(XML_PREFIX, config.serialNumber);

        // INITIALIZE OUTPUT
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
        try {
            // SO_REUSEADDR allows binding to a port that another process (e.g. AIS-Catcher) is also sending to,
            // preventing "Address already in use" when multiple consumers subscribe to the same UDP feed.
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(config.udpPort));
            getLogger().info("Listening for AIS data on UDP port {}", config.udpPort);
        } catch (IOException e) {
            throw new SensorHubException("Cannot bind UDP socket on port " + config.udpPort, e);
        }

        Thread t = new Thread(() -> {
            byte[] buf = new byte[MAX_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (started) {
                try {
                    socket.receive(packet);
                    String aisNmeaMsg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII).trim();
                    nmeaAisHandler.handleNmeaAisMessage(aisNmeaMsg);

                } catch (IOException e) {
                    if (started)
                        getLogger().error("Error reading AIS UDP packet", e);
                }
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

        if (socket != null) {
            socket.close();
            socket = null;
        }

        super.doStop();
    }

    @Override
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    /**
     * Registers an AIS vessel as a Feature of Interest (FOI) keyed by its MMSI.
     * Subsequent calls with the same MMSI are no-ops; the existing FOI UID is returned.
     */

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
     * Forwards a decoded position report to the position output for publishing.
     * Called by {@link NmeaAisHandler} — keeps the handler decoupled from the Outputs package.
     */
    void publishPositionAReport(String nmeaAisMsg, PositionReportClassA report) {
        nmeaAisOutputPositionClassA.setData(nmeaAisMsg, report);
    }
    void publishPositionBReport(String nmeaAisMsg, PositionReportClassB report) {
        nmeaAisOutputPositionClassB.setData(nmeaAisMsg, report);
    }

}
