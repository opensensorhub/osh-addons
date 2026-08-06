package org.sensorhub.impl.service.federation.oshconnect;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Port of the slice of oshconnect.resource_datamodels.SystemResource the broker
 * uses. Backed by the raw CS API JSON (SML+JSON form: {@code id},
 * {@code uniqueId}, {@code label}). Field aliases used here:
 * {@code system_id -> "id"}, {@code uid -> "uniqueId"}, {@code label -> "label"},
 * {@code links -> "links"}.
 */
public class SystemResource
{
    private final JsonObject json;

    public SystemResource(JsonObject json)
    {
        this.json = json;
    }

    public JsonObject getJson()
    {
        return json;
    }

    public String getSystemId()
    {
        return json.has("id") && !json.get("id").isJsonNull() ? json.get("id").getAsString() : null;
    }

    public void setSystemId(String systemId)
    {
        if (systemId == null)
            json.remove("id");
        else
            json.add("id", new JsonPrimitive(systemId));
    }

    public String getUid()
    {
        return json.has("uniqueId") && !json.get("uniqueId").isJsonNull()
                ? json.get("uniqueId").getAsString() : null;
    }

    public String getLabel()
    {
        return json.has("label") && !json.get("label").isJsonNull()
                ? json.get("label").getAsString() : null;
    }

    public boolean hasProperties()
    {
        return json.has("properties") && json.get("properties").isJsonObject();
    }

    public JsonObject getProperties()
    {
        return json.getAsJsonObject("properties");
    }

    public SystemResource modelCopy(Map<String, JsonElement> update)
    {
        return new SystemResource(JsonResources.modelCopy(json, update));
    }

    public String toWireJson()
    {
        return JsonResources.toWireJson(json);
    }
}
