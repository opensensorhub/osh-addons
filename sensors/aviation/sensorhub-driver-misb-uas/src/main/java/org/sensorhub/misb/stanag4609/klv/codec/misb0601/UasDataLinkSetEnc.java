/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv.codec.misb0601;

import org.sensorhub.misb.stanag4609.klv.codec.SetEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.sensorhub.misb.stanag4609.klv.AbstractDataSet.*;

public class UasDataLinkSetEnc {

    private static final Logger logger = LoggerFactory.getLogger(UasDataLinkSetEnc.class);

    private final SetEncoder setEncoder = new SetEncoder(UasDataLinkSet.UAS_LOCAL_SET);

    /**
     * Holds a decoded latitude value referenced as an offset by other fields in the UAS Local Set
     */
    protected int frameCenterLatitude;

    /**
     * Holds a decoded longitude value referenced as an offset by other fields in the UAS Local Set
     */
    protected int frameCenterLongitude;

    public UasDataLinkSetEnc() {
    }

    /**
     * ST0601 Tag 0x02 – Precision Time Stamp (microseconds)
     */
    public UasDataLinkSetEnc precisionTimeStamp(long micros) {
        setEncoder.put((byte) 0x02, micros);
        return this;
    }

    /**
     * ST0601 Tag 0x03 – Mission ID
     */
    public UasDataLinkSetEnc missionId(String id) {
        setEncoder.put((byte) 0x03, id);
        return this;
    }

    /**
     * ST0601 Tag 0x04 – Platform Tail Number
     */
    public UasDataLinkSetEnc platformTailNumber(String tail) {
        setEncoder.put((byte) 0x04, tail);
        return this;
    }

    /**
     * ST0601 Tag 0x05 – Platform Heading Angle (deg → UINT16)
     */
    public UasDataLinkSetEnc platformHeading(double degrees) {
        int scaled = (int) Math.round((degrees / 360.0) * 65535.0);
        setEncoder.put((byte) 0x05, scaled);
        return this;
    }

    /**
     * ST0601 Tag 0x0D – Sensor Latitude (INT32)
     */
    public UasDataLinkSetEnc sensorLatitude(double deg) {
        int scaled = (int) Math.round((deg / 90.0) * Integer.MAX_VALUE);
        setEncoder.put((byte) 0x0D, scaled);
        return this;
    }

    /**
     * ST0601 Tag 0x0E – Sensor Longitude (INT32)
     */
    public UasDataLinkSetEnc sensorLongitude(double deg) {
        int scaled = (int) Math.round((deg / 180.0) * Integer.MAX_VALUE);
        setEncoder.put((byte) 0x0E, scaled);
        return this;
    }

    /**
     * ST0601 Tag 0x17 – Frame Center Latitude
     */
    public UasDataLinkSetEnc frameCenterLatitude(double deg) {
        int scaled = (int) Math.round((deg / 90.0) * Integer.MAX_VALUE);
        setEncoder.put((byte) 0x17, scaled);
        return this;
    }

    /**
     * ST0601 Tag 0x18 – Frame Center Longitude
     */
    public UasDataLinkSetEnc frameCenterLongitude(double deg) {
        int scaled = (int) Math.round((deg / 180.0) * Integer.MAX_VALUE);
        setEncoder.put((byte) 0x18, scaled);
        return this;
    }

    /**
     * Embed Security Local Set (Tag 0x30, Encoding.SET)
     */
    public UasDataLinkSetEnc securityLocalSet(byte[] securityLs) {
        setEncoder.put((byte) 0x30, securityLs);
        return this;
    }

    /**
     * Embed VMTI Local Set (Tag 0x4A, Encoding.SET)
     */
    public UasDataLinkSetEnc vmtiLocalSet(byte[] vmtiLs) {
        setEncoder.put((byte) 0x4A, vmtiLs);
        return this;
    }

