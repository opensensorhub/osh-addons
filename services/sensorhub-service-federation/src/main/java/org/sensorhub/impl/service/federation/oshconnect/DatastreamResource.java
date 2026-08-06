package org.sensorhub.impl.service.federation.oshconnect;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Port of the slice of oshconnect.resource_datamodels.DatastreamResource the
 * broker uses. Backed by the raw CS API JSON. Field aliases used here:
 * {@code ds_id -> "id"}, {@code name -> "name"}, {@code record_schema -> "schema"},
 * {@code system_id -> "system@id"}.
 */
public class DatastreamResource
{
    private final JsonObject json;

    public DatastreamResource(JsonObject json)
    {
        this.json = json;
    }

    public JsonObject getJson()
    {
        return json;
    }

    public String getDsId()
    {
        return json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsString() : null;
    }

    public void setDsId(String dsId)
    {
        if (dsId == null)
            json.remove("id");
        else
            json.add("id", new JsonPrimitive(dsId));
    }

    public String getName()
    {
        return json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : null;
    }

    /** True if the datastream's advertised {@code formats} include the given media type. */
    public boolean advertisesFormat(String mimeType)
    {
        if (!json.has("formats") || !json.get("formats").isJsonArray())
            return false;
        for (JsonElement f : json.getAsJsonArray("formats"))
        {
            if (!f.isJsonNull() && mimeType.equals(f.getAsString()))
                return true;
        }
        return false;
    }

    /** Mirror of {@code ds_res.record_schema} (None when absent). */
    public JsonElement getRecordSchema()
    {
        return json.has("schema") && !json.get("schema").isJsonNull() ? json.get("schema") : null;
    }

    public void setRecordSchema(JsonElement schema)
    {
        json.add("schema", schema);
    }

    public DatastreamResource modelCopy(Map<String, JsonElement> update)
    {
        return new DatastreamResource(JsonResources.modelCopy(json, update));
    }

    public String toWireJson()
    {
        return JsonResources.toWireJson(json);
    }
}
