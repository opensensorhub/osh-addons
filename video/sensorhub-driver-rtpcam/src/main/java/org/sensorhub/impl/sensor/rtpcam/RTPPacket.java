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


/**
 * <p>
 * Implementation of an RTP packet
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 14, 2015
 */
public class RTPPacket implements Comparable<RTPPacket>
{
    // size of the RTP header:
    static final int HEADER_SIZE = 12;

    // fields that compose the RTP header
    public int version;
    public int padding;
    public int extension;
    public int CC;
    public int marker;
    public int payloadType;
    public int sequenceNumber;
    public long expandedSequenceNumber;
    public int timeStamp;
    public int ssrc;

    // bitstream of the RTP header
    public byte[] header;

    // size of the RTP payload
    public int payloadSize;
    
    // bitstream of the RTP payload
    public byte[] payload;


    /**
     * Constructor of an RTPpacket object from header fields and payload bitstream
     * @param payloadType
     * @param seqNum
     * @param timeStamp
     * @param payload
     * @param payloadSize
     */
    public RTPPacket(int payloadType, int seqNum, int timeStamp, byte[] payload, int payloadSize)
    {
        // fill default header fields:
        this.version = 2;
        this.padding = 0;
        this.extension = 0;
        this.CC = 0;
        this.marker = 0;
        this.ssrc = 0;

        // fill changing header fields:
        this.sequenceNumber = seqNum;
        this.timeStamp = timeStamp;
        this.payloadType = payloadType;

        // build the header bistream
        header = new byte[HEADER_SIZE];


        // fill the payload bitstream
        this.payloadSize = payloadSize;
        this.payload = new byte[payloadSize];
        System.arraycopy(payload, 0, this.payload, 0, payloadSize);
    }


    /**
     * Constructor of an RTPpacket object from the packet bistream
     * @param packet
     * @param packetSize
     */
    public RTPPacket(byte[] packet, int packetSize)
    {
        // fill default fields:
        version = 2;
        padding = 0;
        extension = 0;
        CC = 0;
        marker = 0;
        ssrc = 0;

        // check if total packet size is lower than the header size
        if (packetSize >= HEADER_SIZE)
        {
            // get the header bitsream:
            header = new byte[HEADER_SIZE];
            for (int i = 0; i < HEADER_SIZE; i++)
                header[i] = packet[i];

            // get the payload bitstream:
            payloadSize = packetSize - HEADER_SIZE;
            payload = new byte[payloadSize];
            for (int i = HEADER_SIZE; i < packetSize; i++)
                payload[i - HEADER_SIZE] = packet[i];

            //interpret the changing fields of the header:
            payloadType = header[1] & 127;
            sequenceNumber = (header[3] & 0xFF) + 256 * (header[2] & 0xFF);
            timeStamp = (header[7] & 0xFF) + 256 * (header[6] & 0xFF) + 65536 * (header[5] & 0xFF) + 16777216 * (header[4] & 0xFF);
        }
    }


    /**
     * Get the payload bistream of the RTPpacket and its size
     * @param data byte array where to store payload data
     * @return the length of the payload
     */
    public int getPayload(byte[] data)
    {
        for (int i = 0; i < payloadSize; i++)
            data[i] = payload[i];

        return (payloadSize);
    }


    /**
     * @return the length of the payload
     */
    public int getPayloadLength()
    {
        return (payloadSize);
    }


    /**
     * @return the total length of the RTP packet
     */
    public int getLength()
    {
        return (payloadSize + HEADER_SIZE);
    }


    /**
     * Get the packet bitstream and its length
     * @param packet byte array where to store packet data
     * @return the total length of the packet (header + payload)
     */
    public int getPacketBytes(byte[] packet)
    {
        //construct the packet = header + payload
        for (int i = 0; i < HEADER_SIZE; i++)
            packet[i] = header[i];
        for (int i = 0; i < payloadSize; i++)
            packet[i + HEADER_SIZE] = payload[i];

        //return total size of the packet
        return (payloadSize + HEADER_SIZE);
    }


    /**
     * @return the packet time stamp
     */
    public int getTimeStamp()
    {
        return (timeStamp);
    }


    /**
     * @return the packet sequence number
     */
    public int getSequenceNumber()
    {
        return (sequenceNumber);
    } 


    /**
     * @return the packet payload type
     */
    public int getPayloadType()
    {
        return (payloadType);
    }


    @Override
    public int compareTo(RTPPacket other)
    {
        return (int)(expandedSequenceNumber - other.expandedSequenceNumber); 
    }
}
