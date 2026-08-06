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
    private final String serverRoot = "sensorhub";
    private final boolean isSecure;
    private int mqttPort = 1883;

    private final APIHelper apiHelper;
    private MqttCommClient mqttClient;
    private final List<System> systems = new ArrayList<>();

    public Node(String protocol, String address, int port, String username, String password,
                boolean enableMqtt, int mqttPort)
    {
        this.id = "node-" + UUID.randomUUID();
        this.protocol = protocol;
        this.address = address;
        this.port = port;
        this.isSecure = username != null && password != null;

        // Node uses the library defaults server_root='sensorhub', api_root='api'.
        this.apiHelper = new APIHelper(address, protocol, port, serverRoot, "api", null, username, password);
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
