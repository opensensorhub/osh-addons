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
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jetty.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.Base64Encoder;


public class RTSPClient 
{
    static final Logger log = LoggerFactory.getLogger(RTSPClient.class);
    
    private final static String REQ_OPTIONS = "OPTIONS";
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
    
    boolean needAuth;
    boolean connected;
    String videoUrl;
    String userName;
    String passwd;
    String authHeader;
    Socket rtspSocket;
    BufferedReader rtspResponseReader;
    BufferedWriter rtspRequestWriter;
    int rtspSeqNb = 0;          // RTSP sequence number within the session
    String rtspSessionID = "0"; // ID of the RTSP session (given by the RTSP Server)
    int rtpRcvPort;             // port where the client will receive the RTP packets
    int streamIndex;
    
    // digest auth
    String digestRealm;
    String digestNonce;
    
    // info obtained from RTSP server
    int remoteRtpPort;
    int remoteRtcpPort;
    ArrayList<StreamInfo> mediaStreams;
    
    
    public class StreamInfo
    {
        String controlArg;
        public String codecString;
        public String paramSets;
        
        public String toString()
        {
            return controlArg + " (" + codecString + ")";
        }
    }
    
    
    public RTSPClient(String serverHost, int serverPort, String videoPath, String login, String passwd, int rtpRcvPort, int timeout) throws IOException
    {
        this.videoUrl = "rtsp://" + serverHost + ":" + serverPort + ((videoPath != null) ? videoPath : "");        
        this.userName = login;
        this.passwd = passwd;
        
        InetAddress rtspServerIP = InetAddress.getByName(serverHost);
        this.rtspSocket = new Socket();
        rtspSocket.connect(new InetSocketAddress(rtspServerIP, serverPort), timeout);
        rtspSocket.setSoTimeout(timeout); // read timeout
        
        this.rtspResponseReader = new BufferedReader(new InputStreamReader(rtspSocket.getInputStream()));
        this.rtspRequestWriter = new BufferedWriter(new OutputStreamWriter(rtspSocket.getOutputStream()));

        this.rtpRcvPort = rtpRcvPort;
        
        this.mediaStreams = new ArrayList<StreamInfo>();
    }
    
    
    public boolean isConnected()
    {
        return connected;
    }

    
    public void sendOptions() throws IOException
    {
        sendRequestAndParseResponse(REQ_OPTIONS);
    }
    
    
    public void sendDescribe() throws IOException
    {
        sendRequestAndParseResponse(REQ_DESCRIBE);
    }
    
    
    public void sendSetup() throws IOException
    {
        sendRequestAndParseResponse(REQ_SETUP);
    }
    
    
    public void sendPlay(int streamIndex) throws IOException
    {
        this.streamIndex = streamIndex;
        log.info("Playing Stream " + mediaStreams.get(streamIndex));
        sendRequestAndParseResponse(REQ_PLAY);
    }
    
    
    public void sendGetParameter() throws IOException
    {
        sendRequestAndParseResponse(REQ_GET_PARAMETER);
    }
    
    
    public void teardown() throws IOException
    {
        try
        {
            sendRequestAndParseResponse(REQ_TEARDOWN);
        }
        finally
        {
            try { rtspSocket.close(); }
            catch (IOException e) { }
        }        
    }
    
    
    private void sendRequestAndParseResponse(String requestType) throws IOException
    {
        boolean initAuth = needAuth;
        sendRequest(requestType);
        parseResponse(requestType);
        
        // retry once with auth
        if (!initAuth && needAuth)
        {
            sendRequest(requestType);
            parseResponse(requestType);
        }
    }        
    
    
    private void sendRequest(String requestType) throws IOException
    {
        log.trace("Sending " + requestType + " Request to " + videoUrl);
        rtspSeqNb++;
        
        // write the request line:
        rtspRequestWriter.write(requestType + " ");
        String requestUrl = videoUrl;
        if (requestType == REQ_SETUP)
        {
            String controlArg = mediaStreams.get(streamIndex).controlArg;
            if (controlArg.startsWith("rtsp://"))
                requestUrl = controlArg;
            else
                requestUrl = videoUrl + "/" + controlArg;            
        }
        rtspRequestWriter.write(requestUrl + " RTSP/1.0" + CRLF);

        // write the CSeq line: 
        rtspRequestWriter.write("CSeq: " + rtspSeqNb + CRLF);
        
        // add authenticate header if required
        addAuth(requestType, requestUrl);
        
        // depending on request type
        if (requestType == REQ_SETUP) {
            int rtcpPort = rtpRcvPort+1;
            rtspRequestWriter.write("Transport: RTP/AVP;unicast;client_port=" + rtpRcvPort + "-" + rtcpPort + CRLF);
        }
        else if (requestType == REQ_DESCRIBE) {
            rtspRequestWriter.write("Accept: application/sdp" + CRLF);
        }
        else { 
            if (rtspSessionID != "0") {
                rtspRequestWriter.write("Session: " + rtspSessionID + CRLF);
                log.trace("rtpsSessionId " + rtspSessionID);
            }
        }
        
        // end header and flush
        rtspRequestWriter.write(CRLF);
        rtspRequestWriter.flush();
    }
    
    
    private void addAuth(String requestType, String requestUrl) throws IOException
    {
        if (needAuth && userName != null && passwd != null)
        {
            if (digestRealm == null)
                addBasicAuth();
            else
                addDigestAuth(requestType, requestUrl);                
        }
    }
    
    
    private void addBasicAuth() throws IOException
    {
        String creds = userName + ":" + passwd;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64Encoder encoder = new Base64Encoder(baos);
        encoder.write(creds.getBytes());
        encoder.close();
        rtspRequestWriter.write("Authorization: Basic " + new String(baos.toByteArray()) + CRLF);
    }
    
    
    private void addDigestAuth(String method, String digestUri) throws IOException
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] ha1;
            
            // calc A1 digest
            md.update(userName.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(digestRealm.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(passwd.getBytes(StandardCharsets.ISO_8859_1));
            ha1 = md.digest();
            
            // calc A2 digest
            md.reset();
            md.update(method.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(digestUri.getBytes(StandardCharsets.ISO_8859_1));
            byte[] ha2 = md.digest();

            // calc response
            md.update(TypeUtil.toString(ha1, 16).getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');
            md.update(digestNonce.getBytes(StandardCharsets.ISO_8859_1));
            md.update((byte) ':');                
            // TODO add support for more secure version of digest auth
            //md.update(nc.getBytes(StandardCharsets.ISO_8859_1));
            //md.update((byte) ':');
            //md.update(cnonce.getBytes(StandardCharsets.ISO_8859_1));
            //md.update((byte) ':');
            //md.update(qop.getBytes(StandardCharsets.ISO_8859_1));
            //md.update((byte) ':');                
            md.update(TypeUtil.toString(ha2, 16).getBytes(StandardCharsets.ISO_8859_1));
            String response = TypeUtil.toString(md.digest(), 16);
            
            log.trace("username=\"{}\", realm=\"{}\", nonce=\"{}\", uri=\"{}\", response=\"{}\"",
                      userName, digestRealm, digestNonce, digestUri, response);
            
            // write authentication header
            rtspRequestWriter.write("Authorization: Digest");
            rtspRequestWriter.write(" username=\"" + userName + "\"");
            rtspRequestWriter.write(", realm=\"" + digestRealm + "\"");
            rtspRequestWriter.write(", nonce=\"" + digestNonce + "\"");
            rtspRequestWriter.write(", uri=\"" + digestUri + "\"");
            rtspRequestWriter.write(", response=\"" + response + "\"");
            rtspRequestWriter.write(CRLF);
            
        }
        catch (Exception e)
        {
            log.error("Cannot generate Digest Auth header", e);
        }
    }
    
    
    private void parseResponse(String requestType) throws IOException
    {
        try
        {
            String line = rtspResponseReader.readLine();
            log.trace("> {}", line);
            
            // read response code
            int respCode = Integer.parseInt(line.split(" ")[1]);
            
            // detect authentication request
            if (respCode == 401 && !needAuth && parseAuthType())
                return;
            
            // other errors
            if (respCode != 200)
            {
                printResponse();
                throw new IOException("RTSP Server Error: " + line);
            }
            
            // parse response according to request type
            if (requestType == REQ_DESCRIBE)
                parseDescribeResp();        
            else if (requestType == REQ_SETUP)
                parseSetupResp();
            else
                printResponse();
        }
        catch (IOException e)
        {
            connected = false;
            throw e;
        }
        catch (Exception e)
        {
            connected = false;
            throw new IOException("Invalid " + requestType + " response", e);
        }
        
        connected = true;
    }
    
    
    private boolean parseAuthType() throws IOException
    {
        String line;
        while ((line = rtspResponseReader.readLine()) != null)
        {
            if (line.length() == 0)
                break;
            else
                log.trace("> {}", line);
            
            if (line.startsWith("WWW-Authenticate:"))
            {
                if (line.contains("Digest"))
                {
                    int begin, end;
                    
                    begin = line.indexOf("realm=");
                    begin = line.indexOf('"', begin)+1;
                    end = line.indexOf('"', begin);
                    digestRealm = line.substring(begin, end);
                    
                    begin = line.indexOf("nonce=");
                    begin = line.indexOf('"', begin)+1;
                    end = line.indexOf('"', begin);
                    digestNonce = line.substring(begin, end);
                }
                else if (line.contains("Basic"))
                {
                    // nothing to do here
                }
                else
                    throw new IOException("Unsupported Authentication Method");
                
                needAuth = true;
            }
        }
        
        return needAuth;
    }
    
    
    private void parseDescribeResp() throws IOException
    {
        int contentLength = 0;
        BufferedReader reader = rtspResponseReader;
        
        // read header, then content
        String line;
        while ((line = reader.readLine()) != null)
        {
            // detect end of header
            if (line.length() == 0)
            {
                char[] cbuf = new char[contentLength];
                rtspResponseReader.read(cbuf);
                reader = new BufferedReader(new CharArrayReader(cbuf));
                continue;
            }
            
            log.trace("> {}", line);
            
            try
            {
                String[] tokens = line.split(":");
                String key = tokens[0];
                
                // header stuff
                if (key.equalsIgnoreCase("Content-Length"))
                {
                    contentLength = Integer.parseInt(tokens[1].trim());
                }
                
                // SDP content stuff
                else if (line.startsWith("m="))
                {
                    // add new media stream
                    mediaStreams.add(new StreamInfo());
                }
                else if (line.startsWith("a="))
                {
                    // get current stream index and ignore session level a= lines
                    int streamIndex = mediaStreams.size() - 1;
                    if (streamIndex < 0)
                        continue;
                    
                    StreamInfo stream = mediaStreams.get(streamIndex);
                    
                    if (line.startsWith("a=control:"))
                    {
                        stream.controlArg = line.substring(line.indexOf(':')+1);
                    }
                    else if (line.startsWith("a=rtpmap"))
                    {
                        stream.codecString = line.substring(line.indexOf(':')+1);                    
                    }
                    else if (line.startsWith("a=fmtp:96"))
                    {
                        for (String token: line.split(";"))
                        {
                            if (token.startsWith("sprop-parameter-sets"))
                            {
                                stream.paramSets = token.substring(token.indexOf('=')+1).trim();
                                break;
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                throw new IOException("Invalid DESCRIBE response", e);
            }
        }
    }
    
    
    private void parseSetupResp() throws IOException
    {
        String line;
        while ((line = rtspResponseReader.readLine()) != null)
        {
            if (line.length() == 0)
                break;
            else
                log.trace("> {}", line);
            
            try
            {
                if (line.startsWith("Session:"))
                {
                    rtspSessionID = line.split(":|;")[1];
                    log.trace(">> Session ID: {}", rtspSessionID.trim());
                }
                
                else if (line.startsWith("Transport:"))
                {
                    int serverPortIdx = line.indexOf("server_port=") + 12;
                    int endServerPortIdx = line.indexOf(';', serverPortIdx);
                    String serverPortString = endServerPortIdx < 0 ? line.substring(serverPortIdx) : line.substring(serverPortIdx, endServerPortIdx);
                    String[] ports = serverPortString.split("-");
                    remoteRtpPort = Integer.parseInt(ports[0].trim());
                    remoteRtcpPort = Integer.parseInt(ports[1].trim());
                    log.trace(">> Server ports: RTP {}, RTCP {}", remoteRtpPort, remoteRtcpPort);
                }
            }
            catch (Exception e)
            {
                throw new IOException("Invalid SETUP response", e);
            }
        }
    }
    
    
    private void printResponse() throws IOException
    {
        String line;
        while ((line = rtspResponseReader.readLine()) != null)
        {
            if (line.length() == 0)
                break;
            else
                log.trace("> {}", line);
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
    
    
    public Collection<StreamInfo> getMediaStreams()
    {
        return mediaStreams;
    }
    
    
    public void close() throws IOException
    {
        if (rtspSocket != null && rtspSocket.isConnected())
            rtspSocket.close();
    }
    
}
