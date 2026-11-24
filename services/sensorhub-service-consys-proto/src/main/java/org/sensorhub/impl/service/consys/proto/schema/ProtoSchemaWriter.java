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

package org.sensorhub.impl.service.consys.proto.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.vast.util.Asserts;
import com.georobotix.swecommon.SweOptions;
import com.google.protobuf.Timestamp;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.HasRefFrames;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.RangeComponent;
import net.opengis.swe.v20.SimpleComponent;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;


/**
 * <p>
 * Translates a SWE Common {@link DataComponent} (a datastream's record
 * structure) into a protobuf {@link FileDescriptorProto} describing a single
 * observation message, with SWE semantics ({@code definition}, {@code label},
 * {@code uomCode}, {@code referenceFrame}, …) attached to each field as
 * {@link SweOptions} {@code FieldOptions} extensions.
 * </p>
 *
 * <p>
 * The output is the serialized {@code FileDescriptorProto} that ships inside
 * {@code DataStream.schema} (field 50, {@code ProtoObservationSchema}) and that
 * {@link DataStreamSchemaCache#register} resolves on the receiving side. The two
 * file-level imports it emits — {@value #SWE_OPTIONS_PROTO} (always) and
 * {@value #TIMESTAMP_PROTO} (only when an ISO {@code Time} field is present) —
 * MUST match the bootstrap descriptor names registered by
 * {@code ConSysApiProtoService} (see step-2 name-matching contract), or the
 * receiver cannot resolve the schema.
 * </p>
 *
 * <h3>Scope (v1)</h3>
 * <ul>
 *   <li>Translates the <b>full record</b> — every child component, unfiltered.
 *       The value encoder (step 5) must write the same set in the same order.</li>
 *   <li>{@code DataRecord} / {@code Vector} → nested message types; a {@code Vector}'s
 *       {@code referenceFrame} rides on the field that references it.</li>
 *   <li>{@code DataArray} → {@code repeated <elementType>} (the array's size /
 *       {@code elementCount} component is intentionally not emitted as a field).</li>
 *   <li>{@code RangeComponent} → two fields {@code <name>_min} / {@code <name>_max}.</li>
 *   <li>Scalars: Quantity → float/double, Count → int32/uint32, Boolean → bool,
 *       Text/Category → string, ISO Time → {@code google.protobuf.Timestamp},
 *       numeric Time → double.</li>
 *   <li>Not yet handled: {@code DataChoice}, {@code Category} enums, constraints.</li>
 * </ul>
 *
 * @see DataStreamSchemaCache
 * @author Ian Patterson
 * @since 2026
 */
public class ProtoSchemaWriter
{
    /** Import path for the SWE FieldOptions extensions — must equal the
     *  bootstrap key {@code SweOptions.getDescriptor().getName()}. */
    public static final String SWE_OPTIONS_PROTO = "swecommon/swe_options.proto";

    /** Import path for the Timestamp well-known type — must equal the bootstrap
     *  key {@code Timestamp.getDescriptor().getFile().getName()}. */
    public static final String TIMESTAMP_PROTO = "google/protobuf/timestamp.proto";

    static final String TIMESTAMP_TYPE = ".google.protobuf.Timestamp";

    /** Fixed observation-envelope field names occupying numbers 1–5; the SWE
     *  record components follow at 6+. Matches the OSHConnect swe+proto
     *  contract (oshconnect.swe_protobuf.ENVELOPE_FIELD_NAMES). */
    public static final String[] ENVELOPE_FIELD_NAMES = {
        "id", "datastream_id", "foi_id", "phenomenon_time", "result_time"
    };

    /** Fixed command-envelope field names occupying numbers 1–5; the SWE
     *  parameter components follow at 6+. Mirrors the observation envelope
     *  (and the vendored command.proto semantics: controlstream@id,
     *  issueTime, sender). Fields 1–3 + 5 are strings, 4 is a Timestamp. */
    public static final String[] COMMAND_ENVELOPE_FIELD_NAMES = {
        "id", "controlstream_id", "foi_id", "issue_time", "sender"
    };


