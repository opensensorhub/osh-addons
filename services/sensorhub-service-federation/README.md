# Node Federation Broker (`sensorhub-service-federation`)

An OpenSensorHub service module that federates several OSH nodes into one. It
discovers the systems, datastreams and control streams on one or more **remote**
nodes, mirrors them onto a central **commander** node, then keeps them live:

- **Observations** flow remote → commander.
- **Commands** flow commander → remote.


The broker is a **pure network client**. It talks to every node, the commander
included, only through the OGC API – Connected Systems (HTTP) and its MQTT
bindings. It uses no in-process hub APIs, so it can run inside the commander or
on any standalone OSH node that can reach all the others.

```
   remote node A ──┐   MQTT  :data   (observations)          HTTP POST
   remote node B ──┼──────────────────────────► [ federation ] ───────────► commander
   remote node C ──┘                            [   broker    ]
         ▲            HTTP POST  /commands       [             ] ◄─────────── commander
         └───────────────────────────────────── [             ]   MQTT  (commands)
```

| | |
|---|---|
| Module name | Node Federation Broker |
| Version | 0.1.0 |
| Module class | `org.sensorhub.impl.service.federation.FederatedBrokerService` |
| Config class | `org.sensorhub.impl.service.federation.FederatedBrokerConfig` |
| Descriptor | `FederationBrokerDescriptor` (registered in `META-INF/services`) |
| OSGi activator | `org.sensorhub.impl.service.federation.Activator` |

---

## Requirements

**Every federated node (commander and remotes)**

- Connected Systems API service enabled and reachable over HTTP from the broker.
- Connected Systems API MQTT service enabled, with its MQTT broker reachable from
  the broker (default port `1883`).

**Commander**

- Connected Systems API must accept writes (transactional enabled). The broker
  creates systems, datastreams and control streams on it, and POSTs observations
  to it.

**Remotes**

- Must accept command POSTs on any control stream you want commands forwarded to.

**Broker host**

- Network reach to every node's HTTP port and MQTT port.

---

## Build

The module is wired into this repo's composite build in `settings.gradle`, and the
root `build.gradle` includes it in the node distribution:

```groovy
include ':sensorhub-service-federation'
project(':sensorhub-service-federation').projectDir = "$serviceDir/sensorhub-service-federation" as File
```

```groovy
implementation project(':sensorhub-service-federation')
```

Compile just this module from the repo root:

```bash
./gradlew :sensorhub-service-federation:compileJava
```

Or build the full node distribution:

```bash
./gradlew build
```

**Dependencies**

- `sensorhub-core`
- `:sensorhub-comm-mqtt`
- Embedded in the bundle:
  - Eclipse Paho MQTT v3 `1.2.5`, the client used to subscribe to each node's broker
  - Jetty util
  - HiveMQ CE embedded

> On Windows PowerShell, `&&` and `printf` aren't available. Run commands one at a
> time and use `git -C <path>`.

---

## Setup

1. In the OSH admin UI, add a new service module and pick **Node Federation Broker**.
2. Add one entry under **Nodes** for the commander, with **Is Commander** checked.
3. Add one entry for each remote node.
4. Set the global options (see below), then save and start the module.

The broker starts in the background. Watch the module status line: every
`statusReportIntervalSeconds` it reports something like:

```
Federating: 1 commander(s), 2 remote(s); 14 datastream(s), 3 control stream(s) mirrored; 17 active pump(s)
```

If startup fails (most often the first MQTT connect), the module reports the error
instead of failing silently.

### Service options (`FederatedBrokerConfig`)

