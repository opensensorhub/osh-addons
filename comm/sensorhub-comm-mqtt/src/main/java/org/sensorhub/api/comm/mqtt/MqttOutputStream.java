/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm.mqtt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.vast.util.Asserts;


/**
 * <p>
 * Adapter output stream for sending data to an MQTT topic.<br/>
 * Data is buffered in a byte array, then packaged to an MQTT message and sent
 * to the topic when send() is called.
 * </p>
 *
 * @author Alex Robin
 * @since Jul 29, 2021
 */
public class MqttOutputStream extends ByteArrayOutputStream
{
    protected IMqttServer server;
    protected String topic;
    protected ByteBuffer buffer;
    protected boolean autoSendOnFlush;
    
    
    public MqttOutputStream(IMqttServer server, String topic, int bufferSize, boolean autoSendOnFlush)
    {
        super(bufferSize);
        this.server = Asserts.checkNotNull(server, IMqttServer.class);
        this.topic = topic;
        this.buffer = ByteBuffer.wrap(this.buf);
        this.autoSendOnFlush = autoSendOnFlush;
    }
    
    
    @Override
    public void close()
    {        
    }
    

    @Override
    public void flush() throws IOException
    {
        if (autoSendOnFlush)
            send();
    }
    
    
    public void send() throws IOException
    {
        // do nothing if no more bytes have been written since last call
        if (count == 0)
            return;
        
        // detect when buffer has grown
        if (count > buffer.capacity())
            this.buffer = ByteBuffer.wrap(this.buf);
        
        buffer.limit(count);
        server.publish(topic, buffer);
        //System.out.println("Sending " + count + " bytes");
        
        // reset so we can write again in same buffer
        this.reset();
        buffer.rewind();
    }

}
