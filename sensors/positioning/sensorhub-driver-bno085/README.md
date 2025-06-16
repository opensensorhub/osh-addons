# BN0-055 SENSOR
## What is it?
**References:**
- [SHTP Datasheet](https://docs.sparkfun.com/SparkFun_VR_IMU_Breakout_BNO086_QWIIC/assets/component_documentation/Sensor-Hub-Transport-Protocol.pdf)
- [SSH-2 Datasheet](https://www.ceva-ip.com/wp-content/uploads/SH-2-Reference-Manual.pdf)
- [BNO08x Datasheet](https://www.ceva-ip.com/wp-content/uploads/BNO080_085-Datasheet.pdf)
- [Python Example](https://learn.adafruit.com/adafruit-9-dof-orientation-imu-fusion-breakout-bno085/python-circuitpython)

### Finding SpeedTrap
```ping speed02```
```ssh 192.168.50.253```

The BNO085 is a compact 9-DOF orientation sensor that integrates a 3-axis accelerometer, gyroscope,
and magnetometer on a single chip. It also includes an ARM Cortex-M0+ processor that processes the
raw sensor data and fuses it to provide orientation data. This allows the BNO085 to provide various
sensor fusion products like absolute orientation, linear acceleration, and gravity vector, among others.

### CONNECTION DETAILS
### Connectivity and Communication via I2C Interface
While the BNO085 Sensor has multiple interfacing options, this application will
utilize the [I²C protocol](https://learn.sparkfun.com/tutorials/i2c/all) (Inter-Integrated Circuit) to retrieve data from the sensor. This is done by sending and receiving messages using the [SHTP Protocol](https://www.ceva-ip.com/wp-content/uploads/SH-2-Reference-Manual.pdf).

### Available I2C ports / buses on Raspberry Pi
By default, the bno085 will typically use bus 1 (port /dev/i2c-1) on the raspberry pi. Once connected to the raspberry pi,
you can <em>ssh</em> into the pi and run the following commands to discover the i2c bus:

```ls /dev/i2c-*``` to get all available buses and then ```i2cdetect -y <bus number>```

When executing ```i2cdetect -y 1``` you should see the address ```0x4A``` (4A as a decimal = 74). This is the default bno085 sensor address on
the raspberry pi.

## Future Modifications
### Additional Sensor Output Configurations
Currently, the driver is set up to retrieve the following available (5) outputs: Gravity, Acceleration, Gyro, Magnetic Field, and Rotation. The BNO085 has nearly 40 additional outputs that could be configured if desired. 



