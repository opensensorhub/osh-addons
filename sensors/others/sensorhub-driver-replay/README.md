# Replay Sensor Module

The Replay Sensor Module allows for playback of archived data as if it was real time data.  There is no synchronization,
only configure the module to a specific data store and system and the system will be cloned and allow for publication of 
data as if it was coming from a live sensor having its own data stream id and unique identifier.

If replay includes video, initial playback is not immediate as it takes time in order to compute average frame rate and 
process batching of frames.

---

## Configuration

The following describes the configuration parameters for this module.

1. Under the database tab create a database (H2 or System Driver)
2. Configure the selected database
3. Under the sensors tab, select `Sensor Replay` module 
4. Select the Database module that you configured from the list of databases
5. Select the System UID that you want to replay
6. Start the module and verify the data is coming in

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
- **Batch Sizing**: For playback, tuning this output will enable smoother performance, however the higher the value the more memory usage
- **Auto Start**: If checked will immediately start this module when OpenSensorHub is launched

