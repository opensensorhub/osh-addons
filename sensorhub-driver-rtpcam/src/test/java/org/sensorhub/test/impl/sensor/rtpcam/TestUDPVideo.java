/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.impl.sensor.rtpcam;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JFrame;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.sensorhub.impl.sensor.rtpcam.RTPPacket;


public class TestUDPVideo
{
    static DatagramSocket socket;


    public static void main(String[] args) throws Exception
    {
        // connect to RTSP port to start video stream
        new Socket(InetAddress.getByName("10.1.1.1"), 5502);
                
        socket = new DatagramSocket(5600);
        //InetAddress.getByName("10.1.1.1"), 5600
        socket.setReuseAddress(true);
        socket.setReceiveBufferSize(1024*1024);
        System.out.println(socket.getReceiveBufferSize());

        final byte[] receiveData = new byte[1024 * 1024];
        final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        byte[] payload = new byte[1024*1024];
        
        final H264Decoder decoder = new H264Decoder();
        final Picture out = Picture.create(1920, 1088, ColorSpace.YUV420);
        final ByteBuffer dataBuf = ByteBuffer.allocate(1024*1024);
        
        final FileOutputStream fos = new FileOutputStream("/home/alex/test.h264");
        final FileChannel fch = fos.getChannel();
        
        final JFrame window = new JFrame();
        window.setSize(1080, 720);
        window.setVisible(true);
        boolean hasSps = false;
        boolean hasPps = false;
        
        int lastSeqNum = 0;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        while (true)
        {
            socket.receive(receivePacket);
            int length = receivePacket.getLength();
//            System.out.println(length + " bytes received");

            //create an RTPpacket object from the DP
            RTPPacket rtp_packet = new RTPPacket(receiveData, length);

            //print important header fields of the RTP packet received: 
//            System.out.println("Got RTP packet with SeqNum: " + rtp_packet.getsequencenumber() +
//                               ", TimeStamp: " + rtp_packet.gettimestamp() + "ms" +
//                               ", PayloadType: " + rtp_packet.getpayloadtype());

            //get the payload bitstream from the RTPpacket object
            int payload_length = rtp_packet.getPayload(payload);
            
            boolean discardNAL = false;
            if (lastSeqNum > 0 && rtp_packet.sequenceNumber > lastSeqNum+1) {
                System.err.println("Packet Lost");
                discardNAL = true;
            }
            lastSeqNum = rtp_packet.sequenceNumber;
            
            if (rtp_packet.payloadType == 96)
            {
                int packetType = (payload[0] & 0x1F);
                //System.out.println("Packet type = " + packetType);
                
                // case of fragmented packet (FU-4)
                if (packetType == 28)
                {
                    if (hasSps && hasPps)
                    {
                        int nalUnitType = payload[1] & 0x1F;
                        
                        if ((payload[1] & 0x80) != 0) // start of NAL unit
                        {
                            System.out.println("NAL unit type = " + nalUnitType);
                            dataBuf.put((byte)0x0);
                            dataBuf.put((byte)0x0);
                            dataBuf.put((byte)0x0);
                            dataBuf.put((byte)0x1);
                            dataBuf.put((byte)((payload[0] & 0xE0) + nalUnitType));
                        }
                        
                        dataBuf.put(payload, 2, payload_length-2);
                                                
                        if ((payload[1] & 0x40) != 0)  // end of NAL unit
                        {
                            if (!discardNAL)
                            {
                                dataBuf.flip();
                                final ByteBuffer newBuf = ByteBuffer.allocate(dataBuf.limit());
                                newBuf.put(dataBuf);
                                newBuf.flip();
                                
                                executor.execute(new Runnable() {
                                    public void run()
                                    {
                                        try
                                        {
                                            fch.write(newBuf);
                                            fos.flush();
                                            /*newBuf.rewind();
                                            
                                            Picture real = decoder.decodeFrame(newBuf, out.getData());
                                            BufferedImage bi = AWTUtil.toBufferedImage(real);
                                            window.getContentPane().getGraphics().drawImage(bi, 0, 0, null);*/
                                        }
                                        catch (Exception e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                            
                            discardNAL = false;
                            dataBuf.clear();
                        }
                    }
                }
                
                // case of single NAL unit directly as payload (for SPS and PPS)
                else if (packetType <= 23)
                {
                    int nalUnitType = packetType;
                    //System.out.println("NAL unit type = " + (payload[0] & 0x1F));
                    
                    dataBuf.put((byte)0x0);
                    dataBuf.put((byte)0x0);
                    dataBuf.put((byte)0x0);
                    dataBuf.put((byte)0x1);
                    dataBuf.put(payload, 0, payload_length);
                    dataBuf.flip();
                    
                    if (nalUnitType == 7)
                    {
                        fch.write(dataBuf);
                        dataBuf.rewind();
                        
                        dataBuf.position(5);
                        decoder.addSps(Arrays.asList(dataBuf));
                        hasSps = true;
                    }
                    else if (nalUnitType == 8)
                    {
                        fch.write(dataBuf);
                        dataBuf.rewind();
                        
                        dataBuf.position(5);
                        decoder.addPps(Arrays.asList(dataBuf));
                        hasPps = true;
                    }
                    
                    dataBuf.clear();
                }
            }
        }
    }

}
