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
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Base64;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.impl.common.IdEncodersBase32;
import org.sensorhub.impl.service.consys.ResourceParseException;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.vast.swe.SWEHelper;
import com.georobotix.swecommon.SweOptions;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Timestamp;
import net.opengis.swe.v20.HasUom;


/**
 * End-to-end binding test for swe+proto <b>schema ingestion</b>: feed
 * {@link DataStreamSchemaBindingProto#deserialize} the exact JSON envelope its
 * own {@code serialize} emits ({@code obsFormat} + {@code messageType} +
 * base64 {@code fileDescriptorSet}) and assert the reconstructed
 * {@code IDataStreamInfo} carries the right SWE record structure. Also checks
 * the "proto service not running" guard (null ingest cache).
 */
public class TestProtoSchemaIngestBinding
{
    DataStreamSchemaCache cache;
    IdEncodersBase32 idEncoders;


    @Before
    public void setup()
    {
        cache = new DataStreamSchemaCache();
        var extReg = ExtensionRegistry.newInstance();
        SweOptions.registerAllExtensions(extReg);
        cache.setExtensionRegistry(extReg);
        cache.registerBootstrapTree(Timestamp.getDescriptor().getFile());
        cache.registerBootstrapTree(SweOptions.getDescriptor());
        idEncoders = new IdEncodersBase32();
    }


    /** Build the JSON the schema endpoint serves for a given SWE record. */
    private String schemaEnvelope(net.opengis.swe.v20.DataComponent record)
    {
        var schema = new ProtoSchemaWriter().write(record, "obs.proto", "test.ingest", "Observation");
        var b64 = Base64.getEncoder().encodeToString(ProtoSchemaWriter.toFileDescriptorSet(schema));
        return "{\"obsFormat\":\"application/swe+proto\","
            + "\"messageType\":\"" + schema.messageTypeName + "\","
            + "\"fileDescriptorSet\":\"" + b64 + "\"}";
    }


    @Test
    public void testIngestRebuildsDataStreamInfo() throws Exception
    {
        var swe = new SWEHelper();
        var record = swe.createRecord()
            .addField("airTemp", swe.createQuantity().definition("http://x/t").uomCode("Cel"))
            .addField("count", swe.createCount().definition("http://x/c"))
            .build();
        var json = schemaEnvelope(record);

        var ctx = new RequestContext(new ByteArrayOutputStream());
        var binding = new DataStreamSchemaBindingProto(ctx, idEncoders, false, null, cache);
        var dsInfo = binding.deserialize(new JsonReader(new StringReader(json)));

        assertNotNull(dsInfo);
        var rec = dsInfo.getRecordStructure();
        assertEquals(2, rec.getComponentCount());
        assertEquals("airTemp", rec.getComponent(0).getName());
        assertEquals("http://x/t", rec.getComponent(0).getDefinition());
        assertEquals("Cel", ((HasUom) rec.getComponent(0)).getUom().getCode());
        assertEquals("count", rec.getComponent(1).getName());
        assertNotNull(dsInfo.getRecordEncoding());
    }


    @Test
    public void testIngestRefusedWhenServiceNotRunning() throws Exception
    {
        var swe = new SWEHelper();
        var record = swe.createRecord().addField("v", swe.createQuantity()).build();
        var json = schemaEnvelope(record);

        var ctx = new RequestContext(new ByteArrayOutputStream());
        // null ingest cache models the proto service not being started
        var binding = new DataStreamSchemaBindingProto(ctx, idEncoders, false, null, null);
        try
        {
            binding.deserialize(new JsonReader(new StringReader(json)));
            fail("expected ingestion to be refused without an ingest cache");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().toLowerCase().contains("not running")
                || e.getMessage().toLowerCase().contains("unavailable"));
        }
    }


    @Test
    public void testMalformedSchemaRejected() throws Exception
    {
        var ctx = new RequestContext(new ByteArrayOutputStream());
        var binding = new DataStreamSchemaBindingProto(ctx, idEncoders, false, null, cache);
        var json = "{\"obsFormat\":\"application/swe+proto\",\"messageType\":\"x.Y\"}"; // no fileDescriptorSet
        try
        {
            binding.deserialize(new JsonReader(new StringReader(json)));
            fail("expected ResourceParseException for missing fileDescriptorSet");
        }
        catch (ResourceParseException expected)
        {
            assertTrue(expected.getMessage().contains("fileDescriptorSet")
                || expected.getMessage().contains("requires"));
        }
    }
}
