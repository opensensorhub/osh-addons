# UAS

Sensor adapter for MISB STANAG 4609 Compliant Unmanned Air Systems.

## Configuration

Configuring the sensor requires:
Select ```Sensors``` from the left hand accordion control and right click for context sensitive menu in accordion control
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
  - **Airframe Attitude:** If enabled, provides heading, pitch, and roll of the airframe
  - **Airframe Position:** If enabled, provides lat, lon, alt, heading, pitch, and roll of the airframe
  - **Airframe Location:** If enabled, provides lat, lon, alt of the airframe
  - **All Combined:** If enabled, provides all data received through MPEG-TS STANAG 4609 ST 0601.16 UAS Datalink Local Set MetaData
  - **Geo Referenced Image Frame Data:** If enabled, provides data corresponding to the quad lat, lon coordinates of the image being transmitted
  - **Gimbal Attitude:** If enabled, provides yaw, pitch, and roll of the sensor gimbal
  - **Identification:** If enabled, provides airframe identification information 
  - **Security:** If enabled, provides security classification markings for data
  - **Video:** If enabled, provides video feed from the camera sensor 

Storage:
Select ```Storage``` from the left hand accordion control and right click for context sensitive menu in accordion control

Use a ```Real-Time Stream Storage Module``` providing the sensor module as the 
- **Data Source ID:** Select the identifier for the storage module create in configuring sensor step,
use looking glass to select it from list of known sensor modules 
- **Auto Start:** Check the box to start this module when OSH node is launched
- **Process Events:** Check the box if you want events to be stored as new records,
if converting from data file, uncheck after first ingestion (When providing a data file
for the transport stream path, OSH will read the TS and convert it to OSH records which
can then be played back anytime, no longer needing to read from the TS file).
                 
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

The following are a list of example requests.  
The **IP ADDRESS** and **PORT** will need to be specified and point to the instance
of the OpenSensorHub node serving the data.

### Airframe Attitude Request
- **HTTP**
  - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:socom:sensor:uas:uas001-sos&observedProperty=http://sensorml.com/ont/swe/property/AirframeAttitude&temporalFilter=phenomenonTime,2019-03-27T14:39:31.535Z/2019-03-27T15:04:04.163Z&responseFormat=application/json
  
### Airframe Position Request
- **HTTP**
  - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:socom:sensor:uas:uas001-sos&observedProperty=http://sensorml.com/ont/swe/property/AirframePosition&temporalFilter=phenomenonTime,2019-03-27T14:39:31.535Z/2019-03-27T15:04:04.163Z&responseFormat=application/json
  
### Airframe Location Request
- **HTTP**
  - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:socom:sensor:uas:uas001-sos&observedProperty=http://sensorml.com/ont/swe/property/AirframeLocation&temporalFilter=phenomenonTime,2019-03-27T14:39:31.535Z/2019-03-27T15:04:04.163Z&responseFormat=application/json
  
### Identification Request
- **HTTP**
  - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:socom:sensor:uas:uas001-sos&observedProperty=http://sensorml.com/ont/swe/property/Identification&temporalFilter=phenomenonTime,2019-03-27T14:39:31.535Z/2019-03-27T15:04:04.163Z&responseFormat=application/json 
  
### Geo Referenced Image Frame Request
- **HTTP**
  - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:socom:sensor:uas:uas001-sos&observedProperty=http://sensorml.com/ont/swe/property/GeoRefImageFrame&temporalFilter=phenomenonTime,2019-03-27T14:39:31.535Z/2019-03-27T15:04:04.163Z&responseFormat=application/json   
  
### Security Request
- **HTTP**
  - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:socom:sensor:uas:uas001-sos&observedProperty=http://sensorml.com/ont/swe/property/Security&temporalFilter=phenomenonTime,2019-03-27T14:39:31.535Z/2019-03-27T15:04:04.163Z&responseFormat=application/json
  
### Video Request
- **HTTP**
  - http://[IP ADDRESS]:[PORT]/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:socom:sensor:uas:uas001-sos&observedProperty=http://sensorml.com/ont/swe/property/VideoFrame&temporalFilter=phenomenonTime,2019-03-27T14:39:31.535Z/2019-03-27T15:04:04.163Z