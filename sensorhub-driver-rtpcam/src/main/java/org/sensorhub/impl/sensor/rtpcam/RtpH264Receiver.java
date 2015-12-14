/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.rtpcam;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Demuxer for H264 streams received via RTP (RFC6184).<br/>
 * Only single NAL unit packets and FU-A fragmentation units are supported.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 14, 2015
 */
public class RtpH264Receiver extends Thread
{
    static final Logger log = LoggerFactory.getLogger(RtpH264Receiver.class);
    static final int MAX_DATAGRAM_SIZE = 64*1024;
    static final int MAX_FRAME_SIZE = 1024*1024;
    static final byte[] NAL_UNIT_MARKER = new byte[] {0x0, 0x0, 0x0, 0x1};
    static final int SINGLE_NALU_PACKET_TYPE = 23;
    static final int FU4_PACKET_TYPE = 28;
    
    String remoteHost;
    int remotePort;
    int localPort;
    Socket rtspSocket;
    DatagramSocket rtpSocket;
    volatile boolean started;
    RtpH264Callback callback;
    
    
    public RtpH264Receiver(String remoteHost, int remotePort, int localPort, RtpH264Callback callback)
    {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.callback = callback;
    }
    
    
    public void run()
    {
        try
        {
            // connect to RTSP port to start video stream
            rtspSocket = new Socket(InetAddress.getByName(remoteHost), remotePort);
            
            // bind UDP port for receiving RTP packets
            rtpSocket = new DatagramSocket(localPort);
            rtpSocket.setReuseAddress(true);
            rtpSocket.setReceiveBufferSize(MAX_DATAGRAM_SIZE);      

            final byte[] receiveData = new byte[MAX_DATAGRAM_SIZE];
            final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            final byte[] payload = new byte[MAX_DATAGRAM_SIZE];            
            final ByteBuffer dataBuf = ByteBuffer.allocate(MAX_FRAME_SIZE);
            
            boolean hasSps = false;
            boolean hasPps = false;
            boolean discardNAL = false;
            int lastSeqNum = 0;
            
            while (started)
            {
                rtpSocket.receive(receivePacket);
                int length = receivePacket.getLength();
                    
                // create an RTPpacket object from the UDP payload
                RTPPacket rtpPacket = new RTPPacket(receiveData, length);
                if (log.isTraceEnabled())
                {
                    log.trace("RTP packet: seqNum=" + rtpPacket.getSequenceNumber() +
                              ", timeStamp=" + rtpPacket.getTimeStamp() +
                              ", payloadType=" + rtpPacket.getPayloadType());
                }
    
                // get the payload bitstream from the RTPpacket object
                int payload_length = rtpPacket.getPayload(payload);
                
                // to discard entire NAL unit when a packet is lost
                if (lastSeqNum > 0 && rtpPacket.sequenceNumber > lastSeqNum+1) {
                    log.trace("Packet Lost");
                    discardNAL = true;
                }
                lastSeqNum = rtpPacket.sequenceNumber;
                
                if (rtpPacket.payloadType == 96)
                {
                    int packetType = (payload[0] & 0x1F);
                    log.trace("H264 RTP packet type = {}", packetType);
                    
                    // case of fragmented packet (FU-4)
                    if (packetType == FU4_PACKET_TYPE)
                    {
                        if (hasSps && hasPps)
                        {
                            int nalUnitType = payload[1] & 0x1F;
                            
                            // if start of NAL unit
                            if ((payload[1] & 0x80) != 0) 
                            {
                                log.trace("FU-4: Start NAL unit, type = {}", nalUnitType);
                                dataBuf.put(NAL_UNIT_MARKER);
                                dataBuf.put((byte)((payload[0] & 0xE0) + nalUnitType));
                            }
                            
                            // copy NAL payload
                            dataBuf.put(payload, 2, payload_length-2);
                                    
                            // if end of NAL unit
                            if ((payload[1] & 0x40) != 0)
                            {
                                log.trace("FU-4: End NAL unit, type = {}", nalUnitType);
                                
                                if (!discardNAL)
                                {
                                    dataBuf.flip();
                                    callback.onFrame(rtpPacket.getTimeStamp() & 0xFFFFFFFF, dataBuf, discardNAL);
                                    
                                    // copy buffer
                                    final ByteBuffer newBuf = ByteBuffer.allocate(dataBuf.limit());
                                    newBuf.put(dataBuf);
                                    newBuf.flip();
                                }
                                else
                                    log.trace("FU-4: Discarded");
                                
                                discardNAL = false;
                                dataBuf.clear();
                            }
                        }
                    }
                    
                    // case of single NAL unit directly as payload (for SPS and PPS)
                    else if (packetType <= SINGLE_NALU_PACKET_TYPE)
                    {
                        int nalUnitType = packetType;
                        log.trace("Single NAL unit, type = " + packetType);
                        
                        dataBuf.put(NAL_UNIT_MARKER);
                        dataBuf.put(payload, 0, payload_length);
                        
                        // mark when SPS and PPS are received
                        if (nalUnitType == 7)
                            hasSps = true;
                        else if (nalUnitType == 8)
                            hasPps = true;
                    }
                }
            }
        }
        catch (Throwable e)
        {
            log.error("Error while demuxing H264 RTP stream", e);
        }
    }
    
    
    @Override
    public void start()
    {
        started = true;
        super.start();
    }
    
    
    @Override
    public void interrupt()
    {
        started = false;
        super.interrupt();
    }
}
