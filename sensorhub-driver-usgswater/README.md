### USGS Water Data Live

This module implements the storage API and connects to USGS web archive for water data. It retrieves observations on-demand using USGS web service.

Site information is retrieved using the **USGS Site Web Service** documented [here](https://waterservices.usgs.gov/rest/Site-Service.html).

Observations themselves are requested using the **USGS Instantaneous Values Web Service** documented [here](https://waterservices.usgs.gov/rest/IV-Service.html)


#### Design choices

Information for all selected site is retrieved upfront (i.e. on module startup) and kept in memory. One feature of interest is created for each site, and sampled features are created for each selected state and county. Sampled features can also be used for filtering through the storage API.

Observations are retrieved on-demand when a read request is received by the storage interface. It it necessary to retrieve them by batch so they can be properly sorted by time (the USGS server groups observations by site).


#### Filters

The module configuration allows restricting the extent of data exposed by the storage instance. This is done by configuring the "expose filter" with the following criteria:

  * One of the following major filters:
    * One or more site IDs
    * One or more US states
    * One or more US counties
    * A geographic BBOX
  * One or more site types
  * One or more osbserved parameters
  * Time range


The "expose filter" defines the overall extent of the data made available by the module but, obsviously, it is possible to further filter observations when retrieving them from the storage. The filtering criteria are then dictated by 


#### Future Work

  * Request multiple batches in parallel (in multiple threads) to reduce latency  
  * Implement cached storage wrapper allowing use of one of OSH embedded storage options to store locally cached data.
  
