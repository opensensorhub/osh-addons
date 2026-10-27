package org.sensorhub.impl.service.federation.oshconnect;

import java.net.http.HttpResponse;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Minimal stand-in for the {@code requests.Response} object that oshconnect's
 * request wrappers return. Exposes the handful of members the broker reads:
 * {@code ok}, {@code status_code}, {@code headers['Location']}, {@code text},
 * and {@code json()}.
 */
public class ApiResponse
{
    private final HttpResponse<String> response;

    public ApiResponse(HttpResponse<String> response)
    {
        this.response = response;
    }

    public boolean ok()
    {
        int code = response.statusCode();
        return code >= 200 && code < 300;
    }

    public int statusCode()
    {
        return response.statusCode();
    }

    public String text()
    {
        return response.body();
    }

    public String header(String name)
    {
        return response.headers().firstValue(name).orElse(null);
    }

    public JsonElement json()
    {
        return JsonParser.parseString(response.body());
    }

    public void raiseForStatus()
    {
        if (!ok())
            throw new RuntimeException("HTTP " + statusCode() + " — " + text());
    }
}
