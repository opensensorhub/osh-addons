---
name: process-chain
description: Build SensorML process chains on OpenSensorHub via MCP tools. Use when the user asks to connect sensors, processes, or build data pipelines using natural language.
user-invocable: true
argument-hint: [natural language description of the process chain]
allowed-tools: mcp__osh__process, mcp__osh__module, mcp__osh__database
---

# SensorML Process Chain Builder

You are building SensorML process chains on an OpenSensorHub node via MCP tools.
Translate the user's natural language request into the correct sequence of `mcp__osh__process` tool calls.

## User Request

$ARGUMENTS

## Workflow

Follow these steps in order. Skip discovery steps if you already have the needed information.

### Step 1: Discover What's Available

**Find running systems** (to get system UIDs and output names):
```
mcp__osh__database { action: "query", resourceType: "systems" }
mcp__osh__database { action: "query", resourceType: "datastreams", systemUID: "<uid>" }
```

**Find available processes** (to get process URIs):
```
mcp__osh__process { action: "list_processes" }
```

### Step 2: Create the Chain

```
mcp__osh__process { action: "create_chain", chainName: "<name>", uniqueID: "urn:osh:process:<name>:001" }
```

Use short, descriptive chain names like `weatherchain`, `geoptz`, `trackingpipeline`.

### Step 3: Add Components

Add components in data-flow order: sources first, then processors, then sinks.