| Field | Default | Description |
|---|---|---|
| `nodes` | `[]` | Federated nodes: one commander plus one or more remotes. |
| `enableBinaryDatastreams` | `false` | Federates `application/swe+binary` datastreams (e.g. video) as an opaque passthrough. **Must be on for video.** When it's off those streams are skipped, and JSON streams are unaffected. |
| `statusReportIntervalSeconds` | `30` | How often the status summary is published. `0` disables it. |
| `reconcileIntervalSeconds` | `30` | How often remote topology is re-discovered. `0` fixes the topology at startup. Read once at module start. |
| `removedAfterCycles` | `3` | How many consecutive cycles a stream must be missing from a **reachable** remote before it's retired. A node that's down doesn't count. |
| `onRemoved` | `keep` | What happens to a retired stream's commander mirror: `keep` it (history preserved) or `delete` it. |

### Per-node options (`NodeEnvData`)

| Field | Default | Description |
|---|---|---|
| `name` | — | Display name. |
| `protocol` | `http` | `http` or `https`. |
| `address` | — | Hostname or IP. |
| `port` | — | HTTP API port. |
| `isCommander` | `false` | Marks the mirror target. |
| `sensorhubRoot` | `sensorhub` | First HTTP path segment. |
| `apiRoot` | `sensorhub/api` | CS API path. The URL is built as `/{sensorhubRoot}/{apiRoot}`, and a redundant leading `sensorhubRoot` is dropped, so `api` and `sensorhub/api` mean the same thing. |
| `auth.type` | `basic` | Only HTTP Basic is applied today. |
| `auth.username` / `auth.password` | — | Credentials. |
| `mqttPort` | `1883` | MQTT port of the node's broker. |
| `mqttTopicRoot` | `api` | Prefix of every MQTT topic the node publishes. It must match the node's own setup (see below). |

**Setting `mqttTopicRoot`**

The MQTT topic root doesn't depend on the HTTP API root. The two only look linked
because both default to `api`. If `mqttTopicRoot` doesn't match how the node
publishes, no observations or commands will arrive.

- If the node's CS API MQTT service has a `nodeId`, use that value. For example,
  `rmt-axis` gives `rmt-axis/datastreams/{id}/observations:data`.
- If it has no `nodeId`, the node publishes under its API endpoint. Use a leading
  slash, e.g. `/api`. The leading slash is kept as-is.
- Multi-segment roots like `site1/osh` are allowed.
- Nodes that share one MQTT broker must each use a distinct root.

---

## How it works

**Threads**

`FederatedBrokerService` runs three daemon threads:

- `federation-broker` loads the config into an `OSHDataBroker`, then runs the first
  discovery cycle.
- `federation-status` publishes the status summary.
- `federation-reconcile` runs `reconcileOnce()` every `reconcileIntervalSeconds`.

**Reconcile cycle**

`ReconcileMixin.reconcileOnce()` does both the startup discovery and every later
cycle. For each remote node it:

1. Discovers systems, datastreams and control streams. Unreachable nodes are
   skipped and their streams left alone. The MQTT client reconnects on its own,
   so pumps resume when the node returns.
2. Mirrors new systems to the commander, with `id`/`links` cleared.
3. **Activates new datastreams:** subscribes to the remote `:data` topic, creates
   or adopts the commander mirror, then starts an observation pump.
4. **Activates new control streams:** creates or adopts the commander mirror,
   subscribes to its command topic, then starts a `cmd-forward` thread.
5. Detects schema drift. A stream whose record or command schema changed is
   retired and re-mirrored. Binary streams skip this check, because their
   schemas aren't byte-stable across fetches and re-mirroring would cut off
   anyone watching the video.
6. Retires streams that have been missing for `removedAfterCycles` cycles. This
   interrupts the pump, unsubscribes and drops the routing entry. The commander
   mirror is kept or deleted per `onRemoved`.

**Observation path**

- JSON observations are rebuilt for the commander datastream and POSTed to
  `/datastreams/{id}/observations`.
- Binary frames are relayed byte-for-byte.
- A datastream is treated as binary when it advertises `swe+binary` and **not**
  `swe+json`.
- Binary streams subscribe to the same plain `:data` topic as JSON streams, not
  `:data/swe-binary`.

**Command path**

