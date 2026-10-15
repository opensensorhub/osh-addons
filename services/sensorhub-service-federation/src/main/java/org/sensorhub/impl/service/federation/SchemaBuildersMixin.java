package org.sensorhub.impl.service.federation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.sensorhub.impl.service.federation.oshconnect.Datastream;
import org.sensorhub.impl.service.federation.oshconnect.TimeInstant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Port of broker.schema_builders.SchemaBuildersMixin. Pure helpers: topic
 * normalization, observation payload construction, and observation parsing.
 */
public interface SchemaBuildersMixin
{
    /**
     * Normalize a topic: strip the leading '/', optionally prepend a prefix.
     */
    default String normalizeMqttTopic(String topic, String prefix)
    {
        String t = (topic == null ? "" : topic).trim();
        while (t.startsWith("/"))
            t = t.substring(1);

        if (prefix != null)
        {
            String p = prefix.trim();
            while (p.startsWith("/"))
                p = p.substring(1);
            while (p.endsWith("/"))
                p = p.substring(0, p.length() - 1);
            if (!p.isEmpty())
                t = p + "/" + t;
        }
        return t;
    }

    /**
     * Build an observation payload suitable for {@code insert_observation_dict()}.
     * Returns a JSON-serializable object with resultTime, phenomenonTime, result.
     */
    default JsonObject buildInsertObservationDict(Datastream commanderDs, JsonObject item)
    {
        JsonElement phenomenonTime = item.has("phenomenonTime") ? item.get("phenomenonTime") : null;
        JsonElement resultTime = item.has("resultTime") ? item.get("resultTime") : phenomenonTime;

        if (phenomenonTime == null || resultTime == null)
        {
            String isoNow = TimeInstant.nowAsTimeInstant().getIsoTime();
            if (phenomenonTime == null)
                phenomenonTime = new com.google.gson.JsonPrimitive(isoNow);
            if (resultTime == null)
                resultTime = new com.google.gson.JsonPrimitive(isoNow);
        }

        JsonElement incomingResult = item.has("result") && !item.get("result").isJsonNull()
                ? item.get("result") : new JsonObject();

        JsonObject payload = new JsonObject();
        payload.add("resultTime", resultTime);
        payload.add("phenomenonTime", phenomenonTime);
        payload.add("result", incomingResult);
        return payload;
    }

    /**
     * Parse an observation from the inbound deque into a list of observation
     * items. Returns null if parsing fails.
     */
    default List<JsonObject> parseObservation(Object obs, String dsId)
    {
        String text;
        if (obs instanceof byte[])
            text = new String((byte[]) obs, StandardCharsets.UTF_8);
        else if (obs instanceof String)
            text = (String) obs;
        else
            return null;

        JsonElement parsed;
        try
        {
            parsed = JsonParser.parseString(text);
        }
        catch (Exception e)
        {
            return null;
        }

        List<JsonObject> items = new ArrayList<>();
        if (parsed.isJsonObject())
        {
            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("items") && obj.get("items").isJsonArray())
            {
                for (JsonElement x : obj.getAsJsonArray("items"))
                {
                    if (x.isJsonObject())
                        items.add(x.getAsJsonObject());
                }
            }
            else if (obj.has("resultTime") || obj.has("phenomenonTime") || obj.has("result"))
            {
                items.add(obj);
            }
        }

        return items.isEmpty() ? null : items;
    }
}
