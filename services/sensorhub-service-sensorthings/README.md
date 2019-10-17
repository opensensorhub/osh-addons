### SensorThings API Add-on for OSH

This module deploys a servlet implementing OGC SensorThings API v1.0.

It currently complies with the following conformance classes of the standard:

- A.1 SensorThings Read (Core)
- A.3 SensorThings API Create-Update-Delete Extension
- A.5 SensorThings API MultipleDatastream
- A.6 SensorThings API Data Array Extension

And we are currently adding support for the following conformance class:

- A.2 SensorThings API Filtering Extension

A separate module will implement the MQTT extensions with a goal of complying with:

- A.7 SensorThings API Observation Creation via MQTT Extension Tests
- A.8 SensorThings API Receiving Updates via MQTT Extension Tests


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

The service comes with a built-in embedded database based on H2 MVStore, but the database is also defined as an interface so that more DB connectors can be implemented. The database is configured via the `dbConfig` configuration section:

```
...
  "dbConfig": {
	  "objClass": "org.sensorhub.impl.service.sta.STADatabaseConfig",
	  "moduleClass": "org.sensorhub.impl.service.sta.MVSTADatabase",
	  "databaseID": 3,
    "storagePath": "sta_db.dat",
	  "memoryCacheSize": 1024,
	  "autoCommitBufferSize": 1024,
	  "useCompression": false
	}
...
```

The service can also be configured to use an external "observation" database to handle *Sensor*, *Datastream*, *Observation* and *FeatureOfInterest* entities.

When using an external observation database, this database can be shared with other OSH modules such as the SOS Service.


#### OSH as a gateway between SensorThings API and Sensor Observation Service


