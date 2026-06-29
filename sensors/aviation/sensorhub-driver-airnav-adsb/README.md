# AirNav ADS-B FlightStick
The AirNav ADS-B driver connects to a dump1090 TCP stream and publishes aircraft identification, position, altitude, and velocity observations.

### Supported Input Formats

| Format | Port | Description |
|--------|------|-------------|
| **SBS** (default) | `30003` | BaseStation text format — provides barometric altitude only |
| **Beast** | `30005` | Beast binary format — provides both barometric and GNSS geometric altitude |

Beast mode decodes ADS-B Extended Squitter (DF17) messages directly, including airborne position (TC 9–22), velocity (TC 19), and aircraft identification (TC 1–4). It also extracts the GNSS/barometric altitude difference from velocity messages, enabling geometric altitude output even when only barometric position frames are received.

This driver has been tested with
- `dump1090-fa`


### Hardware
| Component                                 | Source                                                     |
|-------------------------------------------|------------------------------------------------------------|
| AirNav ADS-B FlightStick (RTL-SDR dongle) | [airnavradar.com/store](https://www.airnavradar.com/store) |
| ADS-B 1090 MHz Antenna                    | [airnavradar.com/store](https://www.airnavradar.com/store) |
| Rapsberry Pi                               | [raspberrypi.com](https://www.raspberrypi.com/)            |
---


### Prereqs
**macOS**
Install RTL-SDR utilities and `dump1090`:
```brew
brew update
brew install librtlsdr
brew install dump1090-fa
```
Verify the installed
```brew
dump1090-fa --help
```

**Linux**
Install RTL-SDR utilities and `dump1090`:
```bash
sudo apt update
sudo apt install rtl-sdr dump1090-fa
```
Add your user to the plugdev group:
```bash
sudo usermod -aG plugdev $USER
```

### Hardware Setup
1. Connect the antenna to the FlightStick SMA port
2. Plug the FlightStick into a USB port
3. Verify the device is detected
```bash
rtl_test -t
```

### Start dump1090

Start `dump1090` with TCP output enabled. The port depends on the input format you plan to use.

**FlightAware dump1090-fa**
```bash
dump1090-fa --net
```
This enables both the SBS port (`30003`) and Beast output port (`30005`) by default.

**Generic dump1090**
```bash
dump1090 --net-sbs-port 30003 --net-bo-port 30005
```

### Verify the port is open

**SBS (port 30003)**
```bash
nc localhost 30003
```
You should see comma-delimited SBS-1 messages:
```
MSG,3,1,1,A12345,1,2024/01/01,12:00:00.000,2024/01/01,12:00:00.000,,35000,,,33.1234,-97.5678,,,0,0,0,0
```

**Beast (port 30005)**
```bash
nc localhost 30005 | xxd | head
```
You should see binary data with `1a` escape bytes.

Aircraft messages are only output when aircraft are in reception range. Make sure the antenna is placed outdoors with a good line of sight.

### OSH Driver Config
1. Add a TCP Comm Module
2. Update the following fields:
   - host: `localhost`
   - port: `30003` (SBS) or `30005` (Beast)
3. Set **Input Format** to `SBS` or `BEAST` to match the selected port
4. Click `Apply Changes` to initialize the driver
5. Right-click the driver in the `Sensors` tab and click `Start`
6. Verify data is coming in

### Troubleshooting
**No devices found**
- Verify the FlightStick is plugged in: 
- Try a different USB port
- Try a different USB cable
- Confirm `rtl_test -t` detects the device
- On linux,
  - Verify the user is in the `plugdev`
  - replug the device in after updating permission

**Driver connects but no data**
- Confirm `dump1090` is running with `--net` (or the appropriate `--net-sbs-port`/`--net-bo-port` flags)
- Check `nc localhost 30003` (SBS) or `nc localhost 30005` (Beast) produces output
- Verify the **Input Format** in the driver config matches the port you're connected to
- Move antenna outdoors or near a window for better reception
- Verify no firewall is blocking the configured TCP port
- If running `dump1090` remotely, confirm the configured IP is reachable


### References
- [AirNav](https://www.airnavradar.com/)
- [dump1090](https://github.com/flightaware/dump1090)
- http://woodair.net/sbs/article/barebones42_socket_data.htm

