/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class TcpRepeater
{
    List<Socket> clientSockets = new ArrayList<Socket>();
    boolean started;
    ServerSocket server;
    
    Object sync = new Object();
    volatile boolean sending = false;
    byte[] msgBytes;
    
    TcpRepeater(int port) throws IOException
    {
        // create connection receive thread
        server = new ServerSocket(port);
        started = true;
        
        Thread listenThread = new Thread() {
            public void run() 
            {
                while (started)
                {
                    try
                    {
                        Socket s = server.accept();
                        clientSockets.add(s);
                    }
                    catch (IOException e)
                    {
                        System.err.println("Cannot accept client connection");
                        break;
                    }
                }
            }
        };
        listenThread.start();
        
        Thread sendThread = new Thread() {
            public void run() 
            {
                while (started)
                {
                    synchronized(sync)
                    {
                        try { sync.wait(); }
                        catch (InterruptedException e) { return; }
                        sending = true;
                    }
                    
                    Iterator<Socket> it = clientSockets.iterator();
                    while (it.hasNext())
                    {
                        try
                        {
                            Socket s = it.next();
                            if (s.isClosed())
                            {
                                it.remove();
                            }
                            else if (s.isConnected())
                            {
                                s.getOutputStream().write(MtiOutput.PREAMBLE & 0xFF);
                                s.getOutputStream().write(msgBytes);
                                s.getOutputStream().flush();
                            }
                        }
                        catch (IOException e)
                        {
                        }
                    }
                    
                    sending = false;
                }
            }
        };
        sendThread.start();
    }
    
    
    public void sendMessage(byte[] msgBytes) throws IOException
    {
        synchronized(sync)
        {
            if (!sending)
            {
                this.msgBytes = msgBytes.clone();
                sync.notify();
            }
        }
    }
    
    
    public void stop()
    {
        try
        {
            started = false;
            server.close();
            for (Socket s: clientSockets)
                s.close();
            clientSockets.clear();
        }
        catch (IOException e)
        {            
        }
    }
}
