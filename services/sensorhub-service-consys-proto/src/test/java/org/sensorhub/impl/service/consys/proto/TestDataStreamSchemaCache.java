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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.vast.swe.SWEHelper;
import com.georobotix.swecommon.SweOptions;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;


/**
 * Unit tests for {@link DataStreamSchemaCache}.
 *
 * <p>FileDescriptorProtos are built programmatically (no protoc, no
 * generated classes) so the test exercises just the cache contract.</p>
 */
public class TestDataStreamSchemaCache
{
    static final String PKG  = "test.ds";
    static final String MSG  = "Reading";
    static final String FQN  = PKG + "." + MSG;

    DataStreamSchemaCache cache;


    @Before
    public void setup()
    {
        cache = new DataStreamSchemaCache();
        cache.registerBootstrap(Timestamp.getDescriptor().getFile());
    }


    /** Build a tiny FileDescriptorProto with one Timestamp-typed field. */
    static FileDescriptorProto buildDescriptor(String fileName, String fieldName)
    {
        var field = FieldDescriptorProto.newBuilder()
            .setName(fieldName)
            .setNumber(1)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
            .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
            .setTypeName(".google.protobuf.Timestamp")
            .build();

        var msg = DescriptorProto.newBuilder()
            .setName(MSG)
            .addField(field)
            .build();

        return FileDescriptorProto.newBuilder()
            .setName(fileName)
            .setSyntax("proto3")
            .setPackage(PKG)
            .addDependency("google/protobuf/timestamp.proto")
            .addMessageType(msg)
            .build();
    }


    @Test
    public void testRegisterAndLookup() throws Exception
    {
        var fdp = buildDescriptor("ds1.proto", "time");
        var desc = cache.register("ds1", fdp.toByteArray(), FQN);

        assertEquals(FQN, desc.getFullName());
        assertTrue(cache.lookup("ds1").isPresent());
        assertEquals(FQN, cache.lookup("ds1").get().getFullName());
    }


    @Test
    public void testLookupMiss()
    {
        assertFalse(cache.lookup("never-registered").isPresent());
    }


    @Test
    public void testReregisterIdenticalIsNoOp() throws Exception
    {
        // identical re-registration (idempotent retry / reconnect) is accepted
        cache.register("ds1", buildDescriptor("ds1.proto", "time").toByteArray(), FQN);
        cache.register("ds1", buildDescriptor("ds1.proto", "time").toByteArray(), FQN);

        var current = cache.lookup("ds1").orElseThrow();
        assertEquals("time", current.getFields().get(0).getName());
    }


    @Test
    public void testReregisterConflictIsRejected() throws Exception
    {
        // First descriptor: one field "time"
        cache.register("ds1", buildDescriptor("ds1.proto", "time").toByteArray(), FQN);

        // A structurally different descriptor for the same id must be rejected,
        // not silently replace the existing one (reject-on-conflict policy).
        try
        {
            cache.register("ds1", buildDescriptor("ds1.proto", "phenomenon_time").toByteArray(), FQN);
            fail("Expected SchemaConflictException on conflicting re-register");
        }
        catch (DataStreamSchemaCache.SchemaConflictException expected)
        {
            assertTrue(expected.getMessage().contains("ds1"));
        }

        // original is preserved
        assertEquals("time", cache.lookup("ds1").orElseThrow().getFields().get(0).getName());
    }


    @Test
    public void testInvalidate() throws Exception
    {
        cache.register("ds1", buildDescriptor("ds1.proto", "time").toByteArray(), FQN);
        cache.invalidate("ds1");
        assertFalse(cache.lookup("ds1").isPresent());
    }


    @Test
    public void testClear() throws Exception
    {
        cache.register("ds1", buildDescriptor("ds1.proto", "time").toByteArray(), FQN);
        cache.register("ds2", buildDescriptor("ds2.proto", "time").toByteArray(), FQN);
        cache.clear();
        assertFalse(cache.lookup("ds1").isPresent());
        assertFalse(cache.lookup("ds2").isPresent());
    }


