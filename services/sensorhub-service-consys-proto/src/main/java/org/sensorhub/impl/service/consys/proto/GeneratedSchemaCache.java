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

package org.sensorhub.impl.service.consys.proto;

import java.util.concurrent.ConcurrentHashMap;
import org.sensorhub.api.common.BigId;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.HasRefFrames;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.SimpleComponent;
import net.opengis.swe.v20.Time;


/**
 * <p>
 * Memoizes the per-stream artifacts generated from a SWE record structure —
 * the {@link ProtoSchemaWriter.Result}, the resolved {@link Descriptor}, and
 * the wire {@code FileDescriptorSet} bytes — so they are built once per
 * datastream / control stream instead of on every request. (Schema generation
 * is a tree walk, but {@code FileDescriptor.buildFrom} validation on every
 * observation GET adds up.)
 * </p>
 *
 * <p>
 * <b>Keying & invalidation.</b> Entries are keyed by the stream's internal
 * {@link BigId} and guarded by a structural fingerprint of the record
 * structure (names, types, and the SWE semantics that ride into the schema as
 * swe_options). If the stream's structure changes — e.g. the resource is
 * replaced — the fingerprint mismatches and the entry is rebuilt; no explicit
 * invalidation hook is needed. Lookups race benignly (rebuild is idempotent).
 * </p>
 *
 * <p>
 * This is the <i>sending-side</i> twin of {@link DataStreamSchemaCache}
 * (which registers descriptors arriving over the wire). One instance each for
 * observations and commands is owned by {@link ProtoFormat}, the single
 * long-lived object of this module inside the ConSys service.
 * </p>
 *
 * @see ProtoSchemaWriter
 * @author Ian Patterson
 * @since 2026
 */
public class GeneratedSchemaCache
{
    /** Builds the schema for a record structure (obs vs command envelope). */
    @FunctionalInterface
    public interface SchemaBuilder
    {
        ProtoSchemaWriter.Result build(DataComponent struct);
    }


    /** Everything derived from one stream's record structure. */
    public static final class Entry
    {
        public final ProtoSchemaWriter.Result schema;
        public final Descriptor descriptor;
        public final byte[] fileDescriptorSet;
        final long fingerprint;

        Entry(ProtoSchemaWriter.Result schema, Descriptor descriptor, byte[] fileDescriptorSet, long fingerprint)
        {
            this.schema = schema;
            this.descriptor = descriptor;
            this.fileDescriptorSet = fileDescriptorSet;
            this.fingerprint = fingerprint;
        }
    }


    final SchemaBuilder builder;
    final ConcurrentHashMap<BigId, Entry> entries = new ConcurrentHashMap<>();


    public GeneratedSchemaCache(SchemaBuilder builder)
    {
        this.builder = builder;
    }


    /**
     * Get the cached artifacts for {@code streamId}, (re)building them if the
     * stream is unseen or its record structure no longer matches the cached
     * fingerprint.
     */
    public Entry get(BigId streamId, DataComponent struct) throws DescriptorValidationException
    {
        var fp = fingerprint(struct, 17);
        var e = entries.get(streamId);
        if (e != null && e.fingerprint == fp)
            return e;

        var schema = builder.build(struct);
        var descriptor = ProtoSchemaWriter.resolve(schema);
        var fds = ProtoSchemaWriter.toFileDescriptorSet(schema);
        e = new Entry(schema, descriptor, fds, fp);
        entries.put(streamId, e);
        return e;
    }


    /**
     * Cheap structural fingerprint covering everything that influences the
     * generated schema: component class, name, data type, and the SWE
     * semantics emitted as swe_options ({@link ProtoSchemaWriter#sweOptions}).
     * A change in any of these must produce a different fingerprint so the
     * stale descriptor is rebuilt.
     */
    static long fingerprint(DataComponent comp, long h)
    {
        h = mix(h, comp.getClass().getName());
        h = mix(h, comp.getName());
        h = mix(h, comp.getDefinition());
        h = mix(h, comp.getLabel());
        h = mix(h, comp.getDescription());
        h = mix(h, comp.getIdentifier());

        if (comp instanceof SimpleComponent)
        {
            var sc = (SimpleComponent) comp;
            h = mix(h, sc.getDataType() != null ? sc.getDataType().name() : null);
            h = mix(h, sc.getAxisID());
        }
        if (comp instanceof HasUom)
        {
            var uom = ((HasUom) comp).getUom();
            if (uom != null)
            {
                h = mix(h, uom.getCode());
                h = mix(h, uom.getHref());
            }
        }
        if (comp instanceof HasRefFrames)
        {
            var rf = (HasRefFrames) comp;
            h = mix(h, rf.getReferenceFrame());
            h = mix(h, rf.getLocalFrame());
        }
        if (comp instanceof Time)
        {
            var t = (Time) comp;
            h = h * 31 + (t.isIsoTime() ? 1 : 2);
            if (t.isSetReferenceTime() && t.getReferenceTime() != null)
                h = mix(h, t.getReferenceTime().toString());
        }

        // a DataArray's getComponentCount() is its (possibly per-record) SIZE,
        // not its structure — only the element type shapes the schema
        if (comp instanceof DataArray)
            return fingerprint(((DataArray) comp).getElementType(), h * 31 + 1);

        for (int i = 0; i < comp.getComponentCount(); i++)
            h = fingerprint(comp.getComponent(i), h * 31 + i);
        return h;
    }


    private static long mix(long h, String s)
    {
        return h * 31 + (s != null ? s.hashCode() : 0);
    }
}