    /** A built schema: the {@link FileDescriptorProto} plus the fully-qualified
     *  name of the observation message defined inside it (what
     *  {@link DataStreamSchemaCache#register} needs as {@code messageTypeFqn}). */
    public static class Result
    {
        public final FileDescriptorProto fileDescriptor;
        public final String messageTypeName;

        Result(FileDescriptorProto fileDescriptor, String messageTypeName)
        {
            this.fileDescriptor = fileDescriptor;
            this.messageTypeName = messageTypeName;
        }

        public byte[] toByteArray()
        {
            return fileDescriptor.toByteArray();
        }
    }


    /** Per-call state: which file imports are needed and which message type
     *  names are already taken (to keep nested type names unique). */
    private static class BuildState
    {
        boolean usesTimestamp = false;
        final Set<String> usedMessageFqns = new HashSet<>();
    }


    /**
     * Build a {@link FileDescriptorProto} describing {@code root} as a message
     * named {@code messageName} in {@code packageName}.
     *
     * @param root        the record structure to translate (typically a
     *                    {@code DataRecord}; any {@link DataComponent} works).
     * @param fileName    the descriptor file name (e.g. {@code "datastreams/x/obs.proto"}).
     * @param packageName the proto package for the generated message.
     * @param messageName the (simple) name of the root observation message.
     */
    public Result write(DataComponent root, String fileName, String packageName, String messageName)
    {
        return write(root, fileName, packageName, messageName, ENVELOPE_FIELD_NAMES);
    }


    /**
     * Same as {@link #write} but the root message carries the fixed
     * <b>command</b> envelope ({@link #COMMAND_ENVELOPE_FIELD_NAMES}) at
     * fields 1–5, with the control stream's SWE parameter components at 6+.
     */
    public Result writeCommand(DataComponent root, String fileName, String packageName, String messageName)
    {
        return write(root, fileName, packageName, messageName, COMMAND_ENVELOPE_FIELD_NAMES);
    }


    private Result write(DataComponent root, String fileName, String packageName, String messageName, String[] envelope)
    {
        Asserts.checkNotNull(root, DataComponent.class);
        Asserts.checkNotNull(messageName, "messageName");
        var pkg = packageName != null ? packageName : "";

        var state = new BuildState();
        var scopeFqn = pkg.isEmpty() ? "" : "." + pkg;
        var rootName = uniqueMessageName(state, scopeFqn, sanitizeType(messageName, "Observation"));
        var rootMsg = buildEnvelopeMessage(root, rootName, scopeFqn, state, envelope);

        var file = FileDescriptorProto.newBuilder()
            .setName(fileName)
            .setSyntax("proto3")
            .addDependency(SWE_OPTIONS_PROTO)
            .addMessageType(rootMsg);
        if (!pkg.isEmpty())
            file.setPackage(pkg);
        if (state.usesTimestamp)
            file.addDependency(TIMESTAMP_PROTO);

        var fqn = pkg.isEmpty() ? rootName : pkg + "." + rootName;
        return new Result(file.build(), fqn);
    }


    /**
     * Package the schema as a serialized {@code FileDescriptorSet} for delivery
     * over the wire, per the OSHConnect swe+proto contract: the set carries the
     * observation schema plus its non-google import ({@code swe_options.proto}),
     * while google well-known types ({@code timestamp.proto},
     * {@code descriptor.proto}) are intentionally omitted — the receiver seeds
     * those from its own runtime ({@code oshconnect.swe_protobuf._build_message_class}).
     * {@code swe_options} carries no message types, so the receiver still sees
     * exactly one message (the observation) in the set.
     */
    public static byte[] toFileDescriptorSet(Result result)
    {
        return FileDescriptorSet.newBuilder()
            .addFile(SweOptions.getDescriptor().toProto())   // non-google dep travels
            .addFile(result.fileDescriptor)                  // the observation schema
            .build()
            .toByteArray();
    }


