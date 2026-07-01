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

import java.time.Instant;
import java.util.HashSet;
import com.google.flatbuffers.FlexBuffers;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.RangeComponent;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;


/**
 * <p>
 * Decodes a FlexBuffers payload produced by {@link FlexEncoder} back into a flat
 * SWE Common {@link DataBlock} — the exact mirror of the encoder. It walks the
 * (registry-derived) record structure and the self-describing FlexBuffers tree
 * in lock-step with one running flat atom index.
 * </p>
 *
 * <p>
 * Because FlexBuffers carries element counts and keys inline, the pre-pass only
 * has to read vector lengths (to size variable {@link DataArray}s) and choice
 * discriminators (to select the {@link DataChoice} item) before
 * {@code createDataBlock()} — no wire-length bookkeeping is needed the way the
 * proto codec requires. {@code decodeResult} <b>mutates {@code struct}'s
 * DataChoice selection state</b>, so callers must pass a private copy of the
 * shared record structure.
 * </p>
 *
 * @see FlexEncoder
 * @author Ian Patterson
 * @since 2026
 */
public final class FlexDecoder
{
    private FlexDecoder() {}


    /**
     * Decode the {@code result} value of a payload into a fresh {@link DataBlock}
     * laid out per {@code struct}.
     *
     * @param struct    the record structure (a private, mutable copy)
     * @param resultRef the FlexBuffers reference to the payload's {@code result}
     *                  slot (i.e. {@code rootMap.get("result")})
     */
    public static DataBlock decodeResult(DataComponent struct, FlexBuffers.Reference resultRef)
    {
        prepass(resultRef, struct);          // size arrays + apply choice selections
        var block = struct.createDataBlock();
        decodeValue(resultRef, struct, block, new int[]{0});
        return block;
    }


    /**
     * Pre-pass run before {@code createDataBlock()}: apply every DataChoice
     * selection and size every variable-size DataArray from its FlexBuffers
     * vector length, so the block is allocated at the right size.
     */
    private static void prepass(FlexBuffers.Reference ref, DataComponent comp)
    {
        if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            var map = ref.asMap();
            int selected = map.get(FlexKeys.CHOICE_CASE).asInt();
            if (selected < 0 || selected >= choice.getComponentCount())
                throw new IllegalStateException(
                    "DataChoice '" + comp.getName() + "': selection index " + selected + " out of range");
            choice.setSelectedItem(selected);
            prepass(map.get(FlexKeys.CHOICE_VALUE), choice.getComponent(selected));
            return;
        }

        if (comp instanceof DataArray)
        {
            var array = (DataArray) comp;
            var vec = ref.asVector();
            int m = vec.size();
            if (array.isVariableSize())
                array.updateSize(m);
            // nested array (Matrix): size the shared inner dimension from row 0
            if (m > 0 && array.getElementType() instanceof DataArray)
                prepass(vec.get(0), array.getElementType());
            return;
        }

        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            var map = ref.asMap();
            var used = new HashSet<String>();
            for (int i = 0; i < comp.getComponentCount(); i++)
            {
                var child = comp.getComponent(i);
                prepass(map.get(FlexKeys.unique(used, child.getName())), child);
            }
            return;
        }

        // range, scalar, ISO time: fixed atom count, nothing to size
    }


    private static void decodeValue(FlexBuffers.Reference ref, DataComponent comp, DataBlock block, int[] idx)
    {
        if (comp instanceof DataArray)
        {
            var array = (DataArray) comp;
            var elt = array.getElementType();
            if (FlexArrays.elementHasChoice(elt))
                throw new UnsupportedOperationException(
                    "DataArray whose element contains a DataChoice is not yet supported "
                    + "in swe+flatbuffers decoding (field '" + comp.getName() + "')");
            var vec = ref.asVector();
            int size = array.getComponentCount();
            if (vec.size() != size)
                throw new IllegalStateException(
                    "array '" + comp.getName() + "': wire length " + vec.size() + " != sized " + size);
            for (int e = 0; e < size; e++)
                decodeValue(vec.get(e), elt, block, idx);
            return;
        }

        if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            var map = ref.asMap();
            int selected = map.get(FlexKeys.CHOICE_CASE).asInt();
            block.setIntValue(idx[0]++, selected);
            decodeValue(map.get(FlexKeys.CHOICE_VALUE), choice.getComponent(selected), block, idx);
            return;
        }

        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            var map = ref.asMap();
            var used = new HashSet<String>();
            for (int i = 0; i < comp.getComponentCount(); i++)
            {
                var child = comp.getComponent(i);
                decodeValue(map.get(FlexKeys.unique(used, child.getName())), child, block, idx);
            }
            return;
        }

        if (comp instanceof RangeComponent)
        {
            var t = FlexTypes.of(comp);
            var vec = ref.asVector();
            setAtom(block, idx[0]++, t, vec.get(0));
            setAtom(block, idx[0]++, t, vec.get(1));
            return;
        }

        if (comp instanceof Time && ((Time) comp).isIsoTime())
        {
            block.setDoubleValue(idx[0]++, ref.asFloat());
            return;
        }

        setAtom(block, idx[0]++, FlexTypes.of(comp), ref);
    }


    /** Write one FlexBuffers reference into flat {@link DataBlock} atom {@code i}. */
    private static void setAtom(DataBlock block, int i, FlexTypes type, FlexBuffers.Reference ref)
    {
        switch (type)
        {
            case BOOL:   block.setBooleanValue(i, ref.asBoolean()); break;
            case INT:    block.setIntValue(i, ref.asInt()); break;
            case LONG:   block.setLongValue(i, ref.asLong()); break;
            case FLOAT:  block.setFloatValue(i, (float) ref.asFloat()); break;
            case DOUBLE: block.setDoubleValue(i, ref.asFloat()); break;
            case STRING:
            default:     block.setStringValue(i, ref.asString()); break;
        }
    }


    /** Read an envelope string field by key; null if absent/empty. */
    public static String getString(FlexBuffers.Map map, String key)
    {
        var ref = map.get(key);
        if (ref == null || ref.isNull())
            return null;
        var s = ref.asString();
        return s == null || s.isEmpty() ? null : s;
    }


    /** Read an envelope epoch-seconds field by key as an {@link Instant}; null if absent. */
    public static Instant getInstant(FlexBuffers.Map map, String key)
    {
        var ref = map.get(key);
        if (ref == null || ref.isNull() || !ref.isNumeric())
            return null;
        double s = ref.asFloat();
        long secs = (long) Math.floor(s);
        int nanos = (int) Math.round((s - secs) * 1e9);
        if (nanos >= 1_000_000_000) { secs++; nanos -= 1_000_000_000; }
        if (nanos < 0) nanos = 0;
        return Instant.ofEpochSecond(secs, nanos);
    }
}
