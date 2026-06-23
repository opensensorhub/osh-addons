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


    static DataComponent buildVarRec()
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addField("n", swe.createCount().id("NUM").build())
            .addField("samples", swe.createArray().withVariableSize("NUM")
                .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build()))
            .addField("tail", swe.createBoolean().build())
            .build();
    }


    @Test
    public void variableSizeArrayRoundTrip() throws Exception
    {
        var encStruct = buildVarRec();
        var d = desc(encStruct);

        ((DataArray) encStruct.getComponent("samples")).updateSize(4);
        var blk = encStruct.createDataBlock();           // flat: [n, v0..v3, tail] = 6
        assertEquals(6, blk.getAtomCount());
        blk.setIntValue(0, 4);
        blk.setDoubleValue(1, 5); blk.setDoubleValue(2, 6);
        blk.setDoubleValue(3, 7); blk.setDoubleValue(4, 8);
        blk.setBooleanValue(5, true);

        var wire = ProtoObsEncoder.encode(encStruct, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);

        // decode against a FRESH structure with no array size (mirrors the binding,
        // whose schema struct carries no per-observation size) — prepass() sizes it
        var out = ProtoRecordDecoder.decodeRecord(buildVarRec(), msg);

        assertEquals(6, out.getAtomCount());
        assertEquals(4, out.getIntValue(0));
        assertEquals(5.0, out.getDoubleValue(1), 1e-9);
        assertEquals(8.0, out.getDoubleValue(4), 1e-9);
        assertTrue(out.getBooleanValue(5));              // trailing scalar after a variable array
    }


    @Test
    public void nonFlatElementGuard()
    {
        var swe = new SWEHelper();
        // flat: scalar, fixed array of scalars
        assertFalse(ProtoArrays.hasNonFlatLayout(swe.createQuantity().build()));
        assertFalse(ProtoArrays.hasNonFlatLayout(swe.createArray().withFixedSize(2)
            .withElement("v", swe.createQuantity().build()).build()));

        // non-flat: a DataChoice (variable element size), detected when nested too
        var choice = swe.createChoice()
            .addItem("a", swe.createCount().build())
            .addItem("b", swe.createQuantity().build()).build();
        assertTrue(ProtoArrays.hasNonFlatLayout(choice));
        assertTrue(ProtoArrays.hasNonFlatLayout(
            swe.createRecord().addField("c", choice).build()));

        // non-flat: a variable-size sub-array inside the element
        assertTrue(ProtoArrays.hasNonFlatLayout(swe.createRecord()
            .addField("k", swe.createCount().id("K2").build())
            .addField("inner", swe.createArray().withVariableSize("K2")
                .withElement("x", swe.createQuantity().build()))
            .build()));
    }


    @Test
    public void fixedMatrixRoundTrip() throws Exception
    {
        var swe = new SWEHelper();
        // 2x3 matrix: array[2] of (array[3] of double), plus a trailing scalar
        var rec = swe.createRecord()
            .addField("m", swe.createArray().withFixedSize(2)
                .withElement("row", swe.createArray().withFixedSize(3)
                    .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build())
                    .build()))
            .addField("tail", swe.createCount().build())
            .build();
        var d = desc(rec);

        var blk = rec.createDataBlock();          // flat: [m00..m05, tail] = 7
        assertEquals(7, blk.getAtomCount());
        for (int i = 0; i < 6; i++)
            blk.setDoubleValue(i, (i + 1) * 1.5);
        blk.setIntValue(6, 99);

        var wire = ProtoObsEncoder.encode(rec, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);
        var out = ProtoRecordDecoder.decodeRecord(rec, msg);

        assertEquals(7, out.getAtomCount());
        for (int i = 0; i < 6; i++)
            assertEquals((i + 1) * 1.5, out.getDoubleValue(i), 1e-9);
        assertEquals(99, out.getIntValue(6));     // trailing scalar after the matrix
    }
}
