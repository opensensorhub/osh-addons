package org.sensorhub.api.comm.providers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

class DatagramInputStream extends ByteArrayInputStream implements Runnable {

    private final DatagramSocket socket;

    private final AtomicBoolean doWork = new AtomicBoolean(true);

    private final ByteBuffer buffer;

    private ByteBuffer currentBuffer;

    private final Queue<ByteBuffer> buffers = new ConcurrentLinkedQueue<>();

    public DatagramInputStream(final DatagramSocket socket, final ByteBuffer buffer) {

        super(buffer.array());

        this.buffer = buffer;

        this.socket = socket;
    }

    @Override
    public void close() throws IOException {

        super.close();

        doWork.set(false);
    }

    @Override
    public synchronized int read() {

        int value = -1;

        if (!buffers.isEmpty() && currentBuffer == null) {

            currentBuffer = buffers.remove();

            while (currentBuffer == null) {

                currentBuffer = buffers.remove();
            }

        } else {

            value = currentBuffer.get() & 0xFF;
        }

        return value;
    }

//        @Override
//        public int read(byte[] b) throws IOException {
//            return super.read(b);
//        }
//
//        @Override
//        public synchronized int read(byte[] b, int off, int len) {
//            return super.read(b, off, len);
//        }
//
//        @Override
//        public int readNBytes(byte[] b, int off, int len) {
//            return super.readNBytes(b, off, len);
//        }
//
//        @Override
//        public byte[] readNBytes(int len) throws IOException {
//            return super.readNBytes(len);
//        }
//
//        @Override
//        public synchronized byte[] readAllBytes() {
//            return super.readAllBytes();
//        }

    @Override
    public void run() {

        while (doWork.get()) {

            DatagramPacket receivePacket = new DatagramPacket(buffer.array(), buffer.capacity());

            try {

                socket.receive(receivePacket);

                buffers.add(ByteBuffer.wrap(safeClone(receivePacket.getData())));

            } catch (IOException e) {

                throw new RuntimeException(e);
            }

            reset();

            buffer.clear();

            Arrays.fill(buffer.array(), (byte) -1);
        }
    }

    private static byte[] safeClone(byte[] input) {

        return input == null ? null : input.clone();
    }
}
