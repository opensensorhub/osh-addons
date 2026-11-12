# Connected Systems API Part 3 MQTT Service

This module adds MQTT to the OSH Connected Systems (CS) API.
It follows the MQTT binding in OGC CS API Part 3 (draft).
The work is not complete. See [Status](#status) and [Known limits](#known-limits).

The legacy MQTT topics stay in the module `sensorhub-service-consys-mqtt`.
You can use the two modules on the same node. See [Legacy module](#legacy-module).

## What it does

- You subscribe to a topic to get live data. Examples are observations and command status.
- You subscribe to a Resource Event topic to know when a system, subsystem, datastream or control stream is created, updated or deleted.
- You subscribe to a Batch Resource Event topic to know how many observations a datastream got in each time window.
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
- `batchEventWindowMs`: the time window of the Batch Resource Events, in milliseconds. The default is `1000`.
  Each datastream gets at most one event in each window. Set `0` to publish one event each time a datastream gets observations.
- `brokerAddress`: the broker host and port (MQTT over TCP) in the AsyncAPI description, for example `broker.example.org:1883`.
  If it is empty, the service uses the HTTP host and port 1883. Port 1883 is the port of the bundled HiveMQ config. Set this value if the node's HiveMQ config uses another port.
- `webSocketAddress`: the broker address for MQTT over WebSocket in the AsyncAPI description, for example `broker.example.org:8083/mqtt` or `wss://broker.example.org/mqtt`.
  If it is empty, the description lists TCP only.
- `asyncApiViewerUrl`: the base URL of the AsyncAPI viewer for the HTML page. The default is `https://unpkg.com/@asyncapi/react-component@3.2.2`.
  If it is empty, the HTML page shows only the YAML text.

## Service description

The service publishes an AsyncAPI 3.0 description of its topics at:

```
{apiRoot}/asyncapi/mqtt
```

For example, `http://localhost:8181/sensorhub/api/asyncapi/mqtt`.
The media type is `application/asyncapi+yaml`.
The description shows the topics, formats, delivery settings and known limits of this version.

- A browser gets an HTML page that shows the document. You can also use `?f=html`. The page loads the AsyncAPI viewer from `asyncApiViewerUrl`. If it cannot load the viewer, it shows the YAML text.
- Other clients get YAML. You can also use `?f=yaml`.

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

Supported Resource Event topics (no `:data`):

- `{nodeId}/systems/{id}`: a top-level system
- `{nodeId}/{systemPath}/subsystems/{id}`
- `{nodeId}/{systemPath}/datastreams/{id}`
- `{nodeId}/{systemPath}/controlstreams/{id}`

Supported Batch Resource Event topics (no `:data`):

- `{nodeId}/{systemPath}/datastreams/{id}/observations`

The service rejects all other topics.

## Resource Events

Each message on a Resource Event topic is one CloudEvent (CloudEvents 1.0, JSON).
The service sends an event when a resource is created, updated or deleted.
Each event goes on the topic of the resource only. It does not go on the topics of parent systems.

```
topic    node1/systems/134/subsystems/7/datastreams/42
payload  {"specversion": "1.0",
          "id": "7e8d7083-7682-466f-b600-b274ac94ac74",
          "type": "org.ogc.api.csapi.datastream.delete",
          "source": "http://localhost:8181/sensorhub/api",
          "subject": "http://localhost:8181/sensorhub/api/datastreams/42",
          "time": "2026-11-04T23:07:00.458Z",
          "resourceparent": "http://localhost:8181/sensorhub/api/systems/7"}
```

- `type` is `org.ogc.api.csapi.{resource}.{operation}`.
  - `{resource}` is `system`, `datastream` or `controlstream`. Subsystems use `system`.
  - `{operation}` is `create`, `update` or `delete`.
- `source` is the CS API root URL.
- `subject` is the HTTP URL of the resource, for example `{apiRoot}/datastreams/42`. It is not the topic path.
- `resourceparent` is the HTTP URL of the parent system. A top-level system has no `resourceparent`.
- `id` is different for each event.
- Events have no `data` member. Get the resource over HTTP.

You can subscribe only to a resource that exists and that you can read. The service rejects publications on Resource Event topics.

## Batch Resource Events

A datastream can get many observations each second. The service does not send one event for each observation.
It sends one Batch Resource Event with the number of new observations in a time window.

```
topic    node1/systems/134/datastreams/42/observations
payload  {"specversion": "1.0",
          "id": "0b5c2f4e-5d0e-4d8e-9f43-2b1f0f6f7a10",
          "type": "org.ogc.api.csapi.observation.create",
          "source": "http://localhost:8181/sensorhub/api",
          "subject": "http://localhost:8181/sensorhub/api/datastreams/42/observations",
          "time": "2026-11-05T18:00:01.002Z",
          "resourceparent": "http://localhost:8181/sensorhub/api/datastreams/42",
          "datacontenttype": "application/json",
          "data": {"timerange": ["2026-11-05T18:00:00.034Z", "2026-11-05T18:00:00.967Z"],
                   "count": 29}}
```

- Each datastream gets at most one event in each window (`batchEventWindowMs`).
- After a quiet window, the service sends the first event at once. It sends the next events at the end of the window.
- A window with no new observations sends no event.
- `subject` is the HTTP URL of the observations of the datastream. `resourceparent` is the HTTP URL of the datastream.
- `data.timerange` gives the times of the first and last observation event in the batch. `data.count` is the number of observations.
- Get the observations over HTTP, for example `GET {subject}?resultTime=...`, or subscribe to the `:data` topic.

You can subscribe only to a datastream that exists and that you can read. The service rejects publications on Batch Resource Event topics.

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
- Resource Events for systems, subsystems, datastreams and control streams
- AsyncAPI service description at `{apiRoot}/asyncapi/mqtt`
- Batch Resource Events for observations

Not done yet:

- Resource Events for other resources, for example observations, commands and system events
- Batch Resource Events for commands, command status, command results and system events
- Wildcard subscriptions (`+`, `#`)
- MQTT 5 Content Type
- A link to the AsyncAPI description from the CS API landing page

## Known limits

- The service rejects topics with `+` or `#`.
- Without wildcards, you cannot get the `create` event of a new resource. You can subscribe only to a resource that exists.
- An update of a system sends an event only if it adds a new version (a later `validTime`). An update in place sends no event. This is an osh-core limit.
- When you delete a system, osh-core deletes its datastreams and control streams with it. These send no events. Its subsystems send events.
- Enabling or disabling a system, datastream or control stream sends no event.
- Events have no MQTT 5 Content Type. The payload is `application/cloudevents+json`.
- The service sends events for all systems on the node. If the CS API shows only some resources, clients can subscribe only to those.
- The service sends Batch Resource Events for each datastream that gets observations, also when no client subscribes.
- When the service stops, observations that are not yet in an event are not sent.
- Topics `.../observations/{id}` are rejected. There are no events for one observation.
- The service checks the topic shape only. The CS API does not check that each ID in the path belongs to the resource before it. For example, a command sent under the wrong system path still runs.
- A subscription to the status of one command gets the status of all commands on that control stream.
- Publications to `status:data` and `result:data` are rejected. The CS API accepts them only at `/controlstreams/{id}/status` over HTTP.
- Subscriptions to `systems:data`, `subsystems:data`, `datastreams:data`, `controlstreams:data` and `result:data` are rejected. The CS API cannot stream these resources yet. You can publish to the first four to create resources.
- Error replies (MQTT 5 reason codes) come only from the embedded OSH broker. MQTT 3.1.1 clients get no reason. Asynchronous error reports are not done yet.
- A JSON command must put its fields in `parameters`. If it does not, the service rejects the command.
- A default format can also be used on its format subtopic. The Part 3 draft says this SHOULD NOT be offered, because data is then sent twice. Here data is sent only to topics with subscribers.
- A `swe-xml` publication creates the resource, but the reply says "invalid payload". The CS API reads past the end of the record. HTTP has the same problem.
- Video datastreams offer only `om-json` and `swe-binary`. In `om-json`, the result does not include the frame.
- The CS API landing page does not link to the AsyncAPI description yet. Use the path above.
- The AsyncAPI description is YAML and HTML only. There is no JSON version (`f=json`).
- The AsyncAPI description lists WebSocket servers only when `webSocketAddress` is set.
- The service cannot read the HiveMQ settings. The description states the delivery settings (QoS 1, retain false, sessions in memory) of the bundled HiveMQ config. Authentication is not stated.

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
- Data on topics without `:data`. These topics now carry Resource Events.
- The control stream status topic `/api/controlstreams/{csId}/status`. Use the per-command status topic.
