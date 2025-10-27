package com.botts.impl.comm.jssc;

import jssc.SerialPort;
import jssc.SerialPortException;

import java.io.IOException;
import java.io.InputStream;

public class SerialInputStream extends InputStream {

    private final SerialPort port;

    protected SerialInputStream(SerialPort port) {
        this.port = port;
    }

    @Override
    public int read() throws IOException {
        try {
            byte[] data = port.readBytes(1);
            return (data == null || data.length == 0 ? -1 : (data[0] & 0xFF));
        } catch (SerialPortException e) {
            throw new IOException("Failed to read from serial port", e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            byte[] data = port.readBytes(len);
            if (data == null) return -1;
            System.arraycopy(data, 0, b, off, data.length);
            return data.length;
        } catch (SerialPortException e) {
            throw new IOException("Failed to read from serial port", e);
        }
    }

}
