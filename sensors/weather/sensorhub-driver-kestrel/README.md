# Kestrel Weather
---

## Configuration

The following describes the configuration parameters for this module.

### General

- **Module Class** (*Unmodifiable*): The class implementing the interface and connection to the target sensor whose
  configuration is being edited
- **Module Name**: The name assigned to the sensor module by the user
- **Module ID** (*Unmodifiable*): The universally unique identifier of this module instance within OpenSensorHub
- **Description**: The description of the sensor module assigned by the user
- **Last Updated** (*Unmodifiable*): The date on which the configuration was last updated
- **Database Module Id**: The module id of the data store module to use as the archived data store to read from
- **System Unique Id**: The unique id of the system for which archive data is to be transferred
- **Start Time (ISO UTC)** (*Optional*): The timestamp of the earliest observations from the archived data set to transmit, starting transmission of observations\nSee the corresponding data store for when data is available
- **End Time (ISO UTC)** (*Optional*): The timestamp of the latest observations from the archived data set to transmit, ending transmission of observations\nSee the corresponding data store for when data is available
- **Auto Start**: If checked will immediately start this module when OpenSensorHub is launched

