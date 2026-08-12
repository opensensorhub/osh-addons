package org.sensorhub.impl.service.federation.oshconnect;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of oshconnect.csapi4py.mqtt.MQTTCommClient. Wraps a paho MQTT client to
 * provide a simple subscribe/publish interface against a remote node's broker.
 *
 * paho-mqttv3 dispatches all messages through a single {@link MqttCallback};
 * the per-topic callback registration of the Python client
 * ({@code message_callback_add}) is reproduced with a topic to callback map.
 */
public class MqttCommClient
{
    private static final Logger logger = LoggerFactory.getLogger(MqttCommClient.class);

    /** Message callback shape: {@code callback(client, userdata, msg)} -> (topic, payload). */
    public interface MsgCallback
    {
        void onMessage(String topic, byte[] payload);
    }

    private final String url;
    private final int port;
    private final MqttClient client;
    private final Map<String, MsgCallback> callbacks = new ConcurrentHashMap<>();
    private volatile boolean isConnected = false;

    public MqttCommClient(String url, int port, String username, String password, String clientIdSuffix)
    {
        this.url = url;
        this.port = port;
        String clientId = "oscapy_mqtt-" + clientIdSuffix;
        try
        {
            this.client = new MqttClient("tcp://" + url + ":" + port, clientId, new MemoryPersistence());
        }
        catch (MqttException e)
        {
            throw new RuntimeException("Failed to create MQTT client", e);
        }

        this.connectOptions = new MqttConnectOptions();
        if (username != null && password != null)
        {
            connectOptions.setUserName(username);
            connectOptions.setPassword(password.toCharArray());
        }
        connectOptions.setAutomaticReconnect(true);
        connectOptions.setCleanSession(true);

        this.client.setCallback(new MqttCallback()
        {
            @Override
            public void connectionLost(Throwable cause)
            {
                isConnected = false;
                logger.warn("MQTT unexpected disconnect from {}:{} — will attempt reconnect", url, port);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message)
            {
                MsgCallback cb = callbacks.get(topic);
                if (cb != null)
                    cb.onMessage(topic, message.getPayload());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token)
            {
            }
        });
    }

    private final MqttConnectOptions connectOptions;

    /** Attempts for the initial connect, and the delay between them. */
    private static final int CONNECT_ATTEMPTS = 15;
    private static final long CONNECT_RETRY_MS = 1000;

    /**
     * Connect, retrying on failure.
     *
     * The retry is not optional: on the commander this races that node's own
     * in-process MQTT server, which finishes starting a fraction of a second
     * later. {@code setAutomaticReconnect(true)} does not cover it — paho only
     * applies automatic reconnect after one successful connect, so a broker
     * that loses this race never connects at all.
     */
    public void connect()
    {
        logger.info("MQTT connecting to {}:{}", url, port);

        MqttException lastError = null;
        for (int attempt = 1; attempt <= CONNECT_ATTEMPTS; attempt++)
        {
            try
            {
                client.connect(connectOptions);
                isConnected = true;
                logger.info("MQTT connected to {}:{}", url, port);
                return;
            }
            catch (MqttException e)
            {
                lastError = e;
                logger.debug("MQTT connect attempt {}/{} to {}:{} failed: {}",
                        attempt, CONNECT_ATTEMPTS, url, port, e.getMessage());
                if (attempt < CONNECT_ATTEMPTS)
                {
                    try
                    {
                        Thread.sleep(CONNECT_RETRY_MS);
                    }
                    catch (InterruptedException ie)
                    {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while connecting to MQTT", ie);
                    }
                }
            }
        }

        logger.error("MQTT connect to {}:{} failed after {} attempts: {}",
                url, port, CONNECT_ATTEMPTS, lastError == null ? "unknown" : lastError.getMessage());
        throw new RuntimeException(lastError);
    }

    public void subscribe(String topic, int qos, MsgCallback msgCallback)
    {
        if (!isConnected)
            logger.warn("MQTT subscribe called on {} while not connected", topic);
        try
        {
            client.subscribe(topic, qos);
            if (msgCallback != null)
            {
                // One callback per topic: a second subscriber to the same topic
                // replaces the first, which then silently stops receiving.
                if (callbacks.put(topic, msgCallback) != null)
                    logger.warn("MQTT topic {} already had a subscriber; it has been replaced "
                            + "and will no longer receive messages", topic);
            }
            logger.debug("MQTT subscribed to topic: {} (qos={})", topic, qos);
        }
        catch (MqttException e)
        {
            logger.error("MQTT subscribe error on {}: {}", topic, e.getMessage());
        }
    }

    public void publish(String topic, byte[] payload, int qos, boolean retain)
    {
        if (!isConnected)
            logger.warn("MQTT publish called on {} while not connected", topic);
        try
        {
            MqttMessage message = new MqttMessage(payload == null ? new byte[0] : payload);
            message.setQos(qos);
            message.setRetained(retain);
            client.publish(topic, message);
        }
        catch (MqttException e)
        {
            logger.error("MQTT publish error on {}: {}", topic, e.getMessage());
        }
    }

    public void publish(String topic, String payload, int qos, boolean retain)
    {
        publish(topic, payload == null ? null : payload.getBytes(StandardCharsets.UTF_8), qos, retain);
    }

    public void unsubscribe(String topic)
    {
        try
        {
            client.unsubscribe(topic);
            callbacks.remove(topic);
        }
        catch (MqttException e)
        {
            logger.error("MQTT unsubscribe error on {}: {}", topic, e.getMessage());
        }
    }

    public void disconnect()
    {
        try
        {
            client.disconnect();
        }
        catch (MqttException e)
        {
            logger.debug("MQTT disconnect error: {}", e.getMessage());
        }
    }

    /**
     * paho runs its network loop on its own background thread once connected,
     * so this is a no-op kept for parity with the Python client's start().
     */
    public void start()
    {
    }

    public void stop()
    {
        disconnect();
    }

    public boolean isConnected()
    {
        return isConnected;
    }
}
