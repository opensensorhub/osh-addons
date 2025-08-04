# Kraken SDR
The KrakenSDR is a five-channel, phase-coherent software-defined radio (SDR) 
built using RTL-SDR components, designed primarily for radio direction finding. 
It utilizes five synchronized RTL-SDR receivers to achieve accurate 
[beamforming](https://www.techtarget.com/searchnetworking/definition/beamforming) 
and direction-of-arrival estimation. This allows users to locate the source of 
radio signals, making it useful for applications like locating interference, 
tracking assets, and even search and rescue efforts. 

# Hardware Setup
- KrakenSDR
- VK-162 USB GPS


# MANUAL SETUP with Fresh Image of RPi4
## Expected Process:
For the following steps to work, it is assumed that you're starting with a fresh image of a raspberry pi 4. These
steps can be broken into (4) parts:
- Initial Pi setup
- Installation of [Kraken's Heimdall Firmware](https://github.com/krakenrf/heimdall_daq_fw) to handle synchronization of all 5 antennas and serve data
- Installation of  [Kraken DoA DSP](https://github.com/krakenrf/krakensdr_doa) for DoA Digital Signal Processing and establishing DoA

To begin these steps, logon to your Raspberry Pi and begin:

## Setup Pi
1. SSH into your pi and update / install dependencies:
    ```
    sudo apt update && sudo apt upgrade
   ```
2. Make sure java 17 is installed on raspberry pi
   ```
    sudo apt install openjdk-17-jdk
   ```
3. Install prerequisites to turn RTL2832U chip of the Kraken Device into SDR
    ```
    sudo apt-get install libusb-dev libusb-1.0-0-dev build-essential cmake git
    ```
   - Purge old RTL-SDR and librtlsdr install
    ```
    sudo apt purge librtlsdr*
    sudo rm -rvf /usr/lib/librtlsdr* /usr/include/rtl-sdr* /usr/local/lib/librtlsdr* /usr/local/include/rtl-sdr*
    ```
## Install HeIMDALL DAQ Firmware
Follow the [Manual Step by Step Install](https://github.com/krakenrf/heimdall_daq_fw?tab=readme-ov-file#manual-step-by-step-install) instructions
to install the HeIMDALL DAQ Firmware. Depending on your setup, take special car in following the instructions. For example, if you
are using a Raspberry Pi, make sure to follow the ARM instructions. 

## Install DoA DSP Software
Follow the [Manual Install](https://github.com/krakenrf/krakensdr_doa?tab=readme-ov-file#manual-installation-from-a-fresh-os) instructions
to install teh DoA Data Signal Processing Software. 

### Additional Info for GPS Integration
For ***Section 4***, I used a [VK-162](https://www.amazon.com/Navigation-External-Receiver-Raspberry-Geekstory/dp/B078Y52FGQ) GPS. 
To set this up properly, i found the following [YouTube Tutorial](https://www.youtube.com/watch?v=A1zmhxcUOxw). However, you should be able
to type these commands in your Raspberry Pi's terminal:
1. Install GPSD
```commandline
sudo apt-get install gpsd gpsd-clients python-gps
```
2. Stop current GPSD service, rebind to the correct serial, and then restart it.

```
sudo systemctl stop gpsd.socket
sudo systemctl disable gpsd.socket
```
3. Update config StreamAdd
```commandline
sudo nano /lib/systemd/system/gpsd.socket 
```
Update ```ListenStream=127.0.0.1:2947``` to ```0.0.0.0:2947``` and save settings.

4. Kill any ongoing process and rebind gpsd to serial port, most likely ttyAMC0
```
sudo killall gpsd
sudo gpsd /dev/ttyACM0 -F /var/run/gpsd.socket
```
5. Feel free to test by typing ```gpsmon``` in your terminal

### Additional Help for Remote Control
According the `gui_run.sh` shell script in the krakensdr_doa directory, the web-server is
created either using php (if remote control in not enabled) or miniserve (if remote control is enabled). 

To update this, navigate to `krakensdr_doa/_share/settings.json` and update the `en_remote_control` value to *true*

Sometimes, miniserve must be set manaully. If you are not getting data from 8081, then do the following command:
```commandline
miniserve -i 0.0.0.0 -p 8081 -P -u --on-duplicate-files overwrite -- _share
```

this allows the remote server to be updatable.



## Helpful Resources
- [KrakenSDR Wiki](https://github.com/krakenrf/krakensdr_docs/wiki/)
- [Kraken Pi Image](https://github.com/krakenrf/krakensdr_doa/releases)
- [Kraken DOA video](https://www.youtube.com/watch?v=3ugAT5BLBc0)
- [DOA QUICKSTART](https://github.com/krakenrf/krakensdr_docs/wiki/02.-Direction-Finding-Quickstart-Guide)