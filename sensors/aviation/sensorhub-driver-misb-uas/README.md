# MISB UAS

## Overview

This module provides a pair of sensor drivers that will parse a stream of MPEG TS data containing MISB-0601 / STANAG-4609 compliant UAS video and telemetry. The two drivers are nearly identical in functionality, and differ only in when they begin reading data:

- `UasSensor`: This sensor will begin streaming data from the UAS immediately upon initialization. Even if there are no OpenSensorHub clients viewing the data, the driver will stream it from the source. While this may cause significant data transfer, it will allow the OpenSensorHub to receive (and potentially archive) the entire stream of data.
- `UasOnDemandSensor`: This sensor will only connect to the upstream source when it detects that there are OpenSensorHub clients listening for its data. This is appropriate for when OpenSensorHub is acting as a "proxy" for other sources of data, and we do not want to waste bandwidth streaming data that is not actively in use. (A side effect of this is that the OpenSensorHub data archives will only hold data for times when clients were watching.)

## Configuration

When added to an OpenSensorHub node, both of the drivers listed above have the following configuration properties:

- **General:**
  - **Module ID:** *Not editable.* UUID automatically assigned by OpenSensorHub for this driver instance.
  - **Module Class:** *Not editable.* The fully qualified name of the Java class implementing the driver.
  - **Module Name:** A name for the instance of the driver. Should be set to something short and human-readable that describes the upstream source of data.
  - **SensorML URL:** URL to a SensorML description document for the driver or physical device the driver represents. Typically this is left blank, and the sensor implementation will provide the details of the sensor.
  - **UAS ID:** Set this to a string that uniquely identifies this sensor in this OSH node. For physical UAS systems, it is typical to use the platform's serial number. For demonstration purposes, sequential values such as `uas001`, `uas002`, etc., can be used.
  - **Auto Start:** If checked, automatically start this sensor when the OSH node is launched.

- **Connection:**
  - **File Path:** Path to a file (on the OSH node's local file system) that contains an MPEG TS data stream. If set, the driver will only read data from this (historical) data file, and not stream from a "live" source. Only one of **File Path** and **Connection String** should be set. (Though if both are set, **File Path** is used.)
  - **Connection String:** A string that indicates the source of the "live" MPEG TS stream. As currently implemented, this is an ffmpeg connection string, and can take a number of forms. Common examples are URLs, e.g. `https://myserver.local/path/to/stream.m2ts`, or TCP endpoints, e.g. `tcp://192.168.1.200:1234`. See the [ffmpeg documentation for more details of the format of this string](https://www.ffmpeg.org/ffmpeg-protocols.html). Only one of **File Path** and **Connection String** should be set. (Though if both are set, **File Path** is used.)
  - **FPS:** When **File Path** is set, this indicates how fast the data is streamed. A value of `0` means "stream as quickly as possible", and can be used to load historical data into the OSH historical database quickly.
  - **Loop:** When checked and **File Path** is set, indicates that the data should be looped to create a continuous stream of data.

- **Outputs:** Each of the following causes a specific output (data stream) to be separately emitted by the sensor.
  - **Airframe Position:** If enabled, the sensor will have a separate output that provides lat, lon, alt, heading, pitch, and roll of the airframe.
  - **Geo Referenced Image Frame Data:** If enabled, the sensor will have a separate output that provides data corresponding to the corners (as lat, lon coordinates on the earth) of the image being transmitted.
  - **Gimbal Attitude:** If enabled, the sensor will have a separate output that provides yaw, pitch, and roll of the sensor gimbal.
  - **Sensor Parameters:** If enabled, the sensor will have a separate output that provides the imaging sensor's parameters, such as FOV.
  - **Identification:** If enabled, the sensor will have a separate output that provides airframe identification information.
  - **Security:** If enabled, the sensor will have a separate output that provides security classification markings for data.
  - **Target Indicators:** If enabled, the sensor will have a separate output that provides VMTI data. (Note that the source stream may not contain this data, so this output may never emit any observations.)
  - **Video:** If enabled, the sensor will have a separate output that provides the video feed from the camera sensor.
  - **All Combined:** If enabled, the sensor will have an additional (huge) output that combines all data received through MPEG-TS STANAG 4609 ST 0601.16 UAS Datalink Local Set MetaData.

In addition to all of the above configuration, the `UasOnDemandSensor` also has the following configuration:

- **Video:** Provides details about the video frame that the sensor cannot know ahead of time (since it does not read the data stream until interested parties subscribe for its data).
  - **Video Width:** The pixel width of video frames.
  - **Video Height:** The pixel height of video frames.
