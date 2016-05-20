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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.Base64Decoder;


/**
 * <p>
 * Demuxer for H264 streams received via RTP (RFC6184).<br/>
 * Only single NAL unit packets and FU-A fragmentation units are supported.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 14, 2015
 */
public class RTPH264Receiver extends Thread
{
    static final Logger log = LoggerFactory.getLogger(RTPH264Receiver.class);
    static final int MAX_DATAGRAM_SIZE = 64*1024;
    static final int MAX_FRAME_SIZE = 1024*1024;
    static final byte[] NAL_UNIT_MARKER = new byte[] {0x0, 0x0, 0x0, 0x1};
    static final int SINGLE_NALU_PACKET_TYPE = 23;
    static final int FU4_PACKET_TYPE = 28;
    static final int STAPA_PACKET_TYPE = 24;
    static final int STAPB_PACKET_TYPE = 25;
    static final int MTAP16_PACKET_TYPE = 26;
    static final int MTAP24_PACKET_TYPE = 27;
    static final int NALU_DELTAFRAME = 1;
    static final int NALU_KEYFRAME = 5;
    static final int NALU_SPS = 7;
    static final int NALU_PPS = 8;
    
    String remoteHost;
    int localPort;
    DatagramSocket rtpSocket;
    volatile boolean started;    
    RTPH264Callback callback;
    boolean spsReceived = false;
    boolean ppsReceived = false;
    boolean injectParamSets = false;
    byte[] sps, pps;
    
    
    public RTPH264Receiver(String remoteHost, int localPort, RTPH264Callback callback)
    {
        super(RTPH264Receiver.class.getSimpleName());
        this.remoteHost = remoteHost;
        this.localPort = localPort;
        this.callback = callback;
    }
    
    
    public void setParameterSets(String paramSetsSDP)
    {
        try
        {
            // string contains SPS then PPS separated by comma
            String[] params = paramSetsSDP.split(",");
            String sps = params[0];
            String pps = params[1];        
        
            // decode base64 parameter sets
            this.sps = decodeBase64(sps);
            this.pps = decodeBase64(pps);
            this.injectParamSets = true;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Invalid H264 parameter sets", e);
        }
    }
    
    
    private byte[] decodeBase64(String s) throws IOException
    {
        byte[] res = new byte[s.length()*3/4];
        InputStream is = new ByteArrayInputStream(s.getBytes());
        Base64Decoder decoder = new Base64Decoder(is);
        decoder.read(res);
        decoder.close();
        return res;
    }
    
    
    public void run()
    {
        try
        {
            // bind UDP port for receiving RTP packets
            rtpSocket = new DatagramSocket(localPort);
            rtpSocket.setReuseAddress(true);
            rtpSocket.setReceiveBufferSize(MAX_DATAGRAM_SIZE);      

            final byte[] receiveData = new byte[MAX_DATAGRAM_SIZE];
            final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            final byte[] payload = new byte[MAX_DATAGRAM_SIZE];            
            final ByteBuffer dataBuf = ByteBuffer.allocate(MAX_FRAME_SIZE);
            
            boolean discardNAL = false;
            int lastSeqNum = 0;
            
            while (started)
            {
                rtpSocket.receive(receivePacket);
                    
                // create an RTPpacket object from the UDP payload
                int length = receivePacket.getLength();
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
                        int nalUnitType = payload[1] & 0x1F;
                        boolean startNalUnit = (payload[1] & 0x80) != 0;
                        
                        if (injectParamSets && startNalUnit)
                        {
                            // inject SPS and PPS before key frame
                            if (nalUnitType == NALU_KEYFRAME)
                            {
                                log.trace("Injecting SPS and PPS NAL units");
                                dataBuf.put(NAL_UNIT_MARKER);
                                dataBuf.put(sps);
                                dataBuf.put(NAL_UNIT_MARKER);
                                dataBuf.put(pps);
                                spsReceived = true;
                                ppsReceived = true;
                            }
                        }
                        
                        if (spsReceived && ppsReceived)
                        {
                            // if start of NAL unit
                            if (startNalUnit) 
                            {
                                log.trace("FU-4: Start NAL unit, type = {}", nalUnitType);
                                dataBuf.put(NAL_UNIT_MARKER);
                                dataBuf.put((byte)((payload[0] & 0xE0) + nalUnitType));
                            }
                            
                            // copy NAL fragment
                            dataBuf.put(payload, 2, payload_length-2);
                                    
                            // if end of NAL unit
                            if ((payload[1] & 0x40) != 0)
                            {
                                log.trace("FU-4: End NAL unit, type = {}", nalUnitType);
                                
                                if (!discardNAL)
                                {
                                    dataBuf.flip();
                                    callback.onFrame(rtpPacket.getTimeStamp() & 0xFFFFFFFF, rtpPacket.getSequenceNumber(), dataBuf, discardNAL);
                                    
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
                    
                    // single time aggregation units
                    else if (packetType == STAPA_PACKET_TYPE)
                    {
                        int index = 1;
                        while (index+1 < payload_length)
                        {
                            int nalSize = ((payload[index] & 0xFF) << 8) | (payload[index+1] & 0xFF);
                            if (nalSize == 0)
                                break;
                            
                            index += 2;
                            int nalUnitType = payload[index] & 0x1F;
                            log.trace("STAP NAL unit, type = " + nalUnitType);
                            
                            // write nal unit to buffer with a marker
                            dataBuf.put(NAL_UNIT_MARKER);
                            dataBuf.put(payload, index, nalSize);                            
                            index += nalSize;
                            
                            // mark when SPS and PPS are received
                            if (nalUnitType == NALU_SPS)
                                spsReceived = true;
                            else if (nalUnitType == NALU_PPS)
                                ppsReceived = true;
                        }
                    }
                    
                    // case of single NAL unit directly as payload
                    else if (packetType <= SINGLE_NALU_PACKET_TYPE)
                    {
                        int nalUnitType = packetType;
                        log.trace("Single NAL unit, type = " + packetType);
                        
                        dataBuf.put(NAL_UNIT_MARKER);
                        dataBuf.put(payload, 0, payload_length);
                        
                        // mark when SPS and PPS are received
                        if (nalUnitType == NALU_SPS)
                            spsReceived = true;
                        else if (nalUnitType == NALU_PPS)
                            ppsReceived = true;
                    }
                }
            }
        }
        catch (Throwable e)
        {
            if (started)
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
        rtpSocket.close();
    }
}
