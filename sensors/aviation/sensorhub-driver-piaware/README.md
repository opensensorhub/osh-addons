# OSH for PiAware

OpenSensorHub driver for streaming realtime/archive SbsFormat data ingested on a Rasberry Pi that has been set up as a PiAware receiver.  For more information on building a PiAware receiver, see:

https://flightaware.com/adsb/piaware/


## Driver configuration options

The driver can be configured via a standard OSH json configuration file.  Configurable parameters and defaults include:

* PIAWARE_DEVICE_IP = "192.168.1.124";
* RAW_OUTBOUND_PORT = 30002;
* SBS_OUTBOUND_PORT = 30003;
* BEAST_OUTBOUND_PORT = 30005;
* DUMP1090_PATH = "/run/dump1090";
* AIRCRAFT_JSON_FILE = "aircraft.json";

These are all assigned default values matching the standard PiAware receiver software distribution.  With the exception of PIAWARE_DEVICE_IP, the defaults should work as-is.

## PiAware setup 

The driver can be installed as part of a standard OSH v1.4 distribution. It is recommended to use the included systemd service file to ensure continuous operation (see src/main/resources/osh-piaware.service).

## SkyAware web interface

The standard PiAware distribution includes a Web interface running on port 8080 for displaying current active flight paths and statistics.  That interface can be accessed at the local URL (IP should be changed to match your network configuration):

http://192.168.1.124:8080/data/aircraft.json

## Example SOS requests

IP and port should be changed to match the network values corresponding to your receiver

### FeaturesOfInterest- returns XML document containing hexIdent FOI URI for all available aircraft

http://192.168.1.124:8181/sensorhub/sos?service=SOS&version=2.0&request=GetFeatureOfInterest&procedure=urn:osh:sensor:aviation:PiAware

### GetResult- returns JSON representation of all latest airfraft records

http://192.168.1.124:8181/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:osh:piaware&observedProperty=http://sensorml.com/ont/swe/property/sbsOutput&temporalFilter=phenomenonTime,now&responseFormat=application/json

### GetResult (single flight) - returns live stream JSON representation of records for a single aircraft

http://192.168.1.124:8181/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:osh:piaware&observedProperty=http://sensorml.com/ont/swe/property//sbsOutput&featureOfInterest=urn:osh:aviation:flight:ABE42C&temporalFilter=phenomenonTime,now/3000-01-01&responseFormat=application/json	


