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

import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Test;
import org.sensorhub.api.module.ModuleEvent;


public class TestJsscSerialCommProvider
{

    @Test
    public void testEchoAscii() throws Exception
    {
        JsscSerialCommProviderConfig config = new JsscSerialCommProviderConfig();
        config.protocol.baudRate = 115200;
        config.protocol.portName = "/dev/ttyUSB0";
        config.protocol.receiveThreshold = 0;        
        
        JsscSerialCommProvider serialComm = new JsscSerialCommProvider();
        serialComm.init(config);
        serialComm.start();
        serialComm.waitForState(ModuleEvent.ModuleState.STARTED, 5000);
        
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
        JsscSerialCommProviderConfig config = new JsscSerialCommProviderConfig();
        config.protocol.baudRate = 115200;
        config.protocol.portName = "/dev/ttyUSB0";
        config.protocol.receiveThreshold = 0;        
        
        JsscSerialCommProvider serialComm = new JsscSerialCommProvider();
        serialComm.init(config);
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
