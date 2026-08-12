package org.sensorhub.impl.service.federation.oshconnect;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Port of oshconnect.csapi4py.default_api_helpers.APIHelper for the operations
 * the broker exercises: GET resource listings/schemas, POST new resources, and
 * MQTT topic construction.
 */
public class APIHelper
{
    private final String serverUrl;
    private final Integer port;
    private final String protocol;
    private final String serverRoot;
    private final String apiRoot;
    private final String mqttTopicRoot;
    private final String username;
    private final String password;
    private boolean userAuth = false;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public APIHelper(String serverUrl, String protocol, Integer port, String serverRoot, String apiRoot,
                     String mqttTopicRoot, String username, String password)
    {
        this.serverUrl = serverUrl;
        this.protocol = protocol;
        this.port = port;
        this.serverRoot = serverRoot;
        this.apiRoot = apiRoot;
        this.mqttTopicRoot = normalizeMqttRoot(mqttTopicRoot);
        this.username = username;
        this.password = password;
    }

    /**
     * Normalize a configured MQTT topic root so it can be concatenated directly
     * with a resource path.
     *
     * The root is free-form: it may be a single segment ({@code "axis1"}), several
     * ({@code "site1/osh"}), or endpoint-prefixed with a leading slash
     * ({@code "/api"}) — which is the shape a node publishes on when its
     * Connected Systems API MQTT service has no nodeId configured. A leading
     * slash is significant and preserved; trailing slashes are not. Blank or null
     * yields null, so the caller falls back to the API root.
     */
    static String normalizeMqttRoot(String root)
    {
        if (root == null)
            return null;
        String r = root.trim();
        while (r.endsWith("/"))
            r = r.substring(0, r.length() - 1);
        return r.isEmpty() ? null : r;
    }

    /** Trim a path segment of surrounding slashes, falling back to {@code fallback} when blank. */
    static String normalizePathSegment(String value, String fallback)
    {
        if (value == null)
            return fallback;
        String v = value.trim();
        while (v.startsWith("/"))
            v = v.substring(1);
        while (v.endsWith("/"))
            v = v.substring(0, v.length() - 1);
        return v.isEmpty() ? fallback : v;
    }

    /**
     * Normalize the configured API root to the path <em>below</em> {@code serverRoot}.
     *
     * The config field is documented as "sensorhub/api" while the URL is built as
     * {@code /{serverRoot}/{apiRoot}}, so taking it literally would yield
     * {@code /sensorhub/sensorhub/api}. A redundant leading server-root segment is
     * therefore dropped, which makes both "api" and "sensorhub/api" mean the same thing.
     */
    static String normalizeApiRoot(String apiRoot, String serverRoot)
    {
        String a = normalizePathSegment(apiRoot, "api");
        if (serverRoot != null && !serverRoot.isEmpty() && a.equals(serverRoot))
            return "api";
        String prefix = serverRoot + "/";
        if (serverRoot != null && !serverRoot.isEmpty() && a.startsWith(prefix))
            a = a.substring(prefix.length());
        return a.isEmpty() ? "api" : a;
    }

    public void setUserAuth(boolean userAuth)
    {
        this.userAuth = userAuth;
    }

    /** Root used when a node does not configure one; matches the default nodeId of the CS API MQTT service. */
    public static final String DEFAULT_MQTT_ROOT = "api";

    /**
     * The MQTT topic root, which is independent of the HTTP API root.
     *
     * A node's MQTT topics are rooted at its Connected Systems API MQTT service
     * nodeId, and that nodeId has nothing to do with where the CS API is served
     * over HTTP — only a node published without a nodeId falls back to its HTTP
     * endpoint. The two default to the same string ("api"), which makes them look
     * coupled; they are not, so this deliberately does not derive one from the other.
     */
    public String getMqttRoot()
    {
        return mqttTopicRoot != null ? mqttTopicRoot : DEFAULT_MQTT_ROOT;
    }

    // ---- URL building -------------------------------------------------------

    public String getBaseUrl()
    {
        return protocol + "://" + serverUrl + (port != null ? ":" + port : "");
    }

    public String getApiRootUrl()
    {
        return getBaseUrl() + "/" + serverRoot + "/" + apiRoot;
    }

