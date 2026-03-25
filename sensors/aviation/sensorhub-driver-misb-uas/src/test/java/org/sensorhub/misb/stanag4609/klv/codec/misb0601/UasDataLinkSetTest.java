/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv.codec.misb0601;

import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test suite for {@link UasDataLinkSet}
 *
 * @author Nick Garay
 * @since Feb. 6, 2020
 */
public class UasDataLinkSetTest {

    private final byte[] data =
            {
                    // UAS DataLink Universal Label
                    (byte) 0x06, (byte) 0x0E, (byte) 0x2B, (byte) 0x34, (byte) 0x02, (byte) 0x0B, (byte) 0x01, (byte) 0x01,
                    (byte) 0x0E, (byte) 0x01, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    // BER Encoded Length Long Form --> 225 Bytes of data (given by 0xE1)
                    (byte) 0x82, (byte) 0x00, (byte) 0xE1,
                    // Actual KLV Encoded data
                    (byte) 0x02, (byte) 0x08, (byte) 0x00, (byte) 0x05, (byte) 0x85,
                    (byte) 0x02, (byte) 0xFD, (byte) 0xBD, (byte) 0x2A, (byte) 0x6E, (byte) 0x41, (byte) 0x01, (byte) 0x03,
                    (byte) 0x0A, (byte) 0x1D, (byte) 0x41, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x56, (byte) 0x69,
                    (byte) 0x72, (byte) 0x6F, (byte) 0x6E, (byte) 0x6D, (byte) 0x65, (byte) 0x6E, (byte) 0x74, (byte) 0x20,
                    (byte) 0x53, (byte) 0x55, (byte) 0x41, (byte) 0x56, (byte) 0x20, (byte) 0x50, (byte) 0x75, (byte) 0x6D,
                    (byte) 0x61, (byte) 0x41, (byte) 0x45, (byte) 0x20, (byte) 0x44, (byte) 0x44, (byte) 0x4C, (byte) 0x30,
                    (byte) 0x23, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x03,
                    (byte) 0x04, (byte) 0x2F, (byte) 0x2F, (byte) 0x55, (byte) 0x53, (byte) 0x05, (byte) 0x06, (byte) 0x2F,
                    (byte) 0x2F, (byte) 0x46, (byte) 0x4F, (byte) 0x55, (byte) 0x4F, (byte) 0x06, (byte) 0x02, (byte) 0x55,
                    (byte) 0x53, (byte) 0x13, (byte) 0x01, (byte) 0x01, (byte) 0x14, (byte) 0x02, (byte) 0x00, (byte) 0x00,
                    (byte) 0x16, (byte) 0x02, (byte) 0x00, (byte) 0x05, (byte) 0x04, (byte) 0x07, (byte) 0x50, (byte) 0x41,
                    (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x30, (byte) 0x0D, (byte) 0x04, (byte) 0x37,
                    (byte) 0x89, (byte) 0x15, (byte) 0xC7, (byte) 0x0E, (byte) 0x04, (byte) 0xC3, (byte) 0x2E, (byte) 0x31,
                    (byte) 0x77, (byte) 0x0F, (byte) 0x02, (byte) 0x11, (byte) 0x4D, (byte) 0x10, (byte) 0x02, (byte) 0x2A,
                    (byte) 0x10, (byte) 0x11, (byte) 0x02, (byte) 0x2A, (byte) 0x10, (byte) 0x17, (byte) 0x04, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x19, (byte) 0x02, (byte) 0x0B, (byte) 0x97, (byte) 0x1A, (byte) 0x02, (byte) 0x00,
                    (byte) 0x00, (byte) 0x1B, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x1C, (byte) 0x02, (byte) 0x00,
                    (byte) 0x00, (byte) 0x1D, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x1E, (byte) 0x02, (byte) 0x40,
                    (byte) 0x93, (byte) 0x1F, (byte) 0x02, (byte) 0x80, (byte) 0x00, (byte) 0x20, (byte) 0x02, (byte) 0x3C,
                    (byte) 0x65, (byte) 0x21, (byte) 0x02, (byte) 0x80, (byte) 0x00, (byte) 0x0B, (byte) 0x02, (byte) 0x45,
                    (byte) 0x4F, (byte) 0x0C, (byte) 0x0E, (byte) 0x47, (byte) 0x65, (byte) 0x6F, (byte) 0x64, (byte) 0x65,
                    (byte) 0x74, (byte) 0x69, (byte) 0x63, (byte) 0x20, (byte) 0x57, (byte) 0x47, (byte) 0x53, (byte) 0x38,
                    (byte) 0x34, (byte) 0x15, (byte) 0x04, (byte) 0x00, (byte) 0x83, (byte) 0x0F, (byte) 0x14, (byte) 0x12,
                    (byte) 0x04, (byte) 0x17, (byte) 0x63, (byte) 0x09, (byte) 0x84, (byte) 0x13, (byte) 0x04, (byte) 0x04,
                    (byte) 0x51, (byte) 0x9E, (byte) 0xBE, (byte) 0x14, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x05, (byte) 0x02, (byte) 0x38, (byte) 0x4C, (byte) 0x06, (byte) 0x02, (byte) 0x10,
                    (byte) 0x16, (byte) 0x07, (byte) 0x02, (byte) 0x1F, (byte) 0xDB, (byte) 0x38, (byte) 0x01, (byte) 0x0B,
                    (byte) 0x01, (byte) 0x02, (byte) 0xBD, (byte) 0xA3
            };

    @Test
    public void testGetLength() {

        UasDataLinkSet packet = new UasDataLinkSet(data.length, data);
        assertEquals(data.length, packet.getLength());
    }

    @Test
    public void testGetPayload() {

        UasDataLinkSet packet = new UasDataLinkSet(data.length, data);
        assertEquals(data[0], packet.getPayload()[0]);
    }

    @Test
    public void testGetPrecisionTimeStamp() {

        UasDataLinkSet dataLinkSet = new UasDataLinkSet(data.length, data);
        dataLinkSet.decode();
        double expectedTime = 1553622777014L / 1000.0;
        assertEquals(expectedTime, dataLinkSet.getPrecisionTimeStamp(), 0.000000000001);
    }

    @Test
    public void testValidateChecksum() {

        UasDataLinkSet dataLinkSet = new UasDataLinkSet(data.length, data);

        assertTrue(dataLinkSet.validateChecksum());
    }

    @Test
    public void testValidateDesignator() {

        UasDataLinkSet dataLinkSet = new UasDataLinkSet(data.length, data);

        List<String> acceptedDesignators = new ArrayList<>();
        acceptedDesignators.add(UasDataLinkSet.UAS_LOCAL_SET.getDesignator());

        assertTrue(dataLinkSet.validateDesignator(acceptedDesignators));
    }

    public void testDecode() {

        HashMap<Tag, Object> valuesMap = new UasDataLinkSet(data.length, data).decode();

        TagRegistry reg = TagRegistry.getInstance();

//        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x01)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x02)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x41)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x38)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x0A)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x04)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x0B)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x0C)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x30)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x0D)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x0E)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x0F)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x19)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x10)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x11)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x17)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x18)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x1A)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x1C)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x1E)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x1B)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x1D)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x1F)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x20)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x21)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x15)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x12)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x13)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x14)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x05)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x06)));
        assertTrue(valuesMap.containsKey(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte)0x07)));
    }

    @Test
    public void testEncode() {

        TagRegistry reg = TagRegistry.getInstance();

        HashMap<Tag, Object> valuesMap = new UasDataLinkSet(data.length, data).decode();

        try {
            byte[] encodedKlv = new UasDataLinkSetEnc()
//                    .put((byte) 0x02, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x02))) // TimeStamp
                    .put((byte) 0x41, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x41))) // UAS Datalink LS Version Number
                    .put((byte) 0x0A, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x0A))) // Platform Designation
