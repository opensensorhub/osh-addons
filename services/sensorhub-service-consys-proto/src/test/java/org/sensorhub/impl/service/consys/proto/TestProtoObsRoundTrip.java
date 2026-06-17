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
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.vast.swe.SWEHelper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.Descriptor;
import net.opengis.swe.v20.DataComponent;


/**
 * Observation-side codec tests covering the inbound path that
 * {@link ObsBindingProto#deserialize} sits on top of: wire bytes are decoded
 * back into a flat {@link net.opengis.swe.v20.DataBlock} via
 * {@link ProtoRecordDecoder} and every atom is asserted, and the observation
 * envelope ({@link ProtoSchemaWriter#ENVELOPE_FIELD_NAMES} — 3 strings plus
 * {@code phenomenon_time}/{@code result_time} Timestamps) reads back including
 * sub-second precision. This is the obs mirror of
 * {@link TestProtoCommandRoundTrip}; the encoder test only parses the wire
 * message, it never decodes a record back.
 */
public class TestProtoObsRoundTrip
{
    static final String PKG = "test.obs";
    static final String MSG = "Observation";

    DataComponent record;
    ProtoSchemaWriter.Result schema;
    Descriptor desc;


    @Before
    public void setup() throws Exception
    {
        var swe = new SWEHelper();
        record = swe.createRecord()
            .addField("airTemp", swe.createQuantity().definition("http://x/temp").uomCode("Cel"))   // atom 0
            .addField("humidity", swe.createQuantity().definition("http://x/hum").uomCode("%"))      // atom 1
            .addField("sampleCount", swe.createCount().definition("http://x/count"))                 // atom 2
            .addField("valid", swe.createBoolean().definition("http://x/valid"))                     // atom 3
            .build();
        schema = new ProtoSchemaWriter().write(record, "obs.proto", PKG, MSG);
        desc = ProtoSchemaWriter.resolve(schema);
    }


    @Test
    public void testObsEnvelopeShape() throws Exception
    {
        // envelope at 1-5: 3 strings, phenomenon_time + result_time Timestamps
        assertEquals("id", desc.findFieldByNumber(1).getName());
        assertEquals("datastream_id", desc.findFieldByNumber(2).getName());
        assertEquals("foi_id", desc.findFieldByNumber(3).getName());
        assertEquals("phenomenon_time", desc.findFieldByNumber(4).getName());
        assertEquals("google.protobuf.Timestamp", desc.findFieldByNumber(4).getMessageType().getFullName());
        assertEquals("result_time", desc.findFieldByNumber(5).getName());
        assertEquals("google.protobuf.Timestamp", desc.findFieldByNumber(5).getMessageType().getFullName());

        // record components at 6+
        assertEquals("airTemp", desc.findFieldByNumber(6).getName());
        assertEquals("humidity", desc.findFieldByNumber(7).getName());
        assertEquals("sampleCount", desc.findFieldByNumber(8).getName());
        assertEquals("valid", desc.findFieldByNumber(9).getName());
        assertEquals(PKG + "." + MSG, schema.messageTypeName);
    }


    @Test
    public void testEncodeDecodeRoundTrip() throws Exception
    {
        var phenomenonTime = Instant.ofEpochSecond(1_700_000_000L, 250_000_000);
        var resultTime = Instant.ofEpochSecond(1_700_000_001L, 750_000_000);

        var block = record.createDataBlock();
        block.setDoubleValue(0, 21.5);
        block.setDoubleValue(1, 60.0);
        block.setIntValue(2, 7);
        block.setBooleanValue(3, true);

        var env = new ProtoObsEncoder.Envelope("obs123", "ds456", "foi789", phenomenonTime, resultTime);
        var wire = ProtoObsEncoder.encode(record, desc, block, env).toByteArray();

        // decode the record back from the wire bytes (the binding's inbound path)
        var msg = DynamicMessage.parseFrom(desc, wire);
        var decoded = ProtoRecordDecoder.decodeRecord(record, msg);
        assertEquals(21.5, decoded.getDoubleValue(0), 1e-9);
        assertEquals(60.0, decoded.getDoubleValue(1), 1e-9);
        assertEquals(7, decoded.getIntValue(2));
        assertTrue(decoded.getBooleanValue(3));

        // envelope reads back, including sub-second times
        assertEquals("obs123", ProtoRecordDecoder.getString(msg, "id"));
        assertEquals("ds456", ProtoRecordDecoder.getString(msg, "datastream_id"));
        assertEquals("foi789", ProtoRecordDecoder.getString(msg, "foi_id"));
        assertEquals(phenomenonTime, ProtoRecordDecoder.getInstant(msg, "phenomenon_time"));
        assertEquals(resultTime, ProtoRecordDecoder.getInstant(msg, "result_time"));
    }


    @Test
    public void testUnsetEnvelopeTimesAreNull() throws Exception
    {
        // an observation with no result_time set: the binding falls back to
        // phenomenon_time, so the decoder must report the unset field as null
        var block = record.createDataBlock();
        block.setDoubleValue(0, 1.0);
        block.setDoubleValue(1, 2.0);
        block.setIntValue(2, 3);
        block.setBooleanValue(3, false);

        var env = new ProtoObsEncoder.Envelope(null, null, null,
            Instant.ofEpochSecond(1_700_000_000L), null);
        var wire = ProtoObsEncoder.encode(record, desc, block, env).toByteArray();

        var msg = DynamicMessage.parseFrom(desc, wire);
        assertNull(ProtoRecordDecoder.getString(msg, "id"));
        assertEquals(Instant.ofEpochSecond(1_700_000_000L), ProtoRecordDecoder.getInstant(msg, "phenomenon_time"));
        assertNull(ProtoRecordDecoder.getInstant(msg, "result_time"));
    }
}