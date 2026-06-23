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
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;


/**
 * Round-trips a SWE {@link DataBlock} through {@link ProtoObsEncoder}: a nested
 * record (ISO time + scalars + a nested DataRecord + a Vector) is encoded to a
 * {@link DynamicMessage}, serialized, and parsed back against the same
 * descriptor. Nested leaf values are asserted specifically — if the recursion
 * or the flat atom-index threading were wrong, those assertions fail.
 */
public class TestProtoObsEncoder
{
    static final String PKG = "test.obs";
    static final String MSG = "Obs";

    DataComponent record;
    Descriptor desc;


    @Before
    public void setup() throws Exception
    {
        record = buildRecord();
        var schema = new ProtoSchemaWriter().write(record, "obs.proto", PKG, MSG);
        desc = ProtoSchemaWriter.resolve(schema);
    }


    static DataComponent buildRecord()
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addSamplingTimeIsoUTC("time")                                   // atom 0
            .addField("airTemp", swe.createQuantity()
                .definition("http://x/temp").uomCode("Cel").dataType(DataType.FLOAT))  // atom 1
            .addField("count", swe.createCount().definition("http://x/count"))         // atom 2
            .addField("status", swe.createRecord()
                .addField("ok", swe.createBoolean().definition("http://x/ok"))         // atom 3
                .addField("code", swe.createCount().definition("http://x/code")))      // atom 4
            .addField("location", swe.createVector()
                .definition("http://x/loc")
                .refFrame("http://www.opengis.net/def/crs/EPSG/0/4979")
                .addCoordinate("lat", swe.createQuantity().uomCode("deg").dataType(DataType.DOUBLE).build())  // atom 5
                .addCoordinate("lon", swe.createQuantity().uomCode("deg").dataType(DataType.DOUBLE).build())  // atom 6
                .addCoordinate("alt", swe.createQuantity().uomCode("m").dataType(DataType.DOUBLE).build()))   // atom 7
            .build();
    }


    static final Instant PHEN_TIME = Instant.ofEpochSecond(1_600_000_000L, 250_000_000);
    static final Instant RESULT_TIME = Instant.ofEpochSecond(1_600_000_001L);

    static ProtoObsEncoder.Envelope sampleEnvelope()
    {
        return new ProtoObsEncoder.Envelope("obs-1", "ds-1", null, PHEN_TIME, RESULT_TIME);
    }


    static DataBlock sampleData(DataComponent rec)
    {
        var data = rec.createDataBlock();
        data.setDoubleValue(0, 1_600_000_000.5);  // time: epoch seconds + 0.5s
        data.setFloatValue(1, 21.5f);             // airTemp
        data.setIntValue(2, 7);                   // count
        data.setBooleanValue(3, true);            // status.ok
        data.setIntValue(4, 42);                  // status.code
        data.setDoubleValue(5, 34.7);             // location.lat
        data.setDoubleValue(6, -86.6);            // location.lon
        data.setDoubleValue(7, 210.0);            // location.alt
        return data;
    }


    @Test
    public void testEncodeValues()
    {
        var msg = ProtoObsEncoder.encode(record, desc, sampleData(record), sampleEnvelope());

        // envelope (fields 1–5)
        assertEquals("obs-1", msg.getField(desc.findFieldByName("id")));
        assertEquals("ds-1", msg.getField(desc.findFieldByName("datastream_id")));
        var phen = (DynamicMessage) msg.getField(desc.findFieldByName("phenomenon_time"));
        var phenDesc = desc.findFieldByName("phenomenon_time").getMessageType();
        assertEquals(1_600_000_000L, phen.getField(phenDesc.findFieldByName("seconds")));
        assertEquals(250_000_000, phen.getField(phenDesc.findFieldByName("nanos")));

        assertEquals(21.5f, (float) msg.getField(desc.findFieldByName("airTemp")), 1e-6);
        assertEquals(7, msg.getField(desc.findFieldByName("count")));

        // ISO time -> Timestamp sub-message
        var time = (DynamicMessage) msg.getField(desc.findFieldByName("time"));
        var tsDesc = desc.findFieldByName("time").getMessageType();
        assertEquals(1_600_000_000L, time.getField(tsDesc.findFieldByName("seconds")));
        assertEquals(500_000_000, time.getField(tsDesc.findFieldByName("nanos")));

        // nested record leaf
        var status = (DynamicMessage) msg.getField(desc.findFieldByName("status"));
        var statusDesc = desc.findFieldByName("status").getMessageType();
        assertEquals(true, status.getField(statusDesc.findFieldByName("ok")));
        assertEquals(42, status.getField(statusDesc.findFieldByName("code")));

        // vector leaves
        var loc = (DynamicMessage) msg.getField(desc.findFieldByName("location"));
        var locDesc = desc.findFieldByName("location").getMessageType();
        assertEquals(34.7, (double) loc.getField(locDesc.findFieldByName("lat")), 1e-9);
        assertEquals(-86.6, (double) loc.getField(locDesc.findFieldByName("lon")), 1e-9);
        assertEquals(210.0, (double) loc.getField(locDesc.findFieldByName("alt")), 1e-9);
    }


    /** Encode -> wire bytes -> parse back against the descriptor -> values match. */
    @Test
    public void testWireRoundTrip() throws Exception
    {
        var msg = ProtoObsEncoder.encode(record, desc, sampleData(record), sampleEnvelope());
        var parsed = DynamicMessage.parseFrom(desc, msg.toByteArray());

        assertEquals(21.5f, (float) parsed.getField(desc.findFieldByName("airTemp")), 1e-6);

        var status = (DynamicMessage) parsed.getField(desc.findFieldByName("status"));
        var statusDesc = desc.findFieldByName("status").getMessageType();
        assertEquals(42, status.getField(statusDesc.findFieldByName("code")));

        var loc = (DynamicMessage) parsed.getField(desc.findFieldByName("location"));
        var locDesc = desc.findFieldByName("location").getMessageType();
        assertEquals(210.0, (double) loc.getField(locDesc.findFieldByName("alt")), 1e-9);
    }
}
