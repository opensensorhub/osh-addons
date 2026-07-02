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

import org.sensorhub.impl.service.consys.proto.codec.ProtoEncoder;
import org.sensorhub.impl.service.consys.proto.codec.ProtoDecoder;
import static org.junit.Assert.*;
import org.junit.Test;
import org.vast.swe.SWEHelper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.Descriptor;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.HasUom;


/**
 * Tests for {@link ProtoSchemaReader} — the schema-ingestion (inverse) half.
 * Proves two things:
 * <ol>
 *   <li><b>Structure + annotations</b> survive SWE → {@link ProtoSchemaWriter}
 *       → descriptor → {@link ProtoSchemaReader} → SWE.</li>
 *   <li><b>The reconstructed structure decodes real data</b>: a block encoded
 *       against the writer's descriptor decodes — via the reader-rebuilt
 *       structure — to the same atoms. This is the property that matters for
 *       ingesting a foreign schema and then receiving its observations.</li>
 * </ol>
 */
public class TestProtoSchemaReader
{
    static final String PKG = "test.ingest";


    private DataRecord roundTripStructure(DataComponent original)
    {
        var schema = new ProtoSchemaWriter().write(original, "obs.proto", PKG, "Observation");
        Descriptor desc;
        try { desc = ProtoSchemaWriter.resolve(schema); }
        catch (Exception e) { throw new RuntimeException(e); }
        return new ProtoSchemaReader().readRecord(desc);
    }


    @Test
    public void testArrayIngestRoundTrip() throws Exception
    {
        var swe = new SWEHelper();
        var orig = swe.createRecord()
            .addField("n", swe.createCount().id("NUM").build())
            .addField("samples", swe.createArray().withVariableSize("NUM")
                .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build()))
            .addField("tail", swe.createBoolean().build())
            .build();

        var schema = new ProtoSchemaWriter().write(orig, "obs.proto", PKG, "Observation");
        var desc = ProtoSchemaWriter.resolve(schema);

        // foreign receiver rebuilds the structure from the descriptor alone
        var rebuilt = new ProtoSchemaReader().readRecord(desc);
        var arr = (DataArray) rebuilt.getComponent("samples");
        assertTrue("ingested array must be variable-size", arr.isVariableSize());

        // encode with the ORIGINAL struct, decode with the REBUILT one
        ((DataArray) orig.getComponent("samples")).updateSize(3);
        var blk = orig.createDataBlock();             // [n, v0, v1, v2, tail]
        blk.setIntValue(0, 3);
        blk.setDoubleValue(1, 7); blk.setDoubleValue(2, 8); blk.setDoubleValue(3, 9);
        blk.setBooleanValue(4, true);

        var wire = ProtoEncoder.encode(orig, desc, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(desc, wire);
        var out = ProtoDecoder.decodeRecord(rebuilt, msg);

        assertEquals(5, out.getAtomCount());
        assertEquals(3, out.getIntValue(0));
        assertEquals(7.0, out.getDoubleValue(1), 1e-9);
        assertEquals(9.0, out.getDoubleValue(3), 1e-9);
        assertTrue(out.getBooleanValue(4));
    }


    @Test
    public void testScalarsAndAnnotations()
    {
        var swe = new SWEHelper();
        var original = swe.createRecord()
            .addField("airTemp", swe.createQuantity()
                .definition("http://x/temp").label("Air Temperature").uomCode("Cel"))
            .addField("sampleCount", swe.createCount().definition("http://x/count"))
            .addField("valid", swe.createBoolean().definition("http://x/valid"))
            .addField("note", swe.createText().definition("http://x/note"))
            .build();

        var rebuilt = roundTripStructure(original);

        assertEquals(4, rebuilt.getComponentCount());
        assertEquals("airTemp", rebuilt.getComponent(0).getName());
        assertEquals("http://x/temp", rebuilt.getComponent(0).getDefinition());
        assertEquals("Air Temperature", rebuilt.getComponent(0).getLabel());
        assertEquals("Cel", ((HasUom) rebuilt.getComponent(0)).getUom().getCode());
        assertEquals("sampleCount", rebuilt.getComponent(1).getName());
        assertEquals("http://x/count", rebuilt.getComponent(1).getDefinition());
        assertEquals("valid", rebuilt.getComponent(2).getName());
        assertEquals("note", rebuilt.getComponent(3).getName());

        // atom layout matches: 4 scalars => 4 atoms
        assertEquals(4, rebuilt.createDataBlock().getAtomCount());
    }


