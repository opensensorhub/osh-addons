# MISB UAS

Sensor adapter for MISB-0601 / STANAG-4609 Compliant Unmanned Air Systems.

## Configuration

This add-on supports the following configuration properties:

- **General:**
  - **Module ID:** Automatically assigned by OpenSensorHub
  - **Module Class:** Automatically specified as the class implementing the driver
  - **Module Name:** A name for the instance of the driver
  - **SensorML URL:** URL to SensorML description document for the driver or physical device the driver represents
  - **UAS ID:** The platforms serial number, or a unique identifier
  - **Auto Start:** Check the box to start this module when OSH node is launched
  
- **Connection:**
  - **URL:** A string containing a path to a file if interested in reading from a ```Transport Stream``` data file.
  - **Server Ip:** IP Address of server providing ```Transport Stream``` data 
  - **Port:** Used in conjunction with the _Server Ip_ to connect to ```Transport Stream``` data
  
- **Outputs:**
  - **Airframe Position:** If enabled, provides lat, lon, alt, heading, pitch, and roll of the airframe
  - **All Combined:** If enabled, provides all data received through MPEG-TS STANAG 4609 ST 0601.16 UAS Datalink Local Set MetaData
  - **Geo Referenced Image Frame Data:** If enabled, provides data corresponding to the quad lat, lon coordinates of the image being transmitted
  - **Gimbal Attitude:** If enabled, provides yaw, pitch, and roll of the sensor gimbal
  - **Identification:** If enabled, provides airframe identification information 
  - **Security:** If enabled, provides security classification markings for data
  - **Video:** If enabled, provides video feed from the camera sensor 