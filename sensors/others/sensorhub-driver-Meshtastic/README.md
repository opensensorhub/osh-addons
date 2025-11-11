# Meshtastic Driver over Serial Communication
## Dependencies
To work properly, this driver requries a comm module. The driver was tested on a mac using the [sensorhub-comm-jssc](https://github.com/opensensorhub/osh-addons/tree/master/comm/sensorhub-comm-jssc):


## Meshtastic Node Configuration
Prior to using the OSH Meshtastic Driver with a Meshtastic Radio, the radio must be configured with the [Meshtastic Firmware](https://flasher.meshtastic.org/). 
Follow Meshtastic [Getting Started Documentation](https://meshtastic.org/docs/getting-started/) to properly set up a Meshtastic Radio. 

Once the radio has been flashed with Meshtastic Firmware, [connect to the device](https://meshtastic.org/docs/getting-started/initial-config/) to setup it's initial configuration.

To use this Driver over Serial, you must configure the following Meshtastic Settings:
- Settings/Serial
    - Make sure ```Enabled``` is **ON**
    - Make sure ```Mode``` is set to ***Protobufs***


Once this has been configured, you will need to exit the application and plug your radio directly into the computer running your OSH Node. 

# OSH Node Configuration
Once your node has been launched. Make sure a ***Communication Provider*** has been selected. (This has been tested
using the JSSC Serial Comm Provider) and all necessary information has been selected. 

Select ```Apply Changes``` and then ```Start``` the driver. As Meshtastic Protobuf messages are received from your meshtastic radio, 
these messages will appear in the Output of the drier. 

# Text Message Control
Additionally, a Text Message Control has been added to this driver. If you want to send a direct message, type the node number of
the node you want to send a message to. If you want to broadcast a message, type 'broadcast' or 'Broadcast' and selct send command.




# Meshtastic Driver (Serial)
This driver enables an OSH Node to communicate with a Meshtastic radio over a serial connection using Protobuf messages.

## Dependencies
This driver requires a serial communication module. It has been tested using [sensorhub-comm-jssc](https://github.com/opensensorhub/osh-addons/tree/master/comm/sensorhub-comm-jssc)

Make sure this communication module is included in your OSH environment before running the driver.

## Meshtastic Radio Setup
1. Follow the [Meshtastic Getting Started](https://meshtastic.org/docs/getting-started/) guide for initial configuration:

2. Flash your radio with the official [Meshtastic Firmware](https://flasher.meshtastic.org/) if not done in step 1:

3. After flashing and initial setup, open the [Meshtastic app](https://meshtastic.org/docs/getting-started/initial-config/) and configure the radio’s Serial settings:
- Settings → Serial
    - Enabled: ON 
    - Mode: Protobufs

4. Close the Meshtastic app and connect the radio directly to the computer running the OSH Node. ***Note the Port Address***

Note: If another application (like the Meshtastic GUI or CLI) is open and using the serial port, the OSH Node will not be able to connect.

## OSH Node Configuration
1. Launch your OSH Node.

2. Select and configure the ***Communication Provider***.
(This driver has been verified using JSSC Serial Comm Provider)

3. Apply the communication settings.

4. Start the driver. 

As Protobuf messages are received from the Meshtastic radio, sensor outputs will appear in the driver’s output stream.

## Sending Text Messages

This driver includes support for sending text messages through the Meshtastic network.

### To send a message:

- To message a specific node: enter the Node ID of the destination.
- To broadcast a message: enter broadcast (case-insensitive).

Then type your message and select ```Send Command```.


