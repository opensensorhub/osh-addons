### NDBC Buoy Data Driver- Last Updated 2026-03-25

**_Company_**: Botts Innovative Research, Inc.  
**_Developer_**: Tony Cook  
**_Sensor Vendor_**:   

OpenSensorhub driver for supporting data from the National Data Buoy Center network of Oceanicbuoys. It reads near-realtime data from CSV files on the NDBC Observations website located here:  
https://www.ndbc.noaa.gov/data/latest_obs/latest_obs.txt

Documentation on Observation measurements, units, and methods of acquisition can be found here:
https://www.ndbc.noaa.gov/faq/measdes.shtml 

The above links and method of access are subject to change.

## Configuration

- **General:** (*Settings common to all OpenSensorHub drivers on the "General" tab.*)
    - **Module ID:** *Not editable.* UUID automatically assigned by OpenSensorHub for this driver instance.
    - **Module Class:** *Not editable.* The fully qualified name of the Java class implementing the drive
    - **Module Name:** A name for the instance of the driver. Should be set to something short and human-readable that describes the upstream source of data, e.g. "Shout TS Trackers". This does not affect the operation of the driver, but is seen in user interfaces.
    - **Description:** Any descriptive text that an administrator may want to enter for the benefit of users (or themselves).
    - **SensorML URL:** URL to a SensorML description document for the driver or physical device the driver represents. Typically this is left blank, and OpenSensorHub will populate the SensorML description with sensible defaults.
    - **Auto Start:** If checked, this driver will be started when OpenSensorHub starts. This should typically be checked for this driver.
    - **Last Updated:** This should not be changed for this driver. (Though it could theoretically be used to indicate to clients that settings have changed, no known clients will use this value.)

- **BuoyConfig:** *(Settings specific to NDBC OpenSensorHub driver)*
    - **realtimeUrl:** Serial Number/MAC ID for this BeastkitURL of realtime CSV data
    - **pollingPeriod:** Polling Period in ms

---

## Outputs 

#### BuoyOutput
* timestamp - Julian1970 timestamp of the measurement
* id - NDBC buoy ID
* latitude - latitude of buoy location (decimal degrees) 
* longitude - longitude of buoy location in (decimal degrees) 
* altitude? - should this be there? 
* windSpeed - average wind speed  (m/s)
* windDir - wind direction (degrees)
* windGust - peak wind gust (m/s)
* significantWaveHeight - (meters)
* dominantWavePeriod - (seconds)
* averageWavePeriod - (seconds)
* waveDirection - (degrees)
* seaLevelPressure - (hPa)
* pressureTendency - (hPa/?)
* airTemperature - (degrees C)
* waterTemperature - (degrees C)
* dewPoint - (degrees C) 
* visibility - (miles)
* tideWaterLevel - (feet relative to Mean Lower Level Water)