- Commands published to a mirrored commander control stream are picked up from
  its MQTT topic.
- The commander-side `id` and `controlstream@id` are stripped, and the command is
  POSTed to the matching remote control stream.

**Restart dedup**

Mirrors are matched to existing commander resources by output or input name plus
a schema fingerprint (`findExistingMirror*`). They're adopted rather than
duplicated. Routing maps are keyed by node-qualified `address:port/id`, because
bare ids collide across nodes.

**Shutdown**

`doStop()` interrupts all three threads plus every pump and forwarder in the
`StreamRegistry`, then disconnects every node's MQTT client. A restart therefore
doesn't leave duplicate forwarders behind.

### Source layout

```
src/main/java/org/sensorhub/impl/service/federation/
├── FederatedBrokerService.java     OSH service entry point and threads
├── FederatedBrokerConfig.java      Admin-UI config
├── FederationBrokerDescriptor.java Module provider
├── Activator.java                  OSGi activator
├── OSHDataBroker.java              Composes the mixins; shared state (BrokerContext)
├── DiscoveryMixin.java             Remote discovery, MQTT subscribe, observation pumps
├── MirroringMixin.java             System/datastream/control stream mirroring and dedup
├── ObservationMixin.java           Remote → commander observation routing
├── CommandRoutingMixin.java        Commander → remote command forwarding
├── ReconcileMixin.java             Continual discovery and teardown
├── SchemaBuildersMixin.java        Builds commander-side resource and observation bodies
├── StreamRegistry.java, PumpEntry.java   Per-stream pump tracking
├── BrokeredDatastream.java, BrokerLogging.java
├── environment/                    NodeEnvData, AuthData, EnvironmentData (config objects)
├── events/                         In-process event bus (ADD_COMMANDER, ADD_REMOTE_NODE, …)
├── nodes/                          CommanderNode, RemoteNode, BrokeredNode
└── oshconnect/                     Java port of the oshconnect client: Node, System,
                                    Datastream, ControlStream, APIHelper (java.net.http),
                                    MqttCommClient (Paho), JSON resource wrappers
```

---

## Status

**Working, verified live:**

- Datastream and observation federation.
- Video (`swe+binary` passthrough).
- PTZ metrics (JSON).
- Command/tasking federation (commander → remote).

**Implemented, not yet field-verified:**

- Continual discovery and teardown (reconcile).
- Per-node `mqttTopicRoot`, `sensorhubRoot` and `apiRoot`.

## Known limitations

- **Dedup is fragile across builds.** The schema fingerprint is a raw JSON string
  compare. Cosmetic differences between OSH builds can cause false "schema
  changed" results and duplicate mirrors. A canonicalized compare (recursive key
  sort) is a planned fix.
- **Video relay is one HTTP POST per frame.** That's fine for one camera (~30 fps
  × ~150 KB). Scaling to more cameras likely needs a streaming relay.
- **Only HTTP Basic auth is applied.** `oauth` is accepted in config but not used.
- **The reconcile interval is fixed at module start.** Restart the module after
  changing it.
- **The Python ops/telemetry bus was not ported.**

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Mirrors are created but no observations arrive | `mqttTopicRoot` doesn't match the remote's MQTT `nodeId` (or the `/api` endpoint form), or the broker can't reach the remote's MQTT port. |
| Video streams aren't mirrored | `enableBinaryDatastreams` is off. |
| Duplicate mirrors on the commander after a restart | Schema fingerprint mismatch across builds (see Known limitations). |
| 500 creating a control stream on the commander | Incompatible commander OSH build. This blocked command federation until the commander was moved to a compatible build. |
| Module says started but nothing is federated | Check the module status and log for "Federation broker startup failed". |

For quieter logs during testing, raise
`org.sensorhub.impl.service.consys.mqtt` to `info` in
`tools/sensorhub-test/src/main/resources/logback-test.xml`.

---

Maintainer: Robin White (GeoRobotix US) · License: MPL 2.0
