#### MQTT Message Queue Comm Provider

The MQTT message queue comm provider implements the IMessageQueuePush interface and supports both publishing and subscribing to MQTT topics. 


Configuring the sensor requires:
Select ```Sensors``` from the left hand accordion control and right click for context sensitive menu in accordion control
- **Module Name:** A name for the instance of the driver
- **Serial Number:** The platforms serial number, or a unique identifier
- **Auto Start:** Check the box to start this module when OSH node is launched
- **Client ID:** Unique Identifier for the client used by the broker to track connections
- **Protocol:** Connection protocol (TCP, WS, WSS, SSL)
- **Broker Address:** Hostname or IP address & port of the MQTT broker (e.g., localhost:1883)
- **qos:** Quality of service (0,1,or 2)
- **Topic Name:** MQTT topic name for publish/subscribe