    /**
     * Resolve a {@link Result} into a usable {@link Descriptor} for the root
     * observation message, wiring its imports to the well-known bootstrap
     * descriptors ({@code swe_options.proto} / {@code timestamp.proto}). This is
     * the in-process equivalent of what {@link DataStreamSchemaCache#register}
     * does for descriptors arriving over the wire.
     */
    public static Descriptor resolve(Result result) throws DescriptorValidationException
    {
        var fdp = result.fileDescriptor;
        var deps = new ArrayList<FileDescriptor>(fdp.getDependencyCount());
        for (var dep : fdp.getDependencyList())
        {
            if (SWE_OPTIONS_PROTO.equals(dep))
                deps.add(SweOptions.getDescriptor());
            else if (TIMESTAMP_PROTO.equals(dep))
                deps.add(Timestamp.getDescriptor().getFile());
            else
                throw new IllegalStateException("Unexpected descriptor dependency: " + dep);
        }
        var file = FileDescriptor.buildFrom(fdp, deps.toArray(new FileDescriptor[0]));
        return file.getMessageTypes().get(0);
    }


    /**
     * Build the top-level message: a fixed metadata envelope at field numbers
     * 1–5 followed by the SWE record's components at 6+. The envelope shape is
     * either {@link #ENVELOPE_FIELD_NAMES} (observations: 3 strings + 2
     * Timestamps) or {@link #COMMAND_ENVELOPE_FIELD_NAMES} (commands: 3
     * strings + issue_time Timestamp + sender string). Only this top message
     * carries the envelope; nested records/vectors are plain messages
     * numbered from 1.
     */
    private DescriptorProto buildEnvelopeMessage(DataComponent record, String msgName, String scopeFqn, BuildState state, String[] envelope)
    {
        var msg = DescriptorProto.newBuilder().setName(msgName);
        var myFqn = scopeFqn + "." + msgName;

        // fixed envelope (1–5) — resource metadata, no swe_options
        state.usesTimestamp = true;
        msg.addField(stringField(envelope[0], 1));     // id
        msg.addField(stringField(envelope[1], 2));     // datastream_id / controlstream_id
        msg.addField(stringField(envelope[2], 3));     // foi_id
        msg.addField(timestampField(envelope[3], 4));  // phenomenon_time / issue_time
        if (envelope == COMMAND_ENVELOPE_FIELD_NAMES)
            msg.addField(stringField(envelope[4], 5)); // sender
        else
            msg.addField(timestampField(envelope[4], 5)); // result_time

        // SWE record components (6+)
        var usedFieldNames = new HashSet<>(Arrays.asList(envelope));
        int fieldNum = 6;
        if (isStructured(record))
        {
            for (int i = 0; i < record.getComponentCount(); i++)
                fieldNum = addComponent(msg, record.getComponent(i), fieldNum, myFqn, usedFieldNames, state);
        }
        else
        {
            fieldNum = addComponent(msg, record, fieldNum, myFqn, usedFieldNames, state);
        }

        return msg.build();
    }


    private static FieldDescriptorProto stringField(String name, int num)
    {
        return FieldDescriptorProto.newBuilder()
            .setName(name).setNumber(num)
            .setLabel(Label.LABEL_OPTIONAL).setType(Type.TYPE_STRING)
            .build();
    }


    private static FieldDescriptorProto timestampField(String name, int num)
    {
        return FieldDescriptorProto.newBuilder()
            .setName(name).setNumber(num)
            .setLabel(Label.LABEL_OPTIONAL).setType(Type.TYPE_MESSAGE).setTypeName(TIMESTAMP_TYPE)
            .build();
    }


