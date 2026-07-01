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

import java.io.IOException;
import java.util.HashSet;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.HasRefFrames;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.RangeComponent;
import net.opengis.swe.v20.ScalarComponent;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;


/**
 * <p>
 * Writes a JSON Schema that <b>faithfully mirrors the {@code swe+flatbuffers}
 * (FlexBuffers) wire structure</b> emitted by {@link FlexEncoder} — as opposed to
 * the flat, leaf-only logical schema. Each observation is a length-prefixed
 * FlexBuffers map, so the schema is a nested object:
 * </p>
 * <ul>
 *   <li>the fixed envelope ({@code id}, {@code datastream_id}, {@code foi_id},
 *       {@code phenomenon_time}, {@code result_time}) + a {@code result} slot</li>
 *   <li>{@code DataRecord}/{@code Vector} → {@code object} whose property keys are
 *       the exact FlexBuffers map keys (derived via the same {@link FlexKeys})</li>
 *   <li>{@code DataArray} → {@code array} ({@code items} = element schema;
 *       {@code minItems}/{@code maxItems} for fixed size)</li>
 *   <li>{@code DataChoice} → {@code object} {@code {case:int, value:oneOf[...]}}</li>
 *   <li>{@code RangeComponent} → 2-element {@code array} {@code [low, high]}</li>
 *   <li>ISO {@code Time} → {@code number} (epoch seconds); scalars → their type</li>
 * </ul>
 *
 * <p>SWE semantics ({@code definition}, {@code label}, unit, reference frame) are
 * attached at every node as {@code x-ogc-*} keywords, mirroring the logical
 * schema's convention.</p>
 *
 * @author Ian Patterson
 * @since 2026
 */
public final class FlexJsonSchema
{
    private FlexJsonSchema() {}


    /**
     * Write the full payload schema for an observation stream: the fixed envelope
     * plus the {@code result} record.
     */
    public static void writePayloadSchema(JsonWriter w, String title, String description, DataComponent result) throws IOException
    {
        w.beginObject();
        w.name("type").value("object");
        if (notBlank(title))
            w.name("title").value(title);
        w.name("description").value(
            (notBlank(description) ? description + " — " : "")
            + "application/swe+flatbuffers wire schema: each observation is a length-prefixed "
            + "(4-byte big-endian) FlexBuffers map.");
        w.name("x-swe-format").value("application/swe+flatbuffers");

        w.name("properties").beginObject();
        envelopeString(w, "id", "observation local id (present only when set)");
        envelopeString(w, "datastream_id", "datastream local id (present only when set)");
        envelopeString(w, "foi_id", "feature-of-interest local id (present only when set)");
        envelopeTime(w, "phenomenon_time");
        envelopeTime(w, "result_time");
        w.name("result");
        writeComponent(w, result);
        w.endObject();

        w.name("required").beginArray().value("result").endArray();
        w.endObject();
        w.flush();
    }


