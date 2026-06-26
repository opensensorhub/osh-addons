# NMEA AIS Message Decoder Driver

Decodes live AIS (Automatic Identification System) NMEA sentences received over any OSH-supported communication channel (UDP, TCP, serial) and publishes structured observation data for each vessel and aid-to-navigation.

## NMEA Sentence Structure

Each AIS transmission is a comma-delimited NMEA sentence:

```
!AIVDM,1,1,,A,15Muan<000qm2=2CavBWSCL20@2?,0*6A
```

| Field | Example | Meaning |
|-------|---------|---------|
| Sentence type | `!AIVDM` | AIS VHF Data-link Message |
| Fragment count | `1` | Total fragments in this message |
| Fragment number | `1` | This fragment's index |
| Sequential ID | *(blank)* | Links multi-part messages |
| Channel | `A` | AIS Channel A (161.975 MHz) or B (162.025 MHz) |
| Payload | `15Muan<000q...` | 6-bit ASCII-armored AIS payload |
| Fill bits | `0` | Padding bits added to final payload |
| Checksum | `6A` | NMEA XOR checksum |

**Supported sentence types:** This driver only processes sentences beginning with `!AIVDM` (messages received from other vessels) or `!AIVDO` (own-vessel transmissions). All other NMEA sentences arriving on the configured port — such as GPS fix sentences (`$GPGGA`, `$GPRMC`) — are silently ignored. This means the driver can safely share a UDP port carrying mixed NMEA traffic without producing errors or garbled output.

---

## Hardware Requirements

