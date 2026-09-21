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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * Input stream implementation wrapping a UDP Socket. This implementation is distinct in that it does not rely on a
 * socket channel implementation for acquiring the input stream.
 * </p>
 *
 * @author Nick Garay
 * @since Aug 18, 2025
 */
class DatagramInputStream extends InputStream implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DatagramInputStream.class);

    private static final byte EOS = -1;

    private final Object lock = new Object();

    private final DatagramSocket socket;

    private final AtomicBoolean doWork = new AtomicBoolean(true);

    private final Queue<byte[]> buffers = new ConcurrentLinkedQueue<>();

    private byte[] currentBuffer;

    private int bufferIndex = 0;

    int maxBufferSize;

    public DatagramInputStream(final DatagramSocket socket, int bufferSize) throws SocketException {

        maxBufferSize = bufferSize;

        this.socket = socket;
    }

    @Override
    public void close() throws IOException {

        doWork.set(false);

        buffers.clear();
    }

    @Override
    public int read() {

        int value = EOS;

        synchronized (lock) {

            while (currentBuffer == null && doWork.get()) {

                try {

                    if (buffers.isEmpty()) {

                        lock.wait();
                    }

                    currentBuffer = buffers.poll();

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    logger.error("Read interrupted", e);
                }
            }
        }

        if (currentBuffer != null && bufferIndex < currentBuffer.length) {

            value = currentBuffer[bufferIndex++];

            if (bufferIndex >= currentBuffer.length) {

                currentBuffer = null;

                bufferIndex = 0;
            }
        }

        return value;
    }

    @Override
    public void run() {

        byte[] packetBuffer = new byte[maxBufferSize];

        while (doWork.get()) {

            Arrays.fill(packetBuffer, (byte) 0);

            DatagramPacket receivePacket = new DatagramPacket(packetBuffer, maxBufferSize);

            try {

                socket.receive(receivePacket);

                synchronized (lock) {

                    buffers.add(Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength()));

                    lock.notifyAll();
                }

            } catch (IOException e) {

                if (!socket.isClosed()) {

                    logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
