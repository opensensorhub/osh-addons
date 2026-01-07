# FFMPEG Driver

## Overview

This module provides a sensor driver that will parse a stream of MPEG video data.

## Configuration

When added to an OpenSensorHub node, the driver has the following configuration properties:

- **General:**
    - **Module ID:** *Not editable.*
      UUID automatically assigned by OpenSensorHub for this driver instance.
    - **Module Class:** *Not editable.*
      The fully qualified name of the Java class implementing the driver.
    - **Module Name:**
      A name for the instance of the driver.
      Should be set to something short and human-readable that describes the upstream source of data.
    - **SensorML URL:** (Optional)
      URL to a SensorML description document for the driver or physical device the driver represents.
    - **Video Stream ID:**
      A string that uniquely identifies this sensor in this OpenSensorHub node.
      This is used to differentiate between multiple sensors of the same type.
    - **Auto Start:**
      If checked, automatically start this sensor when the OpenSensorHub node is launched.

- **Connection:**
    - **Connection String:**
      A string that indicates the source of the MPEG stream.
      This is an FFmpeg connection string, and can take a number of forms.
      Common examples are URLs, e.g. `https://myserver.local/path/to/stream.m2ts`,
      or TCP endpoints, e.g. `tcp://192.168.1.200:1234`. See the
      [ffmpeg documentation for more details on the format of this string](https://www.ffmpeg.org/ffmpeg-protocols.html).
      <br>
      This may also be a file path, e.g. `c:/path/to/file.mkv`,
      in which case the file will be read from the local file system.
    - **Command Line Arguments:**
      A string that contains FFmpeg command line flags. Each flag should be separated by a "-" preceding the name
        of the flag.
      - [Full list of FFmpeg command line flags](https://gist.github.com/tayvano/6e2d456a9897f55025e25035478a3a50).
      - Common flags include:
        - -timeout [time until timeout]
          - Ex: "timeout 3000000"
        - -rtsp_transport [transport protocol]
          - udp, tcp, udp_multicast, http
          - Ex: "-rtsp_transport tcp" forces rtsp over tcp.
        - -f [format]
          - Force the input format for the video/stream.
          - Ex: "-f dshow" allows for the usage of video input over Windows DirectShow.
            - When using DirectShow, the Connection String will be of the format "video=[DirectShow device name]".
          - Run "ffmpeg -formats" in the command line for a complete list of formats.
    - **FPS:**
      When **Connection String** is a file,
      this indicates how fast the data is streamed.
      A value of `0` will stream as quickly as possible,
      and can be used to load historical data into the OpenSensorHub historical database quickly.
    - **Loop:**
      When **Connection String** is a file,
      this indicates that the data should be looped to create a continuous stream of data.
    - **Inject Extradata:**
      Only for H264. When checked, injects Annex B extradata into the video stream before every keyframe. 
      This is necessary for late-join decoders (those that receive live data from the driver after the driver has started).

- **Position:**
    - **Location:** (Optional)
      The latitude, longitude, and altitude of the sensor.
    - **Orientation:** (Optional)
      The heading, pitch, and roll of the sensor.