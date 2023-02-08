/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv;

import org.sensorhub.impl.sensor.ffmpeg.klv.SecurityLocalSet;
import org.sensorhub.impl.sensor.ffmpeg.klv.UasDataLinkSet;
import org.sensorhub.misb.stanag4609.tags.Encoding;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Unit test suite for {@link Element}
 *
 * @author Nick Garay
 * @since Feb. 13, 2020
 */
public class ElementTest {

//    byte[] data =
//            {
//                    // UAS DataLink Universal Label
//                    (byte) 0x06, (byte) 0x0E, (byte) 0x2B, (byte) 0x34, (byte) 0x02, (byte) 0x0B, (byte) 0x01, (byte) 0x01,
//                    (byte) 0x0E, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//                    // BER Encoded Length Long Form --> 225 Bytes of data (given by 0xE1)
//                    (byte) 0x82, (byte) 0x00, (byte) 0xE1,
//                    // Actual KLV Encoded data
//                    // "Precision Time Stamp" **************************************************************************
//                    (byte) 0x02, (byte) 0x08, (byte) 0x00, (byte) 0x05, (byte) 0x85, (byte) 0x02, (byte) 0xFD, (byte) 0xBD,
//                    (byte) 0x2A, (byte) 0x6E,
//                    // "UAS Datalink LS Version Number" ****************************************************************
//                    (byte) 0x41, (byte) 0x01, (byte) 0x03,
//                    // "Platform Designation","description" ************************************************************
//                    (byte) 0x0A, (byte) 0x1D, (byte) 0x41, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x56, (byte) 0x69,
//                    (byte) 0x72, (byte) 0x6F, (byte) 0x6E, (byte) 0x6D, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x20,
//                    (byte) 0x53, (byte) 0x55, (byte) 0x41, (byte) 0x56, (byte) 0x20, (byte) 0x50, (byte) 0x75, (byte) 0x6D,
//                    (byte) 0x61, (byte) 0x41, (byte) 0x45, (byte) 0x20, (byte) 0x44, (byte) 0x44, (byte) 0x4C,
//                    // "Security Local Set" ****************************************************************************
//                    (byte) 0x30, (byte) 0x23, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01,
//                    (byte) 0x03, (byte) 0x04, (byte) 0x2F, (byte) 0x2F, (byte) 0x55, (byte) 0x53, (byte) 0x05, (byte) 0x06,
//                    (byte) 0x2F, (byte) 0x2F, (byte) 0x46, (byte) 0x4F, (byte) 0x55, (byte) 0x4F, (byte) 0x06, (byte) 0x02,
//                    (byte) 0x55, (byte) 0x53, (byte) 0x13, (byte) 0x01, (byte) 0x01, (byte) 0x14, (byte) 0x02, (byte) 0x00,
//                    (byte) 0x00, (byte) 0x16, (byte) 0x02, (byte) 0x00, (byte) 0x05,
//                    // "Platform Tail Number" **************************************************************************
//                    (byte) 0x04, (byte) 0x07, (byte) 0x50, (byte) 0x41, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
//                    (byte) 0x30,
//                    // "Sensor Latitude","description" *****************************************************************
//                    (byte) 0x0D, (byte) 0x04, (byte) 0x37, (byte) 0x89, (byte) 0x15, (byte) 0xC7,
//                    // "Sensor Longitude" ******************************************************************************
//                    (byte) 0x0E, (byte) 0x04, (byte) 0xC3, (byte) 0x2E, (byte) 0x31, (byte) 0x77,
//                    // "Sensor True Altitude" **************************************************************************
//                    (byte) 0x0F, (byte) 0x02, (byte) 0x11, (byte) 0x4D,
//                    // "Sensor Horizontal Field of View" ***************************************************************
//                    (byte) 0x10, (byte) 0x02, (byte) 0x2A, (byte) 0x10,
//                    // "Sensor Vertical Field of View" *****************************************************************
//                    (byte) 0x11, (byte) 0x02, (byte) 0x2A, (byte) 0x10,
//                    // "Frame Center Latitude" *************************************************************************
//                    (byte) 0x17, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//                    // "Frame Center Longitude" ************************************************************************
//                    (byte) 0x18, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//                    // "Frame Center Elevation" ************************************************************************
//                    (byte) 0x19, (byte) 0x02, (byte) 0x0B, (byte) 0x97,
//                    // "Offset Corner Latitude Point 1" ****************************************************************
//                    (byte) 0x1A, (byte) 0x02, (byte) 0x00, (byte) 0x00,
//                    // "Offset Corner Longitude Point 1" ***************************************************************
//                    (byte) 0x1B, (byte) 0x02, (byte) 0x00, (byte) 0x00,
//                    // "Offset Corner Latitude Point 2" ****************************************************************
//                    (byte) 0x1C, (byte) 0x02, (byte) 0x00, (byte) 0x00,
//                    // "Offset Corner Longitude Point 2" ***************************************************************
//                    (byte) 0x1D, (byte) 0x02, (byte) 0x00, (byte) 0x00,
//                    // "Offset Corner Latitude Point 3" ****************************************************************
//                    (byte) 0x1E, (byte) 0x02, (byte) 0x40, (byte) 0x93,
//                    // "Offset Corner Longitude Point 3" ***************************************************************
//                    (byte) 0x1F, (byte) 0x02, (byte) 0x80, (byte) 0x00,
//                    // "Offset Corner Latitude Point 4" ****************************************************************
//                    (byte) 0x20, (byte) 0x02, (byte) 0x3C, (byte) 0x65,
//                    // "Offset Corner Longitude Point 4" ***************************************************************
//                    (byte) 0x21, (byte) 0x02, (byte) 0x80, (byte) 0x00,
//                    // "Image Source Sensor" ***************************************************************************
//                    (byte) 0x0B, (byte) 0x02, (byte) 0x45, (byte) 0x4F,
//                    // "Image Coordinate System" ***********************************************************************
//                    (byte) 0x0C, (byte) 0x0E, (byte) 0x47, (byte) 0x65, (byte) 0x6F, (byte) 0x64, (byte) 0x65,
//                    (byte) 0x74, (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x57, (byte) 0x47, (byte) 0x53, (byte) 0x38,
//                    (byte) 0x34,
//                    // "Slant Range" ***********************************************************************************
//                    (byte) 0x15, (byte) 0x04, (byte) 0x00, (byte) 0x83, (byte) 0x0F, (byte) 0x14,
//                    // "Sensor Relative Azimuth Angle" *****************************************************************
//                    (byte) 0x12, (byte) 0x04, (byte) 0x17, (byte) 0x63, (byte) 0x09, (byte) 0x84,
//                    // "Sensor Relative Elevation Angle" ***************************************************************
//                    (byte) 0x13, (byte) 0x04, (byte) 0x04, (byte) 0x51, (byte) 0x9E, (byte) 0xBE,
//                    // "Sensor Relative Roll Angle" ********************************************************************
//                    (byte) 0x14, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
//                    // "Platform Heading Angle" ************************************************************************
//                    (byte) 0x05, (byte) 0x02, (byte) 0x38, (byte) 0x4C,
//                    // "Platform Pitch Angle" **************************************************************************
//                    (byte) 0x06, (byte) 0x02, (byte) 0x10, (byte) 0x16,
//                    // "Platform Roll Angle" ***************************************************************************
//                    (byte) 0x07, (byte) 0x02, (byte) 0x1F, (byte) 0xDB,
//                    // "Platform Ground Speed" *************************************************************************
//                    (byte) 0x38, (byte) 0x01, (byte) 0x0B,
//                    // "Checksum" **************************************************************************************
//                    (byte) 0x01, (byte) 0x02, (byte) 0xBD, (byte) 0xA3
//            };