    /**
     * Build a message from {@code comp}. If {@code comp} is structured
     * ({@code DataRecord}/{@code Vector}) its children become fields; otherwise
     * {@code comp} itself is emitted as the single field of a wrapper message.
     *
     * @param scopeFqn the fully-qualified (leading-dot) name of the enclosing
     *                 scope — package for the root, or the parent message's FQN.
     */
    private DescriptorProto buildMessage(DataComponent comp, String msgName, String scopeFqn, BuildState state)
    {
        var msg = DescriptorProto.newBuilder().setName(msgName);
        var myFqn = scopeFqn + "." + msgName;
        var usedFieldNames = new HashSet<String>();

        int fieldNum = 1;
        if (isStructured(comp))
        {
            for (int i = 0; i < comp.getComponentCount(); i++)
                fieldNum = addComponent(msg, comp.getComponent(i), fieldNum, myFqn, usedFieldNames, state);
        }
        else
        {
            fieldNum = addComponent(msg, comp, fieldNum, myFqn, usedFieldNames, state);
        }

        return msg.build();
    }


    /**
     * Append one SWE component to {@code msg} as one or more proto fields
     * (records/vectors add a nested type + message field; ranges add two
     * fields; arrays add a repeated field; scalars add one field).
     *
     * @return the next free field number.
     */
    private int addComponent(DescriptorProto.Builder msg, DataComponent comp, int fieldNum,
                             String scopeFqn, Set<String> usedFieldNames, BuildState state)
    {
        // DataChoice → proto3 oneof: one field per choice item, exactly one of
        // which is set per message. The selection index in the SWE DataBlock
        // (atom 0 of the choice) maps to WHICH oneof field is populated.
        if (comp instanceof DataChoice)
            return addChoice(msg, (DataChoice) comp, fieldNum, scopeFqn, usedFieldNames, state);

        // Aggregate → nested message + reference field
        if (comp instanceof DataRecord || comp instanceof Vector)
        {
            var nestedName = uniqueMessageName(state, scopeFqn, deriveTypeName(comp));
            msg.addNestedType(buildMessage(comp, nestedName, scopeFqn, state));
            msg.addField(FieldDescriptorProto.newBuilder()
                .setName(uniqueFieldName(usedFieldNames, comp.getName()))
                .setNumber(fieldNum)
                .setLabel(Label.LABEL_OPTIONAL)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(scopeFqn + "." + nestedName)
                .setOptions(sweOptions(comp)));
            return fieldNum + 1;
        }

        // Array → repeated field of the element type
        if (comp instanceof DataArray)
        {
            var elt = ((DataArray) comp).getElementType();
            var fb = FieldDescriptorProto.newBuilder()
                .setName(uniqueFieldName(usedFieldNames, comp.getName()))
                .setNumber(fieldNum)
                .setLabel(Label.LABEL_REPEATED)
                .setOptions(sweOptions(comp));

            if (elt instanceof DataRecord || elt instanceof Vector || elt instanceof DataArray)
            {
                // record/vector element → nested message; nested DataArray element
                // (a Matrix row — proto has no repeated-of-repeated) → wrapper
                // message holding the inner array as its single repeated field
                // (buildMessage wraps a non-structured component automatically)
                var nestedName = uniqueMessageName(state, scopeFqn, deriveTypeName(elt));
                msg.addNestedType(buildMessage(elt, nestedName, scopeFqn, state));
                fb.setType(Type.TYPE_MESSAGE).setTypeName(scopeFqn + "." + nestedName);
            }
            else
            {
                fb.setType(scalarProtoType(elt));
            }
            msg.addField(fb);
            return fieldNum + 1;
        }

        // Range → two scalar fields (_min / _max)
        if (comp instanceof RangeComponent)
        {
            var t = scalarProtoType(comp);
            var opts = sweOptions(comp);
            msg.addField(scalarField(uniqueFieldName(usedFieldNames, comp.getName() + "_min"), fieldNum, t, opts));
            msg.addField(scalarField(uniqueFieldName(usedFieldNames, comp.getName() + "_max"), fieldNum + 1, t, opts));
            return fieldNum + 2;
        }

        // ISO time → Timestamp message field
        if (comp instanceof Time && ((Time) comp).isIsoTime())
        {
            state.usesTimestamp = true;
            msg.addField(FieldDescriptorProto.newBuilder()
                .setName(uniqueFieldName(usedFieldNames, comp.getName()))
                .setNumber(fieldNum)
                .setLabel(Label.LABEL_OPTIONAL)
                .setType(Type.TYPE_MESSAGE)
                .setTypeName(TIMESTAMP_TYPE)
                .setOptions(sweOptions(comp)));
            return fieldNum + 1;
        }

        // Plain scalar
        msg.addField(scalarField(uniqueFieldName(usedFieldNames, comp.getName()),
            fieldNum, scalarProtoType(comp), sweOptions(comp)));
        return fieldNum + 1;
    }


