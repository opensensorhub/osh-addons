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

import org.sensorhub.impl.service.consys.proto.observations.ObsBindingProto;
import org.sensorhub.impl.service.consys.proto.schema.ProtoSchemaWriter;
import static org.junit.Assert.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.junit.Test;
import org.vast.swe.SWEHelper;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;


/**
 * Proves the {@code application/swe+proto} wire is self-sufficient: a receiver
 * that has ONLY the delivered {@code FileDescriptorSet} bytes — never the local
 * generated {@code SweOptions}/observation classes — can rebuild the descriptor
 * and decode an observation that {@link ProtoEncoder} produced. Google
 * well-known types are seeded from the runtime, mirroring the OSHConnect-Python
 * receiver ({@code _build_message_class} / {@code seed_google}).
 */
public class TestSweProtoWireInterop
{
    static DataComponent buildRecord()
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addSamplingTimeIsoUTC("time")
            .addField("airTemp", swe.createQuantity()
                .definition("http://x/temp").uomCode("Cel").dataType(DataType.FLOAT))
            .addField("location", swe.createVector()
                .definition("http://x/loc")
                .refFrame("http://www.opengis.net/def/crs/EPSG/0/4979")
                .addCoordinate("lat", swe.createQuantity().uomCode("deg").dataType(DataType.DOUBLE).build())
                .addCoordinate("lon", swe.createQuantity().uomCode("deg").dataType(DataType.DOUBLE).build()))
            .build();
    }


    /**
     * Rebuild the observation {@link Descriptor} from a serialized
     * {@code FileDescriptorSet}, exactly as a foreign receiver would: the
     * swe_options and observation files come from the SET; only the google
     * well-known types are seeded from the runtime.
     */
    static Descriptor descriptorFromWire(byte[] fdsBytes, String messageFqn) throws Exception
    {
        var set = FileDescriptorSet.parseFrom(fdsBytes);

        var built = new HashMap<String, FileDescriptor>();
        // seed google WKTs from runtime (the receiver always has these)
        built.put("google/protobuf/descriptor.proto",
            DescriptorProtos.FileDescriptorProto.getDescriptor().getFile());
        built.put("google/protobuf/timestamp.proto",
            Timestamp.getDescriptor().getFile());

        var provided = new LinkedHashMap<String, FileDescriptorProto>();
        for (var f : set.getFileList())
            provided.put(f.getName(), f);

        // topologically build the provided files from the SET (never from the
        // local SweOptions class) — only add a file once its deps are built
        boolean progressed = true;
        while (!provided.isEmpty() && progressed)
        {
            progressed = false;
            var it = provided.entrySet().iterator();
            while (it.hasNext())
            {
                var fdp = it.next().getValue();
                if (fdp.getDependencyList().stream().allMatch(built::containsKey))
                {
                    var deps = fdp.getDependencyList().stream()
                        .map(built::get).toArray(FileDescriptor[]::new);
                    built.put(fdp.getName(), FileDescriptor.buildFrom(fdp, deps));
                    it.remove();
                    progressed = true;
                }
            }
        }
        assertTrue("unresolved files: " + provided.keySet(), provided.isEmpty());

        for (var fd : built.values())
            for (var mt : fd.getMessageTypes())
                if (mt.getFullName().equals(messageFqn))
                    return mt;
        throw new IllegalStateException("message not found in wire set: " + messageFqn);
    }


    @Test
    public void testForeignReceiverDecodesFromWireBytesAlone() throws Exception
    {
        var record = buildRecord();
        var schema = new ProtoSchemaWriter().write(record, "datastreams/ds1/obs.proto",
            ObsBindingProto.OBS_PACKAGE, ObsBindingProto.OBS_MESSAGE);

        // --- producer side: encode an observation with the local descriptor ---
        var localDesc = ProtoSchemaWriter.resolve(schema);
        var data = record.createDataBlock();
        data.setDoubleValue(0, 1_600_000_000.0);  // time
        data.setFloatValue(1, 21.5f);             // airTemp
        data.setDoubleValue(2, 34.7);             // location.lat
        data.setDoubleValue(3, -86.6);            // location.lon
        var env = new ProtoEncoder.Envelope("obs-9", "ds-9", null,
            Instant.ofEpochSecond(1_600_000_000L, 250_000_000), null);
        var obsBytes = ProtoEncoder.encode(record, localDesc, data, env).toByteArray();

        // --- receiver side: descriptor comes ONLY from the wire FileDescriptorSet ---
        var fdsBytes = ProtoSchemaWriter.toFileDescriptorSet(schema);
        var wireDesc = descriptorFromWire(fdsBytes, schema.messageTypeName);

        var decoded = DynamicMessage.parseFrom(wireDesc, obsBytes);

        // envelope decodes
        assertEquals("obs-9", decoded.getField(wireDesc.findFieldByName("id")));
        var phen = (DynamicMessage) decoded.getField(wireDesc.findFieldByName("phenomenon_time"));
        var phenDesc = wireDesc.findFieldByName("phenomenon_time").getMessageType();
        assertEquals(1_600_000_000L, phen.getField(phenDesc.findFieldByName("seconds")));

        // result scalar + nested vector leaf decode
        assertEquals(21.5f, (float) decoded.getField(wireDesc.findFieldByName("airTemp")), 1e-6);
        var loc = (DynamicMessage) decoded.getField(wireDesc.findFieldByName("location"));
        var locDesc = wireDesc.findFieldByName("location").getMessageType();
        assertEquals(34.7, (double) loc.getField(locDesc.findFieldByName("lat")), 1e-9);
        assertEquals(-86.6, (double) loc.getField(locDesc.findFieldByName("lon")), 1e-9);
    }
}
