/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package org.sensorhub.misb.stanag4609.klv.codec;

import org.sensorhub.misb.stanag4609.klv.Element;
import org.sensorhub.misb.stanag4609.klv.exceptions.ElementEncodingException;
import org.sensorhub.misb.stanag4609.tags.Tag;
import org.sensorhub.misb.stanag4609.tags.TagRegistry;
import org.sensorhub.misb.stanag4609.tags.TagSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class SetEncoder {

    private final TagSet tagSet;

    private final Map<Tag, byte[]> elements = new LinkedHashMap<>();

    public SetEncoder(TagSet tagSet) {

        this.tagSet = tagSet;
    }

    public void put(byte tagId, Object value) {

        Tag tag = TagRegistry.getInstance().getByTagSetAndId(tagSet, tagId);

        if (tag == null) {

            throw new ElementEncodingException("Unknown tag " + tagId);
        }

        elements.put(tag, encodeValue(tag, value));
    }

    public byte[] encode(boolean withKeyAndLength) throws IOException {

        ByteArrayOutputStream valueStream = new ByteArrayOutputStream();

        for (var e : elements.entrySet()) {
            Tag tag = e.getKey();
            byte[] raw = e.getValue();

            valueStream.write(tag.getLocalSetTag());
            writeBerLength(valueStream, raw.length);
            valueStream.write(raw);
        }

        byte[] valueBytes = valueStream.toByteArray();

        ByteArrayOutputStream klv = new ByteArrayOutputStream();
        if (withKeyAndLength) {
            writeTagSet(klv, tagSet);
            writeBerLength(klv, valueBytes.length);
        }
        klv.write(valueBytes);

        return klv.toByteArray();
    }

    private static void writeTagSet(ByteArrayOutputStream out, TagSet tagSet) {

        String[] tokens = tagSet.getDesignator().split(" ");
        for (String token : tokens) {
            out.write(HexFormat.fromHexDigits(token));
        }
    }
    private static void writeBerLength(ByteArrayOutputStream out, int length) {

        if (length < 0x80) {
            out.write(length);
        } else {
            out.write(0x81);
            out.write(length);
        }
    }

    private static byte[] encodeValue(Tag tagId, Object value) {

        Element element = new Element(tagId, value);
        return element.packData();
    }
}