    /**
     * Emit a {@link DataChoice} as a proto3 {@code oneof}: each choice item
     * becomes exactly one field (scalar, Timestamp, or nested message) tagged
     * with the oneof's index, occupying consecutive field numbers. Items that
     * cannot map to a single field (ranges, arrays, nested choices as direct
     * items) are rejected — wrapping them in a {@code DataRecord} works.
     * Choice-level SWE semantics are not emitted (swe_options is FieldOptions
     * only; OneofOptions are out of scope per the v1 decision) — per-item
     * semantics ride on each item's field as usual.
     */
    private int addChoice(DescriptorProto.Builder msg, DataChoice choice, int fieldNum,
                          String scopeFqn, Set<String> usedFieldNames, BuildState state)
    {
        var oneofIndex = msg.getOneofDeclCount();
        msg.addOneofDecl(OneofDescriptorProto.newBuilder()
            .setName(uniqueFieldName(usedFieldNames, sanitizeField(choice.getName(), "choice"))));

        for (int i = 0; i < choice.getComponentCount(); i++)
        {
            var item = choice.getComponent(i);
            if (item instanceof DataChoice || item instanceof DataArray || item instanceof RangeComponent)
                throw new UnsupportedOperationException(
                    item.getClass().getSimpleName() + " is not supported as a direct DataChoice item in swe+proto "
                    + "(item '" + item.getName() + "') — wrap it in a DataRecord");

            FieldDescriptorProto.Builder fb;
            if (item instanceof DataRecord || item instanceof Vector)
            {
                var nestedName = uniqueMessageName(state, scopeFqn, deriveTypeName(item));
                msg.addNestedType(buildMessage(item, nestedName, scopeFqn, state));
                fb = FieldDescriptorProto.newBuilder()
                    .setType(Type.TYPE_MESSAGE)
                    .setTypeName(scopeFqn + "." + nestedName);
            }
            else if (item instanceof Time && ((Time) item).isIsoTime())
            {
                state.usesTimestamp = true;
                fb = FieldDescriptorProto.newBuilder()
                    .setType(Type.TYPE_MESSAGE)
                    .setTypeName(TIMESTAMP_TYPE);
            }
            else
            {
                fb = FieldDescriptorProto.newBuilder().setType(scalarProtoType(item));
            }

            msg.addField(fb
                .setName(uniqueFieldName(usedFieldNames, item.getName()))
                .setNumber(fieldNum + i)
                .setLabel(Label.LABEL_OPTIONAL)
                .setOneofIndex(oneofIndex)
                .setOptions(sweOptions(item)));
        }

        return fieldNum + choice.getComponentCount();
    }


    private static FieldDescriptorProto.Builder scalarField(String name, int num, Type type, FieldOptions opts)
    {
        return FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(num)
            .setLabel(Label.LABEL_OPTIONAL)
            .setType(type)
            .setOptions(opts);
    }


