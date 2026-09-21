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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * <p>
 * Output stream implementation wrapping a UDP Socket. This implementation is distinct in that it does not rely on a
 * socket channel implementation for acquiring the output stream.
 * </p>
 *
 * @author Nick Garay
 * @since Aug 18, 2025
 */
class DatagramOutputStream extends ByteArrayOutputStream {

    private final DatagramSocket socket;

    private final InetAddress address;

    private final int port;

    private int numBytesWritten = 0;

    public DatagramOutputStream(final DatagramSocket socket, String remoteHost, int remotePort, int bufferSize) throws UnknownHostException {

        super(bufferSize);

        this.socket = socket;

        address = InetAddress.getByName(remoteHost);

        port = remotePort;
    }

    @Override
    public synchronized void write(final byte[] data) throws IOException {

        numBytesWritten = data.length;

        super.write(data);
    }

    @Override
    public void flush() throws IOException {

        socket.send(new DatagramPacket(buf, 0, numBytesWritten, address, port));

        // Clear the buffer
        Arrays.fill(buf, (byte) 0);

        super.flush();

        reset();
    }

    @Override
    public void close() throws IOException {

        super.close();
    }
}
