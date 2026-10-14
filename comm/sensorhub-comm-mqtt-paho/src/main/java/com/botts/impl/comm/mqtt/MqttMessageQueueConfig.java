package com.botts.impl.comm.mqtt;

import org.sensorhub.api.comm.MessageQueueConfig;
import org.sensorhub.api.config.DisplayInfo;

public class MqttMessageQueueConfig extends MessageQueueConfig {

    @DisplayInfo.Required
    @DisplayInfo(label="Client", desc="A unique identifier for client, used by the broker to track connections")
    public String clientId;

    @DisplayInfo.Required
    @DisplayInfo(label="Protocol", desc="")
    public Protocol protocol = Protocol.TCP;

    @DisplayInfo.Required
    @DisplayInfo(label="Broker Address", desc="The hostname or IP address and port of the MQTT Broker (e.g. localhost:8282/sensorhub/mqtt")
    public String brokerAddress;

    @DisplayInfo.Required
    @DisplayInfo(label="Quality of Service", desc="Determines the reliability of the message delivery (0,1,2)")
    public QoS qos;

    @DisplayInfo(label="Username", desc="An optional username if needed for connecting to MQTT Broker")
    public String username;

    @DisplayInfo(label="Password", desc="An optional password if needed for connecting to MQTT Broker")
    public String password;

    @DisplayInfo(label="Retain", desc="Check to allow MQTT broker to store the last message sent on the specific topic")
    public boolean retain;

    @DisplayInfo(label="Clean Session", desc="Check ")
    public boolean cleanSession;

    @DisplayInfo(label="Keep Alive Interval", desc="")
    public int keepAlive = 60;

    @DisplayInfo(label="Connection Timeout", desc="")
    public int connectionTimeout = 10;

    @DisplayInfo(label="Automatic Reconnect", desc="")
    public boolean isAutoReconnect = true;

    public enum QoS {
        AT_MOST_ONCE(0),
        AT_LEAST_ONCE(1),
        EXACTLY_ONCE(2);

        final int value;
        QoS(int value){ this.value = value; }
        public int getValue(){ return value; }
    }

    public enum Protocol {
        WS("ws"),
        WSS("wss"),
        TCP("tcp"),
        SSL("ssl");

        final String protocol;
        Protocol(String protocol) { this.protocol = protocol; }
        public String getName() { return protocol; }
    }

}

