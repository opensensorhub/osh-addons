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
import org.sensorhub.impl.service.consys.proto.schema.DataStreamSchemaCache;
import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaReader;
import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaWriter;
import org.vast.data.BinaryComponentImpl;
import org.vast.data.BinaryBlockImpl;
import org.vast.data.BinaryEncodingImpl;
import org.vast.data.DataBlockCompressed;
import org.vast.data.DataBlockMixed;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.RasterHelper;
import com.georobotix.swecommon.SweOptions;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Descriptors.FieldDescriptor;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.ByteOrder;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;


/**
 * Round-trip for a compressed binary-block component — the video-frame shape
 * produced by the rtpcam/ffmpeg/Axis drivers: {@code {time, img, status}} with
 * a {@code BinaryEncoding} whose {@code BinaryBlock} member marks {@code img}
 * as codec-compressed. At runtime the record block is a {@code DataBlockMixed}
 * whose img child is a {@link DataBlockCompressed} carrying the raw compressed
 * frame {@code byte[]} — its atoms can neither be read nor written, so the
 * codec must pass the payload through as a single {@code bytes} field. The
 * trailing {@code status} scalar guards against flat-index drift through the
 * compressed component's (uncompressed-sized) atom span.
 */
public class TestProtoCompressedRoundTrip
{
    static final String PKG = "test.video";
    static final String MSG = "Observation";

    static final int WIDTH = 4, HEIGHT = 3;
    static final int IMG_ATOMS = WIDTH * HEIGHT * 3;   // uncompressed RGB atom span


    /** {@code {time(ISO), img(raster WIDTHxHEIGHT), status(Text)}} with a
     *  JPEG-compressed BinaryBlock assigned to img — the driver-built shape. */
    static DataComponent videoRecord() throws Exception
    {
        var swe = new RasterHelper();
        var rec = swe.createRecord()
            .addField("time", swe.createTime().asSamplingTimeIsoUTC().build())
            .addField("img", swe.newRgbImage(
                swe.createCount().value(WIDTH).build(),
                swe.createCount().value(HEIGHT).build(),
                DataType.BYTE))
            .addField("status", swe.createText().build())
            .build();

        // mirror VideoCamHelper.newVideoOutputCODEC: BinaryEncoding with a
        // BinaryComponent for time and a compressed BinaryBlock for img
        var enc = new BinaryEncodingImpl();
        enc.setByteEncoding(ByteEncoding.RAW);
        enc.setByteOrder(ByteOrder.BIG_ENDIAN);
        var timeEnc = new BinaryComponentImpl();
        timeEnc.setRef("/time");
        timeEnc.setCdmDataType(DataType.DOUBLE);
        enc.addMemberAsComponent(timeEnc);
        var imgEnc = new BinaryBlockImpl();
        imgEnc.setRef("/img");
        imgEnc.setCompression("JPEG");
        enc.addMemberAsBlock(imgEnc);
        SWEHelper.assignBinaryEncoding(rec, enc);
        return rec;
    }


    @Test
    public void schemaEmitsSingleBytesField() throws Exception
    {
        var rec = videoRecord();
        var result = new ProtoSchemaWriter().write(rec, "obs.proto", PKG, MSG);

        // the unresolved FileDescriptorProto retains in-process extension values
        var obsMsg = result.fileDescriptor.getMessageTypeList().stream()
            .filter(m -> m.getName().equals(MSG)).findFirst().orElseThrow();
        var imgField = obsMsg.getFieldList().stream()
            .filter(f -> f.getName().equals("img")).findFirst().orElseThrow();

        assertEquals(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES,
            imgField.getType());
        assertEquals("JPEG", imgField.getOptions().getExtension(SweOptions.compression));

        // and it resolves into a valid descriptor with img as a BYTES field
        var d = ProtoSchemaWriter.resolve(result);
        assertEquals(FieldDescriptor.Type.BYTES, d.findFieldByName("img").getType());
    }


