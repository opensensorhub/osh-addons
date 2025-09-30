import org.eclipse.paho.client.mqttv3.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.comm.IMessageQueuePush;

import javax.validation.constraints.AssertTrue;
import java.util.HashMap;
import java.util.Map;

import static com.botts.impl.comm.mqtt.MqttMessageQueue.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MqttTest {

    private MqttClient client;
    private static final String broker_url = "wss://mqtt.meshtastic.org:1883/mqtt";
    private static final String topic_name = "msh/US/2/json/#";

    MqttConnectOptions connectOptions = new MqttConnectOptions();


    @Before
    public void setUp() throws Exception {
        client = new MqttClient(broker_url, "osh-mqtt-test-id");
    }

    @Test
    public void testMqttConnection() throws MqttException {
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setUserName("meshdev");
        connectOptions.setPassword("large4cats".toCharArray());

        client.connect(connectOptions);

        assertTrue(client.isConnected());

        client.disconnect();
        assertTrue(!client.isConnected());
    }


    @Test
    public void testPublishing() throws MqttException {
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setUserName("meshdev");
        connectOptions.setPassword("large4cats".toCharArray());
        client.connect(connectOptions);

        MqttMessage message = new MqttMessage("".getBytes());

        client.publish(topic_name, message);
    }

    @Test
    public void testSubscribing() throws MqttException {
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setUserName("meshdev");
        connectOptions.setPassword("large4cats".toCharArray());

        client.connect(connectOptions);

        client.subscribe(topic_name, 0);
    }

    @After
    public void tearDown() throws Exception {
        client.disconnect();
        client.close();
    }
}