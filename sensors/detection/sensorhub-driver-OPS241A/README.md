# OmniPreSense OPS241-A Sensor
## What is it?
**References:**
- [OPS241-A Datasheet](https://zwavel.com/heesbeen/OPS241A_datasheet.pdf)
- [API Interface Spec](https://omnipresense.com/wp-content/uploads/2025/05/AN-010-AA_API_Interface.pdf)
- [Omnipresense Applications](https://omnipresense.com/applications/)

 
The OPS241-A is complete short-range Doppler radar sensor providing motion detection, 
speed, and direction reporting. All radar signal processing is done on board and a simple API 
reports the processed data. Flexible control over the reporting format, sample rate, and module power levels is provided.

## Useful Info
- USB, UART
- Uses API
- Can report objects with speeds up to 138 mph
- <b>View Range:</b> wide angle of 78 degree in both azimuth and altitude
- <b>Distance:</b>
  - people (26-32ft)
  - cars (65-82ft)

### Measuring Speed (OPS241-A):
- port = ```/dev/ttyACM0```
- baud rate = ```9600```,
- parity = serial.PARITY_NONE,
- bytesize = 8 Bits,

)


### Finding SpeedTrap
```ping speed02```
```ssh 192.168.50.253```


### CONNECTION DETAILS
### USB on Raspberry Pi
In it's current setup, the OPS241-A is plugged up to the USB on the raspberry pi. Once connected to the raspberry pi,
you can <em>ssh</em> into the pi and run the following commands to discover the port, it should be ```ACM0```:

```ls /dev/tty*``` to get all available ports. Therefore, you should see ```/dev/ttyACM0```










