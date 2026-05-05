# FFmpeg RTMP Driver

OSH sensor driver using FFmpeg to listen to RTMP streams.

This driver depends on the following modules at runtime:
* sensorhub-driver-ffmpeg

## Config

### Connection

* **Host**
  - RTMP server host 
    - **UNSPECIFIED**: listen for remote connections.
    - **LOCALHOST**: listen for local connections.
    - **DOCKER_INTERNAL**: listen for connections inside a docker container.
* **Port**
  - RTMP server port
    - 1935 is the default port for RTMP.
    - Any port may be used as long as it is not in use by another listener (or any other application).
      - RTMP sensor driver modules track ports in use. To release ownership of a port, STOP the module.

## Usage

### Initialization

<p>To initialize the driver, provide a unique serial number in the configuration.
In connection, set the host and provide an unused port. Once initialized, other RTMP sensor driver modules cannot
use the same port. 
<br><br><strong>Stop</strong> the module to free the port.</p>

### Listening

<p>Once initialized, start the module to begin listening for RTMP connections. <strong>After</strong> the driver
is started, begin publishing the RTMP stream. 
<br><br><strong>Note</strong>: The module does NOT attempt to validate the 
username, password, RTMP app, or RTMP playpath. Only the host and port need to match.</p>

