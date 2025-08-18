/*
 *  The contents of this file are subject to the Mozilla Public License, v. 2.0.
 *  If a copy of the MPL was not distributed with this file, You can obtain one
 *  at http://mozilla.org/MPL/2.0/.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the License.
 *
 *  Copyright (C) 2025 Botts Innovative Research, Inc. All Rights Reserved.
 */
package org.sensorhub.api.comm.providers;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * <p>
 * Module for the connectionless UDP network protocol providing an implementation of an i/o stream
 * </p>
 *
 * @author Nick Garay
 * @since Aug 18, 2025
 */
public class UDP2CommProvider extends AbstractModule<UDP2CommProviderConfig> implements ICommProvider<UDP2CommProviderConfig> {

    static final Logger log = LoggerFactory.getLogger(UDP2CommProvider.class.getSimpleName());

    private static final int MAX_PACKET_SIZE = 65507;

    DatagramSocket socket;

    DatagramInputStream is;

    DatagramOutputStream os;

    Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public InputStream getInputStream() throws IOException {

        if (is == null) {

            throw new IOException("Not connected");
        }

        return is;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {

        if (os == null) {

            throw new IOException("Not connected");
        }

        return os;
    }

    @Override
    protected void doInit() throws SensorHubException {
        super.doInit();

        try {
            socket = new DatagramSocket(config.protocol.localPort);

        } catch (SocketException e) {

            throw new SensorHubException("Could not open UDP socket", e);
        }
    }

    @Override
    protected void doStart() throws SensorHubException {

        UDP2Config config = this.config.protocol;

        try {

            os = new DatagramOutputStream(socket,config.remoteHost, config.remotePort, MAX_PACKET_SIZE);

            is = new DatagramInputStream(socket, MAX_PACKET_SIZE);

            executor.execute(is);

        } catch (SocketException | UnknownHostException e) {

            throw new SensorHubException("Failed to create socket", e);
        }
    }

    @Override
    protected void doStop() throws SensorHubException {

        try {

            if (is != null) {

                is.close();
            }

            if (os != null) {

                os.close();
            }

            if (socket != null && socket.isConnected()) {

                socket.disconnect();

                socket.close();

                socket = null;
            }

        } catch (IOException e) {

            throw new SensorHubException("Failed to close stream", e);
        }
    }

}
