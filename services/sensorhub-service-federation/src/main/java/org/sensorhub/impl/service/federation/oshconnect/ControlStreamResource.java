package org.sensorhub.impl.service.federation.oshconnect;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Port of the slice of oshconnect.resource_datamodels.ControlStreamResource the
 * broker uses. Backed by the raw CS API JSON. Field aliases used here:
 * {@code cs_id -> "id"}, {@code name -> "name"}, {@code command_schema -> "schema"}.
 */
public class ControlStreamResource
{
    private final JsonObject json;

    public ControlStreamResource(JsonObject json)
    {
        this.json = json;
    }

    public JsonObject getJson()
    {
        return json;
    }

    public String getCsId()
    {
        return json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsString() : null;
    }

    public void setCsId(String csId)
    {
        if (csId == null)
            json.remove("id");
        else
            json.add("id", new JsonPrimitive(csId));
    }

    public String getName()
    {
        return json.has("name") && !json.get("name").isJsonNull() ? json.get("name").getAsString() : null;
    }

    /** Mirror of {@code cs_res.command_schema} (None when absent). */
    public JsonElement getCommandSchema()
    {
        return json.has("schema") && !json.get("schema").isJsonNull() ? json.get("schema") : null;
    }

    public void setCommandSchema(JsonElement schema)
    {
        json.add("schema", schema);
    }

    public ControlStreamResource modelCopy(Map<String, JsonElement> update)
    {
        return new ControlStreamResource(JsonResources.modelCopy(json, update));
    }

    public String toWireJson()
    {
        return JsonResources.toWireJson(json);
    }
}
