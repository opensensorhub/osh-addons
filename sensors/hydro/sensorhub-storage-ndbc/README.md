### NDBC Buoy Data Archive

This module implements the storage API and connects to NDBC web archive for buoy data. It retrieves observations on-demand using NDBC web service.

Site information is retrieved using the **NDBC Site Web Service** documented [here](http://sdf.ndbc.noaa.gov/sos/).

The module is now operational, supporting air temperature, water temperature, conductivity, and GPS position.  Data Structures have been updated to use the OOT Harmonized models.  

TODO: Add support for remaining properties.
