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
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
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
 * Decodes a protobuf message (parsed against a {@link ProtoSchemaWriter}-built
 * descriptor) back into a flat SWE Common {@link DataBlock} — the exact mirror
 * of {@link ProtoObsEncoder}. This is the inbound half of the swe+proto codec,
 * used to ingest commands (and, later, observations) POSTed/published as
 * {@code application/swe+proto}.
 * </p>
 *
 * <p>
 * Like the encoder, it walks the SWE component tree and the message descriptor
 * in lock-step (proto types are driven by {@link FieldDescriptor#getType()},
 * never re-derived from the component) with one flat running atom index across
 * nested message boundaries. The record components start at field 6 in the top
 * message (after the fixed envelope, see
 * {@link ProtoSchemaWriter#COMMAND_ENVELOPE_FIELD_NAMES}); nested messages are
 * numbered from 1. proto3 semantics apply: absent scalar fields decode as their
 * type defaults (0 / false / "").
 * </p>
 *
 * <p>
 * Envelope fields are read with the static accessors ({@link #getString},
 * {@link #getInstant}) — callers decide which envelope fields they trust (e.g.
 * a server ignores the wire {@code sender} in favor of the authenticated user).
 * </p>
 *
 * @see ProtoObsEncoder
 * @see ProtoSchemaWriter
 * @author Ian Patterson
 * @since 2026
 */
public final class ProtoRecordDecoder
{
    private ProtoRecordDecoder() {}


    /**
     * Decode the SWE record components (message fields 6+) of {@code msg} into
     * a fresh {@link DataBlock} laid out per {@code struct} (the record
     * structure the descriptor was generated from).
     *
     * <p><b>Mutates {@code struct}'s {@link DataChoice} selection state</b>
     * (selections must be applied before {@code createDataBlock()} so the
     * block is sized for the selected item) — callers should pass a private
     * copy of a shared record structure.</p>
     */
    public static DataBlock decodeRecord(DataComponent struct, Message msg)
    {
        // pre-pass: apply choice selections so createDataBlock() allocates
        // room for the selected items (an unselected choice is 1 atom only)
        walkTop(struct, (child, fieldNum) -> selectChoices(msg, child, fieldNum));

        var block = struct.createDataBlock();
        var idx = new int[]{0};
        walkTop(struct, (child, fieldNum) -> decodeComponent(msg, child, fieldNum, block, idx));
        return block;
    }


    private interface ComponentVisitor
    {
        int visit(DataComponent child, int fieldNum);
    }


    /** Walk the top-level components (message fields 6+, after the envelope). */
    private static void walkTop(DataComponent struct, ComponentVisitor visitor)
    {
        var structured = struct instanceof DataRecord || struct instanceof Vector;
        var n = structured ? struct.getComponentCount() : 1;

        int fieldNum = 6;   // record components follow the 1–5 envelope
        for (int i = 0; i < n; i++)
        {
            var child = structured ? struct.getComponent(i) : struct;
            fieldNum = visitor.visit(child, fieldNum);
        }
    }


    /**
     * Selection pre-pass: for every {@link DataChoice}, look up which oneof
     * field is set in {@code msg} and apply it via
     * {@link DataChoice#setSelectedItem(int)}. Mirrors the field-number walk
     * of {@link #decodeComponent} without touching atoms.
     */
    private static int selectChoices(Message msg, DataComponent comp, int fieldNum)
    {
        if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            int selected = selectedIndex(msg, fieldNum, choice);
            choice.setSelectedItem(selected);
            // the selected item may itself contain nested choices
            selectChoices(msg, choice.getComponent(selected), fieldNum + selected);
            return fieldNum + choice.getComponentCount();
        }

        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            var f = field(msg.getDescriptorForType(), fieldNum);
            var sub = (Message) msg.getField(f);
            int subFieldNum = 1;
            for (int i = 0; i < comp.getComponentCount(); i++)
                subFieldNum = selectChoices(sub, comp.getComponent(i), subFieldNum);
            return fieldNum + 1;
        }

        if (comp instanceof RangeComponent)
            return fieldNum + 2;

        return fieldNum + 1;
    }


    /** @return the index of the choice item whose oneof field is set in {@code msg}. */
    private static int selectedIndex(Message msg, int fieldNum, DataChoice choice)
    {
        var first = field(msg.getDescriptorForType(), fieldNum);
        var oneof = first.getContainingOneof();
        if (oneof == null)
            throw new IllegalStateException(
                "schema/decoder mismatch: field #" + fieldNum + " is not part of a oneof for choice '"
                + choice.getName() + "'");
        var set = msg.getOneofFieldDescriptor(oneof);
        if (set == null)
            throw new IllegalArgumentException(
                "DataChoice '" + choice.getName() + "': no command selected (no oneof field set)");
        int selected = set.getNumber() - fieldNum;
        if (selected < 0 || selected >= choice.getComponentCount())
            throw new IllegalStateException(
                "schema/decoder mismatch: oneof field #" + set.getNumber() + " out of range for choice '"
                + choice.getName() + "'");
        return selected;
    }


    /** Read an envelope string field by name; returns null if absent/empty. */
    public static String getString(Message msg, String fieldName)
    {
        var f = msg.getDescriptorForType().findFieldByName(fieldName);
        if (f == null)
            return null;
        var s = (String) msg.getField(f);
        return s == null || s.isEmpty() ? null : s;
    }


    /** Read an envelope {@code google.protobuf.Timestamp} field by name;
     *  returns null if the field is absent or unset. */
    public static Instant getInstant(Message msg, String fieldName)
    {
        var f = msg.getDescriptorForType().findFieldByName(fieldName);
        if (f == null || !msg.hasField(f))
            return null;
        var ts = (Message) msg.getField(f);
        var tsDesc = ts.getDescriptorForType();
        long secs = (Long) ts.getField(tsDesc.findFieldByName("seconds"));
        int nanos = (Integer) ts.getField(tsDesc.findFieldByName("nanos"));
        return Instant.ofEpochSecond(secs, nanos);
    }


    private static int decodeComponent(Message msg, DataComponent comp, int fieldNum, DataBlock block, int[] idx)
    {
        if (comp instanceof DataArray)
            throw new UnsupportedOperationException(
                "DataArray is not yet supported in swe+proto decoding (field '" + comp.getName() + "')");

        // choice → oneof: atom 0 of the choice is the selected-item index,
        // followed by the selected item's atoms (selection was already applied
        // to the component tree by the pre-pass)
        if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            int selected = selectedIndex(msg, fieldNum, choice);
            block.setIntValue(idx[0]++, selected);
            decodeComponent(msg, choice.getComponent(selected), fieldNum + selected, block, idx);
            return fieldNum + choice.getComponentCount();
        }

        // nested record/vector → recurse into the sub-message (fields from 1)
        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            var f = field(msg.getDescriptorForType(), fieldNum);
            var sub = (Message) msg.getField(f);
            int subFieldNum = 1;
            for (int i = 0; i < comp.getComponentCount(); i++)
                subFieldNum = decodeComponent(sub, comp.getComponent(i), subFieldNum, block, idx);
            return fieldNum + 1;
        }

        // range → two scalar fields, two atoms
        if (comp instanceof RangeComponent)
        {
            getScalar(msg, field(msg.getDescriptorForType(), fieldNum), block, idx);
            getScalar(msg, field(msg.getDescriptorForType(), fieldNum + 1), block, idx);
            return fieldNum + 2;
        }

        // ISO time → Timestamp sub-message (one atom: epoch seconds as double)
        if (comp instanceof Time && ((Time) comp).isIsoTime())
        {
            var f = field(msg.getDescriptorForType(), fieldNum);
            var ts = (Message) msg.getField(f);
            var tsDesc = ts.getDescriptorForType();
            long secs = (Long) ts.getField(tsDesc.findFieldByName("seconds"));
            int nanos = (Integer) ts.getField(tsDesc.findFieldByName("nanos"));
            block.setDoubleValue(idx[0]++, secs + nanos / 1e9);
            return fieldNum + 1;
        }

        // plain scalar
        getScalar(msg, field(msg.getDescriptorForType(), fieldNum), block, idx);
        return fieldNum + 1;
    }


    private static void getScalar(Message msg, FieldDescriptor f, DataBlock block, int[] idx)
    {
        int i = idx[0]++;
        var v = msg.getField(f);
        switch (f.getType())
        {
            case FLOAT:
                block.setFloatValue(i, (Float) v);
                break;
            case DOUBLE:
                block.setDoubleValue(i, (Double) v);
                break;
            case INT32: case SINT32: case SFIXED32:
            case UINT32: case FIXED32:
                block.setIntValue(i, (Integer) v);
                break;
            case INT64: case SINT64: case SFIXED64:
            case UINT64: case FIXED64:
                block.setLongValue(i, (Long) v);
                break;
            case BOOL:
                block.setBooleanValue(i, (Boolean) v);
                break;
            case STRING:
                block.setStringValue(i, (String) v);
                break;
            default:
                throw new IllegalStateException(
                    "Unsupported proto field type " + f.getType() + " for field '" + f.getName() + "'");
        }
    }


    private static FieldDescriptor field(Descriptor desc, int number)
    {
        var f = desc.findFieldByNumber(number);
        if (f == null)
            throw new IllegalStateException(
                "schema/decoder field mismatch: no field #" + number + " in " + desc.getFullName());
        return f;
    }
}