    private final byte[] data =
            {
                    // UAS DataLink Universal Label
                    (byte)0x06, (byte)0x0E, (byte)0x2B, (byte)0x34, (byte)0x02, (byte)0x0B, (byte)0x01, (byte)0x01,
                    (byte)0x0E, (byte)0x01, (byte)0x03, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
                    // BER Encoded Length Long Form --> 225 Bytes of data (given by 0xE1)
                    (byte)0x82, (byte)0x00, (byte)0xE1,
                    // Actual KLV Encoded data
                    (byte)0x02, (byte)0x08, (byte)0x00, (byte)0x05, (byte)0x85,
                    (byte)0x02, (byte)0xFD, (byte)0xBD, (byte)0x2A, (byte)0x6E, (byte)0x41, (byte)0x01, (byte)0x03,
                    (byte)0x0A, (byte)0x1D, (byte)0x41, (byte)0x65, (byte)0x72, (byte)0x6F, (byte)0x56, (byte)0x69,
                    (byte)0x72, (byte)0x6F, (byte)0x6E, (byte)0x6D, (byte)0x65, (byte)0x6E, (byte)0x74, (byte)0x20,
                    (byte)0x53, (byte)0x55, (byte)0x41, (byte)0x56, (byte)0x20, (byte)0x50, (byte)0x75, (byte)0x6D,
                    (byte)0x61, (byte)0x41, (byte)0x45, (byte)0x20, (byte)0x44, (byte)0x44, (byte)0x4C, (byte)0x30,
                    (byte)0x23, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x03,
                    (byte)0x04, (byte)0x2F, (byte)0x2F, (byte)0x55, (byte)0x53, (byte)0x05, (byte)0x06, (byte)0x2F,
                    (byte)0x2F, (byte)0x46, (byte)0x4F, (byte)0x55, (byte)0x4F, (byte)0x06, (byte)0x02, (byte)0x55,
                    (byte)0x53, (byte)0x13, (byte)0x01, (byte)0x01, (byte)0x14, (byte)0x02, (byte)0x00, (byte)0x00,
                    (byte)0x16, (byte)0x02, (byte)0x00, (byte)0x05, (byte)0x04, (byte)0x07, (byte)0x50, (byte)0x41,
                    (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x30, (byte)0x0D, (byte)0x04, (byte)0x37,
                    (byte)0x89, (byte)0x15, (byte)0xC7, (byte)0x0E, (byte)0x04, (byte)0xC3, (byte)0x2E, (byte)0x31,
                    (byte)0x77, (byte)0x0F, (byte)0x02, (byte)0x11, (byte)0x4D, (byte)0x10, (byte)0x02, (byte)0x2A,
                    (byte)0x10, (byte)0x11, (byte)0x02, (byte)0x2A, (byte)0x10, (byte)0x17, (byte)0x04, (byte)0x00,
                    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x18, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
                    (byte)0x00, (byte)0x19, (byte)0x02, (byte)0x0B, (byte)0x97, (byte)0x1A, (byte)0x02, (byte)0x00,
                    (byte)0x00, (byte)0x1B, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x1C, (byte)0x02, (byte)0x00,
                    (byte)0x00, (byte)0x1D, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x1E, (byte)0x02, (byte)0x40,
                    (byte)0x93, (byte)0x1F, (byte)0x02, (byte)0x80, (byte)0x00, (byte)0x20, (byte)0x02, (byte)0x3C,
                    (byte)0x65, (byte)0x21, (byte)0x02, (byte)0x80, (byte)0x00, (byte)0x0B, (byte)0x02, (byte)0x45,
                    (byte)0x4F, (byte)0x0C, (byte)0x0E, (byte)0x47, (byte)0x65, (byte)0x6F, (byte)0x64, (byte)0x65,
                    (byte)0x74, (byte)0x69, (byte)0x63, (byte)0x20, (byte)0x57, (byte)0x47, (byte)0x53, (byte)0x38,
                    (byte)0x34, (byte)0x15, (byte)0x04, (byte)0x00, (byte)0x83, (byte)0x0F, (byte)0x14, (byte)0x12,
                    (byte)0x04, (byte)0x17, (byte)0x63, (byte)0x09, (byte)0x84, (byte)0x13, (byte)0x04, (byte)0x04,
                    (byte)0x51, (byte)0x9E, (byte)0xBE, (byte)0x14, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x00,
                    (byte)0x00, (byte)0x05, (byte)0x02, (byte)0x38, (byte)0x4C, (byte)0x06, (byte)0x02, (byte)0x10,
                    (byte)0x16, (byte)0x07, (byte)0x02, (byte)0x1F, (byte)0xDB, (byte)0x38, (byte)0x01, (byte)0x0B,
                    (byte)0x01, (byte)0x02, (byte)0xBD, (byte)0xA3
            };

    @Test
    public void testElement() {

        byte[] data = {0};

        Element element = new Element(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x00, data);
        assertNotNull(element);

        Tag tag = TagRegistry.getInstance().getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x02);
        element = new Element(tag, data);
        assertNotNull(element);
    }

