# OpenSensorHub — WeatherFlow Tempest Driver

An OpenSensorHub (OSH) sensor driver for the **WeatherFlow Tempest Weather Station**. The driver connects to the Tempest hub's local UDP broadcast stream, decodes all JSON message types, and publishes each as a separate OSH data stream via the Connected Systems API and SOS.

---

## Supported Hardware

| Device | Description |
|--------|-------------|
| Tempest Weather Station | All-in-one personal weather station (wind, rain, temperature, humidity, pressure, UV, solar, lightning) |
| Tempest Hub | Local Wi-Fi hub that broadcasts station data over UDP on the local network |
| Sky (obs_sky) | Legacy WeatherFlow Sky module — sky and wind observations |
| Air (obs_air) | Legacy WeatherFlow Air module — pressure, temperature, humidity, lightning |

---

## Message Types and Output Streams

The driver registers one OSH output per Tempest message type:

| Message Type | OSH Output Class | Description |
|---|---|---|
| `obs_st` | `TempestOutputObservation` | Primary unified weather observation (18 fields) |
| `obs_sky` | `TempestOutputSkyObservation` | Sky/wind observation from Sky module (14 fields) |
| `obs_air` | `TempestOutputAirObservation` | Air observation from Air module (8 fields) |
| `rapid_wind` | `TempestOutputRapidWind` | High-frequency wind update every 3 seconds (3 fields) |
| `evt_precip` | `TempestOutputRainStartEvent` | Rain start event — timestamp only |
| `evt_strike` | `TempestOutputLightningStrikeEvent` | Lightning strike event — timestamp, distance, energy |
| `device_status` | `TempestOutputDeviceStatus` | Device health report — voltage, RSSI, sensor status (10 fields) |
| `hub_status` | `TempestOutputHubStatus` | Hub health report — uptime, RSSI, reset flags (7 fields) |

---

## Data Fields

### obs_st — Tempest Observation

| Index | Field | Unit |
|-------|-------|------|
| 0 | Sample Time | UTC |
| 1 | Wind Lull (min 3s) | m/s |
| 2 | Wind Avg | m/s |
| 3 | Wind Gust (max 3s) | m/s |
| 4 | Wind Direction | deg |
| 5 | Wind Sample Interval | s |
| 6 | Station Pressure | mbar |
| 7 | Air Temperature | °C |
| 8 | Relative Humidity | % |
| 9 | Illuminance | lx |
| 10 | UV Index | — |
| 11 | Solar Radiation | W/m² |
| 12 | Rain Accumulated | mm |
| 13 | Precipitation Type | None / Rain / Hail / Rain + Hail |
| 14 | Lightning Strike Avg Distance | km |
| 15 | Lightning Strike Count | count |
| 16 | Battery Voltage | V |
| 17 | Report Interval | min |

### rapid_wind — Rapid Wind

| Index | Field | Unit |
|-------|-------|------|
| 0 | Sample Time | UTC |
| 1 | Wind Speed | m/s |
| 2 | Wind Direction | deg |

### evt_strike — Lightning Strike Event

| Index | Field | Unit |
|-------|-------|------|
| 0 | Sample Time | UTC |
| 1 | Distance | km |
| 2 | Energy | — |

### obs_sky — Sky Observation

| Index | Field | Unit |
|-------|-------|------|
| 0 | Sample Time | UTC |
| 1 | Illuminance | lx |
| 2 | UV Index | — |
| 3 | Rain Accumulated | mm |
| 4 | Wind Lull | m/s |
| 5 | Wind Avg | m/s |
| 6 | Wind Gust | m/s |
| 7 | Wind Direction | deg |
| 8 | Battery Voltage | V |
| 9 | Report Interval | min |
| 10 | Solar Radiation | W/m² |
| 11 | Local Day Rain Accumulation | mm |
| 12 | Precipitation Type | None / Rain / Hail |
| 13 | Wind Sample Interval | s |

### obs_air — Air Observation

| Index | Field | Unit |
|-------|-------|------|
| 0 | Sample Time | UTC |
| 1 | Station Pressure | mbar |
| 2 | Air Temperature | °C |
| 3 | Relative Humidity | % |
| 4 | Lightning Strike Count | count |
| 5 | Lightning Strike Avg Distance | km |
| 6 | Battery Voltage | V |
| 7 | Report Interval | min |

### device_status — Device Status

| Index | Field | Unit |
|-------|-------|------|
| 0 | Sample Time | UTC |
| 1 | Serial Number | — |
| 2 | Hub Serial Number | — |
| 3 | Uptime | s |
| 4 | Voltage | V |
| 5 | Firmware Revision | — |
| 6 | RSSI | dB |
| 7 | Hub RSSI | dB |
| 8 | Sensor Status (bitmask) | — |
| 9 | Debug | 0 / 1 |

### hub_status — Hub Status

| Index | Field | Unit |
|-------|-------|------|
| 0 | Sample Time | UTC |
| 1 | Serial Number | — |
| 2 | Firmware Revision | — |
| 3 | Uptime | s |
| 4 | RSSI | dB |
| 5 | Reset Flags | — |
| 6 | Sequence | — |

---

## Configuration

The driver is configured through the OSH Admin UI (`/sensorhub`). Two settings are required:

| Field | Description |
|-------|-------------|
| **Serial Number** | The Tempest device serial number (e.g. `ST-00000512`). Used to generate the driver's unique ID and XML ID. |
| **Comm Settings** | A comm provider pointing at the Tempest UDP broadcast. See [Communication Setup](#communication-setup) below. |

### Communication Setup

The Tempest hub broadcasts JSON messages to all devices on the local network over **UDP port 50222**. Configure a UDP comm provider in OSH with:

- **Protocol:** UDP
- **Local Port:** `50222`
- **Remote Host:** *(leave blank to receive broadcasts from any host)*

The hub must be on the same local network segment as the OSH node. No WeatherFlow account or internet connection is required for local UDP — the hub broadcasts continuously as long as it has power and Wi-Fi.

> If you need to receive data over TCP or serial (e.g. from a relay), the driver's `commSettings` field accepts any OSH `ICommProvider` implementation.

---

## Features of Interest (FOI)

Each Tempest device is registered as a **Feature of Interest** the first time a message is received from it, keyed by its serial number:

- **Tempest / Sky / Air devices** — FOI UID: `urn:osh:sensor:weather:tempest:foi:<serial_number>` (e.g. `ST-00000512`)
- **Hub** — FOI UID: `urn:osh:sensor:weather:tempest:foi:<hub_serial_number>` (e.g. `HB-00013030`)

Subsequent messages from the same device reuse the existing FOI. All data events are associated with the correct FOI, enabling Connected Systems API consumers to correlate observations to a physical device.

---

## Building

```bash
# From the osh-node root
./gradlew :sensorhub-driver-tempest:build -x test
```

The compiled OSGi bundle is output to `build/libs/` within the module directory.

---

## Dependencies

| Dependency | Version | Notes |
|------------|---------|-------|
| `sensorhub-core` | (project version) | Core OSH API |
| `jackson-databind` | 2.17.2 | JSON parsing for Tempest UDP messages |

---

## References

- [WeatherFlow Tempest UDP Broadcast API](https://apidocs.tempestwx.com/reference/tempest-udp-broadcast)
- [OpenSensorHub Documentation](https://docs.opensensorhub.org)
- [WeatherFlow Tempest Product Page](https://weatherflow.com/tempest-weather-system/)
