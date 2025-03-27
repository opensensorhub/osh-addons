# [NAME]

Sensor adapter for [NAME].

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left hand accordion control and right click for context sensitive menu in accordion control
- **Module Name:** A name for the instance of the driver
- **Serial Number:** The platforms serial number, or a unique identifier
- **Auto Start:** Check the box to start this module when OSH node is launched

Storage:
Select ```Storage``` from the left hand accordion control and right click for context sensitive menu in accordion control
Use a ```Real-Time Stream Storage Module``` providing the sensor module as the 
- **Data Source ID:** Select the identifier for the storage module create in configuring sensor step,
use looking glass to select it from list of know sensor modules 
- **Auto Start:** Check the box to start this module when OSH node is launched
- **Process Events:** Check the box if you want events to be stored as new records.
                 
And then configure the 
- **Storage Config** using a ```Perst Record Storage``` instance providing the 
  - **Storage Path** as the location where the OSH records are to be stored.

SOS Service:
Select ```Services``` from the left hand accordion control, then Offerings, then the **+**
symbol to add a new offering.
Provide the following:
- **Name:** A name for the offering
- **Description:** A description of the offering
- **StorageId:** Select the identifier for the storage module create in previous step,
 use looking glass to select it from list of know storage modules
- **SensorId:** Select the identifier for the storage module create in configuring sensor step,
                 use looking glass to select it from list of know sensor modules
- **Enable:** Check the box to enable this offering

## Sample Requests

The following are a list of example requests and their respective responses.  
The **IP ADDRESS** and **PORT** will need to be specified and point to the instance
of the OpenSensorHub node serving the data.

### [Observed Property] Request
- **HTTP**
   - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=[URN]&observedProperty=[OBSERVED_PROPERTY]&temporalFilter=phenomenonTime,[START_TIME]/[END_TIME]&replaySpeed=1&responseFormat=application/json

Response:
```

```