    private String resourceUrlResolver(APIResourceTypes subresourceType, String subresourceId, String resourceId)
    {
        if (subresourceType == null)
            throw new IllegalArgumentException("Resource type must contain a valid APIResourceType");

        APIResourceTypes parentType = null;
        if (resourceId != null)
            parentType = subresourceType.parentType();

        return constructUrl(parentType, subresourceId, subresourceType, resourceId);
    }

    private String constructUrl(APIResourceTypes resourceType, String subresourceId,
                                APIResourceTypes subresourceType, String resourceId)
    {
        String baseUrl = getApiRootUrl();
        String resourceEndpoint = subresourceType.term();
        String url = baseUrl + "/" + resourceEndpoint;

        if (resourceType != null)
            url = baseUrl + "/" + resourceType.term() + "/" + resourceId + "/" + resourceEndpoint;

        if (subresourceId != null)
            url = url + "/" + subresourceId;

        return url;
    }

    // ---- HTTP ---------------------------------------------------------------

    public ApiResponse createResource(APIResourceTypes resType, String jsonData, String parentResId,
                                      Map<String, String> reqHeaders)
    {
        String url = resourceUrlResolver(resType, null, parentResId);
        return post(url, jsonData, reqHeaders);
    }

    /** Binary-body variant, used to relay opaque swe+binary observation frames. */
    public ApiResponse createResource(APIResourceTypes resType, byte[] body, String parentResId,
                                      Map<String, String> reqHeaders)
    {
        String url = resourceUrlResolver(resType, null, parentResId);
        return post(url, body, reqHeaders);
    }

    /**
     * Mirror of {@code get_resource(resource_type, resource_id, subresource_type, params)}.
     */
    public ApiResponse getResource(APIResourceTypes resourceType, String resourceId,
                                   APIResourceTypes subresourceType, Map<String, String> params)
    {
        String resIdStr = resourceId != null ? "/" + resourceId : "";
        String subResTypeStr = subresourceType != null ? "/" + subresourceType.term() : "";
        String url = getApiRootUrl() + "/" + resourceType.term() + resIdStr + subResTypeStr;
        return get(url, params);
    }

    private ApiResponse get(String url, Map<String, String> params)
    {
        String fullUrl = url + queryString(params);
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(fullUrl)).GET();
        applyAuth(builder);
        return execute(builder);
    }

    private ApiResponse post(String url, String body, Map<String, String> headers)
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8));
        if (headers != null)
        {
            for (Map.Entry<String, String> h : headers.entrySet())
                builder.header(h.getKey(), h.getValue());
        }
        applyAuth(builder);
        return execute(builder);
    }

    private ApiResponse post(String url, byte[] body, Map<String, String> headers)
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body == null ? new byte[0] : body));
        if (headers != null)
        {
            for (Map.Entry<String, String> h : headers.entrySet())
                builder.header(h.getKey(), h.getValue());
        }
        applyAuth(builder);
        return execute(builder);
    }

    private ApiResponse execute(HttpRequest.Builder builder)
    {
        try
        {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void applyAuth(HttpRequest.Builder builder)
    {
        if (userAuth && username != null && password != null)
        {
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + encoded);
        }
    }

    private static String queryString(Map<String, String> params)
    {
        if (params == null || params.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet())
        {
            if (!first)
                sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    // ---- MQTT topic ---------------------------------------------------------

    /**
     * Mirror of {@code get_mqtt_topic(resource_type, subresource_type, resource_id,
     * subresource_id, data_topic)}.
     */
    public String getMqttTopic(APIResourceTypes resourceType, APIResourceTypes subresourceType,
                               String resourceId, String subresourceId, boolean dataTopic)
    {
        String dataSuffix = dataTopic ? ":data" : "";
        String subresourceEndpoint = "/" + subresourceType.term();
        String resourceEndpoint = resourceType == null ? "" : "/" + resourceType.term();
        String resourceIdent = resourceId == null ? "" : "/" + resourceId;
        String subresourceIdent = subresourceId == null ? "" : "/" + subresourceId;
        return getMqttRoot() + resourceEndpoint + resourceIdent + subresourceEndpoint + dataSuffix + subresourceIdent;
    }
}
