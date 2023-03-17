### Nexrad Sensor Driver

**_Company_**: Botts Innovative Research, Inc.  
**_Developer_**: Tony Cook  
**_Sensor Vendor_**: NOAA  

OpenSensorHub driver for supporting Realtime (and Archived in future) Level II Nexrad Radar data.  Details about the Nexrad radar network can be found here: https://www.ncei.noaa.gov/products/radar/next-generation-weather-radar  

-----------------

## Overview

This driver requires aws_access_key and aws_secret_access_key for the user running the module to be present in user $HOME/.aws/credentials. See AWS documentation for more details: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html.

Sample credentials file:
```
[default]
aws_access_key_id=AKIAIOSFODNN7EXAMPLE
aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

On startup, the driver creates a new Amazon Queue with the name given in the configuration if it does not already exist. If it does exist, any existing messages are purged during driver startup. 
The queue is subscribed to the AWS Level II Nexrad data topic (arn:aws:sns:us-east-1:684042711724:NewNEXRADLevel2Object).
The driver can be configured to support one or more Nexrad sites.  

-----------------

## Configuration

- **General:** (*Settings common to all OpenSensorHub drivers on the "General" tab.*)
    - **Module ID:** *Not editable.* UUID automatically assigned by OpenSensorHub for this driver instance.
    - **Module Class:** *Not editable.* The fully qualified name of the Java class implementing the driver
    - **Module Name:** A name for the instance of the driver. Should be set to something short and human-readable that describes the upstream source of data, e.g. "Shout TS Trackers". This does not affect the operation of the driver, but is seen in user interfaces.
    - **Description:** Any descriptive text that an administrator may want to enter for the benefit of users (or themselves).
    - **SensorML URL:** URL to a SensorML description document for the driver or physical device the driver represents. Typically this is left blank, and OpenSensorHub will populate the SensorML description with sensible defaults.
    - **Auto Start:** If checked, this driver will be started when OpenSensorHub starts. This should typically be checked for this driver.
    - **Last Updated:** This should not be changed for this driver. (Though it could theoretically be used to indicate to clients that settings have changed, no known clients will use this value.)

- **Nexrad:** *(Settings specific to Nexrad OpenSensorHub driver)*
    - **siteIds:** ArrayList of 4 letter Nexrad site identifiers (e.g. KHTX)
    - **rootFolder:**  Folder for downloading bzipped files of radials from Nexrad Level II Amazon S3 bucket
    - **numThreads:**  
    - **queueName:**  name of Amazon SQS queue to create for holding messages of new files
    - **queueIdleTimeMinutes:**  
    - **queueFileLimit:**  
    - **archiveStartTime:** not implemented yet- start isoTime for pulling archive data  
    - **archiveStopTime:**  not implemented yet- stop isoTime for pulling archive data

---

## Outputs 
There is only a single output currently which includes all products. May consider separating to allow choosing individual products 

#### Nexrad Output
Output consists of packets of 100 radial "chunks" that, when combined sequentially, constitute complete 3d volumes of Nexrad moment data. Moment data includes reflectivity, velocity, and spectrum width. Dual-polarization product support is being added. This will add differential reflectivity, correlation coefficient, and differential phase shift.

* timestamp  - isoTime of the start of the first radial in the packet
* siteId - 4 letter Nexrad site identifier
* <siteLocationVector - not there currently, I need to add it>
* ElevationAngle - Vertical pointing angle of radar for this radial
* AzimuthAngle - Horizontal radial pointing angle of radar for this radial
* RangeToCenterOfFirstReflectivtyGate - Range to first reflectivity gate of this radial in meters
* ReflectivityGateSpacing - Size of individual reflectivity gate/bin along radial in meters
* NumberOfReflectivityGates - Number of Reflectivty gates along this radial 
* RangeToCenterOfFirstVelocityGate - Range to first velocity gate of this radial in meters
* VelocityGateSpacing - Size of individual velocity gate/bin along radial in meters
* NumberOfVelocityGates - Number of Reflectivty gates along this radial 
* RangeToCenterOfFirstSpectrumWidthGate - Range to first spectrumWidth gate of this radial in meters
* SpectrumWidthGateSpacing - Size of individual spectrumWidth gate/bin along radial in meters
* NumberOfSpecturmWidthGates - Number of SpectrumWidth gates along this radial
* ReflectivityArraySize - Number of Reflectivity values  
* ReflectivityArray - Reflectivity values 
* VelocityArraySize - Number of Velocity values  
* VelocityArray - Velocity values 
* SpectrumWidthArraySize - Number of SpectrumWidth values  
* SpectrumWidthArray - SpectrumWidth values 




