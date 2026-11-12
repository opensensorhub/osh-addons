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

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.sensorhub.api.common.BigId;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataComponent;


/**
 * {@link GeneratedSchemaCache} semantics after the fingerprint cache was parked
 * (2026-06-17): the artifacts are rebuilt on <b>every</b> call, each build
 * yields a usable descriptor + non-empty {@code FileDescriptorSet}, and the
 * output is deterministic for a given structure. The previous caching test
 * (one build per stream, rebuild on fingerprint change, per-stream isolation)
 * is preserved at {@code docs/parked/TestGeneratedSchemaCache.java.txt} and on
 * branch {@code parked/schema-fingerprint-cache}.
 */
public class TestGeneratedSchemaCache
{
    static final BigId DS_1 = BigId.fromLong(1, 101);
    static final BigId DS_2 = BigId.fromLong(1, 102);


    static DataComponent record(String fieldName, String uom)
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addField(fieldName, swe.createQuantity().definition("http://x/q").uomCode(uom))
            .build();
    }


    @Test
    public void testRebuildsOnEveryCall() throws Exception
    {
        var builds = new AtomicInteger();
        var cache = new GeneratedSchemaCache(struct -> {
            builds.incrementAndGet();
            return new ProtoSchemaWriter().write(struct, "obs.proto", "test.memo", "Obs");
        });

        var struct = record("temp", "Cel");
        var e1 = cache.get(DS_1, struct);
        var e2 = cache.get(DS_1, struct);

        // no memoization: distinct Entry instances, one build each
        assertNotSame(e1, e2);
        assertEquals(2, builds.get());

        // every build still yields usable artifacts
        assertNotNull(e1.descriptor);
        assertNotNull(e1.fileDescriptorSet);
        assertTrue(e1.fileDescriptorSet.length > 0);
    }


    @Test
    public void testOutputIsDeterministicForSameStructure() throws Exception
    {
        var cache = new GeneratedSchemaCache(
            struct -> new ProtoSchemaWriter().write(struct, "obs.proto", "test.memo", "Obs"));

        var e1 = cache.get(DS_1, record("temp", "Cel"));
        var e2 = cache.get(DS_1, record("temp", "Cel"));

        // structurally equal inputs → byte-identical schema each rebuild
        assertEquals(e1.schema.messageTypeName, e2.schema.messageTypeName);
        assertTrue(Arrays.equals(e1.fileDescriptorSet, e2.fileDescriptorSet));
    }


    @Test
    public void testDistinctStructuresProduceDistinctSchemas() throws Exception
    {
        var cache = new GeneratedSchemaCache(
            struct -> new ProtoSchemaWriter().write(struct, "obs.proto", "test.memo", "Obs"));

        var e1 = cache.get(DS_1, record("temp", "Cel"));
        var e2 = cache.get(DS_2, record("pressure", "hPa"));

        // different record structures → different generated descriptor bytes
        assertFalse(Arrays.equals(e1.fileDescriptorSet, e2.fileDescriptorSet));
        assertNotNull(e1.descriptor);
        assertNotNull(e2.descriptor);
    }
}
