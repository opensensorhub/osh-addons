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
import java.io.StringWriter;
import org.junit.Test;
import org.vast.swe.SWEHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;


/**
 * Verifies the swe+flatbuffers JSON schema faithfully mirrors the FlexBuffers wire
 * structure produced by {@link FlexEncoder}: envelope + nested {@code result},
 * records→object, arrays→array (with fixed/variable size), choice→{case,value:oneOf},
 * range→2-tuple, iso-time→epoch number, and SWE annotations at each node.
 */
public class TestFlexJsonSchema
{
    static DataComponent stressorRecord()
    {
        var swe = new SWEHelper();
        return swe.createRecord()
            .addField("time", swe.createTime().asSamplingTimeIsoUTC())
            .addField("len", swe.createCount().id("LEN").label("Vector length"))
            .addField("vector", swe.createArray().withVariableSize("LEN")
                .withElement("v", swe.createQuantity().definition("http://x/v").uomCode("m").dataType(DataType.DOUBLE).build()))
            .addField("fixed3", swe.createArray().withFixedSize(3)
                .withElement("f", swe.createQuantity().dataType(DataType.DOUBLE).build()))
            .addField("temp", swe.createQuantityRange().uomCode("Cel").dataType(DataType.DOUBLE).build())
            .addField("mode", swe.createChoice()
                .addItem("a", swe.createCount().build())
                .addItem("b", swe.createQuantity().dataType(DataType.DOUBLE).build())
                .build())
            .build();
    }


    static JsonObject schemaOf(DataComponent rec) throws Exception
    {
        var sw = new StringWriter();
        try (var jw = new JsonWriter(sw))
        {
            FlexJsonSchema.writePayloadSchema(jw, "Test DS", "a description", rec);
        }
        return JsonParser.parseString(sw.toString()).getAsJsonObject();
    }


    @Test
    public void envelopeAndResultShape() throws Exception
    {
        var root = schemaOf(stressorRecord());
        assertEquals("object", root.get("type").getAsString());
        assertEquals("application/swe+flatbuffers", root.get("x-swe-format").getAsString());

        var props = root.getAsJsonObject("properties");
        // envelope
        assertEquals("string", props.getAsJsonObject("id").get("type").getAsString());
        assertEquals("number", props.getAsJsonObject("phenomenon_time").get("type").getAsString());
        assertEquals("epoch-seconds", props.getAsJsonObject("result_time").get("x-swe-time-encoding").getAsString());
        // only result is required
        assertTrue(root.getAsJsonArray("required").toString().contains("result"));
        assertTrue(props.has("result"));
    }


    @Test
    public void nestedStructureIsFaithful() throws Exception
    {
        var result = schemaOf(stressorRecord()).getAsJsonObject("properties").getAsJsonObject("result");
        assertEquals("object", result.get("type").getAsString());
        var rp = result.getAsJsonObject("properties");

        // iso time → epoch-seconds number
        assertEquals("number", rp.getAsJsonObject("time").get("type").getAsString());
        assertEquals("epoch-seconds", rp.getAsJsonObject("time").get("x-swe-time-encoding").getAsString());

        // variable array → array, marked variable, size field named, element carries semantics
        var vector = rp.getAsJsonObject("vector");
        assertEquals("array", vector.get("type").getAsString());
        assertTrue(vector.get("x-swe-variable-size").getAsBoolean());
        var vItems = vector.getAsJsonObject("items");
        assertEquals("number", vItems.get("type").getAsString());
        assertEquals("m", vItems.get("x-ogc-unit").getAsString());
        assertEquals("http://x/v", vItems.get("x-ogc-definition").getAsString());

        // fixed array → array with min/maxItems == 3
        var fixed3 = rp.getAsJsonObject("fixed3");
        assertEquals("array", fixed3.get("type").getAsString());
        assertEquals(3, fixed3.get("minItems").getAsInt());
        assertEquals(3, fixed3.get("maxItems").getAsInt());

        // range → 2-tuple array
        var temp = rp.getAsJsonObject("temp");
        assertEquals("array", temp.get("type").getAsString());
        assertEquals(2, temp.get("minItems").getAsInt());
        assertEquals(2, temp.get("maxItems").getAsInt());
        assertEquals("Cel", temp.get("x-ogc-unit").getAsString());

        // choice → {case:int, value:oneOf[2]}
        var mode = rp.getAsJsonObject("mode");
        assertTrue(mode.get("x-swe-choice").getAsBoolean());
        var mp = mode.getAsJsonObject("properties");
        assertEquals("integer", mp.getAsJsonObject("case").get("type").getAsString());
        var oneOf = mp.getAsJsonObject("value").getAsJsonArray("oneOf");
        assertEquals(2, oneOf.size());
        assertEquals("integer", oneOf.get(0).getAsJsonObject().get("type").getAsString());   // Count item
        assertEquals("number", oneOf.get(1).getAsJsonObject().get("type").getAsString());    // Quantity item
    }
}