    @Test
    public void testToString() {

        final int SECURITY_LOCAL_SET_LOCAL_TAG = 0x30;

        UasDataLinkSet packet = new UasDataLinkSet(data.length, data);

        while (packet.hasMoreElements()) {

            Element element = packet.getNextElement();

            assertNotNull(element);

            String elementString = element.toString();

            assertNotNull(elementString);

            System.out.println(elementString);

            if ((Encoding.SET == element.getTag().getEncoding()) && (SECURITY_LOCAL_SET_LOCAL_TAG == element.getTag().getLocalSetTag())) {

                System.out.println(SecurityLocalSet.SECURITY_LOCAL_SET.getName());

                byte[] securityData = element.getBytes();

                AbstractDataSet securitySet = new SecurityLocalSet(securityData.length, securityData);

                while (securitySet.hasMoreElements()) {

                    // Read data from next element in local data set which can be treated as a set
                    Element securityElement = securitySet.getNextElement();

                    String securityElementString = securityElement.toString();

                    assertNotNull(securityElementString);

                    System.out.println("\t" + securityElementString);
                }
            }
        }

        assertNotNull(packet);
    }

    @Test
    public void testToJsonString() {

        UasDataLinkSet packet = new UasDataLinkSet(data.length, data);

        while (packet.hasMoreElements()) {

            Element element = packet.getNextElement();

            assertNotNull(element);

            String elementString = element.toJsonString();

            assertNotNull(elementString);

            System.out.println(elementString);
        }

        assertNotNull(packet);
    }

