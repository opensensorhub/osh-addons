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

package org.sensorhub.impl.service.consys.flatbuffers.codec;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import org.junit.Test;
import org.vast.swe.SWEHelper;
import com.google.flatbuffers.FlexBuffers;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;


/**
 * Same-node round-trip for the swe+flatbuffers (FlexBuffers) codec: encode a flat
 * {@link DataBlock} to a self-describing FlexBuffers payload and decode it back
 * against a fresh copy of the record structure (the binding's inbound path — the
 * decoder never sees the encoder's sized struct). Mirrors the proto array tests,
 * including the swetypes 'arrays' stressor encoded against a reused, unsized
 * struct.
 */
public class TestFlexRoundTrip
{
    /** Encode {@code blk} against {@code encStruct}, then decode the payload's
     *  {@code result} against {@code decStruct} (a fresh, unsized copy). */
    static DataBlock rt(DataComponent encStruct, DataBlock blk, DataComponent decStruct)
    {
        byte[] bytes = FlexEncoder.encode(encStruct, blk, null);
        var resultRef = FlexBuffers.getRoot(ByteBuffer.wrap(bytes)).asMap().get("result");
        return FlexDecoder.decodeResult(decStruct, resultRef);
    }


    static DataComponent scalarRec()
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addField("t", swe.createTime().asSamplingTimeIsoUTC())
            .addField("airTemp", swe.createQuantity().definition("http://x/temp").uomCode("Cel"))
            .addField("count", swe.createCount().definition("http://x/count"))
            .addField("valid", swe.createBoolean().definition("http://x/valid"))
            .addField("label", swe.createText().definition("http://x/label"))
            .addField("cat", swe.createCategory().definition("http://x/cat"))
            .addField("range", swe.createQuantityRange().definition("http://x/r").uomCode("m").dataType(DataType.DOUBLE))
            .build();
    }


    @Test
    public void scalarsRoundTrip()
    {
        var rec = scalarRec();
        var blk = rec.createDataBlock();           // [t, airTemp, count, valid, label, cat, rLow, rHigh]
        assertEquals(8, blk.getAtomCount());
        blk.setDoubleValue(0, 1_700_000_000.0);
        blk.setDoubleValue(1, 21.5);
        blk.setIntValue(2, 7);
        blk.setBooleanValue(3, true);
        blk.setStringValue(4, "hello");
        blk.setStringValue(5, "GREEN");
        blk.setDoubleValue(6, -1.0);
        blk.setDoubleValue(7, 3.0);

        var out = rt(rec, blk, scalarRec());
        assertEquals(8, out.getAtomCount());
        assertEquals(1_700_000_000.0, out.getDoubleValue(0), 1e-6);
        assertEquals(21.5, out.getDoubleValue(1), 1e-9);
        assertEquals(7, out.getIntValue(2));
        assertTrue(out.getBooleanValue(3));
        assertEquals("hello", out.getStringValue(4));
        assertEquals("GREEN", out.getStringValue(5));
        assertEquals(-1.0, out.getDoubleValue(6), 1e-9);
        assertEquals(3.0, out.getDoubleValue(7), 1e-9);
    }


    @Test
    public void nestedRecordAndTrailingScalar()
    {
        var swe = new SWEHelper();
        var rec = swe.createRecord()
            .addField("inner", swe.createRecord()
                .addField("a", swe.createQuantity().dataType(DataType.DOUBLE).build())
                .addField("b", swe.createCount().build())
                .build())
            .addField("tail", swe.createBoolean().build())
            .build();
        var blk = rec.createDataBlock();           // [a, b, tail]
        assertEquals(3, blk.getAtomCount());
        blk.setDoubleValue(0, 4.25);
        blk.setIntValue(1, 11);
        blk.setBooleanValue(2, true);

        var out = rt(rec, blk, rec.copy());
        assertEquals(4.25, out.getDoubleValue(0), 1e-9);
        assertEquals(11, out.getIntValue(1));
        assertTrue(out.getBooleanValue(2));
    }


    static DataComponent varRec()
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
    public void variableArrayEncodeAgainstUnsizedStruct()
    {
        var src = varRec();
        ((DataArray) src.getComponent("samples")).updateSize(3);
        var blk = src.createDataBlock();           // [n, v0,v1,v2, tail] = 5
        assertEquals(5, blk.getAtomCount());
        blk.setIntValue(0, 3);
        blk.setDoubleValue(1, 5); blk.setDoubleValue(2, 6); blk.setDoubleValue(3, 7);
        blk.setBooleanValue(4, true);

        // encode against an UNSIZED struct (mirrors the binding's shared struct)
        var out = rt(varRec(), blk, varRec());
        assertEquals(5, out.getAtomCount());
        assertEquals(3, out.getIntValue(0));
        assertEquals(5.0, out.getDoubleValue(1), 1e-9);
        assertEquals(7.0, out.getDoubleValue(3), 1e-9);
        assertTrue(out.getBooleanValue(4));        // trailing scalar after a variable array
    }


    /** Exact shape of the swetypes 'arrays' output: Count preamble, then a
     *  variable vector AND a nested variable matrix, then a trailing fixed array. */
    static DataComponent sweTypesArrays()
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
    public void sweTypesArraysReusedStructVaryingDims()
    {
        // The binding reuses ONE recordStruct across observations; swetypes
        // randomizes its dimensions each sample. This is the production trigger
        // that stalled the proto publisher before its sizing fix.
        var encStruct = sweTypesArrays().copy();   // reused, like the binding's recordStruct

        int[][] dims = { {2, 2, 3}, {3, 1, 2}, {1, 3, 4}, {4, 2, 2}, {2, 2, 3} };
        for (var dim : dims)
        {
            int len = dim[0], m = dim[1], n = dim[2];
            var src = sweTypesArrays();
            ((DataArray) src.getComponent("vector")).updateSize(len);
            var matrix = (DataArray) src.getComponent("matrix");
            matrix.updateSize(m);
            for (int r = 0; r < m; r++)
                ((DataArray) matrix.getComponent(r)).updateSize(n);
            var blk = src.createDataBlock();        // [len,rows,cols, vec(len), mtx(m*n), fixed3(3)]
            int expected = 3 + len + m * n + 3;
            assertEquals(expected, blk.getAtomCount());
            int i = 0;
            blk.setIntValue(i++, len); blk.setIntValue(i++, m); blk.setIntValue(i++, n);
            for (int k = 0; k < len; k++) blk.setDoubleValue(i++, k + 0.5);
            for (int k = 0; k < m * n; k++) blk.setDoubleValue(i++, 10.0 + k);
            for (int k = 0; k < 3; k++) blk.setDoubleValue(i++, 100.0 + k);

            var out = rt(encStruct, blk, sweTypesArrays());
            assertEquals("dims " + len + "/" + m + "/" + n, expected, out.getAtomCount());
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
    }


    @Test
    public void dataChoiceRoundTrip()
    {
        var swe = new SWEHelper();
        var choice = swe.createChoice()
            .name("counterControl")
            .addItem("setCountDown", swe.createBoolean().build())
            .addItem("setStep", swe.createCount().build())
            .addItem("setBound", swe.createQuantity().dataType(DataType.DOUBLE).build())
            .build();

        // choice block layout: atom 0 = selection index, atom 1+ = selected item
        choice.setSelectedItem(1);
        var block = choice.createDataBlock();
        block.setIntValue(0, 1);
        block.setIntValue(1, 25);

        var out = rt(choice, block, choice.copy());
        assertEquals(1, out.getIntValue(0));       // selection index
        assertEquals(25, out.getIntValue(1));      // setStep value
    }


    @Test
    public void envelopeAndFramingRoundTrip() throws Exception
    {
        var rec = scalarRec();
        var blk = rec.createDataBlock();
        blk.setDoubleValue(0, 1_700_000_000.0);
        blk.setDoubleValue(1, 21.5);
        blk.setIntValue(2, 7);
        blk.setBooleanValue(3, true);
        blk.setStringValue(4, "hi");
        blk.setStringValue(5, "RED");
        blk.setDoubleValue(6, 0.0);
        blk.setDoubleValue(7, 1.0);

        var pt = Instant.ofEpochSecond(1_700_000_000L);
        var rst = Instant.ofEpochSecond(1_700_000_005L);
        var env = new FlexEncoder.Envelope("obs1", "ds1", "foi1", pt, rst);

        // frame two observations, then read them back off the stream
        var bos = new ByteArrayOutputStream();
        FlexFraming.writeFrame(bos, FlexEncoder.encode(rec, blk, env));
        FlexFraming.writeFrame(bos, FlexEncoder.encode(rec, blk, env));

        var in = new ByteArrayInputStream(bos.toByteArray());
        int frames = 0;
        byte[] payload;
        while ((payload = FlexFraming.readFrame(in)) != null)
        {
            var map = FlexBuffers.getRoot(ByteBuffer.wrap(payload)).asMap();
            assertEquals("obs1", FlexDecoder.getString(map, "id"));
            assertEquals("ds1", FlexDecoder.getString(map, "datastream_id"));
            assertEquals(pt, FlexDecoder.getInstant(map, "phenomenon_time"));
            assertEquals(rst, FlexDecoder.getInstant(map, "result_time"));
            var out = FlexDecoder.decodeResult(scalarRec(), map.get("result"));
            assertEquals(21.5, out.getDoubleValue(1), 1e-9);
            assertEquals("RED", out.getStringValue(5));
            frames++;
        }
        assertEquals(2, frames);
    }
}
