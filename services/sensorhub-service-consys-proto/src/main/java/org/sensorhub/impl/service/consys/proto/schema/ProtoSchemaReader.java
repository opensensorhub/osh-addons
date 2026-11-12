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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.vast.swe.SWEHelper;
import com.georobotix.swecommon.SweOptions;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.HasRefFrames;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.SimpleComponent;
import net.opengis.swe.v20.Time;


/**
 * <p>
 * Inverse of {@link ProtoSchemaWriter}: reconstructs a SWE Common
 * {@link DataComponent} record structure from a resolved protobuf
 * {@link Descriptor} (the observation/command message), reading the
 * {@code swe_options} {@link SweOptions} {@code FieldOptions} extensions
 * (field numbers 1000–1009) to recover each field's SWE semantics
 * ({@code definition}, {@code label}, {@code uomCode}, {@code referenceFrame},
 * …). This is the "ingest a client-supplied schema" half of the bidirectional
 * design — a peer ships a {@code FileDescriptorSet}, we rebuild the record
 * structure so the datastream/control stream can be created without us having
 * authored the schema.
 * </p>
 *
 * <h3>Lossy-but-decodable reconstruction</h3>
 * <p>
 * The writer is not injective (a proto {@code string} can come from SWE
 * {@code Text} or {@code Category}; a {@code double} from {@code Quantity} or a
 * numeric {@code Time}; a nested message from {@code DataRecord} or
 * {@code Vector}; a {@code RangeComponent} becomes two fields). The reader does
 * not try to recover the exact original subtype; it reconstructs a structure
 * that is <b>atom-layout-identical</b>, so {@link ProtoRecordDecoder} — which
 * walks the SWE tree and the descriptor in lock-step and produces one
 * {@link net.opengis.swe.v20.DataBlock} atom per scalar — decodes against it
 * unchanged. Canonicalizations:
 * </p>
 * <ul>
 *   <li>proto {@code float}/{@code double} → {@code Quantity} (matching data type)</li>
 *   <li>proto int family → {@code Count} (matching data type)</li>
 *   <li>proto {@code bool} → {@code Boolean}, {@code string} → {@code Text}</li>
 *   <li>{@code google.protobuf.Timestamp} message → ISO {@code Time}</li>
 *   <li>other nested message → {@code DataRecord} (a {@code Vector} round-trips
 *       as a {@code DataRecord}; both decode identically)</li>
 *   <li>proto3 {@code oneof} → {@code DataChoice} (one item per oneof field)</li>
 *   <li>a {@code RangeComponent}'s two writer-emitted fields come back as two
 *       independent scalars — same two atoms, decodes identically</li>
 * </ul>
 *
 * <p>
 * A {@code repeated} field becomes a <b>variable-size</b> {@code DataArray}
 * (its element the field's scalar/record type, or — for a Matrix-row wrapper
 * message — a nested {@code DataArray}); {@link ProtoRecordDecoder} sizes it
 * from the repeated field's wire length on decode. (A foreign-ingested array is
 * always variable-size, since the descriptor carries no count.)
 * </p>
 *
 * <p>
 * Reading the {@code swe_options} extensions requires the {@link Descriptor} to
 * have been parsed with an {@link com.google.protobuf.ExtensionRegistry} that
 * knows them (see {@link DataStreamSchemaCache#setExtensionRegistry}); otherwise
 * the annotations land in unknown fields and are silently dropped.
 * </p>
 *
 * @see ProtoSchemaWriter
 * @see ProtoRecordDecoder
 * @author Ian Patterson
 * @since 2026
 */
public class ProtoSchemaReader
{
    /** Field numbers 1–5 are the fixed resource envelope (see
     *  {@link ProtoSchemaWriter#ENVELOPE_FIELD_NAMES} /
     *  {@link ProtoSchemaWriter#COMMAND_ENVELOPE_FIELD_NAMES}); the SWE record
     *  components start at field 6. */
    static final int FIRST_RECORD_FIELD = 6;

    private final SWEHelper swe = new SWEHelper();


