# Discovery Service

A simple discovery service used to identify data streams supporting specific properties
as given by ontological definition URIs mapped to a term.

## Configuration

This service supports the following configuration properties:

- **General:**
    - **Module ID:** Automatically assigned by OpenSensorHub
    - **Module Class:** Automatically specified as the class implementing the service
    - **Module Name:** A name for the instance of the service
    - **Description:** Allows for a brief description of the service
    - **Auto Start:** Check the box to start this module when OSH node launched
    - **Endpoint:** The endpoint at which the service can be reached, for example localhost:8282/sensorhub/**_discovery_**?supporting=location
    - **Rules:** Specifies a directory in which rules files are present, rules can be nested and organized in subdirectories
    
## Example Rules File

The rules file can be a simple text file containing a key word followed by a list of ontological definition URI strings 
joined logically by & (AND) and | (OR).  The logical operators can be symbolic or verbatim.

Rule files can contain comments, a comment line begins with the # symbol

    # This is a comment line, there can be unlimited comment lines but each must begin with #
    location http://www.opengis.net/def/property/OGC/0/SensorLocation | http://www.opengis.net/def/property/OGC/0/PlatformLocation & http://sensorml.com/ont/swe/property/GeodeticLatitude & http://sensorml.com/ont/swe/property/Longitude
    orientation http://www.opengis.net/def/property/OGC/0/PlatformOrientation
    sensor_orientation http://www.opengis.net/def/property/OGC/0/SensorOrientation
    position http://www.opengis.net/def/property/OGC/0/PlatformLocation AND http://www.opengis.net/def/property/OGC/0/PlatformOrientation

## Structure and Meaning of Rules

    location http://www.opengis.net/def/property/OGC/0/SensorLocation | http://www.opengis.net/def/property/OGC/0/PlatformLocation & http://sensorml.com/ont/swe/property/GeodeticLatitude & http://sensorml.com/ont/swe/property/Longitude

This rule will return a result set containing the system id, corresponding data stream ids, applicable time ranges, as 
well as information regarding the path to the fields for the given data structure each observation produced by the 
data stream will generate. The location is given by a definition for _SensorLocation_ or _PlatformLocation_ and is 
further restricted by location being given as _GeodeticLatitude_ and _Longitude_

The result set is given below:

      {
        "resultSet": [{
          "systemId": "41s2wfnr",
          "location": [{
              "dataStreamId": "8wl0jfwp",
              "phenomenonTime": [
                "2021-03-23T15:27:20.796Z",
                "2021-03-25T14:24:16.414Z"
              ],
              "paths": [{
                  "name": "location",
                  "definition": "http://www.opengis.net/def/property/OGC/0/SensorLocation",
                  "path": "location"
                },
                {
                  "name": "lat",
                  "definition": "http://sensorml.com/ont/swe/property/GeodeticLatitude",
                  "path": "location.lat"
                },
                {
                  "name": "lon",
                  "definition": "http://sensorml.com/ont/swe/property/Longitude",
                  "path": "location.lon"
                }]
            }]
        }]
      }

## Making Discovery Requests

Simply POST or GET a discovery query to the endpoint specifying discovery **supporting** a comma separated list of rules.
Two such example requests are:

    localhost:8282/sensorhub/discovery?supporting=location

    localhost:8282/sensorhub/discovery?supporting=location,orientation

    localhost:8282/sensorhub/discovery/discover/location

## Accessing the Online Rules Editor

If you have permissions you may access to view or edit the rules by going to

    localhost:8282/sensorhub/discovery/rules
