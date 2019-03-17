### Geospatially Aware Video Camera with Raspberry Pi

OSH distribution with preloaded drivers for creating a geospatially aware camera using a Raspberry Pi and and off-the-shelf components. This includes sensor drivers for NMEA GPS, BNO055 absolute orientation sensor and Video4Linux camera.


#### Build

To build this distribution, just run `./gradlew build`.

The resulting Zip file is in the `build/distributions` folder. You can transfer it directly to your RPi, unzip and run with Java version >= 7.


#### Startup

To start using OSH with the default configuration, just run the command:

    ./launch.sh

If you want to run it on a server through SSH and keep the process running when you log-out, use

    nohup ./launch &


#### Web Admin User Interface

After launching OSH, you can connect to the admin UI at:
<http://localhost:8181/sensorhub/admin>

or view the SOS server capabilities at:
<http://localhost:8181/sensorhub/sos?service=SOS&version=2.0&request=GetCapabilities>

