# Connected Systems API Part 3 MQTT Service

This module adds MQTT to the OSH Connected Systems (CS) API.
It follows the MQTT binding in OGC CS API Part 3 (draft).
The work is not complete. See [Status](#status) and [Known limits](#known-limits).

The legacy MQTT topics stay in the module `sensorhub-service-consys-mqtt`.
You can use the two modules on the same node. See [Legacy module](#legacy-module).

## What it does

- You subscribe to a topic to get live data. Examples are observations and command status.
- You publish to a topic to create a resource. Examples are a system, an observation or a command.
- Each publication is one create operation. MQTT cannot update or delete resources.
- The CS API checks each request. The same users and permissions apply as for HTTP.

## Setup

You need these modules on the node:

- Connected Systems API Service
- MQTT Server (`sensorhub-service-mqtt-hivemq`)
- Connected Systems API Part 3 MQTT Service (this module)

Configuration:

- `nodeId`: the first level of every topic. The default is `api`.
  - Use one topic level only. Do not use `/`, `+`, `#` or NUL. Do not start it with `$`.
  - Give each CS API server on the same broker a different `nodeId`.

## Topics

All topics start with `{nodeId}/`. Then comes the resource path, as in the HTTP API.
Data topics end with `:data` on the last collection name.

`{systemPath}` is the full path of the system: `systems/{id}`, then `/subsystems/{id}` for each subsystem level.

Supported Resource Data topics:

- `{nodeId}/systems:data`: top-level systems
- `{nodeId}/{systemPath}/subsystems:data`
- `{nodeId}/{systemPath}/datastreams:data`
- `{nodeId}/{systemPath}/controlstreams:data`
- `{nodeId}/{systemPath}/events:data`: system events
- `{nodeId}/{systemPath}/datastreams/{id}/observations:data`
- `{nodeId}/{systemPath}/controlstreams/{id}/commands:data`
- `{nodeId}/{systemPath}/controlstreams/{id}/commands/{id}/status:data`
- `{nodeId}/{systemPath}/controlstreams/{id}/commands/{id}/result:data`

The service rejects all other topics.

## Formats

The plain `:data` topic uses the default format of the resource.
To use a different format, add `/{format}` after `:data`:

```
node1/systems/134/datastreams/12/observations:data/swe-csv
```

The format name comes from the media type:

1. Remove parameters (`;` and after).
2. Remove `application/`.
3. Change `+` to `-`.
4. Use lowercase.

Format names:

- `json`: `application/json`
- `geo-json`: `application/geo+json`
- `sml-json`: `application/sml+json`
- `sml-xml`: `application/sml+xml`
- `om-json`: `application/om+json`
- `swe-json`: `application/swe+json`
- `swe-csv`: `application/swe+csv`
- `swe-xml`: `application/swe+xml`
- `swe-binary`: `application/swe+binary`
- Custom formats of the CS API service also get a name, if the name has no `/` or `#`.
  For example, `video/x-motion-jpeg` gets no format subtopic.

On a format subtopic:

- A subscription gets the data in that format only.
- A publication must use that format. The service does not detect the format.
- The service rejects an unknown format name. It does not use the default format.
- The service rejects a format that the resource does not support.
  Each datastream lists its formats in `formats` over HTTP.

Formats found to work in tests:

- Observations, subscribe: `json`, `om-json`, `swe-json`, `swe-csv`, `swe-xml`, `swe-binary`
- Video observations (Axis camera), subscribe: `swe-binary` (MJPEG and H264 frames), `om-json` (no frame data)
- Observations, publish: `om-json`, `swe-json`, `swe-csv`
- Commands, publish: `json`, `swe-json`, `swe-csv`
- Systems, publish: `geo-json`, `sml-json`

## Examples

Get observations from datastream `12` of system `134`:

```
subscribe  node1/systems/134/datastreams/12/observations:data
```

Get the same observations as CSV:

```
subscribe  node1/systems/134/datastreams/12/observations:data/swe-csv
```

Send a command to control stream `3` of system `134`:

```
publish    node1/systems/134/controlstreams/3/commands:data
payload    {"parameters": {"action": "SET_STEP", "intValue": 5, "countDown": false}}
```

The fields in `parameters` come from the control stream schema. Give all of them.

## Status

Done:

- `{nodeId}` topic prefix
- Resource Data topics (`:data`) with the full `{systemPath}`
- Format subtopics (`:data/{format}`)

Not done yet:

- Resource Event topics (topics without `:data`) and CloudEvents messages
- Wildcard subscriptions (`+`, `#`)
- MQTT 5 Content Type
- AsyncAPI service description

## Known limits

- The service rejects topics with `+` or `#`.
- The service rejects topics without `:data`.
- The service checks the topic shape only. The CS API does not check that each ID in the path belongs to the resource before it. For example, a command sent under the wrong system path still runs.
- A subscription to the status of one command gets the status of all commands on that control stream.
- Publications to `status:data` and `result:data` are rejected. The CS API accepts them only at `/controlstreams/{id}/status` over HTTP.
- Subscriptions to `systems:data`, `subsystems:data`, `datastreams:data`, `controlstreams:data` and `result:data` are rejected. The CS API cannot stream these resources yet. You can publish to the first four to create resources.
- Error replies (MQTT 5 reason codes) come only from the embedded OSH broker. MQTT 3.1.1 clients get no reason. Asynchronous error reports are not done yet.
- A JSON command must put its fields in `parameters`. If it does not, the service rejects the command.
- A default format can also be used on its format subtopic. The Part 3 draft says this SHOULD NOT be offered, because data is then sent twice. Here data is sent only to topics with subscribers.
- A `swe-xml` publication creates the resource, but the reply says "invalid payload". The CS API reads past the end of the record. HTTP has the same problem.
- Video datastreams offer only `om-json` and `swe-binary`. In `om-json`, the result does not include the frame.

## Legacy module

This module was split from `sensorhub-service-consys-mqtt`. That module keeps the legacy topics and does not change.

- Nodes that use the legacy topics keep `sensorhub-service-consys-mqtt`. Their config does not change.
- To use the Part 3 topics, add this module. Its classes are in `org.sensorhub.impl.service.csapi.mqtt`.
- You can use the two modules on the same node and broker. Legacy topics start with the HTTP endpoint (`/api/`). Part 3 topics start with `{nodeId}/` (`api/`).

### Topics

Legacy topics use the HTTP endpoint of the CS API service (default `/api`) and the HTTP path.
Part 3 topics use `{nodeId}`, the full `{systemPath}` and `:data`.

- `/api/datastreams/{dsId}/observations`
  - becomes `{nodeId}/systems/{sysId}/datastreams/{dsId}/observations:data`
- `/api/controlstreams/{csId}/commands`
  - becomes `{nodeId}/systems/{sysId}/controlstreams/{csId}/commands:data`
- `/api/controlstreams/{csId}/status`
  - becomes `{nodeId}/systems/{sysId}/controlstreams/{csId}/commands/{cmdId}/status:data`
- `/api/systems` (publish)
  - becomes `{nodeId}/systems:data`
- `/api/systems/{sysId}/datastreams` (publish)
  - becomes `{nodeId}/systems/{sysId}/datastreams:data`
- `...?f=application/swe+csv`
  - becomes `...:data/swe-csv`

To find `{sysId}` for a datastream or control stream, read `system@id` from the HTTP API, for example `GET /api/datastreams/{dsId}`.
For a subsystem, use the full path: `systems/{sysId}/subsystems/{subId}/...`.

### Move a client to Part 3 topics

1. Add this module to the node. Set `nodeId` in its config.
   - If you do not set it, the value is `api`. Topics then start with `api/` (no leading `/`).
   - Use a different `nodeId` for each node on a shared broker.
2. Change each client topic. Use the list above.
3. Remove query strings from topics. Use a format subtopic to select a format.
4. Check JSON commands. Put the fields in `parameters`.
5. For command status, subscribe per command. You get the command ID from the HTTP API.

### What this module does not support

- Topics that start with `/` or with the HTTP endpoint. Use `sensorhub-service-consys-mqtt` for these.
- Query strings in topics. Filters such as `?resultTime=` are not available on MQTT.
- Topics without `:data`. In a later version, these topics carry Resource Events.
- The control stream status topic `/api/controlstreams/{csId}/status`. Use the per-command status topic.
