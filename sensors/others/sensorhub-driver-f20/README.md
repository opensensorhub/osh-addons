# F20 Platform

Sensor adapter for the F20 platform, otherwise known as the [Wintec WW-5H2x](https://www.win-tec.com.tw/portfolio-item/ww-5h2x/).

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left hand accordion control and right click for context sensitive menu in accordion control
- **Module Name:** A name for the instance of the driver
- **Serial Number:** The platforms serial number, or a unique identifier
- **Auto Start:** Check the box to start this module when OSH node is launched
- **MQTT Broker:** MQTT broker to communicate with F20 device
- **Topic ID:** MQTT topic ID of F20 device
- **Username:** Username to authenticate with MQTT broker
- **Password:** Password to authenticate with MQTT broker