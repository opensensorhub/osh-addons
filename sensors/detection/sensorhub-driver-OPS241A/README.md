# OmniPreSense OPS241-A Sensor
## What is it?
**References:**
- [OPS241-A Datasheet](https://zwavel.com/heesbeen/OPS241A_datasheet.pdf)
- [API Interface Spec](https://omnipresense.com/wp-content/uploads/2025/05/AN-010-AA_API_Interface.pdf)
- [Omnipresense Applications](https://omnipresense.com/applications/)

 
The OPS241-A is complete short-range Doppler radar sensor providing motion detection, 
speed, and direction reporting. All radar signal processing is done on board and a simple API 
reports the processed data. Flexible control over the reporting format, sample rate, and module power levels is provided.

### Finding SpeedTrap
```ping speed02```
```ssh 192.168.50.253```


### CONNECTION DETAILS
### Available I2C ports / buses on Raspberry Pi
By default, the bno085 will typically use bus 1 (port /dev/i2c-1) on the raspberry pi. Once connected to the raspberry pi,
you can <em>ssh</em> into the pi and run the following commands to discover the i2c bus:

```ls /dev/i2c-*``` to get all available buses and then ```i2cdetect -y <bus number>```

When executing ```i2cdetect -y 1``` you should see the address ```0x4A```. This is the default bno085 sensor address on
the raspberry pi.




