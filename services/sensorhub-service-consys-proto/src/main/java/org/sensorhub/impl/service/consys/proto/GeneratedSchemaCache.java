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

import org.sensorhub.api.common.BigId;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Builds the per-stream protobuf artifacts from a SWE record structure — the
 * {@link ProtoSchemaWriter.Result}, the resolved {@link Descriptor}, and the
 * wire {@code FileDescriptorSet} bytes.
 * </p>
 *
 * <p>
 * <b>Currently builds on every call — no memoization.</b> The structural
 * fingerprint cache that previously keyed entries by {@link BigId} and rebuilt
 * only on structural change was removed on 2026-06-17 (see the "Schema
 * fingerprint cache — parked" section of the module {@code CLAUDE.md}, the
 * {@code docs/parked/} snapshots, and branch
 * {@code parked/schema-fingerprint-cache}). The class name and the
 * {@link #get(BigId, DataComponent)} signature are intentionally retained — the
 * {@code streamId} is still accepted but unused — so the cache can be reinstated
 * without touching {@link ProtoFormat} or the resource bindings.
 * </p>
 *
 * <p>
 * Rationale for regenerating each time: {@code get()} is called once per request
 * (the binding is per-request and the descriptor is reused for every observation
 * in the response), so each rebuild is one {@code FileDescriptor.buildFrom} per
 * request — negligible for batched/streaming GETs. Revive the cache if profiling
 * shows per-request rebuild cost matters under high-frequency, small-granularity
 * traffic.
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

        Entry(ProtoSchemaWriter.Result schema, Descriptor descriptor, byte[] fileDescriptorSet)
        {
            this.schema = schema;
            this.descriptor = descriptor;
            this.fileDescriptorSet = fileDescriptorSet;
        }
    }


    final SchemaBuilder builder;


    public GeneratedSchemaCache(SchemaBuilder builder)
    {
        this.builder = builder;
    }


    /**
     * Build the artifacts for {@code struct}. Rebuilds on every call (see class
     * doc). {@code streamId} is currently unused but kept in the signature so
     * the per-stream fingerprint cache can be reinstated without touching call
     * sites.
     */
    public Entry get(BigId streamId, DataComponent struct) throws DescriptorValidationException
    {
        var schema = builder.build(struct);
        var descriptor = ProtoSchemaWriter.resolve(schema);
        var fds = ProtoSchemaWriter.toFileDescriptorSet(schema);
        return new Entry(schema, descriptor, fds);
    }
}
