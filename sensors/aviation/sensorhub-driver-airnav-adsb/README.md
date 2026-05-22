# AirNav ADS-B FlightStick
The AirNav ADS-B driver connects to a dump1090 SBS-1/BaseStation TCP stream and publishes aircraft identification, position, altitude, and velocity observations.

This driver has been tested with 
- `dump1090-fa` 
 - `dump1090-mutability`


### Hardware
| Component | Source |
|---|---|
| AirNav ADS-B FlightStick (RTL-SDR dongle) | [airnavradar.com/store](https://www.airnavradar.com/store) |
| ADS-B 1090 MHz Antenna | [airnavradar.com/store](https://www.airnavradar.com/store) |
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
sudo apt install rtl-sdr dump1090-mutability
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

[//]: # (````### Start dump1090)

[//]: # (Start `dump1090` with the SBS-1 TCP output enabled on port `30003`.)

[//]: # ()
[//]: # (FlightAware dump109-fa)

[//]: # (```bash)

[//]: # (dump1090-fa --net-sbs-port 30003)

[//]: # (```)

[//]: # (Generic dump109)

[//]: # (```bash)

[//]: # (dump1090 --net-sbs-port 30003)

[//]: # (```)

[//]: # (The driver consumes the SBS-1 TCP stream format produced by `dump1090`.)

[//]: # ()
[//]: # (### Verify the SBS port is open:)

[//]: # (```bash)

[//]: # (nc localhost 30003)

[//]: # (```)

[//]: # (You should see comma-delimited SBS-1 Messages)

[//]: # (```)

[//]: # (MSG,3,1,1,A12345,1,2024/01/01,12:00:00.000,2024/01/01,12:00:00.000,,35000,,,33.1234,-97.5678,,,0,0,0,0)

[//]: # (```)

[//]: # (Aircraft messages are only outputted when aircraft are in reception range. Make sure you place the antenna outside, )

[//]: # (and have a good line of sight.````)

### OSH Driver Config
1. Add a TCP Comm Module
2. Update the following fields:
   - host: `localhost`
   - port: `30003`
3. Click `Apply Changes` to initialize the driver
4. Right-click the driver in the `Sensors` tab and click `Start`
5. Verify data is coming in

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
- Confirm `dump1090` is running with `--net-sbs-port 30003`
- Check `nc localhost 30003` produces output
- Move antenna outdoors or near a window for better reception
- Verify no firewall is blocking TCP port `30003`
- If running `dump1090` remotely, confirm the configured IP is reachable


### References
- [AirNav](https://www.airnavradar.com/)
- [dump1090](https://github.com/flightaware/dump1090)

