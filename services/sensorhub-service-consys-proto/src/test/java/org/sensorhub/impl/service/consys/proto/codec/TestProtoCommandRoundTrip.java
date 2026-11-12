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
import static org.junit.Assert.*;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.vast.swe.SWEHelper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.Descriptor;
import net.opengis.swe.v20.DataComponent;


/**
 * Command-side codec tests: the command envelope schema
 * ({@link ProtoSchemaWriter#writeCommand}), outbound encoding
 * ({@link ProtoEncoder#encodeCommand}), and — most importantly — the
 * inbound path ({@link ProtoDecoder}): wire bytes are decoded back into
 * a flat {@link net.opengis.swe.v20.DataBlock} and every atom is asserted.
 * The full round trip runs against a descriptor rebuilt ONLY from the wire
 * {@code FileDescriptorSet} (what a foreign sender would hold), so the test
 * also covers the schema-delivery contract for control streams.
 */
public class TestProtoCommandRoundTrip
{
    static final String PKG = "test.cmd";
    static final String MSG = "Command";

    DataComponent params;
    ProtoSchemaWriter.Result schema;
    Descriptor desc;


    @Before
    public void setup() throws Exception
    {
        // mirrors the controllable-counter control interface: bool + 3 counts
        var swe = new SWEHelper();
        params = swe.createRecord()
            .addField("countDown", swe.createBoolean().definition("http://x/countDown"))   // atom 0
            .addField("step", swe.createCount().definition("http://x/step"))               // atom 1
            .addField("lowerBound", swe.createCount().definition("http://x/lower"))        // atom 2
            .addField("upperBound", swe.createCount().definition("http://x/upper"))        // atom 3
            .build();
        schema = new ProtoSchemaWriter().writeCommand(params, "cmd.proto", PKG, MSG);
        desc = ProtoSchemaWriter.resolve(schema);
    }


    @Test
    public void testCommandEnvelopeShape() throws Exception
    {
        // envelope at 1–5: 3 strings, issue_time Timestamp, sender string
        assertEquals("id", desc.findFieldByNumber(1).getName());
        assertEquals("controlstream_id", desc.findFieldByNumber(2).getName());
        assertEquals("foi_id", desc.findFieldByNumber(3).getName());
        assertEquals("issue_time", desc.findFieldByNumber(4).getName());
        assertEquals("google.protobuf.Timestamp", desc.findFieldByNumber(4).getMessageType().getFullName());
        assertEquals("sender", desc.findFieldByNumber(5).getName());
        assertEquals(com.google.protobuf.Descriptors.FieldDescriptor.Type.STRING, desc.findFieldByNumber(5).getType());

        // params at 6+
        assertEquals("countDown", desc.findFieldByNumber(6).getName());
        assertEquals("step", desc.findFieldByNumber(7).getName());
        assertEquals("lowerBound", desc.findFieldByNumber(8).getName());
        assertEquals("upperBound", desc.findFieldByNumber(9).getName());
        assertEquals(PKG + "." + MSG, schema.messageTypeName);
    }


    @Test
    public void testEncodeDecodeRoundTrip() throws Exception
    {
        var issueTime = Instant.ofEpochSecond(1_700_000_000L, 500_000_000);

        var block = params.createDataBlock();
        block.setBooleanValue(0, true);
        block.setIntValue(1, 5);
        block.setIntValue(2, 10);
        block.setIntValue(3, 90);

        var env = new ProtoEncoder.CommandEnvelope("cmd123", "cs456", null, issueTime, "userA");
        var wire = ProtoEncoder.encodeCommand(params, desc, block, env).toByteArray();

        // decode against the same descriptor
        var msg = DynamicMessage.parseFrom(desc, wire);
        var decoded = ProtoDecoder.decodeRecord(params, msg);
        assertTrue(decoded.getBooleanValue(0));
        assertEquals(5, decoded.getIntValue(1));
        assertEquals(10, decoded.getIntValue(2));
        assertEquals(90, decoded.getIntValue(3));

        // envelope reads back, including sub-second issue time
        assertEquals("cmd123", ProtoDecoder.getString(msg, "id"));
        assertEquals("cs456", ProtoDecoder.getString(msg, "controlstream_id"));
        assertNull(ProtoDecoder.getString(msg, "foi_id"));      // unset → null
        assertEquals(issueTime, ProtoDecoder.getInstant(msg, "issue_time"));
        assertEquals("userA", ProtoDecoder.getString(msg, "sender"));
    }


