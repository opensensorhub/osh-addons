# Blues Notecard / Notehub.io
The Blues Notecard is a system-on-module designed for secure, low-power, and reliable device-to-cloud data transfer. Data is transferred
from the Notecard to [Notehub.io](https://notehub.io/). Notehub.io is a cloud service provided by Blues Wireless, acting as a central hub
for managing and routing data from Notecard Devices.

The Blues Notecard has a built-in GPS module for location tracking. This OpenSensorHub module is designed to utilize the Notecard's GPS to
retrieve the device's current latitude and longitude over I²C. 

## SETUP INSTRUCTIONS
This module was tested using a Blues Wireless Notecard. The Notecard was connected to a Raspberry Pi 5 using a Notecarrier Pi via the M2 connector.
If not connected directly to USB, the Notecard should default to communicate over I²C. 

### Connecting to Raspberry Pi
Make sure your Raspberry Pi is connected to your local network. To locate the Pi via SSH, you must first obtain the IP Address of the
Raspberry Pi. In this example, the Pi's username is <em>speed02</em>. In the terminal, type the following command to retrieve the IP 
Address of your device:

```ping speed02.local```

If the devices is connected to the network, you will see an IP Address similar to the one below.
Now, ssh into Pi using the following command

```ssh pi@192.168.1.241```

### Connectivity via I2C Interface
While the Blues Notecard has multiple interfacing options, this application will
utilize [I²C](https://learn.sparkfun.com/tutorials/i2c/all) to retrieve data from the sensor as discussed previously.

By default, the Notecard will use bus 1 (<em>port /dev/i2c-1</em>) on the raspberry pi. After connecting to the
pi via ssh, you can identify all available I²C Ports and what addresses have been assigned to that port by using the following commands:

```ls /dev/i2c-*``` to get all available buses (the last number after i2c- is the bus number)

```i2cdetect -y <bus number>``` to detect all addresses assigned to the port.

When executing ```i2cdetect -y 1``` you should see the address ```0x17```. This is the default address of the Blues Notecard and will
indicate that the device is connected via I²C to the raspberry pi.

### Notecard Communication over I²C
Unlike many fixed-length and register-based I2C protocols, the Notecard defines a variable-length, serial-over-I2C protocol that
allows developers to handle JSON requests and responses in a similar manner as a direct Serial connection.

The Notecard operates the Blues [Serial-over-I2C protocol](https://dev.blues.io/guides-and-tutorials/notecard-guides/serial-over-i2c-protocol/) 
at roughly 100kHz, in chunks of no more than 255 bytes, with a minimum 1ms delay between transactions.

## CONFIGURATION INSTRUCTIONS
For this module to run successfully, the following criteria will be needed:
- [notehub.io account](https://notehub.io/) and <em> Notehub Project UID</em>
- I²C port number (<em>most likely 1 unless configured manually</em>)
- I²C Sensor Address (<em>most likely 0x17 if not manually changed</em>)

**Note: **<em>If you do not have a notehub account or a Notehub Project UID, follow this [walkthrough](https://dev.blues.io/notehub/notehub-walkthrough/). For the GPS 
module to work correctly, it must sync with cellular to retrieve a time reference. Once complete, you have the option in the OSH configuration to either sync with 
Notehub.io or not.

**References and tools:**
- [Notecard Overview](https://dev.blues.io/notecard/notecard-walkthrough/overview/)
- [Notecard CLI](https://dev.blues.io/tools-and-sdks/notecard-cli/)
- [I2C Guide](https://dev.blues.io/guides-and-tutorials/notecard-guides/serial-over-i2c-protocol/)
- [Notecard API Requests](https://dev.blues.io/api-reference/notecard-api/card-requests/latest/)