//                    .put((byte) 0x30, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x30))) // Security Local Set
                    .put((byte) 0x04, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x04))) // Platform Tail Number
                    .put((byte) 0x0D, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x0D))) // Sensor Latitude
                    .put((byte) 0x0E, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x0E))) // Sensor Longitude
                    .put((byte) 0x0F, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x0F))) // Sensor True Altitude
                    .put((byte) 0x10, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x10))) // Sensor Horizontal Field of View
                    .put((byte) 0x11, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x11))) // Sensor Vertical Field of View
                    .put((byte) 0x17, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x17))) // Frame Center Latitude
                    .put((byte) 0x18, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x18))) // Frame Center Longitude
                    .put((byte) 0x19, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x19))) // Frame Center Elevation
                    .put((byte) 0x1A, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x1A))) // Offset Corner Latitude Point 1
                    .put((byte) 0x1B, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x1B))) // Offset Corner Longitude Point 1
                    .put((byte) 0x1C, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x1C))) // Offset Corner Latitude Point 2
                    .put((byte) 0x1D, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x1D))) // Offset Corner Longitude Point 2
                    .put((byte) 0x1E, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x1E))) // Offset Corner Latitude Point 3
                    .put((byte) 0x1F, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x1F))) // Offset Corner Longitude Point 3
                    .put((byte) 0x20, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x20))) // Offset Corner Latitude Point 4
                    .put((byte) 0x21, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x21))) // Offset Corner Longitude Point 4
                    .put((byte) 0x0B, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x0B))) // Image Source Sensor
                    .put((byte) 0x0C, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x0C))) // Image Coordinate System
                    .put((byte) 0x15, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x15))) // Slant Range
                    .put((byte) 0x12, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x12))) // Sensor Relative Azimuth Angle
                    .put((byte) 0x13, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x13))) // Sensor Relative Elevation Angle
                    .put((byte) 0x14, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x14))) // Sensor Relative Roll Angle
                    .put((byte) 0x05, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x05))) // Platform Heading Angle
                    .put((byte) 0x06, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x06))) // Platform Pitch Angle
                    .put((byte) 0x07, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x07))) // Platform Roll Angle
                    .put((byte) 0x38, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x38))) // Platform Ground Speed
                    .put((byte) 0x01, valuesMap.get(reg.getByTagSetAndId(UasDataLinkSet.UAS_LOCAL_SET, (byte) 0x01))) // Checksum
                    .encode(true);

            HashMap<Tag, Object> postEncValuesMap = new UasDataLinkSet(encodedKlv.length, encodedKlv).decode();

            postEncValuesMap.forEach((key, value) -> {
                Object original = valuesMap.get(key);
                assertEquals(value, original);
            });

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
