package org.sensorhub.impl.service.federation.oshconnect;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * Helpers reproducing the pydantic resource-model operations the broker relies
 * on: {@code model_copy(deep=True, update={...})} and
 * {@code model_dump_json(by_alias=True, exclude_none=True)}.
 *
 * Resources are carried as raw {@link JsonObject} already in wire/alias form
 * (that is how they arrive from the remote node's CS API), so a deep copy is a
 * {@code deepCopy()} and a field "update to None" is a key removal.
 */
final class JsonResources
{
    private JsonResources()
    {
    }

    /**
     * Deep-copy {@code src} and apply {@code overrides}. A {@link JsonNull}
     * value clears the field (mirrors {@code update={field: None}} with
     * {@code exclude_none}); any other value replaces it.
     */
    static JsonObject modelCopy(JsonObject src, Map<String, JsonElement> overrides)
    {
        JsonObject copy = src.deepCopy();
        for (Map.Entry<String, JsonElement> e : overrides.entrySet())
        {
            if (e.getValue() == null || e.getValue().isJsonNull())
                copy.remove(e.getKey());
            else
                copy.add(e.getKey(), e.getValue());
        }
        return copy;
    }

    /**
     * Serialize to JSON with null members stripped, mirroring
     * {@code model_dump_json(by_alias=True, exclude_none=True)}.
     */
    static String toWireJson(JsonObject obj)
    {
        return stripNulls(obj.deepCopy()).toString();
    }

    private static JsonObject stripNulls(JsonObject obj)
    {
        for (String key : obj.keySet().toArray(new String[0]))
        {
            JsonElement value = obj.get(key);
            if (value.isJsonNull())
                obj.remove(key);
            else if (value.isJsonObject())
                stripNulls(value.getAsJsonObject());
        }
        return obj;
    }
}
