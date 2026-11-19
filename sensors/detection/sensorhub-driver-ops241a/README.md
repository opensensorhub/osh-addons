# OmniPreSense OPS241-A Sensor
## What is it?
The OPS241-A is complete short-range Doppler radar sensor providing motion detection, 
speed, and direction reporting. All radar signal processing is done on board and a simple API 
reports the processed data. Flexible control over the reporting format, sample rate, and module power levels is provided.

## Helpful Info
- Can report objects with speeds up to 138 mph
- <b>View Range:</b> wide angle of 78 degree in both azimuth and altitude
- <b>Distance:</b>
  - people (26-32ft)
  - cars (65-82ft)

## CONNECTION DETAILS (RxTx)
For this setup, the OPS241-A is plugged up to the USB on the Raspberry Pi, and therefore will use RxTx
to communicate with the Sensor. To connect to the pi and confirm your connection to the sensor,
type the following commands into your terminal:

```
ping <Rpi Username>.local   // to locate find your raspberry pi and get the IP address
ssh 192.168.50.253          // Once you have your address, ssh into the pi
```

Once connected to the pi, you can <em>ssh</em> into the pi and run the following commands to discover 
the appropriate port--it should be ```ACM0```:

Type ```ls /dev/tty*``` to get all available ports. You should see ```/dev/ttyACM0```, which should confirm your
sensor is successfully connected to the Pi over serial. This port address will be needed to configure 
OpenSensorHub through your Admin Panel.

## CONFIGURE ADMIN PANEL
For the most part, the setup for this sensor to properly run is pretty straight forward. Once you have an instance of 
an OSH node running, make sure the Port Address is correct under the **RxTx Settings** in the Admin Panel (by default, 
it should read ```/dev/ttyACM0```).

Next, make sure a serial number is in place and the desired units for the speed measurement is selected (by default, mph
is selected). Next, select <em>Apply Changes</em> to save the configuration and initialize the module.
Once configured, <em>Start</em> the module to begin Radar Detection.

**References:**
- [OPS241-A Datasheet](https://zwavel.com/heesbeen/OPS241A_datasheet.pdf)
- [API Interface Spec](https://omnipresense.com/wp-content/uploads/2025/05/AN-010-AA_API_Interface.pdf)
- [Omnipresense Applications](https://omnipresense.com/applications/)