    @Test
    public void compressedFrameRoundTripWithTrailingScalar() throws Exception
    {
        var rec = videoRecord();
        var d = ProtoSchemaWriter.resolve(new ProtoSchemaWriter().write(rec, "obs.proto", PKG, MSG));

        // driver-shaped block: DataBlockMixed[ time(1), compressed(IMG_ATOMS), status(1) ]
        var blk = rec.createDataBlock();
        assertEquals(1 + IMG_ATOMS + 1, blk.getAtomCount());
        var imgBlock = (DataBlockCompressed) ((DataBlockMixed) blk).getUnderlyingObject()[1];
        assertEquals(IMG_ATOMS, imgBlock.getAtomCount());

        var frame = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x01, 0x02, 0x03, 0x04, (byte) 0xFF, (byte) 0xD9};
        blk.setDoubleValue(0, 1234567890.25);
        imgBlock.setUnderlyingObject(frame);           // how rtpcam/ffmpeg set the frame
        blk.setStringValue(1 + IMG_ATOMS, "ok");       // trailing scalar — drift guard

        var wire = ProtoEncoder.encode(rec, d, blk, null).toByteArray();
        var msg = DynamicMessage.parseFrom(d, wire);

        // the wire carries the COMPRESSED payload, not IMG_ATOMS expanded values
        var wireBytes = ((com.google.protobuf.ByteString) msg.getField(d.findFieldByName("img"))).toByteArray();
        assertArrayEquals(frame, wireBytes);

        var out = ProtoDecoder.decodeRecord(rec, msg);
        assertEquals(1 + IMG_ATOMS + 1, out.getAtomCount());
        assertEquals(1234567890.25, out.getDoubleValue(0), 1e-6);
        var outImg = (DataBlockCompressed) ((DataBlockMixed) out).getUnderlyingObject()[1];
        assertArrayEquals(frame, (byte[]) outImg.getUnderlyingObject());
        assertEquals("ok", out.getStringValue(1 + IMG_ATOMS));
    }


    /**
     * FOREIGN ingest: the receiving node has only the serialized
     * FileDescriptorSet (no SWE structure) — the real node-to-node path. The
     * {@code uncompressedSize} option lets {@code ProtoSchemaReader} rebuild a
     * correctly-sized placeholder, so observations decode against the
     * descriptor-derived structure, trailing scalar included.
     */
    @Test
    public void foreignIngestRoundTrip() throws Exception
    {
        // WRITER side (the video node): schema + one encoded observation
        var rec = videoRecord();
        var result = new ProtoSchemaWriter().write(rec, "obs.proto", PKG, MSG);
        var d = ProtoSchemaWriter.resolve(result);
        var fdsBytes = ProtoSchemaWriter.toFileDescriptorSet(result);

        var blk = rec.createDataBlock();
        var imgBlock = (DataBlockCompressed) ((DataBlockMixed) blk).getUnderlyingObject()[1];
        var frame = new byte[]{(byte) 0xFF, (byte) 0xD8, 9, 8, 7, (byte) 0xFF, (byte) 0xD9};
        blk.setDoubleValue(0, 1000.5);
        imgBlock.setUnderlyingObject(frame);
        blk.setStringValue(1 + IMG_ATOMS, "ok");
        var wire = ProtoEncoder.encode(rec, d, blk, null).toByteArray();

        // FOREIGN side: resolve the descriptor set the same way the ingest path
        // does (extension registry so swe_options survive re-parsing), rebuild
        // the SWE structure from the descriptor alone, and decode against it
        var extReg = ExtensionRegistry.newInstance();
        SweOptions.registerAllExtensions(extReg);
        var cache = new DataStreamSchemaCache();
        cache.setExtensionRegistry(extReg);
        // bootstrap the well-known/annotation files exactly as ConSysApiProtoService does
        cache.registerBootstrapTree(com.google.protobuf.Timestamp.getDescriptor().getFile());
        cache.registerBootstrapTree(SweOptions.getDescriptor());
        var foreignDesc = cache.resolveFromSet(fdsBytes, PKG + "." + MSG);
        var foreignStruct = new ProtoSchemaReader().readRecord(foreignDesc);

        var msg = DynamicMessage.parseFrom(foreignDesc, wire);
        var out = ProtoDecoder.decodeRecord(foreignStruct, msg);

        assertEquals(1 + IMG_ATOMS + 1, out.getAtomCount());   // span rebuilt from uncompressedSize
        assertEquals(1000.5, out.getDoubleValue(0), 1e-6);
        var outImg = (DataBlockCompressed) ((DataBlockMixed) out).getUnderlyingObject()[1];
        assertArrayEquals(frame, (byte[]) outImg.getUnderlyingObject());
        assertEquals("ok", out.getStringValue(1 + IMG_ATOMS));
    }
}
