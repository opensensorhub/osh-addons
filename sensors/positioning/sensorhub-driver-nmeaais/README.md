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

---

## Hardware Requirements

- [ShipXplorer AIS Dongle](https://www.shipxplorer.com/ais-dongle) (or any RTL-SDR / AIS receiver)
- [ShipXplorer AIS Antenna](https://www.shipxplorer.com/ais-antenna) (or suitable VHF antenna)

## Software Requirements

- [AIS-Catcher](https://jvde-github.github.io/AIS-catcher-docs/) — decodes RTL-SDR radio input and forwards NMEA sentences over UDP

---

## Setup

### 1. Install and run AIS-Catcher

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

The driver registers **7 outputs**. Each output creates a Feature of Interest (FOI) keyed by MMSI the first time a vessel is seen.

### 1. NMEA AIS Messages (`nmeaAisOutputRawMessages`)

Publishes every raw sentence as received — before decoding. Useful for logging, replay, and debugging.

| Field | Type | Description |
|-------|------|-------------|
| sampleTime | Time | System time sentence was received |
| sentenceType | Text | e.g. `!AIVDM` |
| fragmentCount | Integer | Total fragments in message |
| fragmentNumber | Integer | This fragment's index |
| sequentialId | Text | Multi-part link ID |
| channel | Text | `A` or `B` |
| frequency | Double (MHz) | 161.975 (A) or 162.025 (B) |
| rawPayload | Text | Encoded AIS payload |
| fillBits | Integer | Padding bit count |
| checkSum | Text | NMEA checksum |

---

### 2. Position Report Class A (`nmeaAisOutputPositionClassA`)

Decoded Class A shipborne position reports. Published for message types **1**, **2**, and **3** — all three carry an identical field layout.

| Field | Type | Description |
|-------|------|-------------|
| messageId | Integer | 1 = Scheduled, 2 = Assigned, 3 = Interrogation response |
| reportDescription | Text | Human-readable type description |
| repeat | Integer | Repeat indicator (0–3) |
| mmsi | Integer | Maritime Mobile Service Identity |
| navStatus | Integer | Navigational status (0 = underway, 1 = at anchor, etc.) |
| rot | Integer | Rate of turn (–128 to +127; –128 = not available) |
| sog | Double (kn) | Speed over ground |
| positionAccuracy | Integer | 1 = high (≤10 m), 0 = low |
| location | Lat/Lon | Position in decimal degrees |
| cog | Double (°) | Course over ground |
| heading | Integer (°) | True heading (511 = not available) |
| timeStamp | Time | UTC second of fix |
| smi | Integer | Special manoeuvre indicator |
| raim | Integer | RAIM flag |
| commState | Integer | Communication state |

---

### 3. Position Report Class B (`nmeaAisOutputPositionClassB`)

Decoded Class B position reports. Handles both **type 18** (Standard CS) and **type 19** (Extended CS). Type 19 additionally carries vessel name, ship type, and dimensions; those fields are empty/zero for type 18 records.

| Field | Type | Description |
|-------|------|-------------|
| messageId | Integer | 18 = Standard, 19 = Extended |
| reportDescription | Text | Human-readable type description |
| repeat | Integer | Repeat indicator |
| mmsi | Integer | MMSI |
| sog | Double (kn) | Speed over ground |
| positionAccuracy | Integer | 1 = high, 0 = low |
| location | Lat/Lon | Position in decimal degrees |
| cog | Double (°) | Course over ground |
| heading | Integer (°) | True heading (511 = not available) |
| timeStamp | Time | UTC second of fix |
| unitFlag | Integer | 0 = SOTDMA unit, 1 = CS unit *(type 18 only)* |
| displayFlag | Integer | Display capability *(type 18 only)* |
| dscFlag | Integer | DSC capability *(type 18 only)* |
| bandFlag | Integer | Band capability *(type 18 only)* |
| message22Flag | Integer | Frequency management flag *(type 18 only)* |
| modeFlag | Integer | 0 = autonomous, 1 = assigned |
| raim | Integer | RAIM flag |
| commStateFlag | Integer | SOTDMA/ITDMA selector *(type 18 only)* |
| commState | Integer | Communication state *(type 18 only)* |
| name | Text | Vessel name *(type 19 only; empty for type 18)* |
| shipType | Integer | Ship type code *(type 19 only)* |
| dimBow | Integer (m) | GPS antenna to bow *(type 19 only)* |
| dimStern | Integer (m) | GPS antenna to stern *(type 19 only)* |
| dimPort | Integer (m) | GPS antenna to port *(type 19 only)* |
| dimStarboard | Integer (m) | GPS antenna to starboard *(type 19 only)* |
| epfd | Integer | EPFD type *(type 19 only)* |
| dte | Integer | Data terminal equipment *(type 19 only)* |
| assignedMode | Integer | Assigned mode flag *(type 19 only)* |

---

### 4. Base Station Report (`nmeaAisOutputBaseStation`)

UTC/date and position broadcasts from fixed AIS base stations. Published for message types **4** and **11** (identical layout).

| Field | Type | Description |
|-------|------|-------------|
| messageId | Integer | 4 = UTC/Date Report, 11 = UTC/Date Response |
| reportDescription | Text | Human-readable type description |
| repeat | Integer | Repeat indicator |
| mmsi | Integer | Base station MMSI |
| utcYear | Integer | UTC year |
| utcMonth | Integer | UTC month (1–12) |
| utcDay | Integer | UTC day (1–31) |
| utcHour | Integer | UTC hour (0–23; 24 = not available) |
| utcMinute | Integer | UTC minute (0–59) |
| utcSecond | Integer | UTC second (0–59) |
| positionAccuracy | Integer | 1 = high, 0 = low |
| location | Lat/Lon | Base station position |
| epfd | Integer | EPFD type |
| raim | Integer | RAIM flag |

---

### 5. Static and Voyage Data (`nmeaAisOutputStaticVoyage`)

Vessel identity and voyage information from Class A vessels. Published for message **type 5**. This is a multi-sentence (two-part) message; AISLib reassembles both parts before this output is updated.

| Field | Type | Description |
|-------|------|-------------|
| messageId | Integer | Always 5 |
| reportDescription | Text | Human-readable type description |
| repeat | Integer | Repeat indicator |
| mmsi | Integer | MMSI |
| aisVersion | Integer | AIS version (0 = ITU1371) |
| imoNumber | Integer | IMO number (0 = not available) |
| callSign | Text | Call sign |
| name | Text | Vessel name |
| shipType | Integer | Ship type code |
| dimBow | Integer (m) | GPS antenna to bow |
| dimStern | Integer (m) | GPS antenna to stern |
| dimPort | Integer (m) | GPS antenna to port |
| dimStarboard | Integer (m) | GPS antenna to starboard |
| epfd | Integer | EPFD type |
| etaMonth | Integer | ETA month |
| etaDay | Integer | ETA day |
| etaHour | Integer | ETA hour |
| etaMinute | Integer | ETA minute |
| draught | Double (m) | Maximum static draught |
| destination | Text | Destination port |
| dte | Integer | Data terminal equipment |

---

### 6. Class B Static Data (`nmeaAisOutputStaticDataClassB`)

Vessel name, callsign, and dimensions from Class B vessels. Published for message **type 24**.

Type 24 is transmitted in two separate sentences: Part A (name only) and Part B (callsign, ship type, dimensions). The driver caches Part A names by MMSI and publishes a combined record when Part B arrives. If Part B is received before Part A, the name field will be empty until the next transmission cycle.

| Field | Type | Description |
|-------|------|-------------|
| messageId | Integer | Always 24 |
| reportDescription | Text | Human-readable type description |
| repeat | Integer | Repeat indicator |
| mmsi | Integer | MMSI |
| name | Text | Vessel name (from Part A; empty if not yet received) |
| callSign | Text | Call sign (from Part B) |
| shipType | Integer | Ship type code |
| dimBow | Integer (m) | GPS antenna to bow |
| dimStern | Integer (m) | GPS antenna to stern |
| dimPort | Integer (m) | GPS antenna to port |
| dimStarboard | Integer (m) | GPS antenna to starboard |
| vendorId | Text | Manufacturer vendor ID |

---

### 7. Aid-to-Navigation Report (`nmeaAisOutputAidNavigation`)

Position and status of fixed and floating aids to navigation (buoys, lighthouses, beacons). Published for message **type 21**.

| Field | Type | Description |
|-------|------|-------------|
| messageId | Integer | Always 21 |
| reportDescription | Text | Human-readable type description |
| repeat | Integer | Repeat indicator |
| mmsi | Integer | Aid MMSI |
| typeOfAidsToNav | Integer | Aid type (1 = unspecified; 2 = reference; 3 = RACON; 4 = fixed; 21–29 = floating; etc.) |
| name | Text | Aid name |
| positionAccuracy | Integer | 1 = high, 0 = low |
| location | Lat/Lon | Aid position |
| dimBow | Integer (m) | Bow dimension |
| dimStern | Integer (m) | Stern dimension |
| dimPort | Integer (m) | Port dimension |
| dimStarboard | Integer (m) | Starboard dimension |
| epfd | Integer | EPFD type |
| utcSecond | Integer | UTC second of report |
| offPositionIndicator | Integer | 0 = on position, 1 = off position |
| raim | Integer | RAIM flag |
| virtualAid | Integer | 0 = physical, 1 = virtual (simulated) |
| assignedMode | Integer | 0 = autonomous, 1 = assigned |

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
