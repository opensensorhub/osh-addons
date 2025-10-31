import com.botts.impl.comm.mqtt.MqttMessageQueue;
import com.botts.impl.comm.mqtt.MqttMessageQueueConfig;
import org.eclipse.paho.client.mqttv3.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.comm.IMessageQueuePush;
import org.sensorhub.api.common.SensorHubException;

import javax.net.ssl.SSLSocketFactory;
import javax.validation.constraints.AssertTrue;
import java.util.HashMap;
import java.util.Map;

import static com.botts.impl.comm.mqtt.MqttMessageQueue.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MqttTest {

    MqttMessageQueue messageQueue = new MqttMessageQueue();


    public MqttMessageQueueConfig getMqttMessageQueueConfig() {
        MqttMessageQueueConfig config = new MqttMessageQueueConfig();
        config.protocol = MqttMessageQueueConfig.Protocol.TCP;
        config.brokerAddress = "mqtt.meshtastic.org:1883";
        config.clientId = "osh-test";
        config.cleanSession = true;
        config.keepAlive = 60;
        config.connectionTimeout = 1000;
        config.isAutoReconnect = true;
        config.username = "meshdev";
        config.password = "large4cats";
        config.topicName = "msh/US/2/json/";

        return config;
    }
    @Test
    public void testPublishToMessageQueue() throws SensorHubException {

        var config = getMqttMessageQueueConfig();

        messageQueue.init(config);
        messageQueue.start();


        var payload = new byte[0];
        messageQueue.publish(payload);

    }

    @Test
    public void testPublish2() throws SensorHubException {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("qos", "0");
        attributes.put("retained", "true");

        var config = getMqttMessageQueueConfig();

        messageQueue.init(config);
        messageQueue.start();
        var payload = new byte[0];

        messageQueue.publish(attributes, payload);
    }


}