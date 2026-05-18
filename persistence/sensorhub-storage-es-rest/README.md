# Elasticsearch storage implementation

This is a storage module allowing one to store and retrieve data to/from an elasticsearch V6+ server. It uses the elasticsearch
 Java High Level REST Client [link](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high.html).

The difference with the repository sensorhub-storage-es is:
 
 - Java High Level REST Client, which executes HTTP requests rather than serialized Java requests
 - Convert data frame into ElasticSearch data component instead of obfuscating into a Java Serialized Object. (except for the OSH MetaData)
 
 The driver that create ElasticSearch index may not support your DataComponent, in this case create an issue with the specification of your unsupported DataComponent. (Missing fields)
 
Three main OSH interfaces have been implemented: 
1. IRecordStorageModule
2. IObsStorageModule
3. IMultiSourceStorage

## Main classes

An iterator wrapper class has been used to wrap scroll response without specify the scroll id every times. The ESIterator takes care 
about making new requests with the specify scrollID when it necessary. 

A bulk processor is in charge of sending create/update/delete requests. The BulkProcessor class offers a simple interface to flush bulk operations automatically based on the number or size of requests, or after a given period. 

Some settings are available through the ESBasicStorageConfig class:
- clusterName: ES cluster name
- user: ElasticSearch user for authentication (leave blank if not required) 
- password: ElasticSearch password for authentication
- autoRefresh: Refresh store on commit. Require indices:admin/refresh rights
- filterByStorageId: Multiple storage instance can use the same index. If the filtering is disabled this driver will see all sensors (should be used only for read-only SOS service)
- certificatesPath: List of additional SSL certificates for ElasticSearch connection
- nodeUrls: list of nodes under the format <host>:<port>
- indexNamePrepend: String to add in index name before the data name
- indexNameMetaData: Index name of the OpenSensorHub metadata
- scrollMaxDuration: When scrolling, the maximum duration ScrollableResults will be usable if no other results are fetched from, in ms
- scrollFetchSize: When scrolling, the number of results fetched by each Elasticsearch call
- connectTimeout: Determines the timeout in milliseconds until a connection is established. A timeout value of zero is interpreted as an infinite timeout.
- socketTimeout: Defines the socket timeout (SO_TIMEOUT) in milliseconds, which is the timeout for waiting for data or, put differently, a maximum period inactivity between two consecutive data packets). 
- maxRetryTimeout: Sets the maximum timeout (in milliseconds) to honour in case of multiple retries of the same request. 
- bulkConcurrentRequests: Set the number of concurrent requests
- bulkActions: execute the bulk every n requests
- bulkSize: flush the bulk every n mb
- bulkFlushInterval: flush the bulk every n seconds whatever the number of requests
- maxBulkRetry: Bulk insertion may fail, client will resend in case of TimeOut exception. Retry is disabled by default in order to avoid overflow of ElasticSearch cluster 

A special parser into this driver will create appropriate default Elastic Search index mapping for each OSH DataComponent.
You can override this mapping using Elastic Search tools. (ex. Kibana)

## Mappings

There are the different mappings depending on the storage used:

1. Open Sensor Hub specific metadata
```json
{
  "mapping": {
    "osh_metadata": {
      "properties": {
        "blob": {
          "type": "binary"
        },
        "index": {
          "type": "keyword"
        },
        "metadataType": {
          "type": "keyword"
        },
        "storageID": {
          "type": "keyword"
        },
        "timestamp": {
          "type": "date",
          "format": "epoch_millis"
        }
      }
    }
  }
```

2. Sensor Location
```json
{
  "mapping": {
    "sensorLocation": {
      "dynamic": "false",
      "properties": {
        "location": {
          "type": "geo_point"
        },
        "location_height": {
          "type": "double"
        },
        "producerID": {
          "type": "keyword"
        },
        "storageID": {
          "type": "keyword"
        },
        "timestamp": {
          "type": "date",
          "format": "epoch_millis"
        }
      }
    }
  }
}
```