**Data source** (reads from a running system's datastream):
```
mcp__osh__process {
  action: "add_datasource",
  chainName: "<name>",
  componentName: "s",          // use short names: s, s1, s2
  systemUID: "<system-uid>",
  outputName: "<output-name>"
}
```

**Processing component** (transforms data):
```
mcp__osh__process {
  action: "add_component",
  chainName: "<name>",
  componentName: "p",          // use short names: p, p1, p2
  processURI: "<process-uri>"
}
```

**Command sink** (sends commands to a system):
```
mcp__osh__process {
  action: "add_commandsink",
  chainName: "<name>",
  componentName: "c",
  systemUID: "<system-uid>",
  inputName: "<input-name>"
}
```

### Step 4: Check Connection Paths

After adding all components, discover available endpoints:
```
mcp__osh__process { action: "get_connection_paths", chainName: "<name>" }
```

This returns paths like:
- `components/s/outputs/weather`
- `components/p/inputs/weather`
- `components/p/outputs/weather`

### Step 5: Add Top-Level Output

Add an output resolved from a component's output. Use the `source` parameter to copy the data structure:
```
mcp__osh__process {
  action: "add_output",
  chainName: "<name>",
  componentName: "weather",     // this becomes the output name
  source: "components/p/outputs/weather"   // resolve structure from here
}
```

### Step 6: Add Connections

Wire components together following the data flow:
```
mcp__osh__process {
  action: "add_connection",
  chainName: "<name>",
  source: "components/s/outputs/weather",
  destination: "components/p/inputs/weather"
}
```

Always end with a connection to the top-level output:
```
mcp__osh__process {
  action: "add_connection",
  chainName: "<name>",
  source: "components/p/outputs/weather",
  destination: "outputs/weather"
}
```

### Step 7: Save and Load

```
mcp__osh__process {
  action: "save_and_load",
  chainName: "<name>",
  filePath: "/home/earocorn/<name>.xml",
  autoStart: true
}
```

## Connection Path Rules

- Top-level: `inputs/<name>`, `outputs/<name>`, `parameters/<name>`
- Component: `components/<compName>/inputs/<ioName>`, `components/<compName>/outputs/<ioName>`
- Nested fields: `components/<compName>/outputs/<ioName>/<fieldName>`
- Source output name must match destination input name in most cases (e.g., `weather` -> `weather`)

## Process URI Reference

### Data Sources/Sinks
| URI | Name | Purpose | Key Parameters |
|-----|------|---------|----------------|
| `datasource:datastream` | DataStream Source | Read from a system datastream | systemUID, outputName |
| `datasink:commandstream` | Command Sink | Send commands to a system | systemUID, inputName |

### Weather
| URI | Name | Purpose |
|-----|------|---------|
| `weather` | Weather Process | Convert weather units (C->F, hPa->PSI, m/s->mph) |

### Geolocation
| URI | Name | Purpose |
|-----|------|---------|
| `geoloc:ECEF2LLA` | ECEF to LLA | ECEF coordinates to Lat/Lon/Alt |
| `geoloc:LLA2ECEF` | LLA to ECEF | Lat/Lon/Alt to ECEF coordinates |
| `geoloc:LosToTarget` | LoS To Target | Compute target location from observer + LOS direction + distance |
| `geoloc:ECEFPos` | ECEF Matrix | Position/orientation matrix from location + attitude |
| `geoloc:RayIntersectEllipsoid` | Ray Ellipsoid Intersect | Ray-WGS84 intersection |
| `geoloc:RayIntersectTerrain` | Ray Terrain Intersect | Ray-DEM terrain intersection |

### Camera/PTZ
| URI | Name | Purpose |
|-----|------|---------|
| `PtzGeoPointing` | PTZ Geo-Pointing | Compute PTZ values to point at a geographic location |
| `geoloc:FovToCamMatrix` | FOV to Camera Matrix | Camera intrinsic matrix from FOV |
| `geoloc:ImageToGround` | Image to Ground | Pixel coords to ground location |
| `geoloc:ImageBboxToGround` | Image Bbox to Ground | Image bboxes to ground polygons |

### Math
| URI | Name | Purpose |
|-----|------|---------|
| `binaryOp` | Binary Operation | ADD, SUB, MUL, DIV, POW |
| `compareOp` | Comparison | GreaterThan, LowerThan, etc. with conditional routing |
| `lookUpTable1D` | Lookup Table | Interpolated mapping |

### Vector Math
| URI | Name | Purpose |
|-----|------|---------|
| `euler2Mat3` | Euler to Matrix | Euler angles to 3x3 rotation matrix |
| `mulMV3` | Matrix-Vector Multiply | 3x3 matrix * 3D vector |
| `mulMM3` | Matrix Multiply 3x3 | 3x3 * 3x3 matrix multiplication |
| `pos2Mat4` | Position to Matrix | Location+orientation to 4x4 homogeneous matrix |

### Video
| URI | Name | Purpose |
|-----|------|---------|
| `video:FFMpegDecoder` | Video Decoder | Decode H264/H265/VP8/VP9 to RGB frames |
| `video:FFMpegTranscoder` | Video Transcoder | Re-encode between codecs |
| `opencv:FaceDetection` | Face Detection | Haar cascade face detection |
| `opencv:ObjectTracking` | Object Tracking | KCF/CSRT video tracking |
| `opencv:BboxCenter` | Bbox Center | Extract center from bounding box |
| `opencv:FeatureDetection` | Feature Detection | Multi-feature cascade detection |

### Satellite
| URI | Name | Purpose |
|-----|------|---------|
| `TLEPredictor` | TLE Predictor | Satellite position from TLE data |
| `ECEF2ECI` | ECEF to ECI | ECEF to J2000 inertial frame |
| `ECI2ECEF` | ECI to ECEF | J2000 inertial to ECEF |

### Drone
| URI | Name | Purpose |
|-----|------|---------|
| `constAltitudeLLA` | Constant Altitude LLA | Maintain altitude while moving |

### Utility
| URI | Name | Purpose |
|-----|------|---------|
| `utils:ArrayIterator` | Array Iterator | Iterate array elements one by one |

## Common Driver UIDs

These are UID patterns for common drivers. The actual UID includes a serial/ID suffix:
- FakeWeather: `urn:osh:sensor:simweather:<serial>`
- FakeGPS: `urn:osh:sensor:simgps:<serial>`
- FakeCam: `urn:osh:sensor:simcam:<cameraID>`
- SimUAV: `urn:osh:system:simuav:<serial>`
- Axis Camera: `urn:axis:cam:<serial>`
- MAVLink (MavSdk): `urn:osh:sensor:mavsdk:<id>`

## Common Patterns

### Pattern 1: Sensor -> Process -> Output
Example: "connect fake weather to weather process"
1. Datasource `s` reads from FakeWeather `weather` output
2. Component `p` uses `weather` process URI
3. Connection: `s/outputs/weather` -> `p/inputs/weather`
4. Connection: `p/outputs/weather` -> `outputs/weather`

### Pattern 2: Sensor -> Geolocation -> PTZ Command
Example: "point axis camera at fake GPS location"
1. Datasource `s` reads GPS `gpsLocation` output
2. Component `p` uses `PtzGeoPointing` process
3. CommandSink `c` sends to axis camera PTZ input
4. Wire: GPS location -> geo-pointing input, PTZ output -> command sink

### Pattern 3: Video -> Decode -> Detect -> Geolocate
Example: "detect faces in axis camera feed and geolocate them"
1. Datasource `s` reads camera video output
2. Component `d` uses `video:FFMpegDecoder`
3. Component `f` uses `opencv:FaceDetection`
4. Component `g` uses `geoloc:ImageBboxToGround`
5. Wire: video -> decoder -> detection -> geolocation -> output

### Pattern 4: Multi-source Fusion
Example: "combine UAV position and gimbal orientation for camera geolocation"
1. Datasource `s1` reads UAV platform_pos
2. Datasource `s2` reads UAV platform_att
3. Component `p` uses `geoloc:ECEFPos` (takes location + attitude)
4. Wire both sources into the position matrix process

## Important Notes

- Always use `list_processes` first if you're unsure about available process URIs
- Always use `get_connection_paths` after adding components to see exact I/O names
- Use short component names (`s`, `p`, `c`, `s1`, `p1`) to keep connection paths brief
- The `source` parameter on `add_output`/`add_input` resolves the data structure from a component
- If `add_output` is called without `source`, `save_and_load` will auto-resolve from connections
- Process chains are saved as SensorML XML and loaded as SMLProcessImpl modules
- Chains auto-start by default; the datasource will connect to the running system