    /**
     * Reconstruct the SWE record structure carried by an observation/command
     * message descriptor. Only fields numbered {@value #FIRST_RECORD_FIELD}+
     * are read (the 1–5 envelope is metadata, not part of the SWE record).
     *
     * @param msgDesc the resolved root message descriptor.
     * @return a {@link DataRecord} mirroring the record components.
     */
    public DataRecord readRecord(Descriptor msgDesc)
    {
        return buildRecord(msgDesc, FIRST_RECORD_FIELD, sanitizeName(msgDesc.getName()));
    }


    /**
     * Build a {@link DataRecord} from {@code desc}'s fields whose number is
     * {@code >= minFieldNum} (6 for the root message, 1 for nested messages),
     * grouping {@code oneof} members into a {@link net.opengis.swe.v20.DataChoice}.
     */
    private DataRecord buildRecord(Descriptor desc, int minFieldNum, String name)
    {
        var rec = swe.createRecord().build();
        rec.setName(name);

        // fields in ascending number order; oneof members stay consecutive
        var fields = new ArrayList<>(desc.getFields());
        fields.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));

        var consumedOneofs = new java.util.HashSet<OneofDescriptor>();
        for (var f : fields)
        {
            if (f.getNumber() < minFieldNum)
                continue;   // skip the envelope at the top level

            var oneof = f.getRealContainingOneof();
            if (oneof != null)
            {
                if (!consumedOneofs.add(oneof))
                    continue;   // already emitted as a DataChoice
                rec.addComponent(oneof.getName(), buildChoice(oneof));
            }
            else
            {
                rec.addComponent(f.getName(), buildComponent(f));
            }
        }
        return rec;
    }


    /** Build a {@link net.opengis.swe.v20.DataChoice} from a proto3 oneof —
     *  one item per oneof field, in field-number order (the selection atom is
     *  synthesized by {@code createDataBlock()}, matching the encoder). */
    private DataComponent buildChoice(OneofDescriptor oneof)
    {
        var choice = swe.createChoice().build();
        choice.setName(oneof.getName());
        var fields = new ArrayList<>(oneof.getFields());
        fields.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
        for (var f : fields)
            choice.addComponent(f.getName(), buildComponent(f));
        return choice;
    }


    /** Build a single SWE component for one proto field (not a oneof member). */
    private DataComponent buildComponent(FieldDescriptor f)
    {
        DataComponent comp;
        if (f.isRepeated())
        {
            comp = buildArray(f);
        }
        else if (f.getType() == FieldDescriptor.Type.MESSAGE)
        {
            if (isTimestamp(f.getMessageType()))
                comp = newIsoTime();
            else
                comp = buildRecord(f.getMessageType(), 1, sanitizeName(f.getName()));
        }
        else
        {
            comp = buildScalar(f);
        }

        applyOptions(comp, f.getOptions());
        comp.setName(f.getName());
        return comp;
    }


    /**
     * Reconstruct a {@link net.opengis.swe.v20.DataArray} from a {@code repeated}
     * field. The array is <b>variable-size</b> (implicit): the descriptor carries
     * no element count, so {@link ProtoRecordDecoder} sizes it from the repeated
     * field's wire length at decode time. The element is the field's scalar type,
     * a nested record, or — for a Matrix-row wrapper message (a single repeated
     * field) — another {@code DataArray}.
     */
    private DataComponent buildArray(FieldDescriptor f)
    {
        DataComponent elt;
        if (f.getType() == FieldDescriptor.Type.MESSAGE)
        {
            var mt = f.getMessageType();
            if (isTimestamp(mt))
                elt = newIsoTime();
            else if (isArrayWrapper(mt))
                elt = buildComponent(mt.getFields().get(0));   // Matrix row → nested array
            else
                elt = buildRecord(mt, 1, sanitizeName(f.getName()) + "Elt");
        }
        else
        {
            elt = buildScalar(f);
        }
        elt.setName(sanitizeName(f.getName()) + "Elt");

        var array = swe.newDataArray();
        array.setElementType(elt.getName(), elt);
        // implicit variable size: an elementCount that HAS a Count value whose
        // value is unset → isImplicitSize() → isVariableSize(); the decoder then
        // updateSize()s it from the repeated field's wire length.
        array.setElementCount(swe.createCount().build());
        return array;
    }


    /** A Matrix-row wrapper message: exactly one field, itself repeated. */
    private static boolean isArrayWrapper(Descriptor mt)
    {
        return mt.getFields().size() == 1 && mt.getFields().get(0).isRepeated();
    }


    /** Map a proto scalar field to a SWE scalar with a matching data type so
     *  {@code createDataBlock()} allocates the slot the decoder writes into. */
    private DataComponent buildScalar(FieldDescriptor f)
    {
        switch (f.getType())
        {
            case BOOL:
                return swe.createBoolean().build();
            case STRING:
                return swe.createText().build();
            case FLOAT:
                return swe.createQuantity().dataType(DataType.FLOAT).build();
            case DOUBLE:
                return swe.createQuantity().dataType(DataType.DOUBLE).build();
            case INT32: case SINT32: case SFIXED32:
                return swe.createCount().dataType(DataType.INT).build();
            case UINT32: case FIXED32:
                return swe.createCount().dataType(DataType.UINT).build();
            case INT64: case SINT64: case SFIXED64:
                return swe.createCount().dataType(DataType.LONG).build();
            case UINT64: case FIXED64:
                return swe.createCount().dataType(DataType.ULONG).build();
            default:
                throw new UnsupportedOperationException(
                    "Unsupported proto field type " + f.getType() + " for field '" + f.getName() + "'");
        }
    }


    private Time newIsoTime()
    {
        var t = swe.createTime().build();
        // ProtoRecordDecoder treats a field as a Timestamp iff comp.isIsoTime();
        // that is true exactly when the uom href is the ISO-8601 unit.
        t.getUom().setHref(Time.ISO_TIME_UNIT);
        return t;
    }


    /** Copy the swe_options FieldOptions extensions onto the reconstructed
     *  component. Each is gated on presence so absent annotations stay unset. */
    private void applyOptions(DataComponent comp, FieldOptions o)
    {
        if (o.hasExtension(SweOptions.id))
            comp.setIdentifier(o.getExtension(SweOptions.id));
        if (o.hasExtension(SweOptions.definition))
            comp.setDefinition(o.getExtension(SweOptions.definition));
        if (o.hasExtension(SweOptions.label))
            comp.setLabel(o.getExtension(SweOptions.label));
        if (o.hasExtension(SweOptions.description))
            comp.setDescription(o.getExtension(SweOptions.description));

        if (comp instanceof HasUom)
        {
            var uom = ((HasUom) comp).getUom();
            if (o.hasExtension(SweOptions.uomCode))
                uom.setCode(o.getExtension(SweOptions.uomCode));
            else if (o.hasExtension(SweOptions.uomHref))
                uom.setHref(o.getExtension(SweOptions.uomHref));
        }

        if (comp instanceof HasRefFrames)
        {
            var rf = (HasRefFrames) comp;
            if (o.hasExtension(SweOptions.referenceFrame))
                rf.setReferenceFrame(o.getExtension(SweOptions.referenceFrame));
            if (o.hasExtension(SweOptions.localFrame))
                rf.setLocalFrame(o.getExtension(SweOptions.localFrame));
        }

        if (comp instanceof SimpleComponent && o.hasExtension(SweOptions.axisID))
            ((SimpleComponent) comp).setAxisID(o.getExtension(SweOptions.axisID));

        if (comp instanceof Time && o.hasExtension(SweOptions.referenceTime))
        {
            try
            {
                ((Time) comp).setReferenceTime(OffsetDateTime.parse(o.getExtension(SweOptions.referenceTime)));
            }
            catch (RuntimeException e)
            {
                // tolerate an unparseable referenceTime rather than failing the
                // whole schema ingest over one annotation
            }
        }
    }


    private static boolean isTimestamp(Descriptor d)
    {
        return d != null && "google.protobuf.Timestamp".equals(d.getFullName());
    }


    /** Proto names are already valid identifiers; only guard against an empty
     *  name (SWE setName rejects null/blank in some impls). */
    private static String sanitizeName(String name)
    {
        return (name == null || name.isBlank()) ? "field" : name;
    }
}
