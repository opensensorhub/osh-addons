package org.sensorhub.impl.service.federation.oshconnect;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Port of oshconnect.streamableresource.ControlStream for the surface the broker
 * uses: PULL-mode MQTT subscribe to the command topic into an inbound deque.
 */
public class ControlStream
{
    private final Node parentNode;
    private ControlStreamResource underlyingResource;
    private final String resourceId;
    private String parentResourceId;

    private final Deque<byte[]> inboundDeque = new ConcurrentLinkedDeque<>();
    private final MqttCommClient mqttClient;
    private String topic;
    private String statusTopic;
    private StreamableModes connectionMode = StreamableModes.PUSH;

    public ControlStream(Node node, ControlStreamResource controlstreamResource)
    {
        this.parentNode = node;
        this.mqttClient = node != null ? node.getMqttClient() : null;
        this.underlyingResource = controlstreamResource;
        this.resourceId = controlstreamResource.getCsId();
        // Always set after the resource ids are set.
        this.statusTopic = getMqttStatusTopic();
    }

    public String getId()
    {
        return underlyingResource.getCsId();
    }

    public ControlStreamResource getUnderlyingResource()
    {
        return underlyingResource;
    }

    public Node getParentNode()
    {
        return parentNode;
    }

    public void setConnectionMode(StreamableModes connectionMode)
    {
        this.connectionMode = connectionMode;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public void setParentResourceId(String resId)
    {
        this.parentResourceId = resId;
    }

    public void initialize()
    {
        initMqtt();
    }

    /** Set the command data topic. */
    public void initMqtt()
    {
        this.topic = parentNode.getApiHelper().getMqttTopic(
                APIResourceTypes.CONTROL_CHANNEL, APIResourceTypes.COMMAND, resourceId, null, true);
    }

    /** MQTT topic for command status updates. */
    public String getMqttStatusTopic()
    {
        return parentNode.getApiHelper().getMqttTopic(
                APIResourceTypes.CONTROL_CHANNEL, APIResourceTypes.STATUS, resourceId, null, true);
    }

    public void start()
    {
        if (mqttClient != null)
        {
            if (connectionMode == StreamableModes.PULL || connectionMode == StreamableModes.BIDIRECTIONAL)
                mqttClient.subscribe(topic, 0, this::mqttSubCallback);
        }
    }

    /** Default callback: append the raw payload to the inbound deque. */
    protected void mqttSubCallback(String msgTopic, byte[] payload)
    {
        inboundDeque.add(payload);
    }

    public Deque<byte[]> getInboundDeque()
    {
        return inboundDeque;
    }
}
