package org.sensorhub.impl.sensor.nexrad.aws;

/**
 * <p>Title: MessageHeader.java</p>
 * <p>Description:  Message Header structure as defined in  
 * 		"DRAFT INTERFACE CONTROL DOCUMENT FOR THE RDA/RPG"
 * 			Table II
 * </p>
 *
 * @author T
 * @date Mar 16, 2016
 */
public class MessageHeader {
	short messageSize;
	int rdaByte;
	int messageType;
	short sequenceNum;
	short daysSince1970;
	int msSinceMidnight;
	short numSegments;
	short segmentNum;
}
