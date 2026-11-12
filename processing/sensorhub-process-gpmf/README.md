# GPMF Telemetry Processing

Decodes GoPro GPMF telemetry packets. One packet in, one telemetry record per sensor stream and one
position record per GPS fix out.

GPMF is the metadata format GoPro cameras record on a `gpmd` data track alongside the video: accelerometer,
gyroscope, GPS, camera state, and whatever else the model records. Which streams appear depends on the
camera model, its firmware, and the capture settings, so the telemetry output is self-describing rather
than shaped around a fixed set of sensors — every record names its stream and units as the camera reported
them, and carries that stream's samples with the camera's own scale divisors already applied.

## Process

`urn:osh:process:gpmf:GpmfDecoder` — `org.sensorhub.impl.process.gpmf.GpmfProcess`

### Input

| Name | Type | Description |
| --- | --- | --- |
| `gpmfPacket` | variable-size array of bytes | One undecoded GPMF packet, exactly as it was demuxed |

### Parameters

| Name | Type | Default | Description |
| --- | --- | --- | --- |
| `requireGpsFix` | boolean | `true` | Only publish position records the receiver had a lock for. A GoPro reports coordinates even with no fix, and those coordinates are meaningless, so clearing this will scatter the track with bad positions. The telemetry output always reports the GPS streams as recorded either way. |
| `altitudeDatum` | text (`MEAN_SEA_LEVEL`, `WGS84_ELLIPSOID`) | `MEAN_SEA_LEVEL` | Reference surface the camera measures altitude from, used only when the camera does not declare it. The HERO8, MAX, and newer declare it in the telemetry; the HERO7 and earlier do not and report height above the WGS84 ellipsoid. The process publishes the camera's readings unchanged and uses this only to describe them correctly. |

### Outputs

`gpmf` — one record per sensor stream in a packet:

| Field | Description |
| --- | --- |
| `sampleTime` | Time of data collection |
| `streamTime` | Offset of these samples from the start of the recording, as reported by the camera |
| `deviceName` | Device that recorded the stream, for example Hero11 Black |
| `streamKey` | Four-character GPMF key, for example ACCL, GYRO, or GPS5 |
| `streamName` | Stream name as the camera reported it |
| `units` | Units as the camera reported them |
| `elementCount` | Values in one sample, for example 3 for a three-axis sensor |
| `sampleCount` | Samples in this record |
| `textValue` | Content of a text-valued stream, empty for numeric streams |
| `valueCount` | Total values in this record |
| `values` | The samples in order, variable-size |

`gps` — one record per GPS fix: `sampleTime`, `gpsTime`, `location` (lat/lon/alt vector), `speed2D`,
`speed3D`, `fixType`, `dop`.

## Feeding it

Any source of demuxed GPMF packets works. `src/test/resources/org/sensorhub/impl/process/gpmf/processchain-gpmf.xml`
is a ready-to-run chain that reads the FFmpeg driver's binary data output and republishes both decoder
outputs as datastreams:

```xml
<sml:source ref="components/source0/outputs/data/data"/>
<sml:destination ref="components/gpmfDecoder/inputs/gpmfPacket"/>
```

Note that the link is to `outputs/data/data` — the byte array inside the driver's record, not the record
that wraps it. Point `parameters/producerURI` at your own camera.

The chain declares `gpmf` and `gps` in its `<sml:outputs>` and links the decoder's outputs to them, which
is what makes the records available as OpenSensorHub datastreams; a chain without those declarations keeps
the records inside the chain. `GpmfProcessChainTest` checks the declarations against the process, so a
change to either output structure fails the build rather than leaving the two silently out of step.

A GoPro MP4 also carries `tmcd` (timecode) and `fdsc` (camera info) data tracks. Feeding one of those here
produces nothing but a warning per packet, so point the chain at the `gpmd` track.
