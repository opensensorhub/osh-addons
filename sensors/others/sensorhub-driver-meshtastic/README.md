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

# Developer Notes

The Meshtastic Java classes (MeshProtos) are automatically generated from the Meshtastic protobuf definitions at build time using 
the [Meshtastic protobufs repo](https://github.com/meshtastic/protobufs).

When you first clone this repository, the generated classes do not exist in the source tree. To compile and access them in your code, run:

```./gradlew build -x test -x osgi```


This will:

1. Generate Java classes from the protobuf definitions in src/main/generated-sources.

2. Compile them into the project’s classpath so your IDE can recognize imports like org.meshtastic.proto.MeshProtos.

These generated sources are not committed to Git, so you always need to build the project before using or editing code that depends on them.


