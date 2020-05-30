### OSH Base Distribution

This distribution includes OSH core and a few other basic modules. It can be run out-of-the-box with the default configuration provided in config.json.

This configuration includes two simulated sensors: GPS and weather. It also includes corresponding pre-configured storage isntances and an SOS instance providing both live and historical data from these two sensors.

WARNING: The simulated GPS sensor needs a working Internet connection and a Google API key to function properly since it uses Google Direction API to generate a realistic trajectory.


#### Build

To build this distribution, just run `./gradlew build`.

The resulting Zip file is in the `build/distributions` folder. Just unzip it and run with Java version >= 7.


#### Startup

To start using OSH with the default configuration, just run the command:

    ./launch.sh

If you want to run it from an SSH session and keep the process running when you log-out, use

    nohup ./launch &


#### Web Admin User Interface

After launching OSH, you can connect to the admin UI at:
<http://localhost:8181/sensorhub/admin>

You can also view the SOS server capabilities at:
<http://localhost:8181/sensorhub/sos?service=SOS&version=2.0&request=GetCapabilities>


#### Test Web Clients

You can also try the included javascript clients connecting to the SOS server.
These are very simple web pages intended to provide examples of how to efficiently render the data
streamed by the SOS server.

<http://localhost:8181/osm_client_websockets.html>
