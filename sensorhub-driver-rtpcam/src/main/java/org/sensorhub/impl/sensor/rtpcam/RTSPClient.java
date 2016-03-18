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
import org.vast.swe.Base64Encoder;


public class RTSPClient 
{
    private final static String REQ_DESCRIBE = "DESCRIBE";
    private final static String REQ_SETUP = "SETUP";
    private final static String REQ_PLAY = "PLAY";
    //private final static String REQ_PAUSE = "PAUSE";
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
    int rtspSeqNb = 0; // RTSP sequence number within the session
    String rtspSessionID = "0"; // ID of the RTSP session (given by the RTSP Server)
    int rtpRcvPort = 25000; // port where the client will receive the RTP packets
    
    
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

    
    public void describe() throws IOException
    {
        send_RTSP_request(REQ_DESCRIBE);
        parse_server_response();
    }
    
    
    public void setup() throws IOException
    {
        send_RTSP_request(REQ_SETUP);
        parse_server_response();
    }
    
    
    public void play() throws IOException
    {
        send_RTSP_request(REQ_PLAY);
        parse_server_response();
    }
    
    
    public void teardown() throws IOException
    {
        try
        {
            send_RTSP_request(REQ_TEARDOWN);
            parse_server_response();
        }
        finally
        {
            try { rtspSocket.close(); }
            catch (IOException e) { }
        }        
    }
    

    private void send_RTSP_request(String request_type) throws IOException
    {
        rtspSeqNb++;
        
        //Use the RTSPBufferedWriter to write to the RTSP socket
        RTPCameraDriver.log.debug("Sending " + request_type + " Request");
        
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
            rtspRequestWriter.write("Transport: RTP/AVP;unicast;client_port=" + rtpRcvPort + CRLF);
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
    
    
    // Parse Server Response
    private void parse_server_response() throws IOException
    {
        int respCode = 0;

        String line;
        while ((line = rtspResponseReader.readLine()) != null)
        {
            if (line.startsWith("RTSP/"))
                respCode = Integer.parseInt(line.split(" ")[1]);
            else if (line.startsWith("Session"))
                rtspSessionID = line.split(" |;")[1];
            
            if (line.length() == 0)
                break;
            else
                RTPCameraDriver.log.trace(line);
        }
      
        if (respCode != 200)
            throw new IOException("RTSP Server Error: " + respCode);
    }
}
