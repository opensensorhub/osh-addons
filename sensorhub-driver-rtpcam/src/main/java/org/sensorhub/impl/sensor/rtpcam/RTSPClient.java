/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtpcam;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.Base64Encoder;


public class RTSPClient 
{
    static final Logger log = LoggerFactory.getLogger(RTSPClient.class);
    
    private final static String REQ_DESCRIBE = "DESCRIBE";
    private final static String REQ_SETUP = "SETUP";
    private final static String REQ_PLAY = "PLAY";
    //private final static String REQ_PAUSE = "PAUSE";
    private final static String REQ_GET_PARAMETER = "GET_PARAMETER";
    private final static String REQ_TEARDOWN = "TEARDOWN";   
    final static String CRLF = "\r\n";
    final static int INIT = 0;
    final static int READY = 1;
    final static int PLAYING = 2;
    
    int state;
    String videoUrl;
    String userName;
    String passwd;
    Socket rtspSocket;
    BufferedReader rtspResponseReader;
    BufferedWriter rtspRequestWriter;
    int rtspSeqNb = 0;          // RTSP sequence number within the session
    String rtspSessionID = "0"; // ID of the RTSP session (given by the RTSP Server)
    int rtpRcvPort;             // port where the client will receive the RTP packets
    int remoteRtpPort;
    int remoteRtcpPort;
    
    
    public RTSPClient(String serverHost, int serverPort, String videoPath, String login, String passwd, int rtpRcvPort) throws IOException
    {
        this.videoUrl = "rtsp://" + serverHost + ":" + serverPort + ((videoPath != null) ? videoPath : "");
        this.userName = login;
        this.passwd = passwd;
        
        InetAddress rtspServerIP = InetAddress.getByName(serverHost);
        this.rtspSocket = new Socket(rtspServerIP, serverPort);
        rtspSocket.setSoTimeout(1000);
        
        this.rtspResponseReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
        this.rtspRequestWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));

        this.rtpRcvPort = rtpRcvPort;
        this.state = INIT;
    }

    
    public void sendDescribe() throws IOException
    {
        sendRequest(REQ_DESCRIBE);
        parseResponse(REQ_DESCRIBE);
    }
    
    
    public void sendSetup() throws IOException
    {
        sendRequest(REQ_SETUP);
        parseResponse(REQ_SETUP);
    }
    
    
    public void sendPlay() throws IOException
    {
        sendRequest(REQ_PLAY);
        parseResponse(REQ_PLAY);
    }
    
    
    public void sendGetParameter() throws IOException
    {
        sendRequest(REQ_GET_PARAMETER);
        parseResponse(REQ_GET_PARAMETER);
    }
    
    
    public void teardown() throws IOException
    {
        try
        {
            sendRequest(REQ_TEARDOWN);
            parseResponse(REQ_TEARDOWN);
        }
        finally
        {
            try { rtspSocket.close(); }
            catch (IOException e) { }
        }        
    }
    
    
    public int getRemoteRtpPort()
    {
        return remoteRtpPort;
    }
    
    
    public int getRemoteRtcpPort()
    {
        return remoteRtcpPort;
    }
    

    private void sendRequest(String request_type) throws IOException
    {
        rtspSeqNb++;
        
        //Use the RTSPBufferedWriter to write to the RTSP socket
        log.trace("Sending " + request_type + " Request");
        
        //write the request line:
        String control = "";
        if (request_type == REQ_SETUP)
            control = "/trackID=0";
        rtspRequestWriter.write(request_type + " " + videoUrl + control + " RTSP/1.0" + CRLF);

        //write the CSeq line: 
        rtspRequestWriter.write("CSeq: " + rtspSeqNb + CRLF);
        addAuth();
        
        //check if request_type is equal to "SETUP" and in this case write the 
        //Transport: line advertising to the server the port used to receive 
        //the RTP packets RTP_RCV_PORT
        if (request_type == REQ_SETUP) {
            int rtcpPort = rtpRcvPort+1;
            rtspRequestWriter.write("Transport: RTP/AVP;unicast;client_port=" + rtpRcvPort + "-" + rtcpPort + CRLF);
        }
        else if (request_type == REQ_DESCRIBE) {
            rtspRequestWriter.write("Accept: application/sdp" + CRLF);
        }
        else {
            //otherwise, write the Session line from the RTSPid field
            rtspRequestWriter.write("Session: " + rtspSessionID + CRLF);
        }
        
        rtspRequestWriter.write(CRLF);
        rtspRequestWriter.flush();
    }
    
    
    private void addAuth() throws IOException
    {
        if (userName != null && passwd != null)
        {
            String creds = userName + ":" + passwd;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Base64Encoder encoder = new Base64Encoder(baos);
            encoder.write(creds.getBytes());
            encoder.close();
            rtspRequestWriter.write("Authorization: Basic " + new String(baos.toByteArray()) + CRLF);
        }
    }
    
    
    private void parseResponse(String reqType) throws IOException
    {
        int respCode = 0;

        String line;
        while ((line = rtspResponseReader.readLine()) != null)
        {
            if (line.startsWith("RTSP/"))
                respCode = Integer.parseInt(line.split(" ")[1]);
            else if (line.startsWith("Session"))
                rtspSessionID = line.split(" |;")[1];
            
            if (reqType == REQ_SETUP && line.startsWith("Transport:"))
            {
                try
                {
                    int serverPortIdx = line.indexOf("server_port=") + 12;
                    String serverPortString = line.substring(serverPortIdx, line.indexOf(';', serverPortIdx));
                    String[] ports = serverPortString.split("-");
                    this.remoteRtpPort = Integer.parseInt(ports[0]);
                    this.remoteRtcpPort = Integer.parseInt(ports[1]);
                    log.debug("Server ports: RTP {}, RTCP {}", remoteRtpPort, remoteRtcpPort);
                }
                catch (Exception e)
                {
                    throw new IOException("Invalid SETUP response");
                }
            }
            
            if (line.length() == 0)
                break;
            else
                log.trace(line);
        }
      
        if (respCode != 200)
            throw new IOException("RTSP Server Error: " + respCode);
    }
}
