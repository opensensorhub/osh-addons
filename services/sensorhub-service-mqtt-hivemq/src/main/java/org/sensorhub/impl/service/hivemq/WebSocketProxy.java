/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.hivemq;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.sensorhub.impl.service.WebSocketUtils;
import org.slf4j.Logger;


/**
 * <p>
 * Simple proxy to forward MQTT over websocket packets to a local MQTT TCP port
 * </p>
 *
 * @author Alex Robin
 * @since Jun 9, 2022
 */
public class WebSocketProxy implements WebSocketListener
{
    final InetSocketAddress mqttHost;
    final Logger log;
    Session session;
    volatile AsynchronousSocketChannel mqttSocket;
    ByteBuffer socketReadBuffer;
    CompletionHandler<Integer, Void> socketReadHandler;
    CompletionHandler<Integer, Void> socketWriteHandler;

    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean writeInProgress = new AtomicBoolean(false);
    
    
    WebSocketProxy(InetSocketAddress mqttHost, Logger logger)
    {
        this.mqttHost = mqttHost;
        this.log = logger;
    }


    @Override
    public void onWebSocketConnect(Session session)
    {
        try
        {
            this.session = session;
            WebSocketUtils.logOpen(session, log);
            
            mqttSocket = AsynchronousSocketChannel.open();
            mqttSocket.setOption(StandardSocketOptions.TCP_NODELAY, true);
            try
            {
                mqttSocket.connect(mqttHost).get();
            }
            catch (Exception e)
            {
                WebSocketUtils.closeSession(session, StatusCode.SERVER_ERROR, "Cannot connect to MQTT backend", log);
                return;
            }
            
            // prepare async socket read handler
            socketReadBuffer = ByteBuffer.allocate(1500); // max TCP MTU
            socketReadHandler = new CompletionHandler<>() {
                @Override
                public void completed(Integer readBytes, Void attachment)
                {
                    if (readBytes > 0)
                    {
                        try
                        {
                            socketReadBuffer.flip();
                            session.getRemote().sendBytes(socketReadBuffer);
                        }
                        catch (IOException e)
                        {
                            log.error("Error forwarding data to websocket");
                        }
                    }
                    
                    socketReadBuffer.clear();
                    readFromTcpSocket();
                }

                @Override
                public void failed(Throwable e, Void attachment)
                {
                    if (!(e instanceof AsynchronousCloseException))
                        log.error("Error reading data from MQTT TCP socket", e);
                }
            };
            
            // start TCP socket read loop
            readFromTcpSocket();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Error connecting to MQTT server", e);
        }
    }
    
    
    protected void readFromTcpSocket()
    {
        if (mqttSocket != null && mqttSocket.isOpen())
            mqttSocket.read(socketReadBuffer, null, socketReadHandler);
    }
    

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        try
        {
            if (mqttSocket != null)
            {
                mqttSocket.close();
                mqttSocket = null;
            }
            
            WebSocketUtils.logClose(session, statusCode, reason, log);
        }
        catch (IOException e)
        {
            log.error("Error closing connection to MQTT server", e);
        }
    }


    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (mqttSocket == null || !mqttSocket.isOpen())
            return;

        ByteBuffer buf = ByteBuffer.wrap(Arrays.copyOfRange(payload, offset, offset + len));

        writeQueue.add(buf);
        tryWrite();
    }

    private void tryWrite() {
        if (!writeInProgress.compareAndSet(false, true))
            return;

        ByteBuffer first = writeQueue.poll();
        if (first == null) {
            writeInProgress.set(false);
            return;
        }

        // Wrapper so the handler can mutate the current buffer
        class State {
            ByteBuffer current = first;
        }

        State state = new State();

        mqttSocket.write(state.current, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer result, Void attachment) {
                // Write the remaining bytes
                if (state.current.hasRemaining()) {
                    mqttSocket.write(state.current, null, this);
                    return;
                }

                // Get next buffer if finished
                ByteBuffer next = writeQueue.poll();
                if (next == null) {
                    writeInProgress.set(false);
                    return;
                }

                state.current = next;
                mqttSocket.write(state.current, null, this);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                writeInProgress.set(false);
                log.error("Error writing to MQTT TCP socket", exc);
            }
        });
    }



    @Override
    public void onWebSocketText(String message)
    {
        WebSocketUtils.closeSession(session, StatusCode.BAD_DATA, WebSocketUtils.INPUT_NOT_SUPPORTED, log);
    }


    @Override
    public void onWebSocketError(Throwable cause)
    {
        log.error("Internal websocket proxy error", cause);
    }

}