    /**
     * Generic setter for any ST0601 tag
     */
    public UasDataLinkSetEnc put(byte tagId, Object value) {

        // Map data to corresponding output in correct data format
        switch (tagId) {

            case 0x01: // "Checksum"
                setEncoder.put(tagId, value);
                break;

            case 0x02: // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
                setEncoder.put(tagId, (long)(convertFromTimeInMillis((double)value) * 1000.0));
                break;

            case 0x41: // "UAS Datalink LS Version Number", "Version number of the UAS Datalink LS document used to generate KLV metadata"
            case 0x38: // "Platform Ground Speed", "Speed projected to the ground of an airborne platform passing overhead", "m/s"
            case 0x0A: // "Platform Designation", "Model name for the platform"
            case 0x04: // "Platform Tail Number", "Identifier of platform as posted"
            case 0x0B: // "Image Source Sensor", "Name of currently active sensor"
            case 0x0C: // "Image Coordinate System", "Name of the image coordinate system used"
                setEncoder.put(tagId, value);
                break;

            case 0x30: // "Security Local Set", "MISB ST 0102 local set Security Metadata items"
                setEncoder.put(tagId, value);
                break;

            case 0x0D: // "Sensor Latitude", "Sensor latitude", "deg"
                setEncoder.put(tagId, convertToInt((double) value, 180.0, 4294967294.0, 0.0));
                break;

            case 0x0E: // "Sensor Longitude", "Sensor longitude", "deg"
                setEncoder.put(tagId, convertToInt((double)value, 360.0, 4294967294.0, 0.0));
                break;

            case 0x0F: // "Sensor True Altitude", "Altitude of sensor as measured from Mean Sea Level (MSL)", "m"
            case 0x19: // "Frame Center Elevation", "Terrain elevation at frame center relative to Mean Sea Level (MSL)", "m"
                setEncoder.put(tagId, convertToInt((double)value, 19900.0, 65535.0, -900.0));
                break;

            case 0x10: // "Sensor Horizontal Field of View", "Horizontal field of view of selected imaging sensor", "deg"
            case 0x11: // "Sensor Vertical Field of View", "Vertical field of view of selected imaging sensor", "deg"
                setEncoder.put(tagId, convertToInt((double)value, 180.0, 65535.0, 0.0));
                break;

            case 0x17: // "Frame Center Latitude", "Terrain latitude of frame center", "deg"
                frameCenterLatitude = convertToInt((double)value, 180.0, 4294967294.0, 0.0);
                setEncoder.put(tagId, frameCenterLatitude);
                break;

            case 0x18: // "Frame Center Longitude", "Terrain longitude of frame center", "deg"
                frameCenterLongitude = convertToInt((double)value, 360.0, 4294967294.0, 0.0);
                setEncoder.put(tagId, frameCenterLongitude);
                break;

            case 0x1A: // "Offset Corner Latitude Point 1", "Frame latitude offset for upper right corner", "deg"
            case 0x1C: // "Offset Corner Latitude Point 2", "Frame latitude offset for lower right corner", "deg"
            case 0x1E: // "Offset Corner Latitude Point 3", "Frame latitude offset for lower left corner", "deg"
                setEncoder.put(tagId, convertToShort((double)value, 0.15, 65534.0, frameCenterLatitude));
                break;

            case 0x1B: // "Offset Corner Longitude Point 1", "Frame longitude offset for upper right corner", "deg"
            case 0x1D: // "Offset Corner Longitude Point 2", "Frame longitude offset for lower right corner", "deg"
            case 0x1F: // "Offset Corner Longitude Point 3", "Frame longitude offset for lower left corner", "deg"
                setEncoder.put(tagId, convertToShort((double)value, 0.15, 65534.0, frameCenterLongitude));
                break;

            case 0x20: // "Offset Corner Latitude Point 4", "Frame latitude offset for upper left corner", "deg"
                setEncoder.put(tagId, convertToShort((double)value, 0.15, 65534.0, frameCenterLatitude));
                frameCenterLatitude = 0;
                break;

            case 0x21: // "Offset Corner Longitude Point 4", "Frame longitude offset for upper left corner", "deg"
                setEncoder.put(tagId, convertToShort((double)value, 0.15, 65534.0, frameCenterLongitude));
                frameCenterLongitude = 0;
                break;

            case 0x15: // "Slant Range", "Slant range in meters", "m"
                setEncoder.put(tagId, convertToInt((double)value, 5000000.0, 4294967295.0, 0.0));
                break;

            case 0x12: // "Sensor Relative Azimuth Angle", "Relative rotation angle of sensor to platform longitudinal axis", "deg"
            case 0x13: // "Sensor Relative Elevation Angle", "Relative elevation angle of sensor to platform longitudinal-transverse plane", "deg"
            case 0x14: // "Sensor Relative Roll Angle", "Relative roll angle of sensor to aircraft platform", "deg"
                setEncoder.put(tagId, convertToInt((double)value, 360.0, 4294967295.0, 0.0));
                break;

            case 0x05: // "Platform Heading Angle", "Aircraft heading angle", "deg"
                setEncoder.put(tagId, convertToInt((double)value, 360.0, 65535.0, 0.0));
                break;

            case 0x06: // "Platform Pitch Angle", "Aircraft pitch angle", "deg"
                setEncoder.put(tagId, convertToShort((double)value, 40.0, 65534.0, 0.0));
                break;

            case 0x07: // "Platform Roll Angle", "Platform roll angle", "deg"
                setEncoder.put(tagId, convertToShort((double)value, 100.0, 65534.0, 0.0));
                break;

            case 0x4A: // "Video Moving Target Indicator and Track Metadata", "MISB ST 0903.4 local set VMTI Metadata items"
                setEncoder.put(tagId, value);
                break;

            default:
                logger.trace("Unsupported tag: {}", tagId);
                break;
        }

        return this;
    }

    public byte[] encode(boolean withKeyAndLength) throws IOException {
        return setEncoder.encode(withKeyAndLength);
    }
}
