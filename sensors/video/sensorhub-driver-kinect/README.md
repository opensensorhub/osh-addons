### OpenSensorHub Kinect Driver

This driver is for the operation of the Microsoft Kinect model 1414 device.  It incorporates https://openkinect.org/ and Freenect java wrapper solutions for connecting with the device through JNA (Java Native Access) APIs.

The Kinect device supports three sensors: rgb video, IR, and depth (point cloud).  The driver allows the operation of the device in one mode per instance/device.  Though the device supports various encodings and these are exposed through the OpenSensorHub admin configuration panel for the device only certain encodings have been implemented to date. Specifically:
  - IR -  IR_8BIT
  - Video - RGB
  - Depth - D11BIT

Other supported properties:
- Frame Width - denotes the window width of the frame supported by kinect
- Frame Height - denotes the window height of the frame supported by kinect
- Tilt Angle - denotes the tilt angle of the kinect, controlled by its internal motor, this field can be adjusted
- Point Cloud Decimation Factor - this setting scales the depth sensor point count, the higher the value the lesser the quality of point cloud data that will be received as a function of reducing the number of points reported.

#### Deploying the Kinect Device (Model 1414)

Setting up Kinect requires IDS 1 Pc Xbox 360 Kinect Sensor USB AV Adapter if using Model 1414 Kinect.

These are the steps to allow non-admin user accounts to connect to the device on Linux.  Consult your version of Linux for specifics on setting up rules, these rules were setup on Linux Mint deployment.

1. Create a new udev rule in /etc/udev/rules.d as administrator or using sudo, in our case we created 51-kinect.rules (use your favorite editor, we used vi).

`$ sudo vi /etc/udev/rules.d/51-kinect.rules`

2. Add the rules for accessing the Kinect device as follows:

```
# ATTR{product}=="Xbox NUI Motor"
SUBSYSTEM=="usb", ATTR{idVendor}=="045e", ATTR{idProduct}=="02b0", MODE="0666" GROUP="video"
# ATTR{product}=="Xbox NUI Audio"
SUBSYSTEM=="usb", ATTR{idVendor}=="045e", ATTR{idProduct}=="02ad", MODE="0666" GROUP="video"
# ATTR{product}=="Xbox NUI Camera"
SUBSYSTEM=="usb", ATTR{idVendor}=="045e", ATTR{idProduct}=="02ae", MODE="0666" GROUP="video"
# ATTR{product}=="Xbox NUI Motor"
SUBSYSTEM=="usb", ATTR{idVendor}=="045e", ATTR{idProduct}=="02c2", MODE="0666" GROUP="video"
# ATTR{product}=="Xbox NUI Motor"
SUBSYSTEM=="usb", ATTR{idVendor}=="045e", ATTR{idProduct}=="02be", MODE="0666" GROUP="video"
# ATTR{product}=="Xbox NUI Motor"
SUBSYSTEM=="usb", ATTR{idVendor}=="045e", ATTR{idProduct}=="02bf", MODE="0666" GROUP="video"
```

3. Add the user profile to the video group

`$ sudo adduser $USER video`


