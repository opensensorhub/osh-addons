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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * RTCP Sender
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 21, 2016
 */
public class RTCPSender extends TimerTask
{
    static final Logger log = LoggerFactory.getLogger(RTCPSender.class);
    String remoteRtcpHost;
    InetAddress remoteIp;
    int localRtcpPort;
    int reportingPeriod;
    DatagramSocket rtcpSocket;
    Timer timer;
    RTSPClient rtspClient;
    long lastRtspReq = 0;
    
    // stats variables
    int highSeqNb;
    int numPktsExpected;    // number of RTP packets expected since the last RTCP packet
    int numPktsLost;        // number of RTP packets lost since the last RTCP packet
    int lastHighSeqNb;      // last highest Seq number received
    int lastCumLost;        // last cumulative packets lost
    float lastFractionLost; // last fraction lost
    
    
    public RTCPSender(String remoteHost, int localRtcpPort, int remoteRtcpPort, int reportingPeriod, RTSPClient rtspClient)
    {
        this.remoteRtcpHost = remoteHost;
        this.localRtcpPort = localRtcpPort;
        this.reportingPeriod = reportingPeriod;
        this.rtspClient = rtspClient;
    }
    
    
    public void start()
    {
        try
        {
            this.remoteIp = InetAddress.getByName(remoteRtcpHost);
            
            // bind UDP port for sending and receiving RTCP packets
            rtcpSocket = new DatagramSocket(localRtcpPort);
            rtcpSocket.setReuseAddress(true);        
        
            timer = new Timer(RTCPSender.class.getSimpleName(), false);
            timer.scheduleAtFixedRate(this, 0, reportingPeriod);
        }
        catch (IOException e)
        {
            log.error("Error while starting RTCP sender thread", e);
        }
    }
    
    
    public void stop()
    {
        if (timer != null)
            timer.cancel();

        synchronized (rtcpSocket)
        {
            rtcpSocket.close();
        }
    }
    
    
    @Override
    public void run()
    {
        sendReport();
    }
    
    
    public void setStats(int seqNb)
    {
        this.highSeqNb = seqNb;
    }
    
    
    protected void sendReport()
    {
        // compute stats for this period
        //numPktsExpected = statHighSeqNum - lastHighSeqNb;
        //numPktsLost = statCumLost - lastCumLost;
        //lastFractionLost = randomGenerator.nextInt(10)/10.0f;
        //lastHighSeqNb = highSeqNb;
        //lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
        //lastCumLost = statCumLost;

        RTCPpacket rtcp_packet = new RTCPpacket(0, 0, highSeqNb);//lastFractionLost, statCumLost, highSeqNb);
        int packetLength = rtcp_packet.getLength();
        byte[] packetBits = new byte[packetLength];
        rtcp_packet.getpacket(packetBits);

        try
        {
            // send RTCP report packet
            synchronized (rtcpSocket)
            {
                DatagramPacket dp = new DatagramPacket(packetBits, packetLength, remoteIp, localRtcpPort);
                rtcpSocket.send(dp);
                log.trace("Sent RTCP report at seq number {}", highSeqNb);
            }
        }
        catch (IOException e)
        {
            log.error("Error while sending RTCP packet", e);
        }
        
        try
        {
            // also send a request to keep RTSP connection alive
            long now = System.currentTimeMillis();
            if (now - lastRtspReq > 10000)
            {
                //rtspClient.sendGetParameter();
                rtspClient.sendOptions();
                lastRtspReq = now;
            }
        }
        catch (IOException e)
        {
            log.trace("Error while sending RTSP keep-alive request", e);
        }
    }
}
