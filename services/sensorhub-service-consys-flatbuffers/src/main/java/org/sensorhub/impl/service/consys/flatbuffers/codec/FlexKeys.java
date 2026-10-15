/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2026 GeoRobotix Innovative Research. All Rights Reserved.

Author: Ian Patterson <ian.patterson@georobotix.us>

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.flatbuffers.codec;

import java.util.Set;


/**
 * <p>
 * Derives FlexBuffers map keys for a SWE record's children from their component
 * names. {@link FlexEncoder} and {@link FlexDecoder} both walk the same
 * (registry-derived) record structure in the same order and call
 * {@link #unique(Set, String)} per child, so they compute identical keys without
 * carrying key metadata on the wire. Mirrors {@code ProtoSchemaWriter}'s field
 * name sanitizing / de-duplication, so a datastream that encodes cleanly as
 * swe+proto keys the same way here.
 * </p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public final class FlexKeys
{
    private FlexKeys() {}

    /** Reserved keys used by the codec's own map structure (choice discriminator
     *  + payload, and the top-level result slot) — never collide with these
     *  because record children live one map level deeper. */
    public static final String CHOICE_CASE = "case";
    public static final String CHOICE_VALUE = "value";
    public static final String RESULT = "result";


    static String sanitize(String name)
    {
        if (name == null || name.isBlank())
            return "field";
        var s = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (!Character.isLetter(s.charAt(0)) && s.charAt(0) != '_')
            s = "_" + s;
        return s;
    }


    /** Return a key for {@code rawName} unique within {@code used} (adds it). */
    public static String unique(Set<String> used, String rawName)
    {
        var base = sanitize(rawName);
        var name = base;
        int n = 2;
        while (!used.add(name))
            name = base + n++;
        return name;
    }
}
