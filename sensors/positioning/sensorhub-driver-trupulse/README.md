### LaserTechnology TruPulse Laser RangeFinder
Sensor adaptor classes supporting the TruPulse Laser RangeFinder output through Bluetooth interface. Provides triggered output of RangeFinder including distance to Line-of-Sight target, inclination, and azimuth relative to magnetic North. Processing also supports calculation of geoposition (lat-long-alt) of target based on instrument position provided by an Android device, for instance.

### User Manual
https://lasertech.com/wp-content/uploads/TruPulse-360i-User-Manual-2024.pdf

### Communication Docs
https://lasertech.com/themencode-pdf-viewer/?file=https://lasertech.com/wp-content/uploads/TruPulse-i-Series-Communication-Protocols-and-Commands.pdf#zoom=auto&pagemode=none

# Connection Steps:
- Pair the LRF to the Android Device
- On the Android device, open the Bluetooth settings
- Find TP360* or TP200* on the list of available devices.
- Select the LRF to pair. Use “1111” as the connection code if prompted.
# Configure OSH-Android App:
- Open the OSH-Android App
- Tap the three dots (menu) and select “Settings”
- Go to “General” settings to set up the endpoint for the LRF data.
- You will need the IP Address, Port number and login credentials (if required).
- Return to the main settings and select “Sensors”
- Enable "Network Location Data”
- Enable “Trupulse Range Finder”, this will unlock additional configuration options.
- Tap the “Trupulse Bluetooth Device Name” and enter the exact name of the LRF as it appears in your Android’s Bluetooth settings.
- Choose the “Trupulse Data Source” (physical or simulated) and choose “Streaming Physical Device”
- Ensure “Push Remote” is checked to send data to a remote node.
# Start Smarthub:
- Return to the main app screen.
- Tap the three dots (menu) and select “Start Smarthub”
# Verify Data at Endpoint:
- Go to the nodes endpoint where it is receiving the Trupulse LRF data
- Check the database for “urn:lasertech:trupulse:*” in the list of System UIDs
