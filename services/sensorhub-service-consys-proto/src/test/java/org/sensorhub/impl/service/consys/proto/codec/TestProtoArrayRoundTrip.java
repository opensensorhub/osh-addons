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

import static org.junit.Assert.*;
import org.junit.Test;
import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaWriter;
import org.vast.swe.SWEHelper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.Descriptor;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;


/**
 * Same-node round-trip for fixed-size {@link DataArray} (increment 1): encode a
 * flat {@link net.opengis.swe.v20.DataBlock} to a {@code repeated} field and
 * decode it back using the local record structure. Tests deliberately put a
 * scalar AFTER the array (and nest an array of records) so a field-number /
 * atom-index drift through the array surfaces. Foreign-ingest (descriptor-only)
 * array round-trip is NOT covered here — it needs the wire-size machinery
 * (increment 2).
 */
public class TestProtoArrayRoundTrip
{
    static final String PKG = "test.arr";
    static final String MSG = "Observation";


    static Descriptor desc(DataComponent rec) throws Exception
    {
        return ProtoSchemaWriter.resolve(new ProtoSchemaWriter().write(rec, "obs.proto", PKG, MSG));
    }


    @Test
    public void fixedScalarArrayWithTrailingScalar() throws Exception
    {
        var swe = new SWEHelper();
        var rec = swe.createRecord()
            .addField("lead", swe.createQuantity().dataType(DataType.DOUBLE).build())
            .addField("samples", swe.createArray().withFixedSize(3)
                .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build()))
            .addField("tail", swe.createCount().build())
            .build();
        var d = desc(rec);

        var blk = rec.createDataBlock();               // flat: [lead, v0, v1, v2, tail]
        assertEquals(5, blk.getAtomCount());
        blk.setDoubleValue(0, 9.9);
        blk.setDoubleValue(1, 1.1); blk.setDoubleValue(2, 2.2); blk.setDoubleValue(3, 3.3);
        blk.setIntValue(4, 42);

        var wire = ProtoObsEncoder.encode(rec, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);
        var out = ProtoRecordDecoder.decodeRecord(rec, msg);

        assertEquals(5, out.getAtomCount());
        assertEquals(9.9, out.getDoubleValue(0), 1e-9);
        assertEquals(1.1, out.getDoubleValue(1), 1e-9);
        assertEquals(2.2, out.getDoubleValue(2), 1e-9);
        assertEquals(3.3, out.getDoubleValue(3), 1e-9);
        assertEquals(42, out.getIntValue(4));          // trailing scalar — catches drift
    }


    @Test
    public void fixedArrayOfRecords() throws Exception
    {
        var swe = new SWEHelper();
        var rec = swe.createRecord()
            .addField("rows", swe.createArray().withFixedSize(2)
                .withElement("row", swe.createRecord()
                    .addField("a", swe.createQuantity().dataType(DataType.DOUBLE).build())
                    .addField("b", swe.createCount().build())
                    .build()))
            .addField("tail", swe.createBoolean().build())
            .build();
        var d = desc(rec);

        var blk = rec.createDataBlock();               // flat: [a0, b0, a1, b1, tail]
        assertEquals(5, blk.getAtomCount());
        blk.setDoubleValue(0, 1.0); blk.setIntValue(1, 10);
        blk.setDoubleValue(2, 2.0); blk.setIntValue(3, 20);
        blk.setBooleanValue(4, true);

        var wire = ProtoObsEncoder.encode(rec, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);
        var out = ProtoRecordDecoder.decodeRecord(rec, msg);

        assertEquals(1.0, out.getDoubleValue(0), 1e-9);
        assertEquals(10, out.getIntValue(1));
        assertEquals(2.0, out.getDoubleValue(2), 1e-9);
        assertEquals(20, out.getIntValue(3));
        assertTrue(out.getBooleanValue(4));
    }


    @Test
    public void variableSizeArrayRejectedLoudly() throws Exception
    {
        var swe = new SWEHelper();
        var rec = swe.createRecord()
            .addField("n", swe.createCount().id("ARRAY_SIZE").build())
            .addField("samples", swe.createArray().withVariableSize("ARRAY_SIZE")
                .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build()))
            .build();
        var arr = (DataArray) rec.getComponent("samples");
        arr.updateSize(2);
        var blk = rec.createDataBlock();
        var d = desc(rec);

        try
        {
            ProtoObsEncoder.encode(rec, d, blk, null);
            fail("expected variable-size DataArray to be rejected");
        }
        catch (UnsupportedOperationException expected)
        {
            assertTrue(expected.getMessage().contains("variable-size DataArray"));
        }
    }
}
