/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.impl.sensor.ffmpeg.klv;

import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;

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
}