    /** Recursively write the schema for one component, mirroring {@code FlexEncoder}. */
    static void writeComponent(JsonWriter w, DataComponent comp) throws IOException
    {
        w.beginObject();

        if (comp instanceof DataArray)
        {
            var array = (DataArray) comp;
            w.name("type").value("array");
            if (!array.isVariableSize())
            {
                int size = array.getComponentCount();
                w.name("minItems").value(size);
                w.name("maxItems").value(size);
            }
            else
            {
                w.name("x-swe-variable-size").value(true);
                var sizeComp = array.getArraySizeComponent();
                if (sizeComp != null && notBlank(sizeComp.getName()))
                    w.name("x-swe-size-field").value(sizeComp.getName());
            }
            w.name("items");
            writeComponent(w, array.getElementType());
            writeAnnotations(w, comp);
        }
        else if (comp instanceof DataChoice)
        {
            var choice = (DataChoice) comp;
            w.name("type").value("object");
            w.name("x-swe-choice").value(true);
            w.name("properties").beginObject();
            w.name("case").beginObject()
                .name("type").value("integer")
                .name("description").value("index of the selected item")
                .endObject();
            w.name("value").beginObject();
            w.name("oneOf").beginArray();
            for (int i = 0; i < choice.getComponentCount(); i++)
                writeComponent(w, choice.getComponent(i));
            w.endArray();
            w.endObject();
            w.endObject();
            w.name("required").beginArray().value("case").value("value").endArray();
            writeAnnotations(w, comp);
        }
        else if (comp instanceof DataRecord || comp instanceof Vector)
        {
            w.name("type").value("object");
            w.name("properties").beginObject();
            var used = new HashSet<String>();
            for (int i = 0; i < comp.getComponentCount(); i++)
            {
                var child = comp.getComponent(i);
                w.name(FlexKeys.unique(used, child.getName()));
                writeComponent(w, child);
            }
            w.endObject();
            writeAnnotations(w, comp);
        }
        else if (comp instanceof RangeComponent)
        {
            w.name("type").value("array");
            w.name("minItems").value(2);
            w.name("maxItems").value(2);
            w.name("items").beginObject().name("type").value(scalarJsonType(comp)).endObject();
            w.name("x-swe-range").value("[low, high]");
            writeAnnotations(w, comp);
        }
        else if (comp instanceof Time && ((Time) comp).isIsoTime())
        {
            w.name("type").value("number");
            w.name("x-swe-time-encoding").value("epoch-seconds");
            writeAnnotations(w, comp);
        }
        else
        {
            // plain scalar
            w.name("type").value(scalarJsonType(comp));
            if (comp instanceof Category && ((Category) comp).getConstraint() != null)
            {
                var values = ((Category) comp).getConstraint().getValueList();
                if (values != null && !values.isEmpty())
                {
                    w.name("enum").beginArray();
                    for (var v : values)
                        w.value(v);
                    w.endArray();
                }
            }
            writeAnnotations(w, comp);
        }

        w.endObject();
    }


    private static void envelopeString(JsonWriter w, String name, String desc) throws IOException
    {
        w.name(name).beginObject()
            .name("type").value("string")
            .name("description").value(desc)
            .endObject();
    }


    private static void envelopeTime(JsonWriter w, String name) throws IOException
    {
        w.name(name).beginObject()
            .name("type").value("number")
            .name("x-swe-time-encoding").value("epoch-seconds")
            .name("description").value("present only when set")
            .endObject();
    }


    /** Map a scalar/range SWE component to a JSON Schema type; {@code "null"} for
     *  shapes with no FlexBuffers scalar mapping (e.g. Geometry). */
    static String scalarJsonType(DataComponent comp)
    {
        try
        {
            switch (FlexTypes.of(comp))
            {
                case BOOL:   return "boolean";
                case INT:
                case LONG:   return "integer";
                case FLOAT:
                case DOUBLE: return "number";
                case STRING: return "string";
                default:     return "null";
            }
        }
        catch (Exception e)
        {
            return "null";
        }
    }


    /** Attach SWE semantics as {@code x-ogc-*} keywords (mirrors ProtoSchemaWriter.sweOptions). */
    static void writeAnnotations(JsonWriter w, DataComponent comp) throws IOException
    {
        if (notBlank(comp.getLabel()))
            w.name("title").value(comp.getLabel());
        if (notBlank(comp.getDescription()))
            w.name("description").value(comp.getDescription());
        if (notBlank(comp.getDefinition()))
            w.name("x-ogc-definition").value(comp.getDefinition());

        if (comp instanceof HasUom)
        {
            var uom = ((HasUom) comp).getUom();
            if (uom != null)
            {
                if (notBlank(uom.getCode()))
                    w.name("x-ogc-unit").value(uom.getCode());
                else if (notBlank(uom.getHref()))
                    w.name("x-ogc-unit").value(uom.getHref());
            }
        }

        if (comp instanceof HasRefFrames)
        {
            var rf = (HasRefFrames) comp;
            if (notBlank(rf.getReferenceFrame()))
                w.name("x-ogc-refFrame").value(rf.getReferenceFrame());
            if (notBlank(rf.getLocalFrame()))
                w.name("x-ogc-localFrame").value(rf.getLocalFrame());
        }

        if (comp instanceof ScalarComponent && notBlank(((ScalarComponent) comp).getAxisID()))
            w.name("x-ogc-axis").value(((ScalarComponent) comp).getAxisID());
    }


    private static boolean notBlank(String s)
    {
        return s != null && !s.isBlank();
    }
}
