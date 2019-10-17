### SensorThings API Add-on for OSH

This module deploys a service implementing OGC SensorThings API v1.0.


#### Selecting Procedures

This service can be configured to expose any sensors/procedures connected to the hub. This is done by listing their UID in the configuration:

```
...
  "exposedProcedures": [
    "urn:example1:uid-sensor1",
    "urn:example1:uid-sensor2",
    "urn:example1:uid-group1",
    "urn:example2:*"
  ]
...
```


#### SensorThings Entity Database

The service commes with a built-in embedded database based on H2 MVStore, but is defined as an interface so that more DB connectors can be implemented.
the service can also be configured to use an external database to handle **Sensor**, **Datastream**, **Observation** and **FeatureOfInterest** entities.