    /** Map a scalar SWE component to a proto field type. */
    static Type scalarProtoType(DataComponent comp)
    {
        if (comp instanceof net.opengis.swe.v20.Boolean)
            return Type.TYPE_BOOL;
        if (comp instanceof Text || comp instanceof Category)
            return Type.TYPE_STRING;
        if (comp instanceof Time && !((Time) comp).isIsoTime())
            return Type.TYPE_DOUBLE;

        if (comp instanceof SimpleComponent)
        {
            var dt = ((SimpleComponent) comp).getDataType();
            if (dt != null)
            {
                switch (dt)
                {
                    case FLOAT:  return Type.TYPE_FLOAT;
                    case DOUBLE: return Type.TYPE_DOUBLE;
                    case BYTE: case SHORT: case INT: return Type.TYPE_INT32;
                    case UBYTE: case USHORT: case UINT: return Type.TYPE_UINT32;
                    case LONG:   return Type.TYPE_INT64;
                    case ULONG:  return Type.TYPE_UINT64;
                    default: break;
                }
            }
        }

        // sensible defaults when no explicit data type is set
        if (comp instanceof Quantity) return Type.TYPE_DOUBLE;
        if (comp instanceof Count) return Type.TYPE_INT32;

        // unknown scalar — fail loud rather than guessing string (which would
        // misrepresent the field and desync the value encoder)
        throw new UnsupportedOperationException(
            "Unsupported SWE scalar type for swe+proto: " + comp.getClass().getSimpleName()
            + " (field '" + comp.getName() + "')");
    }


    /** Collect the SWE semantics of {@code comp} into swe_options FieldOptions. */
    static FieldOptions sweOptions(DataComponent comp)
    {
        var b = FieldOptions.newBuilder();

        if (notEmpty(comp.getIdentifier()))
            b.setExtension(SweOptions.id, comp.getIdentifier());
        if (notEmpty(comp.getDefinition()))
            b.setExtension(SweOptions.definition, comp.getDefinition());
        if (notEmpty(comp.getLabel()))
            b.setExtension(SweOptions.label, comp.getLabel());
        if (notEmpty(comp.getDescription()))
            b.setExtension(SweOptions.description, comp.getDescription());

        if (comp instanceof HasUom)
        {
            var uom = ((HasUom) comp).getUom();
            if (uom != null)
            {
                if (notEmpty(uom.getCode()))
                    b.setExtension(SweOptions.uomCode, uom.getCode());
                else if (notEmpty(uom.getHref()))
                    b.setExtension(SweOptions.uomHref, uom.getHref());
            }
        }

        if (comp instanceof HasRefFrames)
        {
            var rf = (HasRefFrames) comp;
            if (notEmpty(rf.getReferenceFrame()))
                b.setExtension(SweOptions.referenceFrame, rf.getReferenceFrame());
            if (notEmpty(rf.getLocalFrame()))
                b.setExtension(SweOptions.localFrame, rf.getLocalFrame());
        }

        if (comp instanceof SimpleComponent && notEmpty(((SimpleComponent) comp).getAxisID()))
            b.setExtension(SweOptions.axisID, ((SimpleComponent) comp).getAxisID());

        if (comp instanceof Time)
        {
            var t = (Time) comp;
            if (t.isSetReferenceTime() && t.getReferenceTime() != null)
                b.setExtension(SweOptions.referenceTime, t.getReferenceTime().toString());
        }

        return b.build();
    }


    private static boolean isStructured(DataComponent comp)
    {
        return comp instanceof DataRecord || comp instanceof Vector;
    }


    private String uniqueMessageName(BuildState state, String scopeFqn, String base)
    {
        var name = base;
        int n = 2;
        while (!state.usedMessageFqns.add(scopeFqn + "." + name))
            name = base + n++;
        return name;
    }


    private static String uniqueFieldName(Set<String> used, String rawName)
    {
        var base = sanitizeField(rawName, "field");
        var name = base;
        int n = 2;
        while (!used.add(name))
            name = base + n++;
        return name;
    }


    /** Derive a PascalCase message type name from a component's name. */
    private static String deriveTypeName(DataComponent comp)
    {
        var s = sanitizeType(comp.getName(), "Msg");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }


    private static String sanitizeField(String name, String fallback)
    {
        if (name == null || name.isBlank())
            return fallback;
        var s = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (!Character.isLetter(s.charAt(0)) && s.charAt(0) != '_')
            s = "_" + s;
        return s;
    }


    private static String sanitizeType(String name, String fallback)
    {
        if (name == null || name.isBlank())
            return fallback;
        var s = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (!Character.isLetter(s.charAt(0)))
            s = "M" + s;
        return s;
    }


    private static boolean notEmpty(String s)
    {
        return s != null && !s.isBlank();
    }
}
