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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;


/**
 * <p>
 * Per-node descriptor pool. Holds the {@link Descriptor} resolved from each
 * {@code DataStream}'s embedded {@code ProtoObservationSchema} bytes
 * ({@code DataStream.schema.file_descriptor_proto}, field 50), keyed by
 * {@code datastream_id}, so subsequent {@code Observation} messages can be
 * decoded via {@code DynamicMessage.parseFrom(descriptor, bytes)} without a
 * {@code .proto} file on disk or {@code protoc} step.
 * </p>
 *
 * <p>
 * v1 policy:
 * </p>
 * <ul>
 *   <li>In-memory only; lost on restart.</li>
 *   <li>Conflict: <b>reject</b> — re-registering a <i>different</i> descriptor
 *       for a datastream id throws {@link SchemaConflictException}; re-registering
 *       an identical one is an idempotent no-op. Schema <i>changes</i> are made
 *       through the proper resource update endpoints, not by re-registering on
 *       the data path (see {@code docs/architecture.md} §5).</li>
 *   <li>No eviction.</li>
 * </ul>
 *
 * <p>
 * Bootstrap dependency descriptors (Google well-known types,
 * {@code swe_options.proto}, {@code sweCommon3.proto}, {@code cs_common.proto})
 * must be registered via {@link #registerBootstrap(FileDescriptor)} before any
 * client {@code FileDescriptorProto} that declares them as dependencies can
 * resolve. The owning {@link ConSysApiProtoService} does this at module
 * start once the generated extension classes are on the classpath.
 * </p>
 *
 * @see <a href="../../../../../../../docs/architecture.md">docs/architecture.md</a>
 * @author Ian Patterson
 * @since 2026
 */
public class DataStreamSchemaCache
{
    private static final Logger log = LoggerFactory.getLogger(DataStreamSchemaCache.class);

    private final ConcurrentHashMap<String, Descriptor> bySdsId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FileDescriptor> bootstrap = new ConcurrentHashMap<>();
    private volatile ExtensionRegistry extensionRegistry = ExtensionRegistry.getEmptyRegistry();


    /**
     * Set the {@link ExtensionRegistry} used when parsing incoming
     * {@code FileDescriptorProto} bytes. Without a registry that knows the
     * {@code swe_options.proto} {@code FieldOptions} extensions (field numbers
     * 1000–1009), those annotations survive a re-serialize but are invisible to
     * typed {@code getOptions().getExtension(SweOptions.uomCode)} reads — they
     * land in unknown fields. The owning {@link ConSysApiProtoService}
     * populates this from {@code SweOptions.registerAllExtensions(...)} at start.
     */
    public void setExtensionRegistry(ExtensionRegistry registry)
    {
        this.extensionRegistry = registry != null ? registry : ExtensionRegistry.getEmptyRegistry();
    }


    /**
     * Add a known-good {@link FileDescriptor} (typically from a generated
     * extension class's {@code getDescriptor()} / {@code getDescriptor().getFile()})
     * to the bootstrap pool. Subsequent {@link #register} calls whose
     * {@link FileDescriptorProto#getDependencyList()} reference the file's
     * {@link FileDescriptor#getName()} will resolve against it.
     */
    public void registerBootstrap(FileDescriptor file)
    {
        bootstrap.put(file.getName(), file);
    }


    /**
     * Register {@code file} and all of its transitive dependencies into the
     * bootstrap pool. Preferred over {@link #registerBootstrap(FileDescriptor)}
     * for files with their own imports (e.g. {@code swe_options.proto} depends
     * on {@code descriptor.proto}), so a client descriptor that directly imports
     * a transitive dependency still resolves.
     */
    public void registerBootstrapTree(FileDescriptor file)
    {
        if (bootstrap.containsKey(file.getName()))
            return;
        for (var dep : file.getDependencies())
            registerBootstrapTree(dep);
        bootstrap.put(file.getName(), file);
    }