    @Test
    public void testNestedRecord()
    {
        var swe = new SWEHelper();
        var original = swe.createRecord()
            .addField("airTemp", swe.createQuantity().definition("http://x/t").uomCode("Cel"))
            .addField("status", swe.createRecord()
                .addField("code", swe.createCount().definition("http://x/code"))
                .addField("ok", swe.createBoolean()))
            .build();

        var rebuilt = roundTripStructure(original);

        assertEquals(2, rebuilt.getComponentCount());
        assertEquals("status", rebuilt.getComponent(1).getName());
        var nested = (DataRecord) rebuilt.getComponent(1);
        assertEquals(2, nested.getComponentCount());
        assertEquals("code", nested.getComponent(0).getName());
        assertEquals("http://x/code", nested.getComponent(0).getDefinition());
        assertEquals("ok", nested.getComponent(1).getName());

        // 1 scalar + nested(2 scalars) = 3 atoms
        assertEquals(3, rebuilt.createDataBlock().getAtomCount());
    }


    @Test
    public void testChoiceRebuildsAsChoice()
    {
        var swe = new SWEHelper();
        var original = swe.createRecord()
            .addField("seq", swe.createCount())
            .addField("ctl", swe.createChoice()
                .addItem("setStep", swe.createCount().definition("http://x/step").build())
                .addItem("setRate", swe.createQuantity().definition("http://x/rate").uomCode("Hz").build()))
            .build();

        var rebuilt = roundTripStructure(original);

        assertEquals(2, rebuilt.getComponentCount());
        assertTrue("choice should rebuild as DataChoice", rebuilt.getComponent(1) instanceof DataChoice);
        var choice = (DataChoice) rebuilt.getComponent(1);
        assertEquals(2, choice.getComponentCount());
        assertEquals("setStep", choice.getComponent(0).getName());
        assertEquals("setRate", choice.getComponent(1).getName());
        assertEquals("Hz", ((HasUom) choice.getComponent(1)).getUom().getCode());
    }


    @Test
    public void testReconstructedStructureDecodesData() throws Exception
    {
        var swe = new SWEHelper();
        var original = swe.createRecord()
            .addField("airTemp", swe.createQuantity().definition("http://x/t").uomCode("Cel"))
            .addField("count", swe.createCount().definition("http://x/c"))
            .addField("ok", swe.createBoolean())
            .build();

        // encode a block against the writer-built descriptor (what a foreign
        // sender holding only the schema would do)
        var schema = new ProtoSchemaWriter().write(original, "obs.proto", PKG, "Observation");
        var desc = ProtoSchemaWriter.resolve(schema);
        var block = original.createDataBlock();
        block.setDoubleValue(0, 19.25);
        block.setIntValue(1, 8);
        block.setBooleanValue(2, true);
        var env = new ProtoEncoder.Envelope(null, null, null, null, null);
        var wire = ProtoEncoder.encode(original, desc, block, env).toByteArray();

        // decode using ONLY the structure rebuilt by the reader (we no longer
        // have the original record) — the round-trip that schema ingestion needs
        var rebuilt = new ProtoSchemaReader().readRecord(desc);
        var msg = DynamicMessage.parseFrom(desc, wire);
        var decoded = ProtoDecoder.decodeRecord(rebuilt, msg);

        assertEquals(19.25, decoded.getDoubleValue(0), 1e-9);
        assertEquals(8, decoded.getIntValue(1));
        assertTrue(decoded.getBooleanValue(2));
    }
}