    @Test
    public void testMissingDependencyFailsCleanly()
    {
        // Build a descriptor that depends on an unregistered file.
        var fdp = FileDescriptorProto.newBuilder()
            .setName("ds-bad.proto")
            .setSyntax("proto3")
            .setPackage(PKG)
            .addDependency("nonexistent/missing.proto")
            .addMessageType(DescriptorProto.newBuilder().setName(MSG).build())
            .build();

        try
        {
            cache.register("ds-bad", fdp.toByteArray(), FQN);
            fail("Expected IllegalArgumentException for unresolved dependency");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("missing.proto"));
        }
        catch (InvalidProtocolBufferException | DescriptorValidationException e)
        {
            fail("Should have thrown IllegalArgumentException, got " + e);
        }
    }


    /**
     * End-to-end of the {@code ConSysApiProtoService.doStart()} bootstrap: a
     * descriptor importing {@code swecommon/swe_options.proto} and
     * {@code google/protobuf/timestamp.proto} resolves against the bootstrap
     * pool, and a {@code (uomCode)} SWE annotation reads back as a typed
     * extension — which only works if the cache parses with an
     * {@link ExtensionRegistry} that knows swe_options (guards R4).
     */
    @Test
    public void testSweOptionsAnnotationRoundTrips() throws Exception
    {
        // Configure a cache the way the service does.
        var c = new DataStreamSchemaCache();
        var extReg = ExtensionRegistry.newInstance();
        SweOptions.registerAllExtensions(extReg);
        c.setExtensionRegistry(extReg);
        c.registerBootstrapTree(Timestamp.getDescriptor().getFile());
        c.registerBootstrapTree(SweOptions.getDescriptor());

        // Field carrying the (uomCode) = "Cel" SWE annotation.
        var annotated = FieldDescriptorProto.newBuilder()
            .setName("air_temperature")
            .setNumber(6)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
            .setType(FieldDescriptorProto.Type.TYPE_FLOAT)
            .setOptions(FieldOptions.newBuilder()
                .setExtension(SweOptions.uomCode, "Cel")
                .build())
            .build();

        var timeField = FieldDescriptorProto.newBuilder()
            .setName("phenomenon_time")
            .setNumber(4)
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)
            .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
            .setTypeName(".google.protobuf.Timestamp")
            .build();

        var msg = DescriptorProto.newBuilder()
            .setName(MSG)
            .addField(timeField)
            .addField(annotated)
            .build();

        var fdp = FileDescriptorProto.newBuilder()
            .setName("ds-swe.proto")
            .setSyntax("proto3")
            .setPackage(PKG)
            .addDependency("google/protobuf/timestamp.proto")
            .addDependency("swecommon/swe_options.proto")
            .addMessageType(msg)
            .build();

        var desc = c.register("ds-swe", fdp.toByteArray(), FQN);

        // Structural resolution worked...
        assertEquals(FQN, desc.getFullName());
        // ...and the SWE annotation survived as a typed extension.
        var field = desc.findFieldByName("air_temperature");
        assertEquals("Cel", field.getOptions().getExtension(SweOptions.uomCode));
    }


    /**
     * The schema-ingest pipeline: a {@code FileDescriptorSet} packaged exactly
     * as the schema endpoint serves it ({@link ProtoSchemaWriter#toFileDescriptorSet})
     * resolves to the observation message, with its {@code swe_options}
     * annotations readable as typed extensions (so {@link ProtoSchemaReader} can
     * recover the SWE semantics).
     */
    @Test
    public void testResolveFromSet() throws Exception
    {
        var c = new DataStreamSchemaCache();
        var extReg = ExtensionRegistry.newInstance();
        SweOptions.registerAllExtensions(extReg);
        c.setExtensionRegistry(extReg);
        c.registerBootstrapTree(Timestamp.getDescriptor().getFile());
        c.registerBootstrapTree(SweOptions.getDescriptor());

        var swe = new SWEHelper();
        var rec = swe.createRecord()
            .addField("airTemp", swe.createQuantity().definition("http://x/t").uomCode("Cel"))
            .addField("count", swe.createCount().definition("http://x/c"))
            .build();
        var schema = new ProtoSchemaWriter().write(rec, "obs.proto", "test.ds", "Observation");
        var fdsBytes = ProtoSchemaWriter.toFileDescriptorSet(schema);

        var desc = c.resolveFromSet(fdsBytes, schema.messageTypeName);
        assertEquals(schema.messageTypeName, desc.getFullName());
        assertEquals("Cel", desc.findFieldByName("airTemp").getOptions().getExtension(SweOptions.uomCode));

        // and the reader turns it back into a SWE record
        var rebuilt = new ProtoSchemaReader().readRecord(desc);
        assertEquals(2, rebuilt.getComponentCount());
        assertEquals("airTemp", rebuilt.getComponent(0).getName());
    }


    @Test
    public void testUnknownMessageTypeFailsCleanly() throws Exception
    {
        var fdp = buildDescriptor("ds1.proto", "time");
        try
        {
            cache.register("ds1", fdp.toByteArray(), "test.ds.NotInThere");
            fail("Expected IllegalArgumentException for missing message type");
        }
        catch (IllegalArgumentException expected)
        {
            assertTrue(expected.getMessage().contains("NotInThere"));
        }
    }
}
