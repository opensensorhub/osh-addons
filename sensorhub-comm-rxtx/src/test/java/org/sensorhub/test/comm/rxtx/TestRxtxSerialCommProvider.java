/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.comm.rxtx;

import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Test;
import org.sensorhub.impl.comm.RS232Config;
import org.sensorhub.impl.comm.rxtx.RxtxSerialCommProvider;


public class TestRxtxSerialCommProvider
{

    @Test
    public void testEchoAscii() throws Exception
    {
        RS232Config serialConfig = new RS232Config();
        serialConfig.baudRate = 115200;
        serialConfig.portName = "/dev/ttyUSB0";
        serialConfig.receiveThreshold = 0;
        
        
        
        RxtxSerialCommProvider serialComm = new RxtxSerialCommProvider();
        serialComm.init(serialConfig);
        serialComm.start();
        
        OutputStream os = serialComm.getOutputStream();
        InputStream is = serialComm.getInputStream();
        
        for (int i = 0; i < 10; i++)
        {
            String msg = "hello\n";
            os.write(msg.getBytes());
            os.flush();
            System.out.println("Write: " + msg);
            
            byte[] buf = new byte[msg.length()];
            is.read(buf);
            System.out.println("Read: " + new String(buf));
            
            Thread.sleep(1000L);
        }
        
        serialComm.stop();
    }
    
    
    @Test
    public void testEchoBinary() throws Exception
    {
        RS232Config serialConfig = new RS232Config();
        serialConfig.baudRate = 115200;
        serialConfig.portName = "/dev/ttyUSB0";
        serialConfig.receiveThreshold = 0;        
        
        RxtxSerialCommProvider serialComm = new RxtxSerialCommProvider();
        serialComm.init(serialConfig);
        serialComm.start();
        
        OutputStream os = serialComm.getOutputStream();
        InputStream is = serialComm.getInputStream();
        
        for (int i = 0; i < 10; i++)
        {
            byte[] cmd = new byte[] {(byte)0xAA, 0x01, 0x20, 0x08};
            
            os.write(cmd);
            os.flush();
            
            while (is.available() > 0)
                System.out.print(String.format("%02X ", is.read()));
            System.out.println();
            
            Thread.sleep(1000L);
        }

        serialComm.stop();
    }

    
    
}
