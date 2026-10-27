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

        var wire = ProtoEncoder.encode(rec, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);
        var out = ProtoDecoder.decodeRecord(rec, msg);

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

        var wire = ProtoEncoder.encode(rec, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);
        var out = ProtoDecoder.decodeRecord(rec, msg);

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

        var wire = ProtoEncoder.encode(encStruct, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);

        // decode against a FRESH structure with no array size (mirrors the binding,
        // whose schema struct carries no per-observation size) — prepass() sizes it
        var out = ProtoDecoder.decodeRecord(buildVarRec(), msg);

        assertEquals(6, out.getAtomCount());
        assertEquals(4, out.getIntValue(0));
        assertEquals(5.0, out.getDoubleValue(1), 1e-9);
        assertEquals(8.0, out.getDoubleValue(4), 1e-9);
        assertTrue(out.getBooleanValue(5));              // trailing scalar after a variable array
    }


    @Test
    public void elementChoiceGuard()
    {
        var swe = new SWEHelper();
        // no choice → not flagged: scalar, fixed array, AND a variable sub-array
        // (rectangular arrays stay flat, so a variable array is no longer flagged)
        assertFalse(ProtoArrays.elementHasChoice(swe.createQuantity().build()));
        assertFalse(ProtoArrays.elementHasChoice(swe.createArray().withFixedSize(2)
            .withElement("v", swe.createQuantity().build()).build()));
        assertFalse(ProtoArrays.elementHasChoice(swe.createRecord()
            .addField("k", swe.createCount().id("K2").build())
            .addField("inner", swe.createArray().withVariableSize("K2")
                .withElement("x", swe.createQuantity().build()))
            .build()));

        // a DataChoice IS flagged, including when nested in a record
        var choice = swe.createChoice()
            .addItem("a", swe.createCount().build())
            .addItem("b", swe.createQuantity().build()).build();
        assertTrue(ProtoArrays.elementHasChoice(choice));
        assertTrue(ProtoArrays.elementHasChoice(
            swe.createRecord().addField("c", choice).build()));
    }


    static DataComponent buildVarGrid()
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addField("M", swe.createCount().id("M").build())
            .addField("N", swe.createCount().id("N").build())
            .addField("grid", swe.createArray().withVariableSize("M")
                .withElement("row", swe.createArray().withVariableSize("N")
                    .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build())
                    .build()))
            .build();
    }


    static void assertGridRoundTrips(com.google.protobuf.Descriptors.Descriptor d, int m, int n) throws Exception
    {
        var enc = buildVarGrid();
        var outer = (DataArray) enc.getComponent("grid");
        outer.updateSize(m);
        for (int r = 0; r < m; r++)
            ((DataArray) outer.getComponent(r)).updateSize(n);
        var blk = enc.createDataBlock();                 // [M, N, m*n doubles]
        assertEquals(2 + m * n, blk.getAtomCount());
        blk.setIntValue(0, m);
        blk.setIntValue(1, n);
        for (int i = 0; i < m * n; i++)
            blk.setDoubleValue(2 + i, (i + 1) * 0.5);

        var wire = ProtoEncoder.encode(enc, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);
        var out = ProtoDecoder.decodeRecord(buildVarGrid(), msg);

        assertEquals("atoms for " + m + "x" + n, 2 + m * n, out.getAtomCount());
        assertEquals(m, out.getIntValue(0));
        assertEquals(n, out.getIntValue(1));
        for (int i = 0; i < m * n; i++)
            assertEquals((i + 1) * 0.5, out.getDoubleValue(2 + i), 1e-9);
    }


    @Test
    public void rectangularVariableMatrixRoundTrip() throws Exception
    {
        // one schema; M and N vary PER MESSAGE (each message square)
        var d = desc(buildVarGrid());
        assertGridRoundTrips(d, 2, 3);
        assertGridRoundTrips(d, 3, 2);
        assertGridRoundTrips(d, 1, 5);
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

        var wire = ProtoEncoder.encode(rec, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);
        var out = ProtoDecoder.decodeRecord(rec, msg);

        assertEquals(7, out.getAtomCount());
        for (int i = 0; i < 6; i++)
            assertEquals((i + 1) * 1.5, out.getDoubleValue(i), 1e-9);
        assertEquals(99, out.getIntValue(6));     // trailing scalar after the matrix
    }


    @Test
    public void variableCountMatrixRoundTrip() throws Exception
    {
        var swe = new SWEHelper();
        // a VARIABLE number of FIXED-width rows: array[var n] of (array[3] double).
        // Supported — only the outer dimension is variable; the fixed inner row
        // keeps every element a fixed atom count, so the block stays flat.
        var enc = swe.createRecord()
            .addField("n", swe.createCount().id("NUM").build())
            .addField("rows", swe.createArray().withVariableSize("NUM")
                .withElement("row", swe.createArray().withFixedSize(3)
                    .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build())
                    .build()))
            .addField("tail", swe.createBoolean().build())
            .build();
        var d = desc(enc);

        ((DataArray) enc.getComponent("rows")).updateSize(2);   // 2 rows of 3
        var blk = enc.createDataBlock();                        // [n, r0(3), r1(3), tail] = 8
        assertEquals(8, blk.getAtomCount());
        blk.setIntValue(0, 2);
        for (int i = 0; i < 6; i++)
            blk.setDoubleValue(1 + i, i + 1);
        blk.setBooleanValue(7, true);

        var wire = ProtoEncoder.encode(enc, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);

        // decode against a fresh structure (no row count) — prepass sizes the outer
        var dec = swe.createRecord()
            .addField("n", swe.createCount().id("NUM").build())
            .addField("rows", swe.createArray().withVariableSize("NUM")
                .withElement("row", swe.createArray().withFixedSize(3)
                    .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build())
                    .build()))
            .addField("tail", swe.createBoolean().build())
            .build();
        var out = ProtoDecoder.decodeRecord(dec, msg);

        assertEquals(8, out.getAtomCount());
        assertEquals(2, out.getIntValue(0));
        for (int i = 0; i < 6; i++)
            assertEquals(i + 1, out.getDoubleValue(1 + i), 1e-9);
        assertTrue(out.getBooleanValue(7));
    }


    // ---- encode against an UNSIZED struct (the real binding path) ----
    // ObsBindingProto encodes every observation against ONE shared datastream
    // schema copy (recordStruct = dsInfo.getRecordStructure().copy()) that is
    // NOT pre-sized per observation. encode() must therefore size the struct's
    // variable arrays from the block itself. The round-trip tests above all
    // pre-size the encode struct via updateSize(), so they never exercised this.

    @Test
    public void variableArrayEncodeAgainstUnsizedStruct() throws Exception
    {
        var d = desc(buildVarRec());

        // block produced by the data source (a separately sized component)
        var src = buildVarRec();
        ((DataArray) src.getComponent("samples")).updateSize(3);
        var blk = src.createDataBlock();                 // [n, v0,v1,v2, tail] = 5
        assertEquals(5, blk.getAtomCount());
        blk.setIntValue(0, 3);
        blk.setDoubleValue(1, 5); blk.setDoubleValue(2, 6); blk.setDoubleValue(3, 7);
        blk.setBooleanValue(4, true);

        var encStruct = buildVarRec();                   // UNSIZED, like recordStruct
        var wire = ProtoEncoder.encode(encStruct, d, blk, null).toByteArray();
        var out = ProtoDecoder.decodeRecord(buildVarRec(), DynamicMessage.parseFrom(d, wire));

        assertEquals(5, out.getAtomCount());
        assertEquals(3, out.getIntValue(0));
        assertEquals(7.0, out.getDoubleValue(3), 1e-9);
        assertTrue(out.getBooleanValue(4));
    }


    @Test
    public void matrixEncodeAgainstUnsizedStruct() throws Exception
    {
        // nested rectangular variable matrix — the swetypes 'arrays' case that
        // threw "Datablock is incompatible with specified array size" at runtime.
        var d = desc(buildVarGrid());

        int m = 2, n = 3;
        var src = buildVarGrid();
        var outer = (DataArray) src.getComponent("grid");
        outer.updateSize(m);
        for (int r = 0; r < m; r++)
            ((DataArray) outer.getComponent(r)).updateSize(n);
        var blk = src.createDataBlock();                 // [M, N, m*n doubles]
        blk.setIntValue(0, m); blk.setIntValue(1, n);
        for (int i = 0; i < m * n; i++)
            blk.setDoubleValue(2 + i, (i + 1) * 0.5);

        var encStruct = buildVarGrid();                  // UNSIZED, like recordStruct
        var wire = ProtoEncoder.encode(encStruct, d, blk, null).toByteArray();
        var out = ProtoDecoder.decodeRecord(buildVarGrid(), DynamicMessage.parseFrom(d, wire));

        assertEquals(2 + m * n, out.getAtomCount());
        assertEquals(m, out.getIntValue(0));
        assertEquals(n, out.getIntValue(1));
        for (int i = 0; i < m * n; i++)
            assertEquals((i + 1) * 0.5, out.getDoubleValue(2 + i), 1e-9);
    }


    /** Exact shape of the swetypes 'arrays' output: a Count preamble, then a
     *  variable vector AND a nested variable matrix, then a trailing fixed array.
     *  Two leading variable arrays mean a mis-sized first array drifts the offset
     *  into the second — the case the last-field grid above never exposed. */
    static DataComponent buildSweTypesArrays()
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addField("len", swe.createCount().id("LEN").build())
            .addField("rows", swe.createCount().id("ROWS").build())
            .addField("cols", swe.createCount().id("COLS").build())
            .addField("vector", swe.createArray().withVariableSize("LEN")
                .withElement("v", swe.createQuantity().dataType(DataType.DOUBLE).build()))
            .addField("matrix", swe.createArray().withVariableSize("ROWS")
                .withElement("row", swe.createArray().withVariableSize("COLS")
                    .withElement("c", swe.createQuantity().dataType(DataType.DOUBLE).build())
                    .build()))
            .addField("fixed3", swe.createArray().withFixedSize(3)
                .withElement("f", swe.createQuantity().dataType(DataType.DOUBLE).build()))
            .build();
    }


    @Test
    public void sweTypesArraysEncodeAgainstUnsizedStruct() throws Exception
    {
        var d = desc(buildSweTypesArrays());

        int len = 2, m = 2, n = 3;   // cols=3 reproduces "array size: 3"
        var src = buildSweTypesArrays();
        ((DataArray) src.getComponent("vector")).updateSize(len);
        var matrix = (DataArray) src.getComponent("matrix");
        matrix.updateSize(m);
        for (int r = 0; r < m; r++)
            ((DataArray) matrix.getComponent(r)).updateSize(n);
        var blk = src.createDataBlock();   // [len,rows,cols, vec(len), mtx(m*n), fixed3(3)]
        int expected = 3 + len + m * n + 3;
        assertEquals(expected, blk.getAtomCount());
        int i = 0;
        blk.setIntValue(i++, len); blk.setIntValue(i++, m); blk.setIntValue(i++, n);
        for (int k = 0; k < len; k++) blk.setDoubleValue(i++, k + 0.5);
        for (int k = 0; k < m * n; k++) blk.setDoubleValue(i++, 10.0 + k);
        for (int k = 0; k < 3; k++) blk.setDoubleValue(i++, 100.0 + k);

        var encStruct = buildSweTypesArrays().copy();   // mirror dsInfo.getRecordStructure().copy()
        var wire = ProtoEncoder.encode(encStruct, d, blk, null).toByteArray();
        var out = ProtoDecoder.decodeRecord(buildSweTypesArrays(), DynamicMessage.parseFrom(d, wire));

        assertEquals(expected, out.getAtomCount());
        assertEquals(len, out.getIntValue(0));
        assertEquals(m, out.getIntValue(1));
        assertEquals(n, out.getIntValue(2));
        for (int k = 0; k < len; k++)
            assertEquals(k + 0.5, out.getDoubleValue(3 + k), 1e-9);
        for (int k = 0; k < m * n; k++)
            assertEquals(10.0 + k, out.getDoubleValue(3 + len + k), 1e-9);
        for (int k = 0; k < 3; k++)
            assertEquals(100.0 + k, out.getDoubleValue(3 + len + m * n + k), 1e-9);   // trailing fixed3
    }


    @Test
    public void sweTypesArraysReusedStructVaryingDims() throws Exception
    {
        // The binding reuses ONE recordStruct across observations; swetypes
        // randomizes its dimensions each sample. After the first obs sizes the
        // nested arrays, the next encode must re-size them from a NON-default
        // state. This is the production trigger.
        var d = desc(buildSweTypesArrays());
        var encStruct = buildSweTypesArrays().copy();   // reused, like ObsBindingProto.recordStruct

        int[][] dims = { {2, 2, 3}, {3, 1, 2}, {1, 3, 4}, {4, 2, 2}, {2, 2, 3} };
        for (var dim : dims)
        {
            int len = dim[0], m = dim[1], n = dim[2];
            var src = buildSweTypesArrays();
            ((DataArray) src.getComponent("vector")).updateSize(len);
            var matrix = (DataArray) src.getComponent("matrix");
            matrix.updateSize(m);
            for (int r = 0; r < m; r++)
                ((DataArray) matrix.getComponent(r)).updateSize(n);
            var blk = src.createDataBlock();
            int expected = 3 + len + m * n + 3;
            assertEquals(expected, blk.getAtomCount());
            int i = 0;
            blk.setIntValue(i++, len); blk.setIntValue(i++, m); blk.setIntValue(i++, n);
            for (int k = 0; k < len; k++) blk.setDoubleValue(i++, k + 0.5);
            for (int k = 0; k < m * n; k++) blk.setDoubleValue(i++, 10.0 + k);
            for (int k = 0; k < 3; k++) blk.setDoubleValue(i++, 100.0 + k);

            var wire = ProtoEncoder.encode(encStruct, d, blk, null).toByteArray();
            var out = ProtoDecoder.decodeRecord(buildSweTypesArrays(), DynamicMessage.parseFrom(d, wire));

            assertEquals("dims " + len + "/" + m + "/" + n, expected, out.getAtomCount());
            assertEquals(len, out.getIntValue(0));
            assertEquals(m, out.getIntValue(1));
            assertEquals(n, out.getIntValue(2));
            for (int k = 0; k < m * n; k++)
                assertEquals(10.0 + k, out.getDoubleValue(3 + len + k), 1e-9);
        }
    }
}
