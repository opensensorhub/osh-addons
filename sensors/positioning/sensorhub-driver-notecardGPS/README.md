# Blues Notecard / Notehub.io
The Blues Notecard is a system-on-module designed for secure, low-power, and reliable device-to-cloud data transfer. Data is transferred
from the Notecard to [Notehub.io](https://notehub.io/). Notehub.io is a cloud service provided by Blues Wireless, acting as a central hub
for managing and routing data from Notecard Devices.

The Blues Notecard has a built-in GPS module for location tracking.

## CONNECTING TO SPEED TRAP DEVICE
To locate the speed trap via SSH, you must first obtain the IP Address of the
Raspberry Pi. In this example, Pi's name is <em>speed02</em>. In the terminal,
type the following command to retrieve the IP Address:

```ping speed02.local```

If the devices is connected to the network, you will see an IP Address similar to the one below.
Now, ssh into Pi using the following command

```ssh pi@192.168.1.241```

## Connectivity via I2C Interface
While the Blues Notecard has multiple interfacing options, this application will
utilize [I²C](https://learn.sparkfun.com/tutorials/i2c/all) to retrieve data from the sensor.

By default, the Notecard will typically use bus 1 (<em>port /dev/i2c-1</em>) on the raspberry pi. After connecting to the
pi via ssh, you can identify all available I²C Ports and what addresses have been assigned to that port by using the following commands:


```ls /dev/i2c-*``` to get all available buses (the last number after i2c- is the bus number)

```i2cdetect -y <bus number>``` to detect all addresses assigned to the port.

When executing ```i2cdetect -y 1``` you should see the address ```0x17```. This is the default address of the Blues Notecard and will
indicate that the device is connected via I²C to the raspberry pi.

## Notecard Communication over I²C
Unlike many fixed-length and register-based I2C protocols, the Notecard defines a variable-length, serial-over-I2C protocol that
allows developers to handle JSON requests and responses in a similar manner as a direct Serial connection.

The Notecard operates the Blues [Serial-over-I2C protocol](https://dev.blues.io/guides-and-tutorials/notecard-guides/serial-over-i2c-protocol/) at roughly 100kHz, in chunks of no more than 255 bytes, with a minimum 1ms delay between transactions.


### CARD Version Info
- [NOTE-WBNA](https://dev.blues.io/datasheets/notecard-datasheet/note-wbna-500/?__hstc=171785791.2936b6a7dff83f271d76613b2e6d75c1.1749655454727.1750427234945.1750684598010.4&__hssc=171785791.3.1750684598010&__hsfp=3372007040&_gl=1*d0cdtg*_gcl_aw*R0NMLjE3NTA2ODQ3OTAuRUFJYUlRb2JDaE1JOE1PcC05Q0hqZ01WelZyX0FSMEZrd0hwRUFBWUFTQUFFZ0tpQ1BEX0J3RQ..*_gcl_au*MTAyMTI5NDQ5Ni4xNzQ5NjU1NDUz*_ga*MTUzMjIxODQ2Ni4xNzQ5NjU1NDUz*_ga_PJ7RGMWWBX*czE3NTA2ODQzOTgkbzkkZzEkdDE3NTA2ODQ3ODkkajM4JGwwJGg0MjU0NjI5Nw..&_ga=2.54584939.1709567019.1750684398-1532218466.1749655453&_gac=1.178602256.1750684790.EAIaIQobChMI8MOp-9CHjgMVzVr_AR0FkwHpEAAYASAAEgKiCPD_BwE)
    - Wideband (Cat-1)
    - North America
    - Cellular Only

```
    "board": "1.11",
    "body": {
        "built": "Nov 26 2024 14:01:26",
        "org": "Blues Wireless",
        "product": "Notecard",
        "target": "r5",
        "ver_build": 17004,
        "ver_major": 7,
        "ver_minor": 5,
        "ver_patch": 2,
        "version": "notecard-7.5.2"
    },
    "cell": true,
    "device": "dev:869965060858648",
    "gps": true,
    "name": "Blues Wireless Notecard",
    "ordering_code": "XA0XT1N0AFAC",
    "sku": ",
    "version": "notecard-7.5.2.17004"
```

**References:**
- [Notecard Overview](https://dev.blues.io/notecard/notecard-walkthrough/overview/)
- [I2C Guide](https://dev.blues.io/guides-and-tutorials/notecard-guides/serial-over-i2c-protocol/)
- [Notecard API Requests](https://dev.blues.io/api-reference/notecard-api/card-requests/latest/)










