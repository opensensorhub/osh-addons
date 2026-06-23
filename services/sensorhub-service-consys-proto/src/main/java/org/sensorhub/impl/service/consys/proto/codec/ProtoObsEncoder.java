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

package org.sensorhub.impl.service.consys.proto.codec;

import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaWriter;
import java.time.Instant;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
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
 * Encodes a SWE Common {@link DataBlock} into a protobuf {@link DynamicMessage}
 * conforming to a {@link Descriptor} produced by {@link ProtoSchemaWriter}.
 * </p>
 *
 * <p>
 * This is the value-encoding counterpart to {@link ProtoSchemaWriter} and MUST
 * walk the SWE component tree in the same order so that field numbers line up.
 * It walks two structures in lock-step: the component tree (to interpret SWE
 * semantics — ISO time, ranges, nesting) and the {@link Descriptor} (to drive
 * the actual proto types via {@link FieldDescriptor#getType()}). The
 * {@link DataBlock} is flat, so a single running atom index advances across
 * nested message boundaries. Using {@code DynamicMessage} guarantees the emitted
 * bytes match the descriptor — there is no hand-coordinated field numbering.
 * </p>
 *
 * <p>
 * Supports the same shapes {@link ProtoSchemaWriter} emits: records/vectors →
 * nested messages, ISO {@code Time} → {@code Timestamp}, ranges → two fields,
 * scalars → one field, {@code DataChoice} → {@code oneof}, and {@code DataArray}
 * (fixed- or variable-size) → {@code repeated}. Arrays with a non-flat
 * ({@code DataBlockList}) element — one hiding a {@code DataChoice} or a
 * variable-size sub-array — throw, since the flat atom-index walk cannot address
 * a list-backed block.
 * </p>
 *
 * @see ProtoSchemaWriter
 * @author Ian Patterson
 * @since 2026
 */
public final class ProtoObsEncoder
{
    private ProtoObsEncoder() {}


    /** Observation metadata for the fixed envelope (message fields 1–5). */
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


    /** Command metadata for the fixed command envelope (message fields 1–5,
     *  see {@link ProtoSchemaWriter#COMMAND_ENVELOPE_FIELD_NAMES}). */
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


    /**
     * Encode {@code data} (the result block of an observation) into a message of
     * type {@code desc}. The fixed envelope (fields 1–5) is filled from
     * {@code env}; the SWE record components are read from {@code data} starting
     * at field 6, interpreting them through {@code struct} (the record structure
     * {@code desc} was generated from).
     */
    public static DynamicMessage encode(DataComponent struct, Descriptor desc, DataBlock data, Envelope env)
    {
        struct.setData(data);   // bind so variable-size DataArray.getComponentCount() reflects the data
        var msg = DynamicMessage.newBuilder(desc);
        setEnvelope(msg, desc, env);
        encodeRecord(msg, desc, struct, data);
        return msg.build();
    }


    /**
     * Command counterpart of {@link #encode}: fills the command envelope
     * (fields 1–5) from {@code env} and the SWE parameter components from
     * {@code data} starting at field 6.
     */
    public static DynamicMessage encodeCommand(DataComponent struct, Descriptor desc, DataBlock data, CommandEnvelope env)
    {
        struct.setData(data);   // bind so variable-size DataArray.getComponentCount() reflects the data
        var msg = DynamicMessage.newBuilder(desc);
        setCommandEnvelope(msg, desc, env);
        encodeRecord(msg, desc, struct, data);
        return msg.build();
    }


    private static void encodeRecord(DynamicMessage.Builder msg, Descriptor desc, DataComponent struct, DataBlock data)
    {
        var idx = new int[]{0};
        var structured = struct instanceof DataRecord || struct instanceof Vector;
        var n = structured ? struct.getComponentCount() : 1;

        int fieldNum = 6;   // record components follow the 1–5 envelope
        for (int i = 0; i < n; i++)
        {
            var child = structured ? struct.getComponent(i) : struct;
            fieldNum = encodeComponent(msg, desc, child, fieldNum, data, idx);
        }
    }


    private static void setEnvelope(DynamicMessage.Builder msg, Descriptor desc, Envelope env)
    {
        if (env == null)
            return;
        setString(msg, desc, "id", env.id);
        setString(msg, desc, "datastream_id", env.datastreamId);
        setString(msg, desc, "foi_id", env.foiId);
        setTimestamp(msg, desc, "phenomenon_time", env.phenomenonTime);
        setTimestamp(msg, desc, "result_time", env.resultTime);
    }


    private static void setCommandEnvelope(DynamicMessage.Builder msg, Descriptor desc, CommandEnvelope env)
    {
        if (env == null)
            return;
        setString(msg, desc, "id", env.id);
        setString(msg, desc, "controlstream_id", env.controlstreamId);
        setString(msg, desc, "foi_id", env.foiId);
        setTimestamp(msg, desc, "issue_time", env.issueTime);
        setString(msg, desc, "sender", env.sender);
    }


    private static void setString(DynamicMessage.Builder msg, Descriptor desc, String name, String value)
    {
        if (value == null || value.isEmpty())
            return;
        var f = desc.findFieldByName(name);
        if (f != null)
            msg.setField(f, value);
    }


    private static void setTimestamp(DynamicMessage.Builder msg, Descriptor desc, String name, Instant t)
    {
        if (t == null)
            return;
        var f = desc.findFieldByName(name);
        if (f == null)
            return;
        var tsDesc = f.getMessageType();
        msg.setField(f, DynamicMessage.newBuilder(tsDesc)
            .setField(tsDesc.findFieldByName("seconds"), t.getEpochSecond())
            .setField(tsDesc.findFieldByName("nanos"), t.getNano())
            .build());
    }


    private static DynamicMessage encodeMessage(DataComponent comp, Descriptor desc, DataBlock data, int[] idx)
    {
        var msg = DynamicMessage.newBuilder(desc);
        var structured = comp instanceof DataRecord || comp instanceof Vector;
        var n = structured ? comp.getComponentCount() : 1;

        int fieldNum = 1;
        for (int i = 0; i < n; i++)
        {
            var child = structured ? comp.getComponent(i) : comp;
            fieldNum = encodeComponent(msg, desc, child, fieldNum, data, idx);
        }
        return msg.build();
    }


    private static int encodeComponent(DynamicMessage.Builder msg, Descriptor desc, DataComponent comp,
                                       int fieldNum, DataBlock data, int[] idx)
    {
        // array → repeated field. The flat DataBlock lays the K elements out
        // contiguously (K = getComponentCount(), made accurate for variable-size
        // arrays — including nested rectangular matrices — by the setData() bind
        // in encode()/encodeCommand()), so the running atom index walks straight
        // through them. Only a DataChoice element is rejected by the guard below.
        if (comp instanceof DataArray)
        {
            var array = (DataArray) comp;
            var elt = array.getElementType();
            if (ProtoArrays.elementHasChoice(elt))
                throw new UnsupportedOperationException(
                    "DataArray whose element contains a DataChoice is not yet supported "
                    + "in swe+proto encoding (field '" + comp.getName() + "')");
            var f = field(desc, fieldNum);
            // record/vector element → repeated nested message; a nested DataArray
            // element (a Matrix row) → repeated wrapper message (encodeMessage
            // wraps the non-structured inner array as that message's field 1)
            boolean nested = elt instanceof DataRecord || elt instanceof Vector || elt instanceof DataArray;
            int size = array.getComponentCount();
            for (int e = 0; e < size; e++)
            {
                if (nested)
                {
                    msg.addRepeatedField(f, encodeMessage(elt, f.getMessageType(), data, idx));
                }
                else
                {
                    var v = scalarValue(f.getType(), data, idx[0]++);
                    msg.addRepeatedField(f, v != null ? v : "");   // null only for STRING
                }
            }
            return fieldNum + 1;
        }

        // choice → oneof: atom 0 is the selected-item index; only the selected
        // item's field (fieldNum + index) is set, fed by the following atoms
        if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            int selected = data.getIntValue(idx[0]++);
            if (selected < 0 || selected >= choice.getComponentCount())
                throw new IllegalStateException(
                    "DataChoice '" + comp.getName() + "' has no valid selection (index " + selected + ")");
            encodeComponent(msg, desc, choice.getComponent(selected), fieldNum + selected, data, idx);
            return fieldNum + choice.getComponentCount();
        }

        // nested record/vector → sub-message
        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            var f = field(desc, fieldNum);
            msg.setField(f, encodeMessage(comp, f.getMessageType(), data, idx));
            return fieldNum + 1;
        }

        // range → two scalar fields, two atoms
        if (comp instanceof RangeComponent)
        {
            setScalar(msg, field(desc, fieldNum), data, idx);
            setScalar(msg, field(desc, fieldNum + 1), data, idx);
            return fieldNum + 2;
        }

        // ISO time → Timestamp sub-message (one atom: epoch seconds)
        if (comp instanceof Time && ((Time) comp).isIsoTime())
        {
            var f = field(desc, fieldNum);
            msg.setField(f, timestamp(data.getDoubleValue(idx[0]++), f.getMessageType()));
            return fieldNum + 1;
        }

        // plain scalar
        setScalar(msg, field(desc, fieldNum), data, idx);
        return fieldNum + 1;
    }


    private static void setScalar(DynamicMessage.Builder msg, FieldDescriptor f, DataBlock data, int[] idx)
    {
        var v = scalarValue(f.getType(), data, idx[0]++);
        if (v != null)                 // STRING may be null; setField(f, null) would NPE
            msg.setField(f, v);
    }


    /** Box one flat {@link DataBlock} atom as the value for a proto field of
     *  {@code type}. Shared by the scalar and repeated-array encode paths. */
    private static Object scalarValue(FieldDescriptor.Type type, DataBlock data, int i)
    {
        switch (type)
        {
            case FLOAT:  return data.getFloatValue(i);
            case DOUBLE: return data.getDoubleValue(i);
            case INT32: case SINT32: case SFIXED32:
            case UINT32: case FIXED32: return data.getIntValue(i);
            case INT64: case SINT64: case SFIXED64:
            case UINT64: case FIXED64: return data.getLongValue(i);
            case BOOL:   return data.getBooleanValue(i);
            case STRING: return data.getStringValue(i);
            default:
                throw new IllegalStateException("Unsupported proto field type " + type);
        }
    }


    private static DynamicMessage timestamp(double epochSeconds, Descriptor tsDesc)
    {
        long secs = (long) Math.floor(epochSeconds);
        int nanos = (int) Math.round((epochSeconds - secs) * 1e9);
        if (nanos >= 1_000_000_000) { secs++; nanos -= 1_000_000_000; }
        if (nanos < 0) nanos = 0;
        return DynamicMessage.newBuilder(tsDesc)
            .setField(tsDesc.findFieldByName("seconds"), secs)
            .setField(tsDesc.findFieldByName("nanos"), nanos)
            .build();
    }


    private static FieldDescriptor field(Descriptor desc, int number)
    {
        var f = desc.findFieldByNumber(number);
        if (f == null)
            throw new IllegalStateException(
                "schema/encoder field mismatch: no field #" + number + " in " + desc.getFullName());
        return f;
    }
}
