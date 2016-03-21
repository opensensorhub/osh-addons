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
    
    // stats variables
    int highSeqNb;
    int numPktsExpected;    // number of RTP packets expected since the last RTCP packet
    int numPktsLost;        // number of RTP packets lost since the last RTCP packet
    int lastHighSeqNb;      // last highest Seq number received
    int lastCumLost;        // last cumulative packets lost
    float lastFractionLost; // last fraction lost
    
    
    public RTCPSender(String remoteHost, int localRtcpPort, int remoteRtcpPort, int reportingPeriod)
    {
        this.remoteRtcpHost = remoteHost;
        this.localRtcpPort = localRtcpPort;
        this.reportingPeriod = reportingPeriod;
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
        // calculate the stats for this period
//        numPktsExpected = statHighSeqNum - lastHighSeqNb;
//        numPktsLost = statCumLost - lastCumLost;
//        lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
//        lastHighSeqNb = statHighSeqNb;
//        lastCumLost = statCumLost;

        //To test lost feedback on lost packets
        // lastFractionLost = randomGenerator.nextInt(10)/10.0f;

        RTCPpacket rtcp_packet = new RTCPpacket(0, 0, highSeqNb);//lastFractionLost, statCumLost, statHighSeqNb);
        int packet_length = rtcp_packet.getlength();
        byte[] packet_bits = new byte[packet_length];
        rtcp_packet.getpacket(packet_bits);

        try
        {
            DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, remoteIp, localRtcpPort);
            rtcpSocket.send(dp);
            log.trace("Sent RTCP report at seq number {}", highSeqNb);
        }
        catch (IOException e)
        {
            log.error("Error while sending RTCP packet" + e);
        }
    }
}