    /**
     * Resolve a serialized {@link FileDescriptorProto}, find the message type
     * named by {@code messageTypeFqn}, and cache it under {@code datastreamId}.
     *
     * @param datastreamId  encoded datastream id (the public id, not BigId).
     * @param fdpBytes      serialized {@code FileDescriptorProto}.
     * @param messageTypeFqn fully-qualified name of the observation message
     *                      type defined inside the descriptor.
     * @return the resolved {@link Descriptor}.
     */
    public Descriptor register(String datastreamId, byte[] fdpBytes, String messageTypeFqn)
            throws InvalidProtocolBufferException, DescriptorValidationException
    {
        var fdProto = FileDescriptorProto.parseFrom(fdpBytes, extensionRegistry);
        var deps = new ArrayList<FileDescriptor>(fdProto.getDependencyCount());
        for (var depName : fdProto.getDependencyList())
        {
            var dep = bootstrap.get(depName);
            if (dep == null)
                throw new IllegalArgumentException(
                    "Cannot resolve descriptor dependency '" + depName
                    + "' — not in bootstrap pool. Known: " + bootstrap.keySet());
            deps.add(dep);
        }

        var file = FileDescriptor.buildFrom(fdProto, deps.toArray(new FileDescriptor[0]));
        var msg = findMessage(file, messageTypeFqn);
        if (msg == null)
            throw new IllegalArgumentException(
                "Message type '" + messageTypeFqn + "' not found in descriptor "
                + file.getName());

        var prev = bySdsId.get(datastreamId);
        if (prev != null)
        {
            // reject-on-conflict: a different descriptor must not silently
            // replace an existing one on the data path; an identical one is a
            // no-op (idempotent retries / reconnects are fine).
            if (!sameShape(prev, msg))
                throw new SchemaConflictException(datastreamId, prev.getFullName(), msg.getFullName());
            return prev;
        }
        bySdsId.put(datastreamId, msg);
        return msg;
    }


    /** Structural equality of two resolved messages: same proto shape (names,
     *  field numbers/types, options). Field <i>order</i> in the proto is
     *  normalized by {@code DescriptorProto} equality. */
    private static boolean sameShape(Descriptor a, Descriptor b)
    {
        return a.toProto().equals(b.toProto());
    }


    /**
     * Resolve a serialized {@code FileDescriptorSet} (the wire packaging served
     * by the schema endpoint, see {@link ProtoSchemaWriter#toFileDescriptorSet})
     * to the {@link Descriptor} named by {@code messageTypeFqn}, <b>without</b>
     * caching it. Used by the schema-ingest bindings to reconstruct a record
     * structure from a client-supplied schema.
     *
     * <p>Files already present in the bootstrap pool (e.g. {@code swe_options.proto},
     * which travels in the set) are taken from bootstrap rather than rebuilt;
     * google well-known types (omitted from the set by convention) are seeded
     * from bootstrap. The set is parsed with the {@link ExtensionRegistry} so the
     * {@code swe_options} {@code FieldOptions} resolve as typed extensions.</p>
     */
    public Descriptor resolveFromSet(byte[] fileDescriptorSetBytes, String messageTypeFqn)
            throws InvalidProtocolBufferException, DescriptorValidationException
    {
        var set = com.google.protobuf.DescriptorProtos.FileDescriptorSet
            .parseFrom(fileDescriptorSetBytes, extensionRegistry);

        var byName = new java.util.HashMap<String, FileDescriptor>(bootstrap);
        for (var fdp : set.getFileList())
        {
            if (byName.containsKey(fdp.getName()))
                continue;   // already have it (bootstrap / earlier set file)

            var deps = new ArrayList<FileDescriptor>(fdp.getDependencyCount());
            for (var depName : fdp.getDependencyList())
            {
                var dep = byName.get(depName);
                if (dep == null)
                    throw new IllegalArgumentException(
                        "Cannot resolve descriptor dependency '" + depName
                        + "' — not in the FileDescriptorSet or bootstrap pool. Known: " + byName.keySet());
                deps.add(dep);
            }
            byName.put(fdp.getName(), FileDescriptor.buildFrom(fdp, deps.toArray(new FileDescriptor[0])));
        }

        for (var fd : byName.values())
        {
            var msg = findMessage(fd, messageTypeFqn);
            if (msg != null)
                return msg;
        }
        throw new IllegalArgumentException(
            "Message type '" + messageTypeFqn + "' not found in the FileDescriptorSet");
    }


    /** Thrown when a datastream id is re-registered with a structurally
     *  different descriptor (the reject-on-conflict policy). */
    public static class SchemaConflictException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public SchemaConflictException(String datastreamId, String existingType, String incomingType)
        {
            super("swe+proto schema for datastream '" + datastreamId
                + "' conflicts with the registered one (" + existingType + " vs " + incomingType
                + "); change the schema through the resource update endpoint, not the data path");
        }
    }


    public Optional<Descriptor> lookup(String datastreamId)
    {
        return Optional.ofNullable(bySdsId.get(datastreamId));
    }


    public void invalidate(String datastreamId)
    {
        bySdsId.remove(datastreamId);
    }


    public void clear()
    {
        bySdsId.clear();
    }


    public Collection<String> bootstrapNames()
    {
        return bootstrap.keySet();
    }


    private static Descriptor findMessage(FileDescriptor file, String fqn)
    {
        for (var msg : file.getMessageTypes())
        {
            if (msg.getFullName().equals(fqn))
                return msg;
        }
        var dot = fqn.lastIndexOf('.');
        var simple = dot < 0 ? fqn : fqn.substring(dot + 1);
        return file.findMessageTypeByName(simple);
    }
}