    @Test
    public void testDataChoiceRoundTripsAsOneof() throws Exception
    {
        // the real shape of the controllable-counter control interface:
        // a DataChoice of 4 alternative commands
        var swe = new SWEHelper();
        var choice = swe.createChoice()
            .name("counterControl")
            .addItem("setCountDown", swe.createBoolean().definition("http://x/cd").build())
            .addItem("setStep", swe.createCount().definition("http://x/step").build())
            .addItem("setLowerBound", swe.createCount().definition("http://x/lower").build())
            .addItem("setUpperBound", swe.createCount().definition("http://x/upper").build())
            .build();

        var choiceSchema = new ProtoSchemaWriter().writeCommand(choice, "cmd.proto", PKG, "ChoiceCmd");
        var choiceDesc = ProtoSchemaWriter.resolve(choiceSchema);

        // schema: one oneof containing fields 6–9
        assertEquals(1, choiceDesc.getOneofs().size());
        assertEquals("counterControl", choiceDesc.getOneofs().get(0).getName());
        for (int num = 6; num <= 9; num++)
            assertEquals("counterControl", choiceDesc.findFieldByNumber(num).getContainingOneof().getName());
        assertEquals("setStep", choiceDesc.findFieldByNumber(7).getName());

        // encode: select item 1 (setStep) with value 25
        // choice block layout: atom 0 = selection index, atom 1+ = selected item
        choice.setSelectedItem(1);
        var block = choice.createDataBlock();
        block.setIntValue(0, 1);
        block.setIntValue(1, 25);

        var env = new ProtoEncoder.CommandEnvelope(null, null, null, Instant.ofEpochSecond(1_700_000_000L), null);
        var wire = ProtoEncoder.encodeCommand(choice, choiceDesc, block, env).toByteArray();

        // only the selected oneof field travels
        var msg = DynamicMessage.parseFrom(choiceDesc, wire);
        assertEquals("setStep", msg.getOneofFieldDescriptor(choiceDesc.getOneofs().get(0)).getName());

        // decode on a fresh copy (selection state unknown to the receiver)
        var rxStruct = choice.copy();
        var decoded = ProtoDecoder.decodeRecord(rxStruct, msg);
        assertEquals(1, decoded.getIntValue(0));    // selection index
        assertEquals(25, decoded.getIntValue(1));   // setStep value

        // and the SWE component view agrees once the block is applied
        rxStruct.setData(decoded);
        assertEquals("setStep", ((net.opengis.swe.v20.DataChoice) rxStruct).getSelectedItem().getName());
    }


    @Test
    public void testChoiceWithNoSelectionFailsLoud() throws Exception
    {
        var swe = new SWEHelper();
        var choice = swe.createChoice()
            .name("ctl")
            .addItem("a", swe.createCount().build())
            .addItem("b", swe.createBoolean().build())
            .build();
        var s = new ProtoSchemaWriter().writeCommand(choice, "cmd.proto", PKG, "EmptyChoiceCmd");
        var d = ProtoSchemaWriter.resolve(s);

        // a message with no oneof field set must be rejected, not guessed at
        var empty = DynamicMessage.newBuilder(d).build();
        try
        {
            ProtoDecoder.decodeRecord(choice.copy(), empty);
            fail("expected IllegalArgumentException for unset oneof");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("no command selected"));
        }
    }


    @Test
    public void testForeignReceiverDecodesFromWireSchemaOnly() throws Exception
    {
        // package the schema exactly as the /schema endpoint serves it
        var fdsBytes = ProtoSchemaWriter.toFileDescriptorSet(schema);

        // rebuild the descriptor purely from wire bytes + runtime google WKTs
        // (the same recipe OSHConnect uses — no access to our Result object)
        var fds = com.google.protobuf.DescriptorProtos.FileDescriptorSet.parseFrom(fdsBytes);
        var byName = new java.util.HashMap<String, com.google.protobuf.Descriptors.FileDescriptor>();
        byName.put(ProtoSchemaWriter.TIMESTAMP_PROTO, com.google.protobuf.Timestamp.getDescriptor().getFile());
        byName.put("google/protobuf/descriptor.proto", com.google.protobuf.DescriptorProtos.getDescriptor());
        Descriptor wireDesc = null;
        for (var fdp : fds.getFileList())
        {
            var deps = new java.util.ArrayList<com.google.protobuf.Descriptors.FileDescriptor>();
            for (var dep : fdp.getDependencyList())
            {
                var d = byName.get(dep);
                assertNotNull("unresolved wire dependency: " + dep, d);
                deps.add(d);
            }
            var fd = com.google.protobuf.Descriptors.FileDescriptor.buildFrom(
                fdp, deps.toArray(new com.google.protobuf.Descriptors.FileDescriptor[0]));
            byName.put(fd.getName(), fd);
            if (!fd.getMessageTypes().isEmpty())
                wireDesc = fd.getMessageTypes().get(0);
        }
        assertNotNull(wireDesc);
        assertEquals(schema.messageTypeName, wireDesc.getFullName());

        // a "foreign sender" encodes a command against the wire descriptor;
        // the node decodes it with its own locally-built descriptor
        var tsDesc = wireDesc.findFieldByNumber(4).getMessageType();
        var foreign = DynamicMessage.newBuilder(wireDesc)
            .setField(wireDesc.findFieldByNumber(4), DynamicMessage.newBuilder(tsDesc)
                .setField(tsDesc.findFieldByName("seconds"), 1_700_000_123L)
                .setField(tsDesc.findFieldByName("nanos"), 0)
                .build())
            .setField(wireDesc.findFieldByNumber(6), true)    // countDown
            .setField(wireDesc.findFieldByNumber(7), 25)      // step
            .setField(wireDesc.findFieldByNumber(8), 0)       // lowerBound
            .setField(wireDesc.findFieldByNumber(9), 50)      // upperBound
            .build()
            .toByteArray();

        var msg = DynamicMessage.parseFrom(desc, foreign);
        var decoded = ProtoDecoder.decodeRecord(params, msg);
        assertTrue(decoded.getBooleanValue(0));
        assertEquals(25, decoded.getIntValue(1));
        assertEquals(0, decoded.getIntValue(2));
        assertEquals(50, decoded.getIntValue(3));
        assertEquals(Instant.ofEpochSecond(1_700_000_123L), ProtoDecoder.getInstant(msg, "issue_time"));
    }
}