- [ShipXplorer AIS Dongle](https://www.shipxplorer.com/ais-dongle) (or any RTL-SDR / AIS receiver)
- [ShipXplorer AIS Antenna](https://www.shipxplorer.com/ais-antenna) (or suitable VHF antenna)

## External Software Requirements
This driver does not directly receive or decode AIS radio signals from SDR hardware.
Instead, it expects already-decoded NMEA 0183 AIS messages to be forwarded to the driver over a supported network interface
Because of this, users must run external AIS decoding or forwarding software capable of:

- Receiving AIS signals from SDR hardware or another AIS source
- Decoding the AIS transmissions into NMEA 0183 AIS sentences
- Forwarding the decoded messages to this OSH driver 

Examples of compatible software include:
- [AIS-Catcher](https://jvde-github.github.io/AIS-catcher-docs/) — decodes RTL-SDR radio input and forwards NMEA sentences over UDP
- AIS Dispatcher
---

## Setup

### 1. Install and run AIS-Catcher (or other AIS decoding software)

Start AIS-Catcher and forward sentences to your OSH node over UDP:

```bash
# Forward to localhost port 10110
AIS-catcher -u 127.0.0.1 10110
```

For a remote OSH node replace `127.0.0.1` with the node's IP address.

### 2. Add the driver in OSH Admin Panel

1. Navigate to **Sensor Drivers** → **Add Driver**
2. Select **NMEA AIS Message Decoder Driver**
3. Fill in:
   - **Serial Number** — any unique identifier for this driver instance (e.g. `AIS-001`)
   - **Communication Settings** → select **UDP2 Comm Provider**
     - **Local Address** — the address OSH should bind to (e.g. `127.0.0.1`)
     - **Local Port** — must match the port used in AIS-Catcher (e.g. `10110`)
     - Leave Remote Host/Port empty — the driver only *receives* data, it does not send

> **Note:** Configure *Local Port*, not *Remote Port*. Remote Host/Port is for outbound connections; this driver only receives inbound UDP packets.

### 4. Start the driver

Click **Start**. The driver connects to the UDP socket, reads incoming sentences, and begins publishing to all outputs as messages arrive.

---

## Outputs

The driver registers **5 outputs**. Each output creates or updates a Feature of Interest (FOI) keyed by MMSI the first time a vessel or station is seen.

Static vessel identity data (name, callsign, ship type, dimensions, IMO number, etc.) received from message types 5, 19, and 24 is written as properties on the vessel FOI rather than streamed. Dynamic data — including navigational status — is always streamed through the appropriate output.

---

### 1. Raw NMEA AIS Messages (`nmeaAisOutputRawMessages`)

Publishes every received sentence as-is, before decoding. Useful for logging, replay, and debugging.

| Field | Type | Description |
|-------|------|-------------|
| sampleTime | Time | System time sentence was received |
| sentenceType | Text | e.g. `!AIVDM` or `!AIVDO` |
| fragmentCount | Integer | Total fragments in this message |
| fragmentNumber | Integer | This fragment's index |
| sequentialId | Text | Multi-part link ID |
| channel | Text | `A` or `B` |
| frequency | Double (MHz) | 161.975 (A) or 162.025 (B) |
| rawPayload | Text | 6-bit ASCII-armored AIS payload |
| fillBits | Integer | Padding bit count |
| checkSum | Text | NMEA XOR checksum |

---

### 2. Vessel Location (`vesselLocation`)

Unified position output for **all vessel position reports**: types **1**, **2**, **3** (Class A) and **18**, **19** (Class B). The `rot`, `roti`, `smi`, and `navStatus` fields are Class A only; Class B records carry `0` or `""` for those fields.

Type 19 additionally triggers a FOI update with the vessel's static identity data.

| Field | Type | Description |
|-------|------|-------------|
| samplingTime | Time | System time data was received |
| messageId | Integer | 1/2/3 = Class A; 18 = Class B Standard; 19 = Class B Extended |
| reportDescription | Text | Human-readable message type description |
| repeat | Integer | Repeat indicator (0–3) |
| mmsi | Text | Maritime Mobile Service Identity |
| sog | Double (kn) | Speed over ground (0–102.2 kn; 102.3 = not available) |
| positionAccuracy | Boolean | `true` = high (≤10 m); `false` = low (>10 m) |
| location | Lat/Lon | Position in decimal degrees |
| cog | Double (°) | Course over ground (0–359.9; 360 = not available) |
| heading | Integer (°) | True heading (0–359; 511 = not available) |
| utcSecond | Integer | UTC second of fix (0–59; 60 = not available; 61/62/63 = status codes) |
| raim | Boolean | `true` = RAIM in use |
| rot | Double (°/min) | Rate of turn *(Class A only; 0.0 for Class B)* |
| roti | Text | Rate-of-turn indicator *(Class A only; "" for Class B)* |
| smi | Text | Special Maneuver Indicator *(Class A only; "" for Class B)* |
| navStatus | Text | Navigational status e.g. "Under way using engine" *(Class A only; "" for Class B)* |

---

### 3. Voyage Info (`voyageInfo`)

Voyage-specific data from Class A vessels. Published for message **type 5** (two-part). The AIS library assembles both fragments before this output is updated. Static vessel identity fields from the same message (name, callsign, ship type, dimensions, IMO, AIS version, EPFD) are written to the FOI instead of being streamed here.

| Field | Type | Description |
|-------|------|-------------|
| samplingTime | Time | System time data was received |
| messageId | Integer | Always 5 |
| reportDescription | Text | Human-readable message type description |
| repeat | Integer | Repeat indicator (0–3) |
| mmsi | Text | MMSI |
| destination | Text | Destination port (max 20 chars; "@" padding stripped) |
| etaMonth | Integer | ETA month (1–12; 0 = not available) |
| etaDay | Integer | ETA day (1–31; 0 = not available) |
| etaHour | Integer | ETA hour (0–23; 24 = not available) |
| etaMinute | Integer | ETA minute (0–59; 60 = not available) |
| draught | Double (m) | Maximum present static draught (0 = not available) |
| dte | Boolean | `true` = data terminal equipment available |

---

### 4. Base Station Report (`nmeaAisOutputBaseStation`)

UTC/date and position broadcasts from fixed AIS base stations. Published for message types **4** and **11** (identical layout).

| Field | Type | Description |
|-------|------|-------------|
| samplingTime | Time | System time data was received |
| messageId | Integer | 4 = UTC/Date Report; 11 = UTC/Date Response |
| reportDescription | Text | Human-readable message type description |
| repeat | Integer | Repeat indicator |
| mmsi | Text | Base station MMSI |
| utcDateTime | Time | Full UTC date-time from the message |
| positionAccuracy | Boolean | `true` = high (≤10 m) |
| location | Lat/Lon | Base station position |
| epfd | Text | Electronic position fixing device type |
| raim | Boolean | `true` = RAIM in use |

---

### 5. Aid-to-Navigation Report (`nmeaAisOutputAidNavigation`)

Position and status of fixed and floating aids to navigation (buoys, lighthouses, beacons). Published for message **type 21**.

| Field | Type | Description |
|-------|------|-------------|
| samplingTime | Time | System time data was received |
| messageId | Integer | Always 21 |
| reportDescription | Text | Human-readable message type description |
| repeat | Integer | Repeat indicator |
| mmsi | Text | Aid MMSI |
| typeOfAidsToNav | Text | Aid type per IALA Maritime Buoyage System |
| name | Text | Aid name (max 20 chars; "@" padding stripped) |
| positionAccuracy | Boolean | `true` = high (≤10 m) |
| location | Lat/Lon | Aid position |
| dimensions | Record | GPS-antenna reference distances: dimBow, dimStern, dimPort, dimStarboard (m) |
| epfd | Text | Electronic position fixing device type |
| utcSecond | Integer | UTC second of report |
| offPositionIndicator | Text | "On position" / "Off position" (floating AtoN only; "N/A" when UTC second ≥ 60) |
| raim | Boolean | `true` = RAIM in use |
| virtualAid | Boolean | `true` = virtual aid (simulated by nearby AIS station) |
| assignedMode | Text | "autonomous and continuous mode" or "assigned mode" |

---

## Unhandled Message Types

The following AIS message types are received and parsed by the radio layer but are **not published** to any output. Each is silently discarded after the raw sentence is recorded in the `nmeaAisOutputRawMessages` output.

| Type | Name | Reason not output |
|------|------|-------------------|
| 6 | Addressed Binary Message | Application-specific binary payload; no standard decoded fields |
| 7 | Binary Acknowledge | Acknowledgment sequence numbers only; no sensor data |
| 8 | Binary Broadcast Message | Application-specific binary payload |
| 9 | SAR Aircraft Position Report | Position report for SAR aircraft; not currently implemented |
| 10 | UTC/Date Inquiry | Request-only message; no data payload |
| 12 | Addressed Safety-Related Message | Point-to-point text; not broadcast |
| 13 | Safety-Related Acknowledge | Acknowledgment only |
| 14 | Safety-Related Broadcast | Broadcast text alert; not currently implemented |
| 15 | Interrogation | Request-only message |
| 16 | Assignment Mode Command | Administrative base station command |
| 17 | DGNSS Binary Broadcast | Differential GPS corrections; specialized use |
| 20 | Data Link Management | Base station time-slot reservation; no vessel data |
| 22 | Channel Management | Base station VHF frequency management |
| 23 | Group Assignment Command | Base station group command |
| 25 | Single Slot Binary Message | Application-specific binary payload |
| 26 | Multiple Slot Binary Message | Application-specific binary payload |
| 27 | Long-Range AIS Broadcast | Class A/B position report for vessels outside base station coverage; not currently implemented |

All received sentences — including the above types — are available in the raw messages output for custom processing or logging.

## Additional Resources
- [USCG Navigation Center](https://www.navcen.uscg.gov/ais-messages)