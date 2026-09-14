package org.sensorhub.impl.service.federation.environment;

import org.sensorhub.api.config.DisplayInfo;

/**
 * Port of environment.NodeEnvData. Also serves as an admin-UI config object:
 * one entry of the {@code nodes} array that lived in broker-env2.json.
 */
public class NodeEnvData
{
    @DisplayInfo(label = "Name", desc = "Display name of the node")
    public String name;

    @DisplayInfo(label = "Protocol", desc = "Connection protocol (http or https)")
    public String protocol = "http";

    @DisplayInfo(label = "Address", desc = "Hostname or IP address of the node")
    public String address;

    @DisplayInfo(label = "Port", desc = "HTTP API port of the node")
    public int port;

    @DisplayInfo(label = "Is Commander", desc = "True if this node is the central commander node")
    public boolean isCommander = false;

    @DisplayInfo(label = "SensorHub Root", desc = "Root path segment of the node (e.g. sensorhub)")
    public String sensorhubRoot = "sensorhub";

    @DisplayInfo(label = "API Root", desc = "API root path of the node (e.g. sensorhub/api)")
    public String apiRoot = "sensorhub/api";

    @DisplayInfo(label = "Auth", desc = "Authentication credentials for the node")
    public AuthData auth = new AuthData();

    @DisplayInfo(label = "MQTT Port", desc = "MQTT port of the node")
    public int mqttPort = 1883;

    @DisplayInfo(label = "MQTT Topic Root", desc = "Root prefix of every MQTT topic this node "
            + "publishes, which must match how that node is configured or no observations or "
            + "commands will arrive. When the node's Connected Systems API MQTT service has a "
            + "nodeId, this is that nodeId (e.g. 'rmt-axis' -> "
            + "'rmt-axis/datastreams/{id}/observations:data'); when it has none, the node "
            + "publishes under its API endpoint instead, so use a leading slash (e.g. '/api'). "
            + "Multi-segment roots such as 'site1/osh' are allowed. Nodes sharing one broker "
            + "must each use a distinct value.")
    public String mqttTopicRoot = "api";
}
