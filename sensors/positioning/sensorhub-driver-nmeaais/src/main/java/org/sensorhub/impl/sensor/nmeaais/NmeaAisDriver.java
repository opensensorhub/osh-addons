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
    static final String UID_PREFIX = "urn:osh:sensor:shipxplorer:";
    static final String XML_PREFIX = "shipXplorer";

    // GLOBAL VARIABLES FOR SENSOR OPERATION
    NmeaAisHandler nmeaAisHandler;
    NmeaAisOutputPosition nmeaAisOutputPosition;

    static final int MAX_PACKET_SIZE = 4096;

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
        nmeaAisOutputPosition = new NmeaAisOutputPosition(this);
        addOutput(nmeaAisOutputPosition, false);
        nmeaAisOutputPosition.doInit();

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

}
