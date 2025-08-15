/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm.providers;

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * <p>
 * Communication provider for UDP links
 * </p>
 *
 * @author Alex Robin
 * @since Dec 12, 2015
 */
public class UDP2CommProvider extends AbstractModule<UDP2CommProviderConfig> implements ICommProvider<UDP2CommProviderConfig> {

    static final Logger log = LoggerFactory.getLogger(UDP2CommProvider.class.getSimpleName());

    private static final int MAX_PACKET_SIZE = 65507;

    DatagramSocket sendSocket;

    DatagramSocket receiveSocket;

    DatagramInputStream is;

    DatagramOutputStream os;

    Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public InputStream getInputStream() throws IOException {
        return is;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return os;
    }

    @Override
    protected void doStart() throws SensorHubException {

        UDP2Config config = this.config.protocol;

        try {

            sendSocket = new DatagramSocket(new InetSocketAddress(config.remoteHost, config.remotePort));

            os = new DatagramOutputStream(sendSocket, MAX_PACKET_SIZE);

            receiveSocket = new DatagramSocket(new InetSocketAddress(config.localAddress, config.localPort));

            is = new DatagramInputStream(receiveSocket, ByteBuffer.allocate(MAX_PACKET_SIZE));

            executor.execute((Runnable) receiveSocket);

        } catch (SocketException e) {

            throw new SensorHubException("failed to create socket for streaming", e);
        }
    }

    @Override
    protected void doStop() throws SensorHubException {

        try {

            is.close();

            os.close();

            receiveSocket.close();

            sendSocket.close();

        } catch (IOException e) {

            throw new SensorHubException("Failed to close stream", e);
        }
    }

}
