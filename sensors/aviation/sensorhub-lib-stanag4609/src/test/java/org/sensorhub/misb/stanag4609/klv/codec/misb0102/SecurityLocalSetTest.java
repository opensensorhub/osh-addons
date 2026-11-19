/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.
 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv.codec.misb0102;

import org.junit.Test;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class SecurityLocalSetTest {

    byte[] data = {
            // "Security Local Set" ****************************************************************************
            // (byte) 0x30,         // Tag ID in UAS Local Set, denotes Security Set
            // (byte) 0x23,         // Length of data, 35 bytes
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x02, (byte) 0x01, (byte) 0x01,
            (byte) 0x03, (byte) 0x04, (byte) 0x2F, (byte) 0x2F, (byte) 0x55, (byte) 0x53, (byte) 0x05, (byte) 0x06,
            (byte) 0x2F, (byte) 0x2F, (byte) 0x46, (byte) 0x4F, (byte) 0x55, (byte) 0x4F, (byte) 0x06, (byte) 0x02,
            (byte) 0x55, (byte) 0x53, (byte) 0x13, (byte) 0x01, (byte) 0x01, (byte) 0x14, (byte) 0x02, (byte) 0x00,
            (byte) 0x00, (byte) 0x16, (byte) 0x02, (byte) 0x00, (byte) 0x05
    };

    @Test
    public void testDecode() {

        TagRegistry TAG_REGISTRY = TagRegistry.getInstance();

        HashMap<Tag, Object> valuesMap = new SecurityLocalSet(data.length, data).decode();

        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x01)));
        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x02)));
        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x03)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x04)));
        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x05)));
        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x06)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x07)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x08)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x09)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0A)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0B)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0C)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0D)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0E)));
        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x13)));
        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x14)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x15)));
        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x16)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x17)));
//        assertTrue(valuesMap.containsKey(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x18)));
    }

    @Test
    public void testEncode() {

        TagRegistry TAG_REGISTRY = TagRegistry.getInstance();

        HashMap<Tag, Object> valuesMap = new SecurityLocalSet(data.length, data).decode();

        try {
            byte[] encodedKlv = new SecurityLocalSetEnc()
                    .put((byte) 0x01, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x01)))
                    .put((byte) 0x02, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x02)))
                    .put((byte) 0x03, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x03)))
//                    .put((byte) 0x04, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x04)));
                    .put((byte) 0x05, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x05)))
                    .put((byte) 0x06, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x06)))
//                    .put((byte) 0x07, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x07)))
//                    .put((byte) 0x08, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x08)))
//                    .put((byte) 0x09, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x09)))
//                    .put((byte) 0x0A, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0A)))
//                    .put((byte) 0x0B, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0B)))
//                    .put((byte) 0x0C, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0C)))
//                    .put((byte) 0x0D, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0D)))
//                    .put((byte) 0x0E, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x0E)))
                    .put((byte) 0x13, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x13)))
                    .put((byte) 0x14, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x14)))
//                    .put((byte) 0x15, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x15)))
                    .put((byte) 0x16, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.SECURITY_LOCAL_SET, (byte) 0x16)))
//                    .put((byte) 0x17, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x17)))
//                    .put((byte) 0x18, valuesMap.get(TAG_REGISTRY.getByTagSetAndId(SecurityLocalSet.UAS_LOCAL_SET_SECURITY, (byte) 0x18)))
                    .encode(false);

            HashMap<Tag, Object> postEncValuesMap = new SecurityLocalSet(encodedKlv.length, encodedKlv).decode();

            postEncValuesMap.forEach((key, value) -> {
                Object original = valuesMap.get(key);
                assertEquals(value, original);
            });

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
