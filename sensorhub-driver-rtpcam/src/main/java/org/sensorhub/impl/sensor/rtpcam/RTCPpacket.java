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

import java.nio.ByteBuffer;


// RR: Receiver Report RTCP Packet

//         0                   1                   2                   3
//         0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
// header |V=2|P|    RC   |   PT=RR=201   |             length            |
//        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//        |                     SSRC of packet sender                     |
//        +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
// report |                           fraction lost                       |
// block  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//   1    |              cumulative number of packets lost                |
//        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//        |           extended highest sequence number received           |
//        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//        |                      interarrival jitter                      |
//        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//        |                         last SR (LSR)                         |
//        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
//        |                   delay since last SR (DLSR)                  |
//        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


/**
 * <p>
 * Implementation of an RTCP packet
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 21, 2016
 */
class RTCPpacket
{
    final static int HEADER_SIZE = 8;
    final static int BODY_SIZE = 24;

	public int version;			// Version number 2
    public int padding;			// Padding of packet
    public int rcount; 			// Reception report count = 1 for one receiver
    public int payloadType;		// 201 for Receiver Report
    public int length;			// 1 source is always 32 bytes: 8 header, 24 body
    public int ssrc;			// Ssrc of sender
    public float fractionLost;	// The fraction of RTP data packets from sender lost since the previous RR packet was sent
    public int cumLost;			// The total number of RTP data packets from sender that have been lost since the beginning of reception.
    public int highSeqNb;		// Highest sequence number received
    public int jitter;			// Not used
    public int LSR;				// Not used
    public int DLSR;			// Not used

	public byte[] header;	//Bitstream of header
	public byte[] body;		//Bitstream of the body
	

    // constructor from field values
    public RTCPpacket(float fractionLost, int cumLost, int highSeqNb)
    {
    	version = 2;
    	padding = 0;
    	rcount = 1;
    	payloadType = 201;
    	length = 32;
    	//Other fields not used

    	this.fractionLost = fractionLost;
    	this.cumLost = cumLost;
    	this.highSeqNb = highSeqNb;

    	//Construct the bitstreams
    	header = new byte[HEADER_SIZE];
    	body = new byte[BODY_SIZE];

   		header[0] = (byte)(version << 6 | padding << 5 | rcount);
        header[1] = (byte)(payloadType & 0xFF);
        header[2] = (byte)(length >> 8);
        header[3] = (byte)(length & 0xFF); 
        header[4] = (byte)(ssrc >> 24);
        header[5] = (byte)(ssrc >> 16);
        header[6] = (byte)(ssrc >> 8);
        header[7] = (byte)(ssrc & 0xFF);

		ByteBuffer bb = ByteBuffer.wrap(body);
		bb.putFloat(fractionLost);
		bb.putInt(cumLost);
		bb.putInt(highSeqNb);
    }

    
    // constructor from bit stream
    public RTCPpacket(byte[] packet, int packet_size) {

    	header = new byte[HEADER_SIZE];
    	body = new byte[BODY_SIZE];

        System.arraycopy(packet, 0, header, 0, HEADER_SIZE);
        System.arraycopy(packet, HEADER_SIZE, body, 0, BODY_SIZE);

    	// Parse header fields
        version = (header[0] & 0xFF) >> 6;
        payloadType = header[1] & 0xFF;
        length = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
        ssrc = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);

    	// Parse body fields
    	ByteBuffer bb = ByteBuffer.wrap(body); // big-endian by default
    	fractionLost = bb.getFloat();
    	cumLost = bb.getInt();
    	highSeqNb = bb.getInt();
    }

    //--------------------------
    //getpacket: returns the packet bitstream and its length
    //--------------------------
    public int getpacket(byte[] packet)
    {
        //construct the packet = header + body
        System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        System.arraycopy(body, 0, packet, HEADER_SIZE, BODY_SIZE);

        //return total size of the packet
        return (BODY_SIZE + HEADER_SIZE);
    }

    //--------------------------
    //getlength: return the total length of the RTCP packet
    //--------------------------
    public int getLength() {
        return (BODY_SIZE + HEADER_SIZE);
    }

    public String toString() {
    	return "[RTCP] Version: " + version + ", Fraction Lost: " + fractionLost 
    		   + ", Cumulative Lost: " + cumLost + ", Highest Seq Num: " + highSeqNb;
    }
}