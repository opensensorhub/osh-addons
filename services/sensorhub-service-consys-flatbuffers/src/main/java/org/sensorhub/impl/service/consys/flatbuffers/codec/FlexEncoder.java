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

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import com.google.flatbuffers.FlexBuffersBuilder;
import net.opengis.swe.v20.Count;
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
 * Encodes a SWE Common {@link DataBlock} into a self-describing FlexBuffers map.
 * FlexBuffers is the schema-less arm of FlatBuffers, chosen because
 * FlatBuffers-java has no runtime "dynamic table" facility (no protobuf
 * {@code DynamicMessage} analog): the per-datastream shape cannot be built and
 * written at runtime, so values ride as keyed FlexBuffers maps/vectors and the
 * logical shape is described out of band as a JSON schema.
 * </p>
 *
 * <p>
 * The top-level payload is a map of the fixed observation/command envelope
 * (mirroring {@code weather_observation.fbs}: {@code id}, {@code datastream_id},
 * {@code foi_id}, {@code phenomenon_time}, {@code result_time}) plus a
 * {@code result} slot holding the encoded record. The record is walked in the
 * same lock-step discipline as {@code ProtoEncoder} — one running flat atom index
 * across nested boundaries — so a datastream that encodes as swe+proto encodes
 * the same atoms here:
 * </p>
 * <ul>
 *   <li>{@code DataRecord} / {@code Vector} → nested map keyed by child name</li>
 *   <li>{@code DataArray} → vector (variable size is inherent — no pre-sizing on
 *       the wire, unlike the proto codec's array-length bookkeeping)</li>
 *   <li>{@code DataChoice} → map {@code {case:&lt;index&gt;, value:&lt;item&gt;}}</li>
 *   <li>{@code RangeComponent} → 2-element vector {@code [low, high]}</li>
 *   <li>ISO {@code Time} → epoch-seconds double; other scalars → their FlexBuffers type</li>
 * </ul>
 *
 * <p>Unlike protobuf, FlexBuffers carries element counts inline, so the decoder
 * needs no length prepass. A {@code DataChoice} inside an array element is still
 * rejected (a {@code DataBlockList} the flat-index walk can't address — a
 * property of the SWE block layout, not the wire format).</p>
 *
 * @see FlexDecoder
 * @author Ian Patterson
 * @since 2026
 */
public final class FlexEncoder
{
    private FlexEncoder() {}


    /** Observation metadata for the fixed envelope. */
    public static final class Envelope
    {
        final String id;
        final String datastreamId;
        final String foiId;
        final Instant phenomenonTime;
        final Instant resultTime;

        public Envelope(String id, String datastreamId, String foiId, Instant phenomenonTime, Instant resultTime)
        {
            this.id = id;
            this.datastreamId = datastreamId;
            this.foiId = foiId;
            this.phenomenonTime = phenomenonTime;
            this.resultTime = resultTime;
        }
    }


    /** Command metadata for the fixed command envelope. */
    public static final class CommandEnvelope
    {
        final String id;
        final String controlstreamId;
        final String foiId;
        final Instant issueTime;
        final String sender;

        public CommandEnvelope(String id, String controlstreamId, String foiId, Instant issueTime, String sender)
        {
            this.id = id;
            this.controlstreamId = controlstreamId;
            this.foiId = foiId;
            this.issueTime = issueTime;
            this.sender = sender;
        }
    }


    /** Encode an observation result block as a FlexBuffers payload (unframed). */
    public static byte[] encode(DataComponent struct, DataBlock data, Envelope env)
    {
        sizeVariableArrays(struct, data);   // make variable-size arrays' getComponentCount() reflect the data
        var b = new FlexBuffersBuilder(FlexBuffersBuilder.BUILDER_FLAG_SHARE_KEYS);
        int root = b.startMap();
        putStr(b, "id", env != null ? env.id : null);
        putStr(b, "datastream_id", env != null ? env.datastreamId : null);
        putStr(b, "foi_id", env != null ? env.foiId : null);
        putInstant(b, "phenomenon_time", env != null ? env.phenomenonTime : null);
        putInstant(b, "result_time", env != null ? env.resultTime : null);
        encodeValue(b, FlexKeys.RESULT, struct, data, new int[]{0});
        b.endMap(null, root);
        return finishBytes(b);
    }


    /** Encode a command parameter block as a FlexBuffers payload (unframed). */
    public static byte[] encodeCommand(DataComponent struct, DataBlock data, CommandEnvelope env)
    {
        sizeVariableArrays(struct, data);
        var b = new FlexBuffersBuilder(FlexBuffersBuilder.BUILDER_FLAG_SHARE_KEYS);
        int root = b.startMap();
        putStr(b, "id", env != null ? env.id : null);
        putStr(b, "controlstream_id", env != null ? env.controlstreamId : null);
        putStr(b, "foi_id", env != null ? env.foiId : null);
        putInstant(b, "issue_time", env != null ? env.issueTime : null);
        putStr(b, "sender", env != null ? env.sender : null);
        encodeValue(b, FlexKeys.RESULT, struct, data, new int[]{0});
        b.endMap(null, root);
        return finishBytes(b);
    }


    /**
     * Encode one component under map key {@code key} (or as the next vector
     * element when {@code key} is null), advancing {@code idx} through the flat
     * block exactly as {@code ProtoEncoder.encodeComponent} does.
     */
    private static void encodeValue(FlexBuffersBuilder b, String key, DataComponent comp, DataBlock data, int[] idx)
    {
        if (comp instanceof DataArray)
        {
            var array = (DataArray) comp;
            var elt = array.getElementType();
            if (FlexArrays.elementHasChoice(elt))
                throw new UnsupportedOperationException(
                    "DataArray whose element contains a DataChoice is not yet supported "
                    + "in swe+flatbuffers encoding (field '" + comp.getName() + "')");
            int size = array.getComponentCount();
            int start = b.startVector();
            for (int e = 0; e < size; e++)
                encodeValue(b, null, elt, data, idx);   // elements are keyless in the vector
            b.endVector(key, start, false, false);
            return;
        }

        if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            int selected = data.getIntValue(idx[0]++);
            if (selected < 0 || selected >= choice.getComponentCount())
                throw new IllegalStateException(
                    "DataChoice '" + comp.getName() + "' has no valid selection (index " + selected + ")");
            int start = b.startMap();
            b.putInt(FlexKeys.CHOICE_CASE, selected);
            encodeValue(b, FlexKeys.CHOICE_VALUE, choice.getComponent(selected), data, idx);
            b.endMap(key, start);
            return;
        }

        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            int start = b.startMap();
            var used = new HashSet<String>();
            for (int i = 0; i < comp.getComponentCount(); i++)
            {
                var child = comp.getComponent(i);
                encodeValue(b, FlexKeys.unique(used, child.getName()), child, data, idx);
            }
            b.endMap(key, start);
            return;
        }

        if (comp instanceof RangeComponent)
        {
            var t = FlexTypes.of(comp);
            int start = b.startVector();
            putScalar(b, null, t, data, idx[0]++);
            putScalar(b, null, t, data, idx[0]++);
            b.endVector(key, start, false, false);
            return;
        }

        if (comp instanceof Time && ((Time) comp).isIsoTime())
        {
            double v = data.getDoubleValue(idx[0]++);
            if (key == null) b.putFloat(v); else b.putFloat(key, v);
            return;
        }

        putScalar(b, key, FlexTypes.of(comp), data, idx[0]++);
    }


    /** Write one flat atom as the FlexBuffers value for {@code type} (keyed if
     *  {@code key != null}, else as a vector element). */
    private static void putScalar(FlexBuffersBuilder b, String key, FlexTypes type, DataBlock data, int i)
    {
        switch (type)
        {
            case BOOL:
                if (key == null) b.putBoolean(data.getBooleanValue(i)); else b.putBoolean(key, data.getBooleanValue(i));
                break;
            case INT:
                if (key == null) b.putInt(data.getIntValue(i)); else b.putInt(key, data.getIntValue(i));
                break;
            case LONG:
                if (key == null) b.putInt(data.getLongValue(i)); else b.putInt(key, data.getLongValue(i));
                break;
            case FLOAT:
                if (key == null) b.putFloat(data.getFloatValue(i)); else b.putFloat(key, data.getFloatValue(i));
                break;
            case DOUBLE:
                if (key == null) b.putFloat(data.getDoubleValue(i)); else b.putFloat(key, data.getDoubleValue(i));
                break;
            case STRING:
            default:
                var s = data.getStringValue(i);
                if (s == null) s = "";
                if (key == null) b.putString(s); else b.putString(key, s);
                break;
        }
    }


    private static void putStr(FlexBuffersBuilder b, String key, String v)
    {
        if (v != null && !v.isEmpty())
            b.putString(key, v);
    }


    private static void putInstant(FlexBuffersBuilder b, String key, Instant t)
    {
        if (t != null)
            b.putFloat(key, t.getEpochSecond() + t.getNano() / 1e9);
    }


    private static byte[] finishBytes(FlexBuffersBuilder b)
    {
        ByteBuffer bb = b.finish();
        byte[] out = new byte[bb.remaining()];
        bb.get(out);
        return out;
    }


    // ─── Variable-array pre-sizing (ported verbatim from ProtoEncoder) ────────
    //
    // Make every variable-size DataArray's getComponentCount() reflect the data
    // before the encode walk, so encodeValue() emits the right element count.
    // Reads each array's size Count value from the flat block (its LEN/ROWS/COLS
    // atom), the same way ProtoEncoder does; a single struct.setData(data) cannot
    // size sibling-Count-driven nested (Matrix) variable arrays in one pass.

    private static void sizeVariableArrays(DataComponent struct, DataBlock data)
    {
        var counts = new HashMap<String, Integer>();
        var idx = new int[]{0};
        var structured = struct instanceof DataRecord || struct instanceof Vector;
        var n = structured ? struct.getComponentCount() : 1;
        for (int i = 0; i < n; i++)
            sizeComponent(structured ? struct.getComponent(i) : struct, data, idx, counts);
    }


    private static void sizeComponent(DataComponent comp, DataBlock data, int[] idx, Map<String, Integer> counts)
    {
        if (comp instanceof DataArray)
        {
            var array = (DataArray) comp;
            if (array.isVariableSize())
            {
                var sizeComp = array.getArraySizeComponent();
                var sizeId = sizeComp != null ? sizeComp.getId() : null;
                var size = sizeId != null ? counts.get(sizeId) : null;
                if (size == null)
                    throw new IllegalStateException(
                        "swe+flatbuffers encode: cannot size variable array '" + comp.getName()
                        + "' — its size component" + (sizeId != null ? " '" + sizeId + "'" : "")
                        + " was not found as a Count before it in the record");
                array.updateSize(size);
            }
            int size = array.getComponentCount();
            var elt = array.getElementType();
            for (int e = 0; e < size; e++)
                sizeComponent(elt, data, idx, counts);
            return;
        }

        if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            int selected = data.getIntValue(idx[0]++);
            if (selected >= 0 && selected < choice.getComponentCount())
                sizeComponent(choice.getComponent(selected), data, idx, counts);
            return;
        }

        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            for (int i = 0; i < comp.getComponentCount(); i++)
                sizeComponent(comp.getComponent(i), data, idx, counts);
            return;
        }

        if (comp instanceof RangeComponent)   // two atoms, never a size source
        {
            idx[0] += 2;
            return;
        }

        if (comp instanceof Count && comp.getId() != null)
            counts.put(comp.getId(), data.getIntValue(idx[0]));
        idx[0]++;
    }
}
