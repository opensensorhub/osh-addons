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

    @DisplayInfo(label = "MQTT Node ID", desc = "The nodeId configured on this node's Connected Systems "
            + "API MQTT service. It is the root segment of every MQTT topic that node publishes "
            + "(e.g. a nodeId of 'rmt-axis' gives 'rmt-axis/datastreams/{id}/observations:data'), so it "
            + "must match that service's nodeId exactly or no observations or commands will arrive. "
            + "Nodes sharing one broker must each use a distinct value.")
    public String mqttNodeId = "api";
}
