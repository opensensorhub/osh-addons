import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.joda.time.Instant;
import com.hivemq.client.internal.mqtt.message.publish.puback.MqttPubAck;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5MessageException;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;


public class TestMqttClient
{

    public static void main(String[] args) throws Exception
    {
        var client = MqttClient.builder()
            .identifier("test-client-001")
            //.serverHost("localhost")
            .serverHost("ogct17.georobotix.io")
            .useMqttVersion5()
            .buildAsync();
        
        client.connect().get();
        /*client.connectWith()
            .simpleAuth()
                .username("admin")
                .password("test".getBytes())
                .applySimpleAuth()
            .send()
            .exceptionally(TestMqttClient::printError)
            .get();*/
        
        /*// test subscribe to invalid topic
        client.subscribeWith()
            .topicFilter("/api/datastreams/fffffff/observations")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback(TestMqttClient::printPublish)
            .send()
            .exceptionally(TestMqttClient::printError);*/
        
        // test subscribe to SWE API topic
        client.subscribeWith()
            .topicFilter("/api/datastreams/18ic63yst5gtz/observations")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback(TestMqttClient::printPublish)
            .send()
            .exceptionally(TestMqttClient::printError);
        
        /*// test subscribe to STA topic
        client.subscribeWith()
            .topicFilter("/sta/v1.0/MultiDatastreams(10863282023507905)/Observations")
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback(TestMqttClient::printPublish)
            .send()
            .exceptionally(TestMqttClient::printError);
        
        Thread.sleep(3000);*/

        /*client.unsubscribeWith()
            .addTopicFilter("/api/datastreams/tfy230tgp7xu/observations")
            .send();*/
        
        /*// test publish to STA topic
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            var ts = Instant.now();
            client.publishWith()
                .topic("/sta/v1.0/Datastreams(101)/Observations")
                .qos(MqttQos.AT_LEAST_ONCE)            
                .payload(StandardCharsets.UTF_8.encode(new String("{\n"
                    + "  \"phenomenonTime\" : \"" + ts + "\",\n"
                    + "  \"resultTime\" : \"" + ts + "\",\n"
                    + "  \"result\" : 3\n"
                    + "}")))
                .send()
                .exceptionally(TestMqttClient::printError);
        }, 0, 500L, TimeUnit.MILLISECONDS);*/          
            
        /*// test publish to SWE API topic
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            var ts = Instant.now();
            client.publishWith()
                .topic("/api/datastreams/1wjbe2iv/observations")
                .contentType("")
                .qos(MqttQos.AT_LEAST_ONCE)            
                .payload(StandardCharsets.UTF_8.encode(new String("{\n"
                    + "  \"phenomenonTime\": \"" + ts + "\",\n"
                    + "  \"result\": {\n"
                    + "    \"temp\": " + (Math.random()*2.0-1+20.0) + ",\n"
                    + "    \"press\": " + (Math.random()*6.0-1+1015.0) + "\n"
                    + "  }\n"
                    + "}")))
                .send()
                .exceptionally(TestMqttClient::printError);
        }, 0, 500L, TimeUnit.MILLISECONDS);
        
        Thread.sleep(10000);*/
    }
    
    
    static void printPublish(Mqtt5Publish msg)
    {
        var txt = StandardCharsets.UTF_8.decode(msg.getPayload().get());
        System.out.println("\nMessage received on " + msg.getTopic() + ":\n" + txt);
    }
    
    
    static <T> T printError(Throwable e)
    {
        var ackMsg = ((Mqtt5MessageException)e).getMqttMessage();
        if (ackMsg instanceof MqttPubAck)
            System.out.println(ackMsg + ", " + ((MqttPubAck) ackMsg).getReasonCode());
        else
            System.out.println(ackMsg);
        return null;
    }

}
