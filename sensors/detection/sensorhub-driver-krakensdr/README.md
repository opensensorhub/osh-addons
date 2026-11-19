# KrakenSDR Direction Finding System

The KrakenSDR is a five-channel, phase-coherent software-defined radio (SDR) built using RTL-SDR components, designed for radio direction finding. It uses five synchronized receivers to perform direction-of-arrival (DOA) estimation, making it useful for locating interference, tracking assets, and search and rescue operations.

---

## Hardware

| Component | Notes |
|---|---|
| KrakenSDR | 5-channel phase-coherent SDR |
| Krakentennas | 5-element antenna array |
| VK-162 USB GPS | For location/heading data |
| Raspberry Pi 4 | With 128 GB SD card |
| USB-C Power | Separate supplies for KrakenSDR and Pi |

---

## Quick Start — Flash the Existing Image

This is the recommended path. A pre-configured master image is available that includes the full software stack: Raspberry Pi OS, heimdall DAQ firmware, the custom `krakensdr_doa` WebSocket application, and a systemd service that auto-starts on boot.

**Additional hardware needed:** SD card reader

### Default credentials

| Setting | Value |
|---|---|
| Hostname | `kraken1` |
| Username | `osh` |
| Password | `oshtest1234` |

### Steps

**1. Flash the image**

