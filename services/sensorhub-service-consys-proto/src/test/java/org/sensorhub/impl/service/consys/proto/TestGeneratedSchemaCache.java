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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.sensorhub.api.common.BigId;
import org.vast.swe.SWEHelper;
import net.opengis.swe.v20.DataComponent;


/**
 * {@link GeneratedSchemaCache} semantics: one build per stream while the
 * structure is unchanged (same Entry instance returned), automatic rebuild
 * when the structure's fingerprint changes, and per-stream isolation.
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
    public void testBuildsOncePerStreamAndReturnsSameEntry() throws Exception
    {
        var builds = new AtomicInteger();
        var cache = new GeneratedSchemaCache(struct -> {
            builds.incrementAndGet();
            return new ProtoSchemaWriter().write(struct, "obs.proto", "test.memo", "Obs");
        });

        var struct = record("temp", "Cel");
        var e1 = cache.get(DS_1, struct);
        var e2 = cache.get(DS_1, struct);
        // structurally equal but distinct instance — fingerprint must hit anyway
        var e3 = cache.get(DS_1, record("temp", "Cel"));

        assertSame(e1, e2);
        assertSame(e1, e3);
        assertEquals(1, builds.get());
        assertNotNull(e1.descriptor);
        assertNotNull(e1.fileDescriptorSet);
        assertTrue(e1.fileDescriptorSet.length > 0);
    }


    @Test
    public void testRebuildsWhenStructureChanges() throws Exception
    {
        var builds = new AtomicInteger();
        var cache = new GeneratedSchemaCache(struct -> {
            builds.incrementAndGet();
            return new ProtoSchemaWriter().write(struct, "obs.proto", "test.memo", "Obs");
        });

        var e1 = cache.get(DS_1, record("temp", "Cel"));
        // same stream id, changed uom → stale entry must be replaced
        var e2 = cache.get(DS_1, record("temp", "K"));

        assertNotSame(e1, e2);
        assertEquals(2, builds.get());

        // and the new entry is now the cached one
        assertSame(e2, cache.get(DS_1, record("temp", "K")));
        assertEquals(2, builds.get());
    }


    @Test
    public void testStreamsAreIsolated() throws Exception
    {
        var builds = new AtomicInteger();
        var cache = new GeneratedSchemaCache(struct -> {
            builds.incrementAndGet();
            return new ProtoSchemaWriter().write(struct, "obs.proto", "test.memo", "Obs");
        });

        var e1 = cache.get(DS_1, record("temp", "Cel"));
        var e2 = cache.get(DS_2, record("pressure", "hPa"));

        assertNotSame(e1, e2);
        assertEquals(2, builds.get());
        assertSame(e1, cache.get(DS_1, record("temp", "Cel")));
        assertSame(e2, cache.get(DS_2, record("pressure", "hPa")));
        assertEquals(2, builds.get());
    }
}