    @Test
    public void decodeDataTest() {

        byte[] data = {
                // UAS DataLink Universal Label
                (byte)0x06, (byte)0x0E, (byte)0x2B, (byte)0x34, (byte)0x02, (byte)0x0B, (byte)0x01, (byte)0x01,
                (byte)0x0E, (byte)0x01, (byte)0x03, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
                // BER Encoded Length Long Form --> 475 Bytes of data (given by 0x01DB)
                (byte) 0x82, (byte) 0x01, (byte) 0xDB,
                (byte) 0x02, (byte) 0x08, (byte) 0x00, (byte) 0x04, (byte) 0x59, (byte) 0xF4, (byte) 0xA6, (byte) 0xAA, (byte) 0x4A, (byte) 0xA8,
                (byte) 0x03, (byte) 0x09, (byte) 0x4D, (byte) 0x49, (byte) 0x53, (byte) 0x53, (byte) 0x49, (byte) 0x4F, (byte) 0x4E, (byte) 0x30, (byte) 0x31,
                (byte) 0x04, (byte) 0x06, (byte) 0x41, (byte) 0x46, (byte) 0x2D, (byte) 0x31, (byte) 0x30, (byte) 0x31,
                (byte) 0x05, (byte) 0x02, (byte) 0x71, (byte) 0xC2,
                (byte) 0x06, (byte) 0x02, (byte) 0xFD, (byte) 0x3D,
                (byte) 0x07, (byte) 0x02, (byte) 0x08, (byte) 0xB8,
                (byte) 0x08, (byte) 0x01, (byte) 0x93,
                (byte) 0x09, (byte) 0x01, (byte) 0x9F,
                (byte) 0x0A, (byte) 0x05, (byte) 0x4D, (byte) 0x51, (byte) 0x31, (byte) 0x2D, (byte) 0x42,
                (byte) 0x0B, (byte) 0x02, (byte) 0x45, (byte) 0x4F,
                (byte) 0x0C, (byte) 0x06, (byte) 0x57, (byte) 0x47, (byte) 0x53, (byte) 0x2D, (byte) 0x38, (byte) 0x34,
                (byte) 0x0D, (byte) 0x04, (byte) 0x55, (byte) 0x95, (byte) 0xB6, (byte) 0x6D,
                (byte) 0x0E, (byte) 0x04, (byte) 0x5B, (byte) 0x53, (byte) 0x60, (byte) 0xC4,
                (byte) 0x0F, (byte) 0x02, (byte) 0xC2, (byte) 0x21,
                (byte) 0x10, (byte) 0x02, (byte) 0xCD, (byte) 0x9C,
                (byte) 0x11, (byte) 0x02, (byte) 0xD9, (byte) 0x17,
                (byte) 0x12, (byte) 0x04, (byte) 0x72, (byte) 0x4A, (byte) 0x0A, (byte) 0x20,
                (byte) 0x13, (byte) 0x04, (byte) 0x87, (byte) 0xF8, (byte) 0x4B, (byte) 0x86,
                (byte) 0x14, (byte) 0x04, (byte) 0x7D, (byte) 0xC5, (byte) 0x5E, (byte) 0xCE,
                (byte) 0x15, (byte) 0x04, (byte) 0x03, (byte) 0x83, (byte) 0x09, (byte) 0x26,
                (byte) 0x16, (byte) 0x02, (byte) 0x12, (byte) 0x81,
                (byte) 0x17, (byte) 0x04, (byte) 0xF1, (byte) 0x01, (byte) 0xA2, (byte) 0x29,
                (byte) 0x18, (byte) 0x04, (byte) 0x14, (byte) 0xBC, (byte) 0x08, (byte) 0x2B,
                (byte) 0x19, (byte) 0x02, (byte) 0x34, (byte) 0xF3,
                (byte) 0x1A, (byte) 0x02, (byte) 0x17, (byte) 0x50,
                (byte) 0x1B, (byte) 0x02, (byte) 0x06, (byte) 0x3F,
                (byte) 0x1C, (byte) 0x02, (byte) 0xF9, (byte) 0xC1,
                (byte) 0x1D, (byte) 0x02, (byte) 0x17, (byte) 0x50,
                (byte) 0x1E, (byte) 0x02, (byte) 0xED, (byte) 0x1F,
                (byte) 0x1F, (byte) 0x02, (byte) 0xF7, (byte) 0x32,
                (byte) 0x20, (byte) 0x02, (byte) 0x01, (byte) 0xD0,
                (byte) 0x21, (byte) 0x02, (byte) 0xEB, (byte) 0x3F,
                (byte) 0x22, (byte) 0x01, (byte) 0x02,
                (byte) 0x23, (byte) 0x02, (byte) 0xA7, (byte) 0xC4,
                (byte) 0x24, (byte) 0x01, (byte) 0xB2,
                (byte) 0x25, (byte) 0x02, (byte) 0xBE, (byte) 0xBA,
                (byte) 0x26, (byte) 0x02, (byte) 0xCA, (byte) 0x35,
                (byte) 0x27, (byte) 0x01, (byte) 0x54,
                (byte) 0x28, (byte) 0x04, (byte) 0x8F, (byte) 0x69, (byte) 0x52, (byte) 0x62,
                (byte) 0x29, (byte) 0x04, (byte) 0x76, (byte) 0x54, (byte) 0x57, (byte) 0xF2,
                (byte) 0x2A, (byte) 0x02, (byte) 0xF8, (byte) 0x23,
                (byte) 0x2B, (byte) 0x01, (byte) 0x03,
                (byte) 0x2C, (byte) 0x01, (byte) 0x0F,
                (byte) 0x2D, (byte) 0x02, (byte) 0x1A, (byte) 0x95,
                (byte) 0x2E, (byte) 0x02, (byte) 0x26, (byte) 0x11,
                (byte) 0x2F, (byte) 0x01, (byte) 0x31,
// 0x30 Security Local Set Skipped as it is an Embedded packet
                (byte) 0x31, (byte) 0x02, (byte) 0x3D, (byte) 0x07,
                (byte) 0x32, (byte) 0x02, (byte) 0xC8, (byte) 0x83,
                (byte) 0x33, (byte) 0x02, (byte) 0xD3, (byte) 0xFE,
                (byte) 0x34, (byte) 0x02, (byte) 0xDF, (byte) 0x79,
                (byte) 0x35, (byte) 0x02, (byte) 0x6A, (byte) 0xF4,
                (byte) 0x36, (byte) 0x02, (byte) 0x76, (byte) 0x70,
                (byte) 0x37, (byte) 0x01, (byte) 0x81,
                (byte) 0x38, (byte) 0x01, (byte) 0x8C,
                (byte) 0x39, (byte) 0x04, (byte) 0xB3, (byte) 0x8E, (byte) 0xAC, (byte) 0xF1,
                (byte) 0x3A, (byte) 0x02, (byte) 0xA4, (byte) 0x5D,
                (byte) 0x3A, (byte) 0x02, (byte) 0xA4, (byte) 0x5D,
                (byte) 0x3B, (byte) 0x07, (byte) 0x54, (byte) 0x4F, (byte) 0x50, (byte) 0x20, (byte) 0x47, (byte) 0x55, (byte) 0x4E,
                (byte) 0x3C, (byte) 0x02, (byte) 0xAF, (byte) 0xD8,
                (byte) 0x3D, (byte) 0x01, (byte) 0xBA,
                (byte) 0x3E, (byte) 0x02, (byte) 0x06, (byte) 0xCF,
                (byte) 0x3F, (byte) 0x01, (byte) 0x02,
                (byte) 0x40, (byte) 0x02, (byte) 0xDD, (byte) 0xC5,
                (byte) 0x41, (byte) 0x01, (byte) 0x0D,
// TAG 0x42 Deprecated
                (byte) 0x43, (byte) 0x04, (byte) 0x85, (byte) 0xA1, (byte) 0x5A, (byte) 0x39,
                (byte) 0x44, (byte) 0x04, (byte) 0x00, (byte) 0x1C, (byte) 0x50, (byte) 0x1C,
                (byte) 0x45, (byte) 0x02, (byte) 0x0B, (byte) 0xB3,
                (byte) 0x46, (byte) 0x06, (byte) 0x41, (byte) 0x50, (byte) 0x41, (byte) 0x43, (byte) 0x48, (byte) 0x45,
                (byte) 0x47, (byte) 0x02, (byte) 0x17, (byte) 0x2F,
                (byte) 0x47, (byte) 0x02, (byte) 0x17, (byte) 0x2F,
                (byte) 0x48, (byte) 0x08, (byte) 0x00, (byte) 0x02, (byte) 0xD5, (byte) 0xCF, (byte) 0x4D, (byte) 0xDC, (byte) 0x9A, (byte) 0x35,
// 0x49 RVT Local Set Skipped as it is an Embedded packet
// 0x4A VMTI Local Set Skipped as it is an Embedded packet
                (byte) 0x4B, (byte) 0x02, (byte) 0xC2, (byte) 0x21,
                (byte) 0x4C, (byte) 0x02, (byte) 0x0B, (byte) 0xB3,
                (byte) 0x4D, (byte) 0x01, (byte) 0x01,
                (byte) 0x4E, (byte) 0x02, (byte) 0x0B, (byte) 0xB3,
                (byte) 0x4F, (byte) 0x02, (byte) 0x09, (byte) 0xFB,
                (byte) 0x50, (byte) 0x02, (byte) 0x04, (byte) 0xBC,
// 0x51 Image Horizon Pixel Pack Skipped as it is an Embedded packet
                (byte) 0x52, (byte) 0x04, (byte) 0xF1, (byte) 0x06, (byte) 0x9B, (byte) 0x63,
                (byte) 0x53, (byte) 0x04, (byte) 0x14, (byte) 0xBC, (byte) 0xB2, (byte) 0xC0,
                (byte) 0x54, (byte) 0x04, (byte) 0xF1, (byte) 0x00, (byte) 0x4D, (byte) 0x00,
                (byte) 0x55, (byte) 0x04, (byte) 0x14, (byte) 0xBE, (byte) 0x84, (byte) 0xC8,
                (byte) 0x56, (byte) 0x04, (byte) 0xF0, (byte) 0xFD, (byte) 0x9B, (byte) 0x17,
                (byte) 0x57, (byte) 0x04, (byte) 0x14, (byte) 0xBB, (byte) 0x17, (byte) 0xAF,
                (byte) 0x58, (byte) 0x04, (byte) 0xF1, (byte) 0x02, (byte) 0x05, (byte) 0x2A,
                (byte) 0x59, (byte) 0x04, (byte) 0x14, (byte) 0xB9, (byte) 0xD1, (byte) 0x76,
                (byte) 0x5A, (byte) 0x04, (byte) 0xFF, (byte) 0x62, (byte) 0xE2, (byte) 0xF2,
                (byte) 0x5B, (byte) 0x04, (byte) 0x04, (byte) 0xD8, (byte) 0x04, (byte) 0xDF,
                (byte) 0x5C, (byte) 0x04, (byte) 0xF3, (byte) 0xAB, (byte) 0x48, (byte) 0xEF,
                (byte) 0x5D, (byte) 0x04, (byte) 0xDE, (byte) 0x17, (byte) 0x93, (byte) 0x23,
// 0x5E MIIS Core Identifier Skipped - see ST 1204 for definition
// 0x5F SAR Motion Imagery Set Skipped as it is an Embedded packet- see ST 1206 for definition
// 0x60: Target Width Extended Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x61: Range Image Local Set Skipped as it is an Embedded packet - see ST 1002 for definition
// 0x62: Geo-Registration Local Set as it is an Embedded packet - see ST 1601 for definition
// 0x63: Composite Imaging Local Set as it is an Embedded packet - see ST 1602 for definition
// 0x64: Segment Local Set as it is an Embedded packet - see ST 1607 for definition
// 0x65: Amend Local Set as it is an Embedded packet - see ST 1607 for definition
// 0x66: SDCC-FLP Skipped as it is encoded floating point value - see ST 1010 for procedure
// 0x67: Density Altitude Extended Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x68: Sensor Ellipsoid Height Extended Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x69: Alternate Platform Ellipsoid Height Extended Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
                (byte) 0x6A, (byte) 0x04, (byte) 0x42, (byte) 0x4C, (byte) 0x55, (byte) 0x45,
                (byte) 0x6B, (byte) 0x06, (byte) 0x42, (byte) 0x41, (byte) 0x53, (byte) 0x45, (byte) 0x30, (byte) 0x31,
                (byte) 0x6C, (byte) 0x04, (byte) 0x48, (byte) 0x4F, (byte) 0x4D, (byte) 0x45,
// 0x6D: Range to Recovery Location Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
                (byte) 0x6E, (byte) 0x02, (byte) 0x4D, (byte) 0xAF,
                (byte) 0x6F, (byte) 0x02, (byte) 0x0B, (byte) 0xB8,
// 0x70: Platform Course Angle Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x71: Altitude AGL Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x72: Radar Altimeter Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x73: Control Command Skipped as value is DLP
// 0x74: Control Command Verification List Skipped as value is DLP
// 0x75: Sensor Azimuth Rate Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x76: Sensor Elevation Rate Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x77: Sensor Roll Rate Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x78: On-board MI Storage Percent Full Skipped as value is Reverse IMAPB Decoded - see ST 1201 for procedure
// 0x79: Active Wavelength List Skipped as value is DLP
// 0x7A: Country Codes Skipped as value is VLP
                (byte) 0x7B, (byte) 0x01, (byte) 0x07,
                (byte) 0x7C, (byte) 0x01, (byte) 0x03,
                (byte) 0x7D, (byte) 0x01, (byte) 0x09,
                (byte) 0x7E, (byte) 0x01, (byte) 0x05,
// 0x7F: Sensor Frame Rate Pack Skipped as value is DLP
// 0x80: Wavelengths List
// 0x81: Target ID
// 0x82: Airbase Locations
// 0x83: Take-off Time
// 0x84: Transmission Frequency
// 0x85: On-board MI Storage Capacity
// 0x86: Zoom Percentage
// 0x87: Communications Method
// 0x88: Leap Seconds
// 0x89: Correction Offset
// 0x8A: Payload List
// 0x8B: Active Payloads
// 0x8C: Weapons Stores
// 0x8D: Waypoint List
                // Checksum is always last element
                (byte) 0x01, (byte) 0x02, (byte) 0xAD, (byte) 0x8D,
        };

        UasDataLinkSet packet = new UasDataLinkSet(data.length, data);

        while (packet.hasMoreElements()) {

            Element element = packet.getNextElement();

            assertNotNull(element);

            Object result = element.unpackData();

            double frameCenterLatitude = 0.0;
            double frameCenterLongitude = 0.0;

            switch (element.getTag().getLocalSetTag()) {
                case 0x01: // "Checksum", "Checksum used to detect errors within a UAS Datalink LS packet"
                    assertEquals(44429, result);
                    break;
                case 0x02: // "Precision Time Stamp", "Timestamp for all metadata in this Local Set; used to coordinate with Motion Imagery", "microseconds"
                    long epochMicroSeconds = (long) result;
                    long epochSeconds = epochMicroSeconds / 1000000L;
                    long nanoOffset = (epochMicroSeconds % 1000000L) * 1000L;
                    Instant instant = Instant.ofEpochSecond(epochSeconds, nanoOffset);
                    assertEquals("2008-10-24T00:13:29.913Z", instant.toString());
                    break;
                case 0x03: // "Mission ID", "Descriptive mission identifier to distinguish event or sortie"
                    assertEquals("MISSION01", result);
                    break;
                case 0x04: // "Platform Tail Number", "Identifier of platform as posted"
                    assertEquals("AF-101", result);
                    break;
                case 0x05: // "Platform Heading Angle", "Aircraft heading angle", "deg"
                    assertEquals(159.974365, (360.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x06: // "Platform Pitch Angle", "Aircraft pitch angle", "deg"
                    assertEquals(-0.431531724, (40.0 / 65534.0) * (short) result, 0.0001);
                    break;
                case 0x07: // "Platform Roll Angle", "Platform roll angle", "deg"
                    assertEquals(3.40586566, (100.0 / 65534.0) * (short) result, 0.0001);
                    break;
                case 0x08: // "Platform True Airspeed", "True airspeed (TAS) of platform", "m/s"
                    assertEquals(147, (int) result);
                    break;
                case 0x09: // "Platform Indicated Airspeed", "Indicated airspeed (IAS) of platform", "m/s"
                    assertEquals(159, (int) result);
                    break;
                case 0x0A: // "Platform Designation", "Model name for the platform"
                    assertEquals("MQ1-B", result);
                    break;
                case 0x0B: // "Image Source Sensor", "Name of currently active sensor"
                    assertEquals("EO", result);
                    break;
                case 0x0C: // "Image Coordinate System", "Name of the image coordinate system used"
                    assertEquals("WGS-84", result);
                    break;
                case 0x0D: // "Sensor Latitude", "Sensor latitude", "deg"
                    assertEquals(60.176822966978335, (180.0 / 4294967294.0) * (int) result, 0.00001);
                    break;
                case 0x0E: // "Sensor Longitude", "Sensor longitude", "deg"
                    assertEquals(128.42675904204452, (360.0 / 4294967294.0) * (int) result, 0.00001);
                    break;
                case 0x0F: // "Sensor True Altitude", "Altitude of sensor as measured from Mean Sea Level (MSL)", "m"
                    assertEquals(14190.7195, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x10: // "Sensor Horizontal Field of View", "Horizontal field of view of selected imaging sensor", "deg"
                    assertEquals(144.571298, (180.0 / 65535.0) * (int) result, 0.00001);
                    break;
                case 0x11: // "Sensor Vertical Field of View", "Vertical field of view of selected imaging sensor", "deg"
                    assertEquals(152.643626, (180.0 / 65535.0) * (int) result, 0.00001);
                    break;
                case 0x12: // "Sensor Relative Azimuth Angle", "Relative rotation angle of sensor to platform longitudinal axis", "deg"
                    assertEquals(160.71921143697557, (360.0 / 4294967295.0) * (int) result, 0.00001);
                    break;
                case 0x13: // "Sensor Relative Elevation Angle", "Relative elevation angle of sensor to platform longitudinal-transverse plane", "deg"
                    assertEquals(-168.79232483394085, (360.0 / 4294967295.0) * (int) result, 0.00001);
                    break;
                case 0x14: // "Sensor Relative Roll Angle", "Relative roll angle of sensor to aircraft platform", "deg"
                    assertEquals(176.86543764939194, (360.0 / 4294967295.0) * (int) result, 0.00001);
                    break;
                case 0x15: // "Slant Range", "Slant range in meters", "m"
                    assertEquals(68590.983298744770, (5000000.0 / 4294967295.0) * (int) result, 0.00001);
                    break;
                case 0x16: // "Target Width", "Target width within sensor field of view", "m"
                    assertEquals(722.819867, (10000.0 / 65535.0) * (int) result, 0.00001);
                    break;
                case 0x17: // "Frame Center Latitude", "Terrain latitude of frame center", "deg"
                    frameCenterLatitude = (180.0 / 4294967294.0) * (int) result;
                    assertEquals(-10.542388633146132, frameCenterLatitude, 0.00001);
                    break;
                case 0x18: // "Frame Center Longitude", "Terrain longitude of frame center", "deg"
                    frameCenterLongitude = (360.0 / 4294967294.0) * (int) result;
                    assertEquals(29.157890122923014, frameCenterLongitude, 0.00001);
                    break;
                case 0x19: // "Frame Center Elevation", "Terrain elevation at frame center relative to Mean Sea Level (MSL)", "m"
                    assertEquals(3216.03723, (19900.0 / 65535.0) * (int) result - 900.0, 0.00001);
                    break;
                case 0x1A: // "Offset Corner Latitude Point 1", "Frame latitude offset for upper left corner", "deg"
                    assertEquals(0.0136602540, (0.15 / 65534.0) * (short) result + frameCenterLatitude, 0.00001);
                    break;
                case 0x1B: // "Offset Corner Longitude Point 1", "Frame longitude offset for upper left corner", "deg"
                    assertEquals(0.0036602540, (0.15 / 65534.0) * (short) result + frameCenterLongitude, 0.00001);
                    break;
                case 0x1C: // "Offset Corner Latitude Point 2", "Frame latitude offset for upper right corner", "deg"
                    assertEquals(-0.0036602540, (0.15 / 65534.0) * (short) result + frameCenterLatitude, 0.00001);
                    break;
                case 0x1D: // "Offset Corner Longitude Point 2", "Frame longitude offset for upper right corner", "deg"
                    assertEquals(0.0136602540, (0.15 / 65534.0) * (short) result + frameCenterLongitude, 0.00001);
                    break;
                case 0x1E: // "Offset Corner Latitude Point 3", "Frame latitude offset for lower right corner", "deg"
                    assertEquals(-0.0110621778, (0.15 / 65534.0) * (short) result + frameCenterLatitude, 0.00001);
                    break;
                case 0x1F: // "Offset Corner Longitude Point 3", "Frame longitude offset for lower right corner", "deg"
                    assertEquals(-0.0051602540, (0.15 / 65534.0) * (short) result + frameCenterLongitude, 0.00001);
                    break;
                case 0x20: // "Offset Corner Latitude Point 4", "Frame latitude offset for lower left corner", "deg"
                    assertEquals(0.0010621778, (0.15 / 65534.0) * (short) result + frameCenterLatitude, 0.00001);
                    break;
                case 0x21: // "Offset Corner Longitude Point 4", "Frame longitude offset for lower left corner", "deg"
                    assertEquals(-0.0121602540, (0.15 / 65534.0) * (short) result + frameCenterLongitude, 0.00001);
                    break;
                case 0x22: // "Icing Detected", "Flag for icing detected at aircraft location", "code"
                    assertTrue(((int) result == 0) || ((int) result == 1) || ((int) result == 2));
                    break;
                case 0x23: // "Wind Direction", "Wind direction at aircraft location", "deg"
                    assertEquals(235.924010, (360.0 / 65535.0) * (int) result, 0.00001);
                    break;
                case 0x24: // "Wind Speed", "Wind speed at aircraft location", "m/s"
                    assertEquals(69.8039216, (100.0 / 255.0) * (int) result, 0.00001);
                    break;
                case 0x25: // "Static Pressure", "Static pressure at aircraft location", "mbar"
                    assertEquals(3725.18502, (5000.0 / 65535.0) * (int) result, 0.00001);
                    break;
                case 0x26: // "Density Altitude", "Density altitude at aircraft location", "m"
                    assertEquals(14818.6770, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x27: // "Outside Air Temperature", "Temperature outside of aircraft", "degC"
                    assertEquals(84, (byte) result);
                    break;
                case 0x28: // "Target Location Latitude", "Calculated target latitude", "deg"
                    assertEquals(-79.163850051892850, (180.0 / 4294967294.0) * (int) result, 0.00001);
                    break;
                case 0x29: // "Target Location Longitude", "Calculated target longitude", "deg"
                    assertEquals(166.40081296041646, (360.0 / 4294967294.0) * (int) result, 0.00001);
                    break;
                case 0x2A: // "Target Location Elevation", "Calculated target elevation", "m"
                    assertEquals(18389.0471, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x2B: // "Target Track Gate Width", "Tracking gate width (x value) of tracked target within field of view", "Pixels"
                    assertEquals(6, 2 * (int) result);
                    break;
                case 0x2C: // "Target Track Gate Height", "Tracking gate height (y value) of tracked target within field of view", "Pixels"
                    assertEquals(30, 2 * (int) result);
                    break;
                case 0x2D: // "Target Error Estimate - CE90", "Circular error 90 (CE90) is the estimated error distance in the horizontal direction", "m"
                    assertEquals(425.215152, (4095.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x2E: // "Target Error Estimate - LE90", "Lateral error 90 (LE90) is the estimated error distance in the vertical (or lateral) direction", "m"
                    assertEquals(608.9231, (4095.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x2F: // "Generic Flag Data", "Generic metadata flags"
                    assertEquals(49, (int) result);
                    break;
                case 0x30: // "Security Local Set", "MISB ST 0102 local let Security Metadata items"
                    break;
                case 0x31: // "Differential Pressure", "Differential pressure at aircraft location", "mbar"
                    assertEquals(1191.95850, (5000.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x32: // "Platform Angle of Attack", "Platform attack angle", "deg"
                    assertEquals(-8.67030854, (40.0 / 65534.0) * (short) result, 0.0001);
                    break;
                case 0x33: // "Platform Vertical Speed", "Vertical speed of the aircraft relative to zenith", "m/s"
                    assertEquals(-61.8878750, (360.0 / 65534.0) * (short) result, 0.0001);
                    break;
                case 0x34: // "Platform Sideslip Angle", "Angle between the platform longitudinal axis and relative wind", "deg"
                    assertEquals(-5.08255257, (40.0 / 65534.0) * (short) result, 0.0001);
                    break;
                case 0x35: // "Airfield Barometric Pressure", "Local pressure at airfield of known height", "mbar"
                    assertEquals(2088.96010, (5000.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x36: // "Airfield Elevation", "Elevation of airfield corresponding to Airfield Barometric Pressure", "m"
                    assertEquals(8306.80552, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x37: // "Relative Humidity", "Relative humidity at aircraft location", "%"
                    assertEquals(50.5882353, (100 / 255.0) * (int) result, 0.0001);
                    break;
                case 0x38: // "Platform Ground Speed", "Speed projected to the ground of an airborne platform passing overhead", "m/s"
                    assertEquals(140, (int) result);
                    break;
                case 0x39: // "Ground Range", "Horizontal distance from ground position of aircraft relative to nadir, and target of interest", "m"
                    assertEquals(3506979.0316063400, (5000000.0 / 4294967295.0) * ((int) result & 0x0FFFFFFFFL), 0.0001);
                    break;
                case 0x3A: // "Platform Fuel Remaining", "Remaining fuel on airborne platform", "kg"
                    assertEquals(6420.53864, (10000.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x3B: // "Platform Call Sign", "Call sign of platform or operating unit"
                    assertEquals("TOP GUN", result);
                    break;
                case 0x3C: // "Weapon Load", "Current weapons stored on aircraft"
                    assertEquals(45016, (int) result);
                    break;
                case 0x3D: // "Weapon Fired", "Indication when a particular weapon is released"
                    assertEquals(186, (int) result);
                    break;
                case 0x3E: // "Laser PRF Code", "A laser's Pulse Repetition Frequency (PRF) code used to mark a target"
                    assertEquals(1743, (int) result);
                    break;
                case 0x3F: // "Sensor Field of View Name", "Sensor field of view names"
                    assertEquals(2, (int) result);
                    break;
                case 0x40: // "Platform Magnetic Heading", "Aircraft magnetic heading angle", "deg"
                    assertEquals(311.868162, (360.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x41: // "UAS Datalink LS Version Number", "Version number of the UAS Datalink LS document used to generate KLV metadata"
                    assertEquals(13, (int) result);
                    break;
                case 0x42: // "Deprecated", "This item has been deprecated."
                    break;
                case 0x43: // "Alternate Platform Latitude", "Alternate platform latitude", "deg"
                    assertEquals(-86.041207348947040, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x44: // "Alternate Platform Longitude", "Alternate platform longitude", "deg"
                    assertEquals(0.15552755452484243, (360.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x45: // "Alternate Platform Altitude", "Altitude of alternate platform as measured from Mean Sea Level (MSL)", "m"
                    assertEquals(9.44533455, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x46: // "Alternate Platform Name", "Name of alternate platform connected to UAS"
                    assertEquals("APACHE", result);
                    break;
                case 0x47: // "Alternate Platform Heading", "Heading angle of alternate platform connected to UAS", "deg"
                    assertEquals(32.6024262, (360.0 / 65535.0) * (int) result, 0.0001);
                    break;
                case 0x48: // "Event Start Time - UTC", "Start time of scene, project, event, mission, editing event, license, publication, etc.", "microseconds"
                    long dateEpochMicroSeconds = (long) result;
                    long dateEpochSeconds = dateEpochMicroSeconds / 1000000L;
                    long dateNanoOffset = (dateEpochMicroSeconds % 1000000L) * 1000L;
                    Instant dateInstant = Instant.ofEpochSecond(dateEpochSeconds, dateNanoOffset);
                    Date date = Date.from(dateInstant);
                    SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("MMMMMMMM d, yyyy. kk:mm:ss");
                    simpleDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT+1:00"));
                    assertEquals("April 16, 1995. 13:44:54", simpleDateFormatter.format(date));
                    break;
                case 0x49: // "RVT Local Set", "RVT Local Set metadata items"
                    break;
                case 0x4A: // "VMTI Local Set", "VMTI Local Set metadata items"
                    break;
                case 0x4B: // "Sensor Ellipsoid Height", "Sensor ellipsoid height as measured from the reference WGS84 ellipsoid", "m"
                    assertEquals(14190.7195, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x4C: // "Alternate Platform Ellipsoid Height", "Alternate platform ellipsoid height as measured from the reference WGS84 Ellipsoid", "m"
                    assertEquals(9.44533455, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x4D: // "Operational Mode", "Indicates the mode of operations of the event portrayed"
                    assertEquals(1, (int) result);
                    break;
                case 0x4E: // "Frame Center Height Above Ellipsoid", "Frame center ellipsoid height as measured from the reference WGS84 ellipsoid", "m"
                    assertEquals(9.44533455, (19900.0 / 65535.0) * (int) result - 900.0, 0.0001);
                    break;
                case 0x4F: // "Sensor North Velocity", "Northing velocity of the sensor or platform", "m/s"
                    assertEquals(25.4977569, (654.0 / 65534.0) * (short) result, 0.01);
                    break;
                case 0x50: // "Sensor East Velocity", "Easting velocity of the sensor or platform", "m/s"
                    assertEquals(12.1, (654.0 / 65534.0) * (short) result, 0.01);
                    break;
                case 0x51: // "Image Horizon Pixel Pack", "Location of earth-sky horizon in the Imagery"
                    break;
                case 0x52: // "Corner Latitude Point 1 (Full)", "Frame latitude for upper left corner", "deg"
                    assertEquals(-10.528728379108287, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x53: // "Corner Longitude Point 1 (Full)", "Frame longitude for upper left corner", "deg"
                    assertEquals(29.161550376960857, (360.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x54: // "Corner Latitude Point 2 (Full)", "Frame latitude for upper right corner", "deg"
                    assertEquals(-10.546048887183977, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x55: // "Corner Longitude Point 2 (Full)", "Frame longitude for upper right corner", "deg"
                    assertEquals(29.171550376960860, (360.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x56: // "Corner Latitude Point 3 (Full)", "Frame latitude for lower right corner", "deg"
                    assertEquals(-10.553450810972622, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x57: // "Corner Longitude Point 3 (Full)", "Frame longitude for lower right corner", "deg"
                    assertEquals(29.152729868885170, (360.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x58: // "Corner Latitude Point 4 (Full)", "Frame latitude for lower left corner", "deg"
                    assertEquals(-10.541326455319641, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x59: // "Corner Longitude Point 4 (Full)", "Frame longitude for lower left corner", "deg"
                    assertEquals(29.145729868885170, (360.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x5A: // "Platform Pitch Angle (Full)", "Aircraft pitch angle", "deg"
                    assertEquals(-0.43152510208614414, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x5B: // "Platform Roll Angle (Full)", "Platform roll angle", "deg"
                    assertEquals(3.4058139815022304, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x5C: // "Platform Angle of Attack (Full)", "Platform attack angle", "deg"
                    assertEquals(-8.6701769841230370, (180.0 / 4294967294.0) * (int) result, 0.0001);
                    break;
                case 0x5D: // "Platform Sideslip Angle (Full)", "Angle between the platform longitudinal axis and relative wind", "deg"
                    assertEquals(-47.683, (360.0 / 4294967294.0) * (int) result, 0.001);
                    break;
                case 0x5E: // "MIIS Core Identifier", "MISB ST 1204 MIIS Core Identifier binary value"
                case 0x5F: // "SAR Motion Imagery Local Set", "MISB ST 1206 SAR Motion Imagery Metadata Local Set metadata items"
                case 0x60: // "Target Width Extended", "Target width within sensor field of view", "m"
                case 0x61: // "Range Image Local Set", "MISB ST 1002 Range Imaging Local Set metadata items"
                case 0x62: // "Geo-Registration Local Set", "MISB ST 1601 Geo-Registration Local Set metadata items"
                case 0x63: // "Composite Imaging Local Set", "MISB ST 1602 Composite Imaging Local Set metadata items"
                case 0x64: // "Segment Local Set", "MISB ST 1607 Local Set metadata items, used to enable metadata sharing"
                case 0x65: // "Amend Local Set", "MISB ST 1607 Amend Local Set metadata items, used to provide metadata corrections"
                case 0x66: // "SDCC-FLP", "MISB ST 1010 Floating Length Pack (FLP) metadata item, providing Standard Deviation and Cross Correlation (SDCC) metadata"
                case 0x67: // "Density Altitude Extended", "Density altitude above MSL at aircraft location", "m"
                case 0x68: // "Sensor Ellipsoid Height Extended", "Sensor ellipsoid height extended as measured from the reference WGS84 ellipsoid", "m"
                case 0x69: // "Alternate Platform Ellipsoid Height Extended", "Alternate platform ellipsoid height extended as measured from the reference WGS84 ellipsoid", "m"
                    break;
                case 0x6A: // "Stream Designator", "A second designation given to a sortie"
                    assertEquals("BLUE", result);
                    break;
                case 0x6B: // "Operational Base", "Name of the operational base hosting the platform"
                    assertEquals("BASE01", result);
                    break;
                case 0x6C: // "Broadcast Source", "Name of the source, where the Motion Imagery is first broadcast"
                    assertEquals("HOME", result);
                    break;
                case 0x6D: // "Range To Recovery Location", "Distance from current position to airframe recovery position", "km"
                    break;
                case 0x6E: // "Time Airborne", "Number of seconds aircraft has been airborne", "s"
                    assertEquals(19887, (long) result);
                    break;
                case 0x6F: // "Propulsion Unit Speed", "The speed the engine (or electric motor) is rotating at", "RPM"
                    assertEquals(3000, (long) result);
                    break;
                case 0x70: // "Platform Course Angle", "Direction the aircraft is moving relative to True North", "deg"
                case 0x71: // "Altitude AGL", "Above Ground Level (AGL) height above the ground/water", "m"
                case 0x72: // "Radar Altimeter", "Height above the ground/water as reported by a RADAR altimeter", "m"
                case 0x73: // "Control Command", "Record of command from GCS to Aircraft"
                case 0x74: // "Control Command Verification List", "Acknowledgement of one or more control commands were received by the platform"
                case 0x75: // "Sensor Azimuth Rate", "The rate the sensors azimuth angle is changing", "deg/s"
                case 0x76: // "Sensor Elevation Rate", "The rate the sensors elevation angle is changing", "deg/s"
                case 0x77: // "Sensor Roll Rate", "The rate the sensors roll angle is changing", "deg/s"
                case 0x78: // "On-board MI Storage Percent Full", "Amount of on-board Motion Imagery storage used as a percentage of the total storage", "%"
                case 0x79: // "Active Wavelength List", "List of wavelengths in Motion Imagery"
                case 0x7A: // "Country Codes", "Country codes which are associated with the platform and its operation"
                    break;
                case 0x7B: // "Number of NAVSATs in View", "Count of navigation satellites in view of platform", "count"
                    assertEquals(7, (long) result);
                    break;
                case 0x7C: // "Positioning Method Source", "Source of the navigation positioning information. (e.g., NAVSAT-GPS, NAVSAT-Galileo, INS)"
                    assertEquals(3, (long) result);
                    break;
                case 0x7D: // "Platform Status", "Enumeration of operational modes of the platform (e.g., in-route, RTB)"
                    assertEquals(9, (long) result);
                    break;
                case 0x7E: // "Sensor Control Mode", "Enumerated value for the current sensor control operational status"
                    assertEquals(5, (long) result);
                    break;
                case 0x7F: // "Sensor Frame Rate Pack", "Values used to compute the frame rate of the Motion Imagery at the sensor"
                case 0x80: // "Wavelengths List", "List of wavelength bands provided by sensor(s)"
                case 0x81: // "Target ID", "Alpha-numeric identification of a target"
                case 0x82: // "Airbase Locations", "Geographic location of the take-off site and recovery site"
                case 0x83: // "Take-off Time", "Time when aircraft became airborne", "microseconds"
                case 0x84: // "Transmission Frequency", "Radio frequency used to transmit the Motion Imagery", "MHz"
                case 0x85: // "On-board MI Storage Capacity", "The total capacity of on-board Motion Imagery storage", "GB"
                case 0x86: // "Zoom Percentage", "For a variable zoom system, the percentage of zoom", "%"
                case 0x87: // "Communications Method", "Type of communications used with platform"
                case 0x88: // "Leap Seconds", "Number of leap seconds to adjust Precision Time Stamp (Tag 2) to UTC", "s"
                case 0x89: // "Correction Offset", "Post-flight time adjustment to correct Precision Time Stamp (Tag 2) as needed", "microseconds"
                case 0x8A: // "Payload List", "List of payloads available on the Platform"
                case 0x8B: // "Active Payloads", "List of currently active payloads from the payload list (Tag 138)"
                case 0x8C: // "Weapons Stores", "List of weapon stores and status"
                case 0x8D: // "Waypoint List", "List of waypoints and their status"
                default:
                    break;
            }
        }

        assertNotNull(packet);
    }
}
