package org.sensorhub.impl.service.federation.oshconnect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Port of oshconnect.streamableresource.Node for the surface the broker uses:
 * construction with HTTP + optional MQTT, system discovery, and system insert.
 */
public class Node
{
    private final String id;
    private final String protocol;
    private final String address;
    private final int port;
    private final String serverRoot;
    private final boolean isSecure;
    private int mqttPort = 1883;

    private final APIHelper apiHelper;
    private MqttCommClient mqttClient;
    private final List<System> systems = new ArrayList<>();

    public Node(String protocol, String address, int port, String username, String password,
                boolean enableMqtt, int mqttPort)
    {
        this(protocol, address, port, username, password, enableMqtt, mqttPort, null);
    }

    /**
     * @param mqttTopicRoot root prefix of this node's MQTT topics. Free-form: a
     *        single segment (its Connected Systems API MQTT service nodeId),
     *        several segments, or an endpoint-prefixed root with a leading slash
     *        for a node publishing without a nodeId. Null or blank falls back to
     *        the API root ("api").
     */
    public Node(String protocol, String address, int port, String username, String password,
                boolean enableMqtt, int mqttPort, String mqttTopicRoot)
    {
        this(protocol, address, port, username, password, enableMqtt, mqttPort, mqttTopicRoot, null, null);
    }

    /**
     * @param mqttTopicRoot root prefix of this node's MQTT topics (see above).
     * @param sensorhubRoot first path segment of the node's HTTP URLs, default
     *        "sensorhub". Independent of {@code mqttTopicRoot}.
     * @param apiRoot path of the Connected Systems API below {@code sensorhubRoot},
     *        default "api". Independent of {@code mqttTopicRoot}: a node's MQTT
     *        topics are rooted at its MQTT service nodeId, not at where the API is
     *        served over HTTP.
     */
    public Node(String protocol, String address, int port, String username, String password,
                boolean enableMqtt, int mqttPort, String mqttTopicRoot,
                String sensorhubRoot, String apiRoot)
    {
        this.id = "node-" + UUID.randomUUID();
        this.protocol = protocol;
        this.address = address;
        this.port = port;
        this.isSecure = username != null && password != null;
        this.serverRoot = APIHelper.normalizePathSegment(sensorhubRoot, "sensorhub");

        this.apiHelper = new APIHelper(address, protocol, port, this.serverRoot,
                APIHelper.normalizeApiRoot(apiRoot, this.serverRoot),
                mqttTopicRoot, username, password);
        if (isSecure)
            apiHelper.setUserAuth(true);

        if (enableMqtt)
        {
            this.mqttPort = mqttPort;
            this.mqttClient = new MqttCommClient(address, this.mqttPort, username, password,
                    UUID.randomUUID().toString().replace("-", ""));
            this.mqttClient.connect();
            this.mqttClient.start();
        }
    }

    public String getId()
    {
        return id;
    }

    public String getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    public int getMqttPort()
    {
        return mqttPort;
    }

    public APIHelper getApiHelper()
    {
        return apiHelper;
    }

    public MqttCommClient getMqttClient()
    {
        return mqttClient;
    }

    /**
     * GET {@code /systems?f=application/sml+json} and create a {@link System}
     * for each entry. New systems are appended to this node's list and returned;
     * {@code null} if the HTTP request failed.
     */
    public List<System> discoverSystems()
    {
        ApiResponse result = apiHelper.getResource(APIResourceTypes.SYSTEM, null, null,
                Map.of("f", "application/sml+json"));
        if (result.ok())
        {
            List<System> newSystems = new ArrayList<>();
            JsonArray systemObjs = result.json().getAsJsonObject().getAsJsonArray("items");

            // Replace rather than append: discoverSystems() is called several times
            // per run, and appending left the list holding a stale duplicate of every
            // system for each earlier call. Cleared only once the fetch has succeeded.
            this.systems.clear();

            for (JsonElement systemJson : systemObjs)
            {
                SystemResource system = new SystemResource(systemJson.getAsJsonObject());
                System sysObj = System.fromResource(system, this);
                this.systems.add(sysObj);
                newSystems.add(sysObj);
            }
            return newSystems;
        }
        return null;
    }

    public List<System> systems()
    {
        return systems;
    }

    public System addSystem(System system, boolean insertResource)
    {
        if (insertResource)
            system.insertSelf();
        system.setParentNode(this);
        this.systems.add(system);
        return system;
    }
}
