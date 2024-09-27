# Sony SNC VB600 Driver

## Overview

This module provides a sensor driver that will connect to
and stream video and audio data from a FLIR DH-390 camera.

## Configuration

When added to an OpenSensorHub node, the driver has the following configuration properties:

- **General:**
    - **Module ID:** *Not editable.*
      UUID automatically assigned by OpenSensorHub for this driver instance.
    - **Module Class:** *Not editable.*
      The fully qualified name of the Java class implementing the driver.
    - **Module Name:**
      A name for the instance of the driver.
      Should be set to something short and human-readable that describes the upstream source of data.
    - **SensorML URL:** (Optional)
      URL to a SensorML description document for the driver or physical device the driver represents.
    - **Serial Number:**
      A string that uniquely identifies this sensor in this OpenSensorHub node.
      This is used to differentiate between multiple sensors of the same type.
    - **Auto Start:**
      If checked, automatically start this sensor when the OpenSensorHub node is launched.

- **Connection:**
    - **IP Address:**
      The IP address of the camera.
    - **Username:**
      The username to use when connecting to the camera.
    - **Password:**
      The password to use when connecting to the camera.

- **Position:**
    - **Location:** (Optional)
      The latitude, longitude, and altitude of the sensor.
    - **Orientation:** (Optional)
      The heading, pitch, and roll of the sensor.