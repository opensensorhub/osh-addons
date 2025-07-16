# ONVIF Camera Driver

This is a driver for sending PTZ commands to and streaming video/audio from ONVIF-compatible IP cameras.

## Features

- Auto-discovery of ONVIF devices and stream endpoints
- Manual configuration of connection settings
- Codec selection for video streaming
- PTZ commands and presets
<br />

---

## Configuration
<br />

### ONVIF Connection Options

Configure the connection parameters used to communicate with the ONVIF device.

| Field                               | Description                                     | Default                                |
|-------------------------------------|-------------------------------------------------|----------------------------------------|
| `Remote Port`                       | Remote port to connect to                       | `80`                                   |
| `User`                              | Username for ONVIF authentication               | `""`                                   |
| `Password`                          | Password for ONVIF authentication               | `""`                                   |
| `Discovered ONVIF Device Endpoints` | Automatically discovered ONVIF device endpoints | _Populated at runtime. Do not modify._ |
| `ONVIF Path`                        | Path to ONVIF device service                    | `"/onvif/device_service"`              |

<br />

### AV Streaming Options

Control the streaming behavior and media preferences.

| Field                         | Description                                                   | Default                                |
|-------------------------------|---------------------------------------------------------------|----------------------------------------|
| `Manual Stream Endpoint`      | Manually override stream URI (leave empty for auto-discovery) | `""`                                   |
| `Discovered Stream Endpoints` | Automatically discovered stream endpoints                     | _Populated at runtime. Do not modify._ |
| `Preferred Codec`             | Preferred video codec (`JPEG`, `MPEG4`, `H264`)               | `JPEG`                                 |

<br />

---

## Commands
<br />

| Command        | Description                                                               |
|----------------|---------------------------------------------------------------------------|
| `pan`          | Set the absolute pan position of the camera.                              |
| `tilt`         | Set the absolute tilt position of the camera.                             |
| `zoom`         | Set the absolute zoom level of the camera.                                |
| `rpan`         | Apply a relative pan movement from the current position.                  |
| `rtilt`        | Apply a relative tilt movement from the current position.                 |
| `rzoom`        | Apply a relative zoom adjustment from the current zoom level.             |
| `preset`       | Move the camera to a preset position identified by a token.               |
| `presetAdd`    | Create and store a new preset at the current camera position.             |
| `presetRemove` | Remove an existing preset identified by its token.                        |
| `ptzPos`       | Set absolute pan, tilt, and zoom simultaneously.                          |
| `ptzCont`      | Start continuous pan, tilt, and zoom movement with given velocity values. |