Download and install [Raspberry Pi Imager](https://www.raspberrypi.com/software/). Open it, select your Pi model, then scroll to the bottom of the OS list and choose **Use Custom**. Select the [`kraken_master.img.gz`](https://drive.google.com/file/d/1OIIlloYC_ZOIv3CSfz0G7qAH_DmJRhhZ/view?usp=drive_link) file. Complete the setup — flashing may take several hours.

**2. Set a unique hostname (multi-unit deployments)**

If you're running more than one unit on the same network, each Pi must have a unique hostname. SSH into the new Pi and run:

```bash
# Edit the cloud-init user-data
sudo nano /boot/firmware/user-data
# Change: hostname: kraken1  →  hostname: kraken2

# Prevent cloud-init from overwriting the hostname on reboot
sudo nano /etc/cloud/cloud.cfg
# Ensure: preserve_hostname: true

# Apply immediately and reboot
sudo hostnamectl set-hostname kraken2
sudo reboot
```

**3. Fix a known DoA issue**

The Direction of Arrival function won't work until one setting is changed. Open the GUI at `http://<kraken-ip>:8080` in a browser, go to **VFO Configuration**, and change **Output VFO** from `1` to `ALL`.

**4. Assign static IPs (multi-unit deployments)**

Cloned units may initially receive conflicting IP addresses because your router's DHCP history is tied to MAC addresses. The cleanest fix is to set static DHCP reservations in your router admin panel — one entry per Pi, each mapped by MAC address to a fixed IP. Find each Pi's MAC with:

```bash
ip link show wlan0   # Wi-Fi
ip link show eth0    # Ethernet
```

---

## Running the Software

### Normal operation (auto-start)

The `kraken.service` systemd unit starts automatically on boot. Make sure the Raspberry Pi is configured to your local network. Use the following url to access your Kraken's GUI:

```
https://<KRAKEN_IP>:8080/
```

### WebSocket interface

All data output and remote control is available via a single WebSocket endpoint:

```
ws://<KRAKEN_IP>:8082/ws/kraken
```

No authentication required. On connect, the server immediately pushes the current settings snapshot.

**Outbound messages (server → client)**

Every message is a JSON object with a `type` field and a `timestamp` (epoch milliseconds):

```json
{ "type": "settings", "timestamp": 1714000000000, "center_freq": 416.588 }
{ "type": "doa",      "timestamp": 1714000000000, "doa_max": 135.0 }
{ "type": "spectrum", "timestamp": 1714000000000, "freq_axis": [], "channels": { "ch0": [] } }
```

**Inbound commands (client → server)**

Send a JSON object to update settings. Only included keys are changed — everything else is preserved.

```json
{
  "type": "command",
  "action": "update_settings",
  "data": {
    "center_freq": 433.92,
    "uniform_gain": 20.0
  }
}
```

### Settings reference

| Key | Type | Valid values | Default |
|---|---|---|---|
| `center_freq` | float (MHz) | ≥ 24.0 | `416.588` |
| `uniform_gain` | float (dB) or string | See gain table below, or `"Auto"` | `15.7` |
| `data_interface` | string | `"shmem"`, `"eth"` | `"shmem"` |
| `default_ip` | string | Any IP | `"0.0.0.0"` |
| **DoA** | | | |
| `en_doa` | bool | `true`, `false` | `true` |
| `ant_arrangement` | string | `"UCA"`, `"ULA"`, `"Custom"` | `"UCA"` |
| `ula_direction` | string | `"Both"`, `"Forward"`, `"Backward"` | `"Both"` |
| `ant_spacing_meters` | float (m) | ≥ 0.001 | `0.21` |
| `custom_array_x_meters` | string | Comma-separated floats | `"0.21,0.06,-0.17,-0.17,0.07"` |
| `custom_array_y_meters` | string | Comma-separated floats | `"0.00,-0.20,-0.12,0.12,0.20"` |
| `array_offset` | int (degrees) | Any integer | `0` |
| `doa_method` | string | `"Bartlett"`, `"Capon"`, `"MEM"`, `"TNA"`, `"MUSIC"`, `"ROOT-MUSIC"` | `"MUSIC"` |
| `doa_decorrelation_method` | string | `"Off"`, `"FBA"`, `"TOEP"`, `"FBSS"`, `"FBTOEP"` | `"Off"` |
| `expected_num_of_sources` | int | 1–4 | `1` |
| `compass_offset` | int (degrees) | Any integer | `0` |
| `doa_fig_type` | string | `"Linear"`, `"Polar"`, `"Compass"` | `"Linear"` |
| `en_peak_hold` | bool | `true`, `false` | `false` |
| **Station** | | | |
| `station_id` | string | Any string | `"NOCALL"` |
| `location_source` | string | `"None"`, `"Static"`, `"gpsd"` | `"None"` |
| `latitude` | float | -90.0 to 90.0 | `0.0` |
| `longitude` | float | -180.0 to 180.0 | `0.0` |
| `heading` | float (degrees) | 0.0–360.0 | `0.0` |
| `gps_fixed_heading` | bool | `true`, `false` | `false` |
| `gps_min_speed` | float (m/s) | > 0 | `2` |
| `gps_min_speed_duration` | int (s) | > 0 | `3` |
| `doa_data_format` | string | `"Kraken App"`, `"Kraken Pro Local"`, `"Kraken Pro Remote"`, `"Kerberos App"`, `"DF Aggregator"`, `"RDF Mapper"`, `"Full POST"` | `"Kraken App"` |
| `krakenpro_key` | string | Any string | — |
| `mapping_server_url` | string | WebSocket URL | `"wss://map.krakenrf.com:2096"` |
| `rdf_mapper_server` | string | HTTP URL | — |
| **VFO** | | | |
| `spectrum_calculation` | string | `"Single"`, `"All"` | `"Single"` |
| `vfo_mode` | string | `"Standard"`, `"Auto"` | `"Standard"` |
| `active_vfos` | int | 1–16 | `1` |
| `output_vfo` | int | -1 (all), 0–15 | `0` |
| `dsp_decimation` | int | ≥ 1 | `1` |
| `vfo_default_squelch_mode` | string | `"Auto"`, `"Manual"`, `"Auto Channel"` | `"Auto"` |
| `vfo_default_demod` | string | `"None"`, `"FM"` | `"None"` |
| `vfo_default_iq` | string | `"False"`, `"True"` | `"False"` |
| `max_demod_timeout` | int (s) | > 0 | `60` |
| `en_optimize_short_bursts` | bool | `true`, `false` | `false` |
| **Per-VFO** (replace `N` with 0–15) | | | |
| `vfo_freq_N` | float (Hz) | Within tuned bandwidth | center freq |
| `vfo_bw_N` | int (Hz) | ≥ 100 | `12500` |
| `vfo_fir_order_factor_N` | int | ≥ 2 | `2` |
| `vfo_squelch_N` | int (dBFS) | Any integer | `-120` |
| `vfo_squelch_mode_N` | string | `"Default"`, `"Auto"`, `"Manual"`, `"Auto Channel"` | `"Default"` |
| `vfo_demod_N` | string | `"Default"`, `"None"`, `"FM"` | `"Default"` |
| `vfo_iq_N` | string | `"Default"`, `"False"`, `"True"` | `"Default"` |

**Valid gain values (dB):** `0, 0.9, 1.4, 2.7, 3.7, 7.7, 8.7, 12.5, 14.4, 15.7, 16.6, 19.7, 20.7, 22.9, 25.4, 28.0, 29.7, 32.8, 33.8, 36.4, 37.2, 38.6, 40.2, 42.1, 43.4, 43.9, 44.5, 48.0, 49.6`

### Middleware API (port 8042)

```bash
GET  http://<KRAKEN_IP>:8042/settings   # retrieve current settings
POST http://<KRAKEN_IP>:8042/settings   # update settings
```

---

## GPS Setup (VK-162)

```bash
sudo apt-get install gpsd gpsd-clients
pip3 install gpsd-py3

sudo systemctl stop gpsd.socket
sudo systemctl disable gpsd.socket

# Update ListenStream to 0.0.0.0:2947
sudo nano /lib/systemd/system/gpsd.socket

sudo killall gpsd
sudo gpsd /dev/ttyACM0 -F /var/run/gpsd.socket

# Verify
gpsmon
```

---

## Manual Installation (Fresh Pi Image)

Use this path only if you need to rebuild from scratch. If you have access to the pre-built image, use the Quick Start section above.

**Software stack:**
- [heimdall DAQ firmware](https://github.com/krakenrf/heimdall_daq_fw) — handles synchronization across all 5 antennas
- [Botts Inc. krakensdr_doa](https://github.com/Botts-Innovative-Research/krakensdr_doa) — DoA DSP and WebSocket server

### 1. Initial Pi setup

```bash
sudo apt update && sudo apt upgrade
sudo apt install openjdk-21-jdk
sudo apt-get install libusb-dev libusb-1.0-0-dev build-essential cmake git

# Remove any conflicting RTL-SDR packages
sudo apt purge librtlsdr*
sudo rm -rvf /usr/lib/librtlsdr* /usr/include/rtl-sdr* \
             /usr/local/lib/librtlsdr* /usr/local/include/rtl-sdr*
```

### 2. Install heimdall DAQ firmware

Follow the [Manual Step-by-Step Install](https://github.com/krakenrf/heimdall_daq_fw?tab=readme-ov-file#manual-step-by-step-install) instructions. If you're on a Raspberry Pi, follow the ARM-specific steps.

### 3. Install the DoA DSP software

Follow the [Manual Install](https://github.com/Botts-Innovative-Research/krakensdr_doa#manual-install) instructions for the Botts Inc. modified DoA software.

### 4. First run

```bash
./kraken_doa_start.sh
```

On the first run, allow 1–2 minutes for the numba JIT compiler to compile optimized functions. Subsequent starts will be much faster as they read from cache.

---

## Resources

- [KrakenSDR Wiki](https://github.com/krakenrf/krakensdr_docs/wiki/)
- [Direction Finding Quickstart Guide](https://github.com/krakenrf/krakensdr_docs/wiki/02.-Direction-Finding-Quickstart-Guide)
- [DoA overview video](https://www.youtube.com/watch?v=3ugAT5BLBc0)
- [GPS setup tutorial](https://www.youtube.com/watch?v=A1zmhxcUOxw)
