# sensorhub-comm-udp2

**UDP Connectionless Communication Provider for OpenSensorHub**

Version: 1.0.1
Author: Nick Garay — Botts Innovative Research, Inc.
License: Mozilla Public License v2.0

## What Is This Module?

`sensorhub-comm-udp2` is an alternative UDP communication provider for OpenSensorHub that replaces the NIO channel-based approach used by the core `UDPCommProvider` with a traditional `DatagramSocket` implementation backed by explicit threading and packet-level buffering.

It implements the standard `ICommProvider` interface, so any sensor driver or processing module that accepts a comm provider can use it as a drop-in replacement for the built-in UDP provider.

## Why Does UDP2 Exist When UDPCommProvider Already Ships with OSH Core?

The core `UDPCommProvider` (in `osh-core`) uses Java NIO `DatagramChannel` with `Channels.newInputStream()` / `Channels.newOutputStream()`. While this works for many cases, the NIO channel approach has limitations:

| Concern | Core UDPCommProvider | UDP2CommProvider |
|---------|---------------------|------------------|
| **Socket API** | NIO `DatagramChannel` | Traditional `DatagramSocket` |
| **Input stream** | `Channels.newInputStream(channel)` — NIO adapter with opaque buffering | Custom `DatagramInputStream` — preserves individual datagram boundaries in a `ConcurrentLinkedQueue` |
| **Output stream** | `Channels.newOutputStream(channel)` — NIO adapter | Custom `DatagramOutputStream` — buffers writes, sends one UDP packet per `flush()` |
| **Threading model** | Synchronous; blocks the calling thread on read | Dedicated background receiver thread queues packets; consumer reads from queue |
| **Connection model** | Calls `channel.connect()` which filters packets to a single remote peer | Connectionless; binds locally and sends to a configured remote address without filtering |
| **Port auto-detection** | Supports `remotePort <= 0` to auto-detect from first incoming packet | Not supported; remote port must be explicitly configured |
| **Max packet size** | Not enforced | Hardcoded at 65,507 bytes (UDP maximum) |

### When to Choose UDP2 Over Core UDP

- **Datagram boundary preservation** — UDP2's `DatagramInputStream` queues each received packet as a discrete byte array. If your sensor protocol relies on one-message-per-packet semantics (common in binary sensor protocols), UDP2 makes this explicit rather than relying on NIO channel behavior.
- **Non-blocking consumer reads** — The background receiver thread continuously drains the socket into a queue. Your driver's read calls pull from the queue rather than blocking on the socket directly, which can simplify driver threading.
- **Connectionless operation** — Core UDP calls `channel.connect()`, which binds the channel to a single remote peer and silently discards packets from other sources. UDP2 does not call `connect()` on its socket, making it more suitable for scenarios where multiple remote sources may send to the same local port, or where the remote endpoint address may change.

## Module Architecture

```
UDP2CommProvider (main module class)
  ├── UDP2CommProviderConfig (configuration container)
  │     └── UDP2Config (protocol-level settings: host, ports)
  ├── DatagramInputStream (background receiver thread + packet queue)
  ├── DatagramOutputStream (buffered sender, one packet per flush)
  ├── UDP2CommModuleDescriptor (SensorHub module discovery)
  └── Activator (OSGi bundle lifecycle)
```

All classes live in the package `org.sensorhub.api.comm.providers`.

## Configuration

UDP2 is configured through `UDP2Config`, which extends the standard `IPConfig` base class. The configurable fields are:

| Field | Type | Description |
|-------|------|-------------|
| `remoteHost` | `String` | IP address or DNS name of the remote endpoint to send packets to. **Required.** |
| `remotePort` | `int` | Port on the remote host (0–65535). Set to `0` for auto. |
| `localPort` | `int` | Local port to bind and listen on (0–65535). **Required.** |
| `localAddress` | `String` | Local network interface to bind to. Defaults to `"AUTO"` (all interfaces). |

### Example: JSON Configuration Block

When configuring a sensor driver that uses a comm provider, you would reference the UDP2 module in the driver's `commProviderConfig` section of your `config.json`:

```json
{
  "objClass": "org.sensorhub.api.comm.providers.UDP2CommProviderConfig",
  "protocol": {
    "objClass": "org.sensorhub.api.comm.providers.UDP2Config",
    "remoteHost": "192.168.1.100",
    "remotePort": 5600,
    "localPort": 5601,
    "localAddress": "AUTO"
  }
}
```

### Example: SensorHub Admin UI

If configuring through the SensorHub web admin interface:

1. Navigate to **Modules** and select the sensor driver you want to configure.
2. In the driver's **Communication Provider** settings, select **UDP2 Comm Provider** from the module type dropdown.
3. Fill in the protocol settings:
   - **Remote Host** — the IP or hostname of the device sending/receiving UDP data.
   - **Remote Port** — the port the remote device listens on. Leave at `0` if you only need to receive.
   - **Local Port** — the port on this machine to listen for incoming UDP packets.
   - **Local Address** — leave as `AUTO` unless you need to bind to a specific network interface.
4. Save and start the module.

## How It Works Internally

### Startup Sequence

1. **`doInit()`** — Creates a `DatagramSocket` bound to the configured `localPort`.
2. **`doStart()`** — Instantiates the `DatagramInputStream` and `DatagramOutputStream` wrappers around the socket, then submits the input stream's receiver loop to a single-threaded executor.

### Receiving Data

`DatagramInputStream` implements `Runnable`. When started on the executor thread, it enters a loop:

1. Allocates a 65,507-byte receive buffer.
2. Calls `socket.receive()` (blocking).
3. Copies the received bytes into a right-sized array.
4. Enqueues the array into a `ConcurrentLinkedQueue`.
5. Notifies any waiting reader thread via `lock.notifyAll()`.

Consumer code calling `read()` on the input stream will block (via `lock.wait()`) until data is available in the queue, then returns bytes sequentially from the current packet buffer.

### Sending Data

`DatagramOutputStream` extends `ByteArrayOutputStream`. All `write()` calls accumulate in the internal byte buffer. When `flush()` is called:

1. The buffered bytes are wrapped in a `DatagramPacket` addressed to the configured remote host and port.
2. The packet is sent via `socket.send()`.
3. The internal buffer is reset for the next message.

This means **each `flush()` produces exactly one UDP datagram** — align your flush calls with your protocol's message boundaries.

### Shutdown Sequence

1. **`doStop()`** — Closes the input stream (which sets its `doWork` flag to `false`, causing the receiver thread to exit), closes the output stream, then closes the underlying socket.


## Troubleshooting

- **"Not connected" IOException on getInputStream/getOutputStream** — The module must be started before accessing streams. Ensure the module lifecycle has reached the `STARTED` state.
- **No data received** — Verify `localPort` matches what the remote device is sending to. Check firewall rules. If the remote device is on a different subnet, ensure `localAddress` is set to the correct interface or left as `AUTO`.
- **Packets being dropped** — The receiver thread queues packets as fast as they arrive, but if the consumer cannot keep up, the queue will grow. There is no built-in queue size limit, so sustained high-rate traffic without consumption could lead to memory pressure.
- **Sending fails** — Confirm `remoteHost` and `remotePort` are correctly set. The output stream resolves the hostname to an `InetAddress` at construction time, so DNS must be reachable at startup.
