package com.botts.impl.comm.jssc;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;
import java.io.OutputStream;

public class SerialOutputStream extends OutputStream {

    private final SerialPort port;

    protected SerialOutputStream(SerialPort port) {
        this.port = port;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] chunk = new byte[len];
        System.arraycopy(b, off, chunk, 0, len);
        try {
            port.writeBytes(chunk);
        } catch (SerialPortException e) {
            throw new IOException("Failed to write to serial port", e);
        }
    }

}
