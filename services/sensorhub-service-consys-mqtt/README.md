# Connected Systems API MQTT Service

This module adds MQTT to the OSH Connected Systems (CS) API.
It follows the MQTT binding in OGC CS API Part 3 (draft).
The work is not complete. See [Status](#status) and [Known limits](#known-limits).

## What it does

- You subscribe to a topic to get live data. Examples are observations and command status.
- You publish to a topic to create a resource. Examples are a system, an observation or a command.
- Each publication is one create operation. MQTT cannot update or delete resources.
- The CS API checks each request. The same users and permissions apply as for HTTP.

## Setup

You need these modules on the node:

- Connected Systems API Service
- MQTT Server (`sensorhub-service-mqtt-hivemq`)
- Connected Systems API MQTT Service (this module)

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

## Examples

Get observations from datastream `12` of system `134`:

```
subscribe  node1/systems/134/datastreams/12/observations:data
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

Not done yet:

- Format subtopics (`:data/{format}`). Each topic uses the default format.
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
- Each topic uses the default JSON format of its resource. Other formats need format subtopics, which are not done yet.

## Change in version 3.0.0

This is a breaking change.
Earlier versions used the HTTP path as the topic, for example `/api/datastreams/12/observations`.
These topics do not work now. Use `{nodeId}/{systemPath}/...:data` topics.
