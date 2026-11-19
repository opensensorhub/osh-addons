/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
 Developer are Copyright (C) 2025 the Initial Developer. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

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
