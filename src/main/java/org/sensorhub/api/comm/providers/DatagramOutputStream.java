package org.sensorhub.api.comm.providers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

class DatagramOutputStream extends ByteArrayOutputStream {

    private final DatagramSocket socket;

    DatagramOutputStream(DatagramSocket socket, int bufferSize) {

        super(bufferSize);
        this.socket = socket;
    }

    @Override
    public void flush() throws IOException {

        socket.send(new DatagramPacket(buf, 0, buf.length));

        super.flush();

        reset();
    }

    @Override
    public void close() throws IOException {

        super.close();
    }
}
