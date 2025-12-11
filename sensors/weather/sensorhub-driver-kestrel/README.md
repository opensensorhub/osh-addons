# Kestrel Weather Meter Driver
This driver supports the Kestrel Weather Meters through Bluetooth Low Energy (BLE) interface. It provides continuous environmental data output including wind speed, temperature, humidity, barometric pressure, and various derived measurements.

## Supported Devices
This driver was developed and tested with the following Kestrel models:
- [Kestrel 5000 Environmental Meter](https://kestrelmeters.com/kestrel-5000-environmental-meter),
- [Kestrel 5500 Weather Meter with Compass](https://kestrelmeters.com/kestrel-5500-weather-meter)
- [Kestrel 5700 Ballistics Weather Meter](https://kestrelmeters.com/products/kestrel-5700-ballistics-weather-meter-with-link).

**Note**: While the Kestrel 5700 includes advanced ballistic features, this driver currently supports only its weather and environmental sensing capabilities.


## Output Data
The driver provides the following environmental measurements:

### Sensor Measurements
- **Wind Speed (m/s)** - Current wind velocity
- **Temperature (°C)** - Ambient dry bulb temperature
- **Globe Temperature (°C)** - Black globe temperature for heat stress
- **Humidity (%)** - Relative humidity
- **Station Pressure (hPa)** - Absolute atmospheric pressure
- **Magnetic Direction (degrees)** - Wind direction relative to magnetic north
- **Air Speed (m/s)** - Total air movement velocity

### Derived Measurements
- **True Direction (degrees)** - Wind direction relative to true north
- **Air Density (kg/m³)** - Current air density
- **Altitude (m)** - Elevation above sea level
- **Barometric Pressure (hPa)** - Station pressure adjusted to sea level
- **Crosswind (m/s)** - Wind component perpendicular to heading
- **Headwind (m/s)** - Wind component parallel to heading
- **Density Altitude (m)** - Altitude adjusted for air density
- **Relative Air Density (%)** - Air density as percentage of standard
- **Dew Point (°C)** - Temperature at which condensation occurs
- **Heat Stress Index (°C)** - Combined effect of temperature and humidity
- **Wet Bulb Temperature (°C)** - Temperature accounting for evaporative cooling
- **Wind Chill (°C)** - Apparent temperature due to wind

## Getting Started
To use this driver, the following configuration is required:

- **Kestrel Device Address:** The Bluetooth Address of the Kestrel Device
- **Bluetooth LE Network ID**: The local identifier of the BLE Network module in OpenSensorHub.
- **Serial Number**: The serial number printed on the Kestrel Device.

### Connection Steps
1. Set up the Kestrel Device
- Power on the Kestrel weather meter
- Ensure Bluetooth is enabled on the device for PC/Mobile
2. Configure OpenSensorHub Node on the Android Device
- Open the OSH Android App
- Tap the three dots (menu) and select "Settings"
- Navigate to the "Sensors" section and enable the "Kestrel Weather Meter"
- Select the Kestrel device from the list of scanned BLE devices
- Return to the settings and navigate "General" settings
- Configure the endpoint for the Kestrel Data 
  - IP Address
  - Port
  - Resource path
  - Username
  - Password
  - Enable the services (Connected Systems/ SOS) and client (SOS-T or Connected Systems Client) to push the Kestrel data to remote node
3. Start SmartHub
- Return to the main app screen
- Tap the three dots (menu) and select "Start SmartHub"
- The app will begin pushing data to the configured node, and you can see the statuses of the outputs displayed on the main screen.
4. Verify Data at endpoint
- Go to the node's endpoint where it is receiving the Kestrel Data
- Check the database for 'urn:osh:sensor:kestrel:*' in the list of system UIDs