# Meshtastic Driver Using MQTT
## How to make this work in OpenSensorHub

### 1. Make sure to include the mqtt-hivemq service and the mqtt comm provider in your project
```
osh-addons/services/sensorhub-service-mqtt-hivemq
osh-addons/services/sensorhub-comm-mqtt
```

### 2. Update the config.xml or config.tls.xml file for the mqtt hivemq service to allow wildcard-subscriptions:
```xml
//Directory Location:
//osh-addons/services/sensorhub-service-mqtt-hivemq/src/main/resources/org/sensorhub/impl/service/hivemq
//Update this xml:

<wildcard-subscriptions>
    <enabled>true</enabled>
</wildcard-subscriptions>
```




- update OshAuthorizers to 
```java
@Override
    public void authorizePublish(PublishAuthorizerInput authInput, PublishAuthorizerOutput authOutput)
    {
        //publishHandler.authorizePublish(authInput, authOutput);
        String topic = authInput.getPublishPacket().getTopic();

        if("/will".equals(topic)){
            authOutput.authorizeSuccessfully();
            return;
        }
    }